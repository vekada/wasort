/* $Id$ */
/**
 * Title:       SortTransformModel.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.job.transforms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sas.etl.models.IModel;
import com.sas.etl.models.IModelListener;
import com.sas.etl.models.NotifyEvent;
import com.sas.etl.models.ServerException;
import com.sas.etl.models.data.BadLibraryDefinitionException;
import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.data.IDataObject;
import com.sas.etl.models.data.IPhysicalTable;
import com.sas.etl.models.data.ITable;
import com.sas.etl.models.impl.ModelEvent;
import com.sas.etl.models.impl.ModelLogger;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.job.ICodeSegment;
import com.sas.etl.models.job.ISortingTransform;
import com.sas.etl.models.job.ITransformTableOptions;
import com.sas.etl.models.job.impl.AbstractDataTransform;
import com.sas.etl.models.job.impl.CodegenException;
import com.sas.etl.models.job.transforms.common.ISortColumn;
import com.sas.etl.models.job.transforms.common.ISorting;
import com.sas.etl.models.other.BadServerDefinitionException;
import com.sas.etl.models.prompts.IPromptModel;
import com.sas.etl.models.prompts.IPromptValueChangeListener;
import com.sas.etl.models.prompts.impl.BaseDataTransformModelListener;
import com.sas.etl.models.prompts.impl.BaseDataTransformPromptModel;
import com.sas.etl.models.prompts.impl.BaseDataTransformValueChangedListener;
import com.sas.etl.models.prompts.impl.PromptDataProvider;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.OrderByClause;
import com.sas.metadata.remote.Select;
import com.sas.prompts.PromptValueChangeEventInterface;
import com.sas.prompts.definitions.PromptDefinitionInterface;
import com.sas.prompts.groups.PromptGroupInterface;
import com.sas.services.ServiceException;
import com.sas.storage.exception.ServerConnectionException;

/**
 * SortTransformModel is the model for a sort transform.
 */
public class SortTransformModel extends AbstractDataTransform implements ISortingTransform
{
   private static final String TRANSFORMATION_CLASS = "com.sas.wadmin.visuals.SASSort";
   private static final String TRANSFORMATION_ROLE  = "com.sas.wadmin.visuals.SASSort";
   private static final String ARM_DISPLAY_NAME = "Sort"; /*I18NOK:EMS**/
   
   /** Allows duplicates */
   public static final int ALLOW_DUPLICATES     = 0;
   /** Removes duplicates where duplicates are defined by the columns used in the sort.
    *  This value is equivalent to "NODUPKEY" in a SAS proc sort */
   public static final int NO_DUPLICATE_KEYS    = 1;
   /** Removes duplicates where duplicates are defined by all columns in the record.
    *  This value is equivalent to "NODUPRECS" in a SAS proc sort */
   public static final int NO_DUPLICATE_RECORDS = 2;

   //---------------------------------------------------------------------------
   // Persistence
   //---------------------------------------------------------------------------

   // constants
   private static final String SORT_OPTIONS   = "SORT";

   private static final String STABLE_OPTION  = "Equals";                        // I18NOK:EMS
   private static final String STABLE_TRUE    = "Default (EQUALS)";              // I18NOK:EMS
   private static final String STABLE_FALSE   = "NOEQUALS";
   
   private static final String REPLACE_DATASET_OPTION = "Force";                 // I18NOK:EMS
   private static final String REPLACE_DATASET_TRUE   = "FORCE";
   private static final String REPLACE_DATASET_FALSE  = "Default (no FORCE)";    // I18NOK:EMS

   private static final String SORT_TAGS_OPTION = "Tagsort";                     // I18NOK:EMS
   private static final String SORT_TAGS_TRUE   = "TAGSORT";
   private static final String SORT_TAGS_FALSE  = "Default (no TAGSORT)";        // I18NOK:EMS
   
   private static final String OVERWRITE_OPTION = "Overwrite";                   // I18NOK:EMS
   private static final String OVERWRITE_TRUE   = "OVERWRITE";                   // I18NOK:EMS
   private static final String OVERWRITE_FALSE  = "Default (no overwrite)";      // I18NOK:EMS
   
   private static final String DUPLICATES_OPTION               = "Duplicates";   // I18NOK:EMS
   private static final String DUPLICATES_NO_DUPLICATE_KEYS    = "NODUPKEY";
   private static final String DUPLICATES_NO_DUPLICATE_RECORDS = "NODUPRECS";
   private static final String DUPLICATES_ALLOW_DUPLICATES     = "";
   
   private static final String SORT_SEQUENCE_OPTION            = "sortseq";
   private static final String SORT_SIZE_OPTION                = "sortsize";
   private static final String PROC_SORT_OPTIONS               = "ProcSortOptions";
   
   // event types
   /** event type for whether a stable sort is used has changed */
   public static final String STABLE_CHANGED                    = "SortTransform:StableChanged";
   /** event type for whether a tag sort is used has changed */
   public static final String SORT_TAGS_CHANGED                 = "SortTransform:SortTagsChanged";
   /** event type for replace dataset has change */
   public static final String REPLACE_DATASET_CHANGED           = "SortTransform:ReplaceDataSetChanged";
   /** event type for memory size changed */
   public static final String MEMORY_SIZE_CHANGED               = "SortTransform:MemorySizeChanged";
   /** event type for collating sequence changed */
   public static final String COLLATING_SEQUENCE_CHANGED        = "SortTransform:CollatingSequenceChanged";
   /** event type for duplicate record handling changed */
   public static final String DUPLICATE_RECORD_HANDLING_CHANGED = "SortTransform:DuplicateRecordHandlingChanged";
   /** event type for proc sort options changed */
   public static final String PROC_SORT_OPTIONS_CHANGED         = "SortTransform:ProcSortOptionsChanged";
   /** event type for overwrite option changed */
   public static final String OVERWRITE_OPTIONS_CHANGED         = "SortTransform:OverwriteChanged";
   /** event type for sort columns changed */
   public static final String SORT_COLUMNS_CHANGED              = "SortTransform:ColumnsChanged";

   /**    */
   public static final String SORT_ORDER_CHANGED                = "SortTransform:SortOrderChanged";
   
   private boolean  m_bStable;
   private boolean  m_bSortTags;
   private boolean  m_bReplaceDataSet;
   private String   m_sCollatingSequenceName;
   private String   m_sMemorySize;
   private String   m_sProcSortOptions;
   private int      m_eDuplicateRecordHandling;
   private ISorting m_order;
   private boolean  m_bOverwrite;
   
   /**
    * Constructs a sort transform model.
    * 
    * @param sID   the id of the sort transform
    * @param model the model
    */
   public SortTransformModel( String sID, IModel model )
   {
      super( sID, model );
      m_bStable                  = true;
      m_bSortTags                = false;
      m_bReplaceDataSet          = false;
      m_sCollatingSequenceName   = "";
      m_sMemorySize              = "";
      m_sProcSortOptions         = "";
      m_eDuplicateRecordHandling = ALLOW_DUPLICATES;
      m_bOverwrite                = false;
      
      // only want to create a new order is it's a brand new object
      m_order = getModel().getObjectFactory().createNewSorting( getID() );
      m_order.addNotifyListener( this );
   }

   /**
    * Gets the OMR load template map for the transform.
    * 
    * @return the OMR load template map
    * 
    * @see com.sas.etl.models.IOMRPersistable#getOMRLoadTemplateMap()
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssoc = (List)map.get( getClassifierMapType() );
      if (lAssoc==null)
         lAssoc = new ArrayList();
      
      lAssoc.add( Select.ASSOCIATION_ORDERBYFORSELECT_NAME );
      
      map.put( getClassifierMapType(), lAssoc );
      return map;
   }
   
   /**
    * Get the transform type ID
    * 
    * @return String "Loader:HIDE" for the transform type ID
    */
   public static String getTransformTypeID()
   {
      return TRANSFORMATION_CLASS;
   }

   protected String getTransformRole()
   {
      return TRANSFORMATION_ROLE;
   }
   
   protected String getTransformClass()
   {
      return TRANSFORMATION_CLASS; 
   }
   
   /**
    * Gets the object's default name.
    * 
    * @return the name of the object
    */
   protected String getDefaultName()
   {
      return RB.getStringResource( "SortTransformModel.Name.txt" );
   }

   /**
    * Get the display type of the transform.  This will be displayed as the
    * node type in quick properties for the transform node and should be a
    * localized string.
    * @return the display type for the transform
    */
   public String getDisplayType()
   {
      return RB.getStringResource( "SortTransformModel.DisplayType.txt" );
   }
   
    /**
    * Get the absolute name of the transform that will be used for the ARM records.  The ARM value can only 
    * be a SAS Literal, therefore no special characters are supported.
    * @return absolute transform name
    */
   public String getAbsoluteName()
   {	   
	   return ARM_DISPLAY_NAME;
   }

   /**
    * Adds the default setting for the transform
    * 
    */
   public void addDefaultSettings()
   {
      addNewWorkTable();   
   }
   
   /**
    * Removes a data target from the transform.  A protected implementation to not
    * remove mappings.  This protected implementation is used by undo.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   protected void preRemoveDataTarget( IDataObject target )
   {
      super.preRemoveDataTarget( target );
      //Need to remove all of the sort columns
      ISortColumn[] aSortCols = m_order.getSortColumns();
      for (int i=0; i<aSortCols.length; i++)
      {
      	ITable table  = aSortCols[i].getColumn().getTable();
      	if (table == target)
      		m_order.removeSortColumn( aSortCols[i] ); 
      }
   }
   
   /**
    * Replaces target table and replaces any by columns that have
    * the same name
    * @param oldTable 
    * @param newTable 
    * @param columnsMap 
    * @param portIndexes 
    */
   public void replaceTargetTable( ITable oldTable, ITable newTable, Map columnsMap, Integer[] portIndexes )
   {
      startCompoundUndoable();
      try
      {         
         IColumn[] aNewColumns = newTable.getColumns();
         m_order.replaceSortColumns(aNewColumns, newTable.getTableName());
         super.replaceTargetTable(oldTable, newTable, columnsMap);
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Are expressions allowed by this transformation?  Sort does not allow 
    * expressions.
    * 
    * @return false = expressions are not allowed
    */
   protected boolean areExpressionsAllowed()
   {
      return false;
   }

   /**
    * Is the transform changed?  This method is overridden to return true if any
    * of the sources, any of the targets, or any of the mappings have changed.
    * 
    * @return true = the object has changed
    * 
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isChanged()
   {
      return super.isChanged() || m_order.isChanged();
   }
   
   /**
    * 
    * @see com.sas.etl.models.job.impl.AbstractDataTransform#delete()
    */
   public void delete()
   {
      super.delete();
      
      getSortOrder().delete();
   }
   
   /**
    * @param omr
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.job.impl.AbstractDataTransform#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void deleteFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      super.deleteFromOMR( omr );

      getSortOrder().deleteFromOMR( omr );
   }
   
   /**
    * Updates the new ids of the objects after a save. This method is necessary
    * to change the new ids to their new real ids.
    * 
    * @param mapIDs the map of ids
    * 
    * @see com.sas.etl.models.IOMRPersistable#updateIDs(java.util.Map)
    */
   public void updateIDs( Map mapIDs )
   {
      super.updateIDs( mapIDs );
      
      getSortOrder().updateIDs( mapIDs );
   }
   
   //---------------------------------------------------------------------------
   // Completeness
   //---------------------------------------------------------------------------
   
   /**
    * Is the transform complete?  This method is overridden to return true only
    * if there is a least one source, one target, and one mapping.  If this is
    * not correct for a transform, the subclass must override the method.
    * 
    * @see com.sas.etl.models.IObject#isComplete()
    */
   public boolean isComplete()
   {
      return super.isComplete() && (isUsingUserWrittenCode() || m_order.isComplete());
   }
   
   /**
    * Gets the reasons the transform is incomplete.
    * 
    * @return a list of Strings that are the reasons.
    * 
    * @see com.sas.etl.models.job.ITransform#getReasonsIncomplete()
    */
   public List getReasonsIncomplete()
   {
      List lReasons = super.getReasonsIncomplete();
      if (!isUsingUserWrittenCode() && m_order.size() == 0)
      {
         lReasons.add( RB.getStringResource( "SortTransformModel.ReasonIncomplete.NoByColumn.txt"  ) );
      }
      return lReasons;
    }

   /**
    * Gets the metadata type of the object to use for the classifier map in OMR.
    * The type may be MetadataObjects.CLASSIFIERMAP or MetadataObjects.SELECT.
    * 
    * @return MetadataObjects.CLASSIFIERMAP (the default)
    */
   protected String getClassifierMapType()
   {
      return MetadataObjects.SELECT;
   }
   
   /**
    * Sets whether the sort that is executed is stable or not.  A stable sort 
    * keeps records that are "equal" in their original order.  In an unstable 
    * sort, equal records may be in any order.  Stable is the equivalent to the 
    * "EQUALS" option on a SAS proc sort. 
    * 
    * @param bStable true = stable
    */
   public void setStable( boolean bStable )
   {
      if (m_bStable == bStable)
         return;

      if (isUndoSupported())
         undoableEditHappened( new SetStableUndoable( m_bStable, bStable ) );

      m_bStable = bStable;
      fireModelChangedEvent( STABLE_CHANGED, null );
   }
   
   /**
    * Is the sort a stable sort?  
    * 
    * @return true = stable sort
    * 
    * @see #setStable(boolean)
    */
   public boolean isStable()
   {
      return m_bStable;
   }
   
   /**
    * Sets whether the sort that is executed is allowed to immediately overwrite the 
    * source table if the source and target are the same.  True will save disk space, but 
    * if the sort fails the source table is lost.
    * 
    * @param bOverwrite true = overwrite
    */
   public void setOverwrite( boolean bOverwrite )
   {
      if (m_bOverwrite == bOverwrite)
         return;

      if (isUndoSupported())
         undoableEditHappened( new SetOverwriteUndoable( m_bOverwrite, bOverwrite ) );

      m_bOverwrite = bOverwrite;
      fireModelChangedEvent( OVERWRITE_OPTIONS_CHANGED, null );
   }
   
   /**
    * Is the sort overwriting the source table?  
    * 
    * @return true = overwrite
    * 
    * @see #setOverwrite(boolean)
    */
   public boolean isOverwrite()
   {
      return m_bOverwrite;
   }
 
   /**
    * Sets whether the sort replaces the input data set even if the input data
    * set is indexed.  If replace data set is true, the input data set is 
    * replaced.  A value of true is equivalent to the "FORCE" option on a SAS
    * proc sort.
    * 
    * @param bReplace true = replace
    */
   public void setReplaceDataSet( boolean bReplace )
   {
      if (m_bReplaceDataSet == bReplace)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetReplaceDataSetUndoable( m_bReplaceDataSet, bReplace ) );
            
      m_bReplaceDataSet = bReplace;
      fireModelChangedEvent( REPLACE_DATASET_CHANGED, null );
   }
   
   /**
    * Is the sort replacing input data sets?
    * 
    * @return true = replace
    * 
    * @see #setReplaceDataSet(boolean)
    */
   public boolean isReplaceDataSet()
   {
      return m_bReplaceDataSet;
   }
   
   /**
    * Sets whether the sort sorts tags.  Tags are essentially the columns used
    * in the sort and the record number for each record in the table.  The tags
    * are then sorted and the table is sorted using the sorted tags.  When the 
    * total length of the sort column values is small compared to the entire 
    * record's length, sorting tags will reduce temporary disk usage.  However
    * processing time may be much longer.  A value of true is equivalent to the
    * "TAGSORT" option on a SAS proc sort.
    * 
    * @param bSortTags true = sort tags
    */
   public void setSortTags( boolean bSortTags )
   {
      if (m_bSortTags == bSortTags)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetSortTagsUndoable( m_bSortTags, bSortTags ) );
      
      m_bSortTags = bSortTags;
      fireModelChangedEvent( SORT_TAGS_CHANGED, null );
   }
   
   /**
    * Is the sort sorting tags?
    * 
    * @return true = sort tags
    * 
    * @see #setSortTags(boolean)
    */
   public boolean isSortTags()
   {
      return m_bSortTags;
   }
   
   /**
    * Sets the amount of memory to be used by the sort. This value is equivalent 
    * to the value specified by the "SORTSIZE" option on a SAS proc sort.  
    * Because no validation was done on this string in previous versions of the
    * sort transform, this attribute is a string instead of an int (or long).
    * This method also does no validation.  Valid values for this attribute 
    * include:
    * <ul>
    * <li>"MAX" which means use the maximum amount of memory available, 
    * <li>"nnnn" which means use the number of bytes specified by "nnnn" which 
    *     is a real number,  
    * <li>"nnnnK" which means use the number of kilobytes specified by "nnnn" 
    *     which is a real number,  
    * <li>"nnnnM" which means use the number of megabytes specified by "nnnn" 
    *     which is a real number,  
    * <li>"nnnnG" which means use the number of gigabytes specified by "nnnn" 
    *     which is a real number,
    * <li>"" which means the value is unset.
    *  
    * @param sSize amount of memory to be used by the sort
    */
   public void setMemorySize( String sSize )
   {
      if (m_sMemorySize.equals( sSize ))
         return;
      
      if (sSize == null)
         throw new NullPointerException( "size must not be null" );     // I18NOK:EMS

      if (isUndoSupported())
         undoableEditHappened( new SetMemorySizeUndoable( m_sMemorySize, sSize ) );
      
      m_sMemorySize = sSize;
      fireModelChangedEvent( MEMORY_SIZE_CHANGED, null );
   }
   
   /**
    * Gets the memory size to be used by the sort.  This value is a string for
    * historical reasons.  See @see #setMemorySize(String) for valid values.
    * 
    * @return the memory size string
    */
   public String getMemorySize()
   {
      return m_sMemorySize;
   }
   
   /**
    * Sets the collating sequence used by the sort.  The collating sequence can 
    * be any of the values listed below.  However, the value is not validated
    * because it is believe that the below list is incomplete.  This value is
    * equivalent to the value specified by the "SORTSEQ" option on a SAS proc
    * sort.
    * <ul>
    * <li>Danish</li>
    * <li>Finnish</li>
    * <li>Italian</li>
    * <li>Norwegian</li>
    * <li>Spanish</li>
    * <li>Swedish</li>
    * <li>"" (use default collating sequence)</li>
    * </ul>
    *  
    * @param sSequenceName the name of the collating sequence.
    */
   public void setCollatingSequence( String sSequenceName )
   {
      if (m_sCollatingSequenceName.equals( sSequenceName ))
         return;
      
      if (sSequenceName == null)
         throw new NullPointerException( "collating sequence must not be null" );   // I18NOK:EMS

      if (isUndoSupported())
         undoableEditHappened( new SetCollatingSequenceUndoable( m_sCollatingSequenceName, sSequenceName ) );
            
      m_sCollatingSequenceName = sSequenceName;
      fireModelChangedEvent( COLLATING_SEQUENCE_CHANGED, null );
   }

   /**
    * Gets the collating sequence used by the sort.
    * 
    * @return the collating sequence
    * 
    * @see #setCollatingSequence(String)
    */
   public String getCollatingSequence()
   {
      return m_sCollatingSequenceName;
   }
   
   /**
    * Sets how duplicate records are defined and handled during the sort.  There
    * are three options:
    * <ul>
    * <li>ALLOW_DUPLICATES         - allows duplicate records
    * <li>REMOVE_DUPLICATE_KEYS    - removes records that match another record
    *                                in the columns on which the sort is being
    *                                performed.  This value is equivalent to the
    *                                "NODUPKEY" option on a SAS proc sort.
    * <li>REMOVE_DUPLICATE_RECORDS - removes records that match another record
    *                                in all the columns.  This value is 
    *                                equivalent to the "NODUPRECS" option on a
    *                                SAS proc sort.
    * </ul>
    * 
    * @param eDuplicateRecordHandling specifies how duplicate records are 
    *                                 defined and handled.  See above for valid 
    *                                 values.
    */
   public void setDuplicateRecordHandling( int eDuplicateRecordHandling )
   {
      if (m_eDuplicateRecordHandling == eDuplicateRecordHandling)
         return;
      
      if ((eDuplicateRecordHandling != ALLOW_DUPLICATES) &&
          (eDuplicateRecordHandling != NO_DUPLICATE_KEYS) &&
          (eDuplicateRecordHandling != NO_DUPLICATE_RECORDS))
         throw new IllegalArgumentException( "Invalid duplicate record handling value: " + eDuplicateRecordHandling );  // I18NOK:COS
      
      if (isUndoSupported())
         undoableEditHappened( new SetDuplicateRecordHandlingUndoable( m_eDuplicateRecordHandling, eDuplicateRecordHandling ) );
      
      
      m_eDuplicateRecordHandling = eDuplicateRecordHandling;
      fireModelChangedEvent( DUPLICATE_RECORD_HANDLING_CHANGED, null );
   }
   
   /**
    * Gets how duplicate records will be handled by the sort.
    * 
    * @return how duplicate records will be handled by the sort.  See @see #setDuplicateRecordHandling(int)
    *         for valid values.
    */
   public int getDuplicateRecordHandling()
   {
      return m_eDuplicateRecordHandling;
   }

   /**
    * Sets options to be used on the SAS proc sort.  There is no validation done
    * on these options.
    * 
    * @param sOptions the options
    */
   public void setProcSortOptions( String sOptions )
   {
      if (m_sProcSortOptions.equals( sOptions ))
         return;
      
      if (sOptions == null)
         throw new NullPointerException( "sort options must not be null" );   // I18NOK:EMS

      if (isUndoSupported())
         undoableEditHappened( new SetProcSortOptionsUndoable( m_sProcSortOptions, sOptions ) );
      
      
      m_sProcSortOptions = sOptions;
      fireModelChangedEvent( PROC_SORT_OPTIONS_CHANGED, null );
   }
   
   /**
    * Gets the options to be used on the SAS proc sort.
    * 
    * @return the options
    * 
    * @see #setProcSortOptions(String)
    */
   public String getProcSortOptions()
   {
      return m_sProcSortOptions;
   }
   
   
   /**
    * Returns sort order
    * 
    * @return Sort order
    */
   public ISorting getSortOrder()
   {
      return m_order;
   }
   
   /**
    * @param sort
    */
   public void setSortOrder(ISorting sort)
   {
      if (sort==null)
         throw new IllegalArgumentException("sort order cannot be null");
      
      if (m_order==sort)
         return;
      
      if (m_order!=null)
      {
         m_order.removeNotifyListener( this );
         addToDeletedObjects( m_order );
      }
      
      if (isUndoSupported())
         undoableEditHappened( new SetSortOrderChangedUndoable(m_order, sort) );
      
      m_order = sort;
      
      if (m_order!=null)
      {
         m_order.addNotifyListener( this );
         removeFromDeletedObjects( m_order );
      }
      
      fireModelChangedEvent( SORT_ORDER_CHANGED, m_order );
   }
   
   /**
    * Create the option model to use with this transform.
    * @return the option or prompt model
    * @throws IOException
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws FileNotFoundException
    * @throws ServerConnectionException
    * @throws ServiceException
    * @throws MdException
    * @see com.sas.etl.models.job.impl.AbstractDataTransform#createOptionModel()
    */
   protected IPromptModel createOptionModel() throws IOException, ParserConfigurationException, SAXException, FileNotFoundException,
   ServerConnectionException, ServiceException, MdException
   {
      return new cSortOptionModel();
   }
   
   /**
    * Saves the sort transform to OMR
    * 
    * @param omr the OMR adapter used to save the sort transform.
    * 
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.IOMRPersistable#saveToOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (!isChanged())
         return;
      
      super.saveToOMR( omr );
      
      String sStable     = m_bStable         ? STABLE_TRUE          : STABLE_FALSE;
      String sReplace    = m_bReplaceDataSet ? REPLACE_DATASET_TRUE : REPLACE_DATASET_FALSE;
      String sSortTags   = m_bSortTags       ? SORT_TAGS_TRUE       : SORT_TAGS_FALSE;
      String sDuplicates = m_eDuplicateRecordHandling == NO_DUPLICATE_KEYS    ? DUPLICATES_NO_DUPLICATE_KEYS    :
                           m_eDuplicateRecordHandling == NO_DUPLICATE_RECORDS ? DUPLICATES_NO_DUPLICATE_RECORDS :
                                                                                DUPLICATES_ALLOW_DUPLICATES;
      String sOverwrite   = m_bOverwrite       ? OVERWRITE_TRUE       : OVERWRITE_FALSE;

      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, STABLE_OPTION,          STABLE_OPTION,          "SORT", sStable,                  Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, REPLACE_DATASET_OPTION, REPLACE_DATASET_OPTION, "SORT", sReplace,                 Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, SORT_TAGS_OPTION,       SORT_TAGS_OPTION,       "SORT", sSortTags,                Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, DUPLICATES_OPTION,      DUPLICATES_OPTION,      "SORT", sDuplicates,              Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, "Sortseq",              SORT_SEQUENCE_OPTION,   "SORT", m_sCollatingSequenceName, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES|SET_DELIMITER );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, "Sortsize",             SORT_SIZE_OPTION,       "SORT", m_sMemorySize,            Types.VARCHAR, USE_PROPERTYSET_PROPERTIES|SET_DELIMITER );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, "PROC SORT Options",    PROC_SORT_OPTIONS,      "SORT", m_sProcSortOptions,       Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, OVERWRITE_OPTION,       OVERWRITE_OPTION,       "SORT", sOverwrite,               Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
      
      ISorting sortOrder = getSortOrder();
      sortOrder.saveToOMR( omr );
      OrderByClause mdoOrder = (OrderByClause) omr.acquireOMRObject( sortOrder );
      Select mdoSelect = (Select)getClassifierMapObject( omr );
      mdoSelect.setOrderByForSelect( mdoOrder );
      
      setChanged(false);
   }
   
   /**
    * Loads the sort transform from OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.IOMRPersistable#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      super.loadFromOMR( omr );
      
      String sStable     = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, STABLE_OPTION,          STABLE_TRUE,                 USE_PROPERTYSET_PROPERTIES );
      String sReplace    = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, REPLACE_DATASET_OPTION, REPLACE_DATASET_FALSE,       USE_PROPERTYSET_PROPERTIES );
      String sSortTags   = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, SORT_TAGS_OPTION,       SORT_TAGS_FALSE,             USE_PROPERTYSET_PROPERTIES );
      String sDuplicates = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, DUPLICATES_OPTION,      DUPLICATES_ALLOW_DUPLICATES, USE_PROPERTYSET_PROPERTIES ); 
      String sOverwrite =  loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, OVERWRITE_OPTION,      OVERWRITE_FALSE, USE_PROPERTYSET_PROPERTIES ); 

      setStable(         !sStable  .equals( STABLE_FALSE         ) );
      setReplaceDataSet(  sReplace .equals( REPLACE_DATASET_TRUE ) );
      setSortTags(        sSortTags.equals( SORT_TAGS_TRUE       ) );
      setOverwrite( sOverwrite.equals( OVERWRITE_TRUE ) );
      
      setDuplicateRecordHandling( sDuplicates.equals( DUPLICATES_NO_DUPLICATE_KEYS    ) ? NO_DUPLICATE_KEYS    : 
                                  sDuplicates.equals( DUPLICATES_NO_DUPLICATE_RECORDS ) ? NO_DUPLICATE_RECORDS :
                                                                                          ALLOW_DUPLICATES       );
      
      setCollatingSequence( loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, SORT_SEQUENCE_OPTION, "", USE_PROPERTYSET_PROPERTIES ) );
      setMemorySize(        loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, SORT_SIZE_OPTION,     "", USE_PROPERTYSET_PROPERTIES ) );
      setProcSortOptions(   loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, PROC_SORT_OPTIONS,    "", USE_PROPERTYSET_PROPERTIES ) );

      Select        mdoSelect = (Select) getClassifierMapObject( omr );
      OrderByClause mdoOrder  = mdoSelect.getOrderByForSelect();
      if (mdoOrder != null)
      {
         setSortOrder( (ISorting) omr.acquireObject( mdoOrder ));
      }
      
      setChanged( false );
   }
   
   /**
    * Notifies the listener of the changes.  This class only adds itself as a 
    * listener to the sorting container to generate a sort columns changed event 
    * from this transformation.  Therefore, this method is overridden to 
    * generate a sort columns changed event when the sort columns changed.
    * 
    * @param ev the notification event
    * 
    * @see com.sas.etl.models.INotifyListener#notify(com.sas.etl.models.NotifyEvent)
    */
   public void notify( NotifyEvent ev )
   {
      if ((ev.getSource() == m_order) &&
          (ev.getType() == NotifyEvent.OBJECT_CHANGED))
         fireModelChangedEvent( SORT_COLUMNS_CHANGED, null );
      
      super.notify( ev );
   }
   
   /**
    * Generates code into the code string buffer.
    * 
    * @param codeSegment the code generation environment
    * 
    * @return the string buffer that contains the code
    * @throws CodegenException 
    */
   protected ICodeSegment getGeneratedCode( ICodeSegment codeSegment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      super.getGeneratedCode( codeSegment );

      IPhysicalTable source = (IPhysicalTable)getDataSources()[0];

      // there should only be 1 output, so just have to get the first classifier source
      IPhysicalTable target = (IPhysicalTable)getDataTargets()[0];

      // get the options for PROC SORT
      StringBuffer sortOptions = null;
      IPromptModel sortOptionModel = null;
      try
      {
         sortOptionModel = getOptionModel();
         sortOptions = sortOptionModel.getOptionsString(codeSegment, SORT_OPTIONS );

         // delete the target table
         genTableDelete(codeSegment, target);

         ITransformTableOptions sourceOptions = getTableOptionObject( source, true );
         String sourceTableOptions = sourceOptions.getTableOptions(codeSegment.getCurrentServer());
         
         boolean mappingNeeded = isMappingNeeded( codeSegment.isQuoting(), source, target );
         String mappingStepOutputName = "";

         if (mappingNeeded)
         {
            mappingStepOutputName = codeSegment.getUniqueWorkTableName(true,0);
            
            getOrdinaryMappingCode( codeSegment, source, target, mappingStepOutputName, "&SYSLAST", sourceTableOptions,null, true,true,
                                    true, null, null, false, null,null, null );
         }
            
         // proc sort statement
         codeSegment.addSourceCode( "proc sort data = &SYSLAST \n");
         codeSegment.indent();

         // data= option
       if (!mappingNeeded && sourceTableOptions.length() > 0)
       {
          codeSegment.indent()
          .addSourceCode("(" + sourceTableOptions + ") \n")
          .unIndent()
          ;
       }

         // out= option
         codeSegment.addSourceCode("out = " + target.getFullNameQuotedAsNeeded( codeSegment ));

         ITransformTableOptions targetOptionObj = getTableOptionObject( target, false );
         String targetOptions = targetOptionObj.getTableOptions(true, codeSegment.getCurrentServer());
         if (targetOptions.length() > 0)
         {
            codeSegment.addSourceCode("\n")
            .indent()
            .addSourceCode(targetOptions.trim())
            .unIndent();
         }

         // proc sort options
         if (sortOptions!=null && sortOptions.length()>0) 
         {
            codeSegment.addSourceCode("\n")
            .indent()
            .addSourceCode(sortOptions)
            .unIndent();
         }

         codeSegment.addSourceCode("; \n");
         // generate by statement
         m_order.getGeneratedCode( codeSegment, false ) ;

         codeSegment.unIndent()
                    .addSourceCode("run; \n\n")
                    .genRCSetCall("&syserr");   /*I18nOK:LINE*/
         
         // Delete the MAPPED view if it was created
         if (mappingNeeded && !target.isView())
            codeSegment.genTableDelete(mappingStepOutputName);

      }
      catch(Exception e)
      {
         throw new CodegenException(e, this);
      }
      

      return codeSegment;

   }
   
   protected class cSortOptionModel extends BaseDataTransformPromptModel implements IPromptModel
   {
      /**
       * Constructor

       * @throws IOException
       * @throws ParserConfigurationException
       * @throws SAXException
       * @throws FileNotFoundException
       * @throws ServerConnectionException 
       * @throws ServiceException 
       * @throws MdException 
       */
      public cSortOptionModel()
      throws IOException, ParserConfigurationException, SAXException, FileNotFoundException,
      ServerConnectionException, ServiceException, MdException
      {
         super(SortTransformModel.this.getModel(),SortTransformModel.this);
         
      }
           
      /**
       * Create a data provider
       * @return data provider
       * @throws RemoteException
       * @throws MdException
       * @throws ServerConnectionException
       * @throws ServiceException
       */
      public PromptDataProvider createDataProvider()
      throws ServiceException, RemoteException, MdException, ServerConnectionException
      {
         
         return null;
      }
      
      /**
       * This creates a group of options for the sort transform and also inherits options from the AbstractData and
       * AbstractTransform classes.
       * 
       * @return PromptGroup
       * @throws IOException
       * @throws ParserConfigurationException
       * @throws SAXException
       * @throws FileNotFoundException
       * @see com.sas.etl.models.prompts.impl.BaseDataTransformPromptModel#getPromptGroup()
       */
      protected PromptGroupInterface getPromptGroup()
      throws IOException, ParserConfigurationException, SAXException, FileNotFoundException
      {
         // get group from parent classes
         PromptGroupInterface grp = super.getPromptGroup();
         
         // get group for sort transform which should not contain options from the parents
         //   such as SystemOptions, SYSLAST, or Diagnostics
         //   the parent classes will handle these options
         PromptGroupInterface grp1 = createPromptGroup( cSortOptionModel.class.getResource("res/Options_SASSort_Template.xml") );
         
         // return a combination of the groups, the first group is the first one ordered in the resulting group
         return combinePromptGroups( grp1, grp );
      }
      
      /**
       * Set the values for the prompt model from the values from the transform model
       * 
       * @see com.sas.etl.models.prompts.impl.BaseDataTransformPromptModel#setValuesFromModel()
       */
      protected void setValuesFromModel()
      throws RemoteException, MdException, ServiceException, ServerConnectionException
      {
         super.setValuesFromModel();
         
         SortTransformModel transform = (SortTransformModel)getOwner();
         
         setOptionValue( OVERWRITE_OPTION, transform.isOverwrite() ? OVERWRITE_TRUE : OVERWRITE_FALSE );
         setOptionValue( REPLACE_DATASET_OPTION, transform.isReplaceDataSet() ? REPLACE_DATASET_TRUE : REPLACE_DATASET_FALSE );
         
         setDuplicateOptionValue( transform.getDuplicateRecordHandling() );
         
         setOptionValue( STABLE_OPTION, transform.isStable() ? STABLE_TRUE : STABLE_FALSE );
         setOptionValue( SORT_TAGS_OPTION, transform.isSortTags() ? SORT_TAGS_TRUE : SORT_TAGS_FALSE);
         setOptionValue( SORT_SEQUENCE_OPTION, transform.getCollatingSequence() );
         setOptionValue( SORT_SIZE_OPTION, transform.getMemorySize());
         setOptionValue( PROC_SORT_OPTIONS, transform.getProcSortOptions());
         
      }

      private void setDuplicateOptionValue(int duplicateHandling)
      throws RemoteException, MdException, ServiceException, ServerConnectionException
      {
         String duplicates = "";
         
         if (duplicateHandling==NO_DUPLICATE_KEYS)
            duplicates = DUPLICATES_NO_DUPLICATE_KEYS;
         else if (duplicateHandling==NO_DUPLICATE_RECORDS)
            duplicates = DUPLICATES_NO_DUPLICATE_RECORDS;

         setOptionValue( DUPLICATES_OPTION, duplicates);
      }
      
      protected IModelListener createModelListener()
      {
         return new cSortModelChangeListener();
      }
      
      /**
       * Change listener for the visual to set the transform model based on changes from the prompting framework
       * @return change listener
       * @see com.sas.etl.models.prompts.impl.BaseDataTransformPromptModel#createChangeListener()
       */
      protected IPromptValueChangeListener createChangeListener()
      {
         return new SortTransformValueChangedListener((SortTransformModel)getOwner(),this);
      }
      
      
      private class cSortModelChangeListener extends BaseDataTransformModelListener
      {
         /**
          * Constructor
          *
          */
         public cSortModelChangeListener()
         {
            super(getOwner(),cSortOptionModel.this);
         }

         /**
          * Model changed event.  This sets the prompting model to values from the transform model.  This 
          * enables undo/redo.
          * 
          * @param ev event 
          * @see com.sas.etl.models.prompts.impl.BaseDataTransformModelListener#modelChanged(com.sas.etl.models.impl.ModelEvent)
          */
         public void modelChanged( ModelEvent ev )
         {
            if (ev.getModelObject()!=getModelObject())
               return;
            
            super.modelChanged( ev );

            getPromptModel().setListeningForChanges( false );

            String type = ev.getType();

            try
            {
               if (type.equals( OVERWRITE_OPTIONS_CHANGED ))
               {
                  setOptionValue( OVERWRITE_OPTION, isOverwrite() ? OVERWRITE_TRUE : OVERWRITE_FALSE );
               }
               else if (type.equals( REPLACE_DATASET_CHANGED  ))
               {
                  setOptionValue( REPLACE_DATASET_OPTION, isReplaceDataSet() ? REPLACE_DATASET_TRUE : REPLACE_DATASET_FALSE );
               }
               else if (type.equals( DUPLICATE_RECORD_HANDLING_CHANGED  ))
               {
                  setDuplicateOptionValue( getDuplicateRecordHandling() );
               }
               else if (type.equals( STABLE_CHANGED  ))
               {
                  setOptionValue( STABLE_OPTION, isStable() ? STABLE_TRUE : STABLE_FALSE );
               }
               else if (type.equals( SORT_TAGS_CHANGED  ))
               {
                  setOptionValue( SORT_TAGS_OPTION, isSortTags() ? SORT_TAGS_TRUE : SORT_TAGS_FALSE);
               }
               else if (type.equals( COLLATING_SEQUENCE_CHANGED  ))
               {
                  setOptionValue( SORT_SEQUENCE_OPTION, getCollatingSequence() );
               }
               else if (type.equals( MEMORY_SIZE_CHANGED  ))
               {
                  setOptionValue( SORT_SIZE_OPTION, getMemorySize());
               }
               else if (type.equals( PROC_SORT_OPTIONS_CHANGED  ))
               {
                  setOptionValue( PROC_SORT_OPTIONS, getProcSortOptions());
               }
            } 
            catch(RemoteException exc)
            {
               ModelLogger.getDefaultLogger().error( "RemoteException",exc );
            }
            catch(MdException exc)
            {
               ModelLogger.getDefaultLogger().error( "MdException", exc );
            }
            catch(ServerConnectionException exc)
            {
               ModelLogger.getDefaultLogger().error( "ServerConnectionException", exc );
            }
            catch(ServiceException exc)
            {
               ModelLogger.getDefaultLogger().error( "ServiceException", exc );
            }
            finally
            {
               getPromptModel().setListeningForChanges( true );
            }
         }
         
      }//end cSortModelChangeListener
      
      
      private class SortTransformValueChangedListener extends BaseDataTransformValueChangedListener
      {
         /**
          * Constructor
          * 
          * @param transformModel the transform model
          * @param promptModel    the prompt model
          */
         public SortTransformValueChangedListener(SortTransformModel transformModel, IPromptModel promptModel)
         {
            super(transformModel,promptModel);
         }

         /**
          * Prompt value change event.  This will set the value on the transform model from a change event from the 
          * prompting framework
          * @param event event
          * @see com.sas.etl.models.prompts.impl.BaseDataTransformValueChangedListener#promptValueChanged(com.sas.prompts.PromptValueChangeEventInterface)
          */
         public void promptValueChanged( PromptValueChangeEventInterface event )
         {
            if (!isListeningForChanges())
               return;

            getOwner().getModel().startCompoundUndoable();

            try
            {
               super.promptValueChanged( event );

               PromptDefinitionInterface def = event.getPromptDefinition();

               String optionName = def.getPromptName();
               Object value = m_model.getOptionValue( def.getPromptName() );

               if (value==null)
                  value = "";

               if (optionName.equals( STABLE_OPTION ))
               {
                  setStable( value.equals( STABLE_TRUE ) );
               }
               else if (optionName.equals( REPLACE_DATASET_OPTION ))
               {
                  setReplaceDataSet( value.equals( REPLACE_DATASET_TRUE ) );
               }
               else if (optionName.equals( OVERWRITE_OPTION ))
               {
                  setOverwrite( value.equals( OVERWRITE_TRUE ) );
               }
               else if (optionName.equals( SORT_TAGS_OPTION ))
               {
                  setSortTags( value.equals( SORT_TAGS_TRUE ) );
               }
               else if (optionName.equals( SORT_SIZE_OPTION ))
               {
                  setMemorySize( value.toString() );
               }
               else if (optionName.equals( SORT_SEQUENCE_OPTION ))
               {
                  setCollatingSequence( value.toString() );
               }
               else if (optionName.equals( DUPLICATES_OPTION ))
               {
                  String sDuplicates = value.toString();
                  setDuplicateRecordHandling( sDuplicates.equals( DUPLICATES_NO_DUPLICATE_KEYS    ) ? NO_DUPLICATE_KEYS    : 
                     sDuplicates.equals( DUPLICATES_NO_DUPLICATE_RECORDS ) ? NO_DUPLICATE_RECORDS :
                        ALLOW_DUPLICATES    );
               }
               else if (optionName.equals( PROC_SORT_OPTIONS ))
               {
                  setProcSortOptions( value.toString() );
               }
            }
            
            finally
            {
               getOwner().getModel().endCompoundUndoable();


            }
         }         
      }//end SortTransformValueChangedListener
   }//end cSortOptionModel

   //---------------------------------------------------------------------------
   // Undoables
   //---------------------------------------------------------------------------
   /**
    * SetStableUndoable is the undoable for setting the sort transform's stable 
    * attribute.
    */
   private class SetStableUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldStable;
      private boolean m_newStable;
      
      /**
       * Constructs the set stable attribute undoable
       * 
       * @param oldStable the old stable attribute
       * @param newStable the new stable attribute
       */
      public SetStableUndoable( boolean oldStable, boolean newStable )
      {
         m_oldStable = oldStable;
         m_newStable = newStable;
      }
      
      /**
       * Undoes the setting of the stable attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setStable( m_oldStable );
      }
      
      /**
       * Redoes the setting of the stable attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setStable( m_newStable );
      }
   } // SetStableUndoable

   /**
    * SetReplaceDataSetUndoable is the undoable for setting the sort transform's 
    * replace dataset attribute.
    */
   private class SetReplaceDataSetUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldReplaceDataSet;
      private boolean m_newReplaceDataSet;
      
      /**
       * Constructs the set replace dataset attribute undoable
       * 
       * @param oldReplaceDataSet the old replace dataset attribute
       * @param newReplaceDataSet the new replace dataset attribute
       */
      public SetReplaceDataSetUndoable( boolean oldReplaceDataSet, boolean newReplaceDataSet )
      {
         m_oldReplaceDataSet = oldReplaceDataSet;
         m_newReplaceDataSet = newReplaceDataSet;
      }
      
      /**
       * Undoes the setting of the replace dataset attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setReplaceDataSet( m_oldReplaceDataSet );
      }
      
      /**
       * Redoes the setting of the replace dataset attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setReplaceDataSet( m_newReplaceDataSet );
      }
   } // SetReplaceDataSetUndoable

   /**
    * SetSortTagsUndoable is the undoable for setting the sort transform's sort 
    * tags attribute.
    */
   private class SetSortTagsUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldSortTags;
      private boolean m_newSortTags;
      
      /**
       * Constructs the set sort tags attribute undoable
       * 
       * @param oldSortTags the old sort tags attribute
       * @param newSortTags the new sort tags attribute
       */
      public SetSortTagsUndoable( boolean oldSortTags, boolean newSortTags )
      {
         m_oldSortTags = oldSortTags;
         m_newSortTags = newSortTags;
      }
      
      /**
       * Undoes the setting of the sort tags attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setSortTags( m_oldSortTags );
      }
      
      /**
       * Redoes the setting of the sort tags attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setSortTags( m_newSortTags );
      }
   } // SetSortTagsUndoable

   /**
    * SetDuplicateRecordHandlingUndoable is the undoable for setting the sort 
    * transform's duplicate record handling attribute.
    */
   private class SetDuplicateRecordHandlingUndoable extends AbstractUndoableEdit
   {
      private int m_oldDuplicateRecordHandling;
      private int m_newDuplicateRecordHandling;
      
      /**
       * Constructs the set duplicate record handling attribute undoable
       * 
       * @param oldDuplicateRecordHandling the old duplicate record handling attribute
       * @param newDuplicateRecordHandling the new duplicate record handling attribute
       */
      public SetDuplicateRecordHandlingUndoable( int oldDuplicateRecordHandling, int newDuplicateRecordHandling )
      {
         m_oldDuplicateRecordHandling = oldDuplicateRecordHandling;
         m_newDuplicateRecordHandling = newDuplicateRecordHandling;
      }
      
      /**
       * Undoes the setting of the duplicate record handling attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setDuplicateRecordHandling( m_oldDuplicateRecordHandling );
      }
      
      /**
       * Redoes the setting of the duplicate record handling attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setDuplicateRecordHandling( m_newDuplicateRecordHandling );
      }
   } // SetDuplicateRecordHandlingUndoable
   
   /**
    * SetMemorySizeUndoable is the undoable for setting the sort transform's 
    * memory size attribute.
    */
   private class SetMemorySizeUndoable extends AbstractUndoableEdit
   {
      private String m_oldMemorySize;
      private String m_newMemorySize;
      
      /**
       * Constructs the set memory size attribute undoable
       * 
       * @param oldMemorySize the old memory size attribute
       * @param newMemorySize the new memory size attribute
       */
      public SetMemorySizeUndoable( String oldMemorySize, String newMemorySize )
      {
         m_oldMemorySize = oldMemorySize;
         m_newMemorySize = newMemorySize;
      }
      
      /**
       * Undoes the setting of the memory size attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setMemorySize( m_oldMemorySize );
      }
      
      /**
       * Redoes the setting of the memory size attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setMemorySize( m_newMemorySize );
      }
   } // SetMemorySizeUndoable
   
   /**
    * SetCollatingSequenceUndoable is the undoable for setting the sort 
    * transform's collating sequence attribute.
    */
   private class SetCollatingSequenceUndoable extends AbstractUndoableEdit
   {
      private String m_oldCollatingSequence;
      private String m_newCollatingSequence;
      
      /**
       * Constructs the set collating sequence attribute undoable
       * 
       * @param oldCollatingSequence the old collating sequence attribute
       * @param newCollatingSequence the new collating sequence attribute
       */
      public SetCollatingSequenceUndoable( String oldCollatingSequence, String newCollatingSequence )
      {
         m_oldCollatingSequence = oldCollatingSequence;
         m_newCollatingSequence = newCollatingSequence;
      }
      
      /**
       * Undoes the setting of the collating sequence attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setCollatingSequence( m_oldCollatingSequence );
      }
      
      /**
       * Redoes the setting of the collating sequence attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setCollatingSequence( m_newCollatingSequence );
      }
   } // SetCollatingSequenceUndoable

   /**
    * SetOverwriteUndoable is the undoable for setting the sort 
    * transform's overwrite attribute.
    */
   private class SetOverwriteUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldOverwrite;
      private boolean m_newOverwrite;
      
      /**
       * Constructs the overwrite undoable
       * 
       * @param oldOverwrite the old Overwrite attribute
       * @param newOverwrite the new Overwrite attribute
       */
      public SetOverwriteUndoable( boolean oldOverwrite, boolean newOverwrite )
      {
         m_oldOverwrite = oldOverwrite;
         m_newOverwrite = newOverwrite;
      }
      
      /**
       * Undoes the setting of the overwrite attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setOverwrite( m_oldOverwrite );
      }
      
      /**
       * Redoes the setting of the overwrite attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setOverwrite( m_newOverwrite );
      }
   } // SetOverwriteUndoable
   
   
   /**
    * SetProcSortOptionsUndoable is the undoable for setting the sort 
    * transform's PROC SORT options attribute.
    */
   private class SetProcSortOptionsUndoable extends AbstractUndoableEdit
   {
      private String m_oldProcSortOptions;
      private String m_newProcSortOptions;
      
      /**
       * Constructs the set PROC SORT options attribute undoable
       * 
       * @param oldProcSortOptions the old PROC SORT options attribute
       * @param newProcSortOptions the new PROC SORT options attribute
       */
      public SetProcSortOptionsUndoable( String oldProcSortOptions, String newProcSortOptions )
      {
         m_oldProcSortOptions = oldProcSortOptions;
         m_newProcSortOptions = newProcSortOptions;
      }
      
      /**
       * Undoes the setting of the PROC SORT options attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setProcSortOptions( m_oldProcSortOptions );
      }
      
      /**
       * Redoes the setting of the PROC SORT options attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setProcSortOptions( m_newProcSortOptions );
      }
   } // SetProcSortOptionsUndoable
   
   
   /**
    * SetSortOrderChangedUndoable is the undoable for setting the sort order attribute.
    */
   private class SetSortOrderChangedUndoable extends AbstractUndoableEdit
   {
      private ISorting m_oldSortOrder;
      private ISorting m_newSortOrder;
      
      /**
       * Constructs the set PROC SORT options attribute undoable
       * 
       * @param oldSortOrder the old sort order attribute
       * @param newSortOrder the new sort order attribute
       */
      public SetSortOrderChangedUndoable( ISorting oldSortOrder, ISorting newSortOrder )
      {
         m_oldSortOrder = oldSortOrder;
         m_newSortOrder = newSortOrder;
      }
      
      /**
       * Undoes the setting of the sort order attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setSortOrder( m_oldSortOrder );
      }
      
      /**
       * Redoes the setting of the sort order attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setSortOrder( m_newSortOrder );
      }
   } // SetSortOrderChangedUndoable
}