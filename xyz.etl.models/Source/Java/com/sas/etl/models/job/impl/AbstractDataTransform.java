/* $Id$ */
/**
 * Title:       AbstractDataTransform.java
 * Description: An abstract implementation of a data transform.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.job.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sas.etl.models.IFilter;
import com.sas.etl.models.IModel;
import com.sas.etl.models.IObject;
import com.sas.etl.models.IObjectFactory;
import com.sas.etl.models.IPersistableObject;
import com.sas.etl.models.NotifyEvent;
import com.sas.etl.models.ServerException;
import com.sas.etl.models.data.BadLibraryDefinitionException;
import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.data.IDataObject;
import com.sas.etl.models.data.IExternalTable;
import com.sas.etl.models.data.ILibrary;
import com.sas.etl.models.data.IPhysicalTable;
import com.sas.etl.models.data.ITable;
import com.sas.etl.models.data.IWorkTable;
import com.sas.etl.models.data.dbmstypes.DBMSNamesUtil;
import com.sas.etl.models.data.dbmstypes.DBMSTypeFactory;
import com.sas.etl.models.data.dbmstypes.IDBMSType;
import com.sas.etl.models.impl.AbstractPrimaryModelList;
import com.sas.etl.models.impl.ModelList;
import com.sas.etl.models.impl.ModelLogger;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.impl.ObjectComparator;
import com.sas.etl.models.data.impl.BaseWorkTable;
import com.sas.etl.models.job.ICodeGenerationEnvironment;
import com.sas.etl.models.job.ICodeSegment;
import com.sas.etl.models.job.IDataTransform;
import com.sas.etl.models.job.IExpression;
import com.sas.etl.models.job.IJob;
import com.sas.etl.models.job.IMapping;
import com.sas.etl.models.job.IMappingRule;
import com.sas.etl.models.job.ITextExpression;
import com.sas.etl.models.job.ITransformTableOptions;
import com.sas.etl.models.job.transforms.FileReaderTransformModel;
import com.sas.etl.models.job.transforms.FileWriterTransformModel;
import com.sas.etl.models.job.transforms.common.IGroupBy;
import com.sas.etl.models.job.transforms.common.ISorting;
import com.sas.etl.models.other.BadServerDefinitionException;
import com.sas.etl.models.other.ISASClientConnection;
import com.sas.etl.models.other.IServer;
import com.sas.etl.models.prompts.IPromptDefinitionValue;
import com.sas.etl.models.prompts.IPromptModel;
import com.sas.etl.models.prompts.impl.BaseDataTransformPromptModel;
import com.sas.etl.models.prompts.impl.PromptUtils;
import com.sas.metadata.MdFactory;
import com.sas.metadata.remote.AbstractTransformation;
import com.sas.metadata.remote.ClassifierMap;
import com.sas.metadata.remote.FeatureMap;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.PhysicalTable;
import com.sas.metadata.remote.PropertySet;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.StepPrecedence;
import com.sas.metadata.remote.TransformationStep;
import com.sas.metadata.remote.WorkTable;
import com.sas.services.ServiceException;
import com.sas.storage.exception.ServerConnectionException;

/**
 * AbstractDataTransform is an abstract implementation of a data transform.
 */
public abstract class AbstractDataTransform extends AbstractTransform
implements IDataTransform
{
   // custom option names
   private static final String INCLUDED_IN_MAPPING       = "IncludedInMapping";
   private static final String INCLUDED_IN_PROPAGATION   = "IncludedInPropagation";
        
   // custom list names
   private static final String COLUMNS_EXCLUDED_FROM_MAPPING_NAME       = "ColumnsExcludedFromMapping"; 
   private static final String COLUMNS_EXCLUDED_FROM_PROPAGATION_NAME   = "ColumnsExcludedFromPropagation";
   private static final String CONNECTED_SOURCES_NAME                   = "ConnectedSources";
      
   private List m_lDataSources;
   private List m_lDataTargets;
   private List m_lMappings;
   private List m_lColumnsExcludedFromMapping;
   private List m_lColumnsExcludedFromPropagation;
   
   // dawong - 30 Jan 2012
   // S0819931, S0828790 - The list of sources added/removed through diagram changes needs
   // to be kept separately from the list of data sources.  This is because the list of 
   // data sources may include tables from subqueries on other statements in the transform.
   // If a data source was manually connected on the diagram, it shouldn't be automatically
   // removed due to that table being removed from a query.
   private List m_lConnectedSources;
   
   private boolean m_bGenerateIndexesOnTables;
   
   // attributes
   private boolean m_bGenerateSYSLAST;
//stwatk S0266081
   private boolean m_bAppendForce;
   
   private boolean m_bColMacroVars;
   
   private boolean m_bFileMacroVars;
   
//   private boolean m_bGenerateFormat;
   private boolean m_bIncludedInPropagation;
   private boolean m_bIncludedInMapping;
   private boolean m_collectSourceTableRowCounts;  // default
   private boolean m_collectTargetTableRowCounts;

   private String m_sClassifierMapID;
   private String m_sGenerateFormatsInformats;
   private String m_sUseConnectUsing;

   private boolean m_bTargetDataAutomaticallyMoved;

   private int     m_iDBMSType;
   private String  m_sDBMSTypeName;
   
   private List m_lTableOptionModels;
   
   protected static final String BLANK = "";
   protected static final String DBI_DIRECT_EXEC = "DBIDIRECTEXEC";
   protected static final String NO_DBI_DIRECT_EXEC = "NODBIDIRECTEXEC";
   
   private static final String[] DBI_DIRECT_VALUES = {BLANK, DBI_DIRECT_EXEC, NO_DBI_DIRECT_EXEC};
   
   private String m_sDBIDirectExec;
   
   /**
    * Constructs an the abstract data transform.
    * 
    * @param sID   the id of the data transform
    * @param model the model
    */
   public AbstractDataTransform( String sID, IModel model )
   {
      super( sID, model );  

      m_sClassifierMapID = createIDForNewObject();
      
      m_lDataSources                      = new DataSourcesList();
      m_lDataTargets                      = new DataTargetsList();
      m_lMappings                         = new ArrayList();
      m_lColumnsExcludedFromMapping       = new ModelList( this, new String[]{ COLUMN_EXCLUDED_FROM_MAPPING,     COLUMN_INCLUDED_IN_MAPPING     }, ModelList.SAVE_BY_OWNER, IColumn.class ); 
      m_lColumnsExcludedFromPropagation   = new ModelList( this, new String[]{ COLUMN_EXCLUDED_FROM_PROPAGATION, COLUMN_INCLUDED_IN_PROPAGATION }, ModelList.SAVE_BY_OWNER, IColumn.class );      
      m_lConnectedSources                 = new ConnectedSourcesList();

      
      m_bGenerateSYSLAST         = true;
//      m_bGenerateFormat  = true;
//stwatk S0266081
      m_bAppendForce             = true;
      m_bColMacroVars            = true;
      m_bFileMacroVars           = true;
      m_bIncludedInMapping       = true;
      m_bIncludedInPropagation   = true;
      m_bGenerateIndexesOnTables = true;
      
      m_collectSourceTableRowCounts = true;
      m_collectTargetTableRowCounts = false;
      m_bTargetDataAutomaticallyMoved = false;
      //default is sas execution
      m_iDBMSType = IDataTransform.SAS_DBMS_EXECUTION_TYPE;
      m_sDBMSTypeName = IDBMSType.SAS_DBMS_TYPE_NAME;
      
      m_lTableOptionModels = new ArrayList();
      
      m_sGenerateFormatsInformats = FORMATSINFORMATS_JOB;
      m_sUseConnectUsing = FORMATSINFORMATS_JOB;
      
      
      m_sDBIDirectExec = getDefaultDBIDirectExecValue();
   }

   /**
    * Gets the maximum data source count.  This method is used to limit the 
    * number of data sources.  Since many transforms have one source input, the
    * default maximum data source count is one.  Override this method to allow
    * for more data sources.
    * 
    * @return the maximum data source count (default = 1)
    */
   protected int getMaximumDataSourceCount()
   {
      return 1;
   }
   
   /**
    * Does the transform allow data targets?  Checks the maximum data targets count
    * 
    * @return true if data targets are allowed
    */
   public boolean isDataTargetsAllowed()
   {
      return (getMaximumDataTargetCount() > 0 ? true : false);
   }

   /**
    * Does the transform allow data sources?  Checks the maximum data sources count
    * 
    * @return true if data sources are allowed
    */
   public boolean isDataSourcesAllowed()
   {
      return (getMaximumDataSourceCount() == 0 ? true : false);
   }
   
   /**
    * Gets the maximum data target count.  This method is used to limit the 
    * number of data targets.  Since many transforms have one target input, the
    * default maximum data target count is one.  Override this method to allow
    * for more data targets.
    * 
    * @return the maximum data target count (default = 1)
    */
   protected int getMaximumDataTargetCount()
   {
      return 1;
   }
   
   
   protected IPromptModel createOptionModel()
   throws IOException, ParserConfigurationException, SAXException, FileNotFoundException,
   ServerConnectionException, ServiceException, MdException
   {
      return new BaseDataTransformPromptModel(getModel(),this);
   }
   
   protected String getClassifierMapId()
   {
      return m_sClassifierMapID;
   }
   
   protected void setClassifierMapId(String sID)
   {
     m_sClassifierMapID = sID;
   }
   
   public IPromptDefinitionValue[] getParameters(boolean includeSubComponents)
   {
      List parameters = new ArrayList();
      IPromptDefinitionValue[] prompts = super.getParameters();
      if (prompts!=null)
         parameters.addAll(Arrays.asList(prompts));
      
      ITable[] sources = getSourceTables();
      if (sources!=null)
         for (int i=0; i<sources.length; i++)
            parameters.addAll( Arrays.asList(sources[i].getParameters() ));
      
      ITable[] targets = getTargetTables();
      if (targets!=null)
         for (int i=0; i<targets.length; i++)
            parameters.addAll( Arrays.asList(targets[i].getParameters() ));
      
      return (IPromptDefinitionValue[])parameters.toArray(new IPromptDefinitionValue[parameters.size()]);
      
   }

   //---------------------------------------------------------------------------
   // Attributes
   //---------------------------------------------------------------------------
   protected String getDefaultDBIDirectExecValue()
   {
      return BLANK;
   }

   public void setDBIDirectExec(String value)
   {
      if (!(BLANK.equals( value ) || DBI_DIRECT_EXEC.equals( value ) || NO_DBI_DIRECT_EXEC.equals( value )))
         throw new IllegalArgumentException("value for dbidirectexec invalid");
      
      if (value.equals( m_sDBIDirectExec ))
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetDBIDirectExecUndoable(m_sDBIDirectExec, value) );
      
      m_sDBIDirectExec = value;
      
      fireModelChangedEvent( DBI_DIRECT_EXEC_VALUE_CHANGED, value );
   }
   
   public String getDBIDirectExecValue()
   {
      return m_sDBIDirectExec;
   }
   
   
   public void setGenerateIndexesOnTargetTables(boolean genIndexes)
   {
      if (m_bGenerateIndexesOnTables==genIndexes)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetGenerateIndexesOnTargetTables(m_bGenerateIndexesOnTables, genIndexes) );
      
      m_bGenerateIndexesOnTables = genIndexes;
      
      fireModelChangedEvent( GENERATE_INDEXES_ON_TARGET_TABLES_CHANGED, null );
   }
   
   public boolean isGenerateIndexesOnTargetTables()
   {
      return m_bGenerateIndexesOnTables;
   }
   
   /**
    * Sets whether the SYSLAST macro variable should be generated.
    * 
    * @param  enabled  true = generate the SYSLAST macro variable
    */
   public void setSYSLASTVariableGenerationEnabled (boolean enabled)
   {
      if (enabled == m_bGenerateSYSLAST)
         return;
      if (isUndoSupported())
         undoableEditHappened (new SetSYSLASTVariableGenerationEnabledUndoable (m_bGenerateSYSLAST, enabled));

      m_bGenerateSYSLAST = enabled;
      fireModelChangedEvent (GENERATE_SYSLAST_CHANGED, null);
   }
   
   /**
    * Is the SYSLAST macro variable set to be generated?
    * 
    * @return true = generate the SYSLAST macro variable
    */
   public boolean isSYSLASTVariableGenerationEnabled()
   {
      return m_bGenerateSYSLAST;
   }
   
   /**
    * Sets whether the SYSLAST macro variable should be generated.
    * 
    * @param  enabled  true = generate the SYSLAST macro variable
    */
//   public void setFormatGenerationEnabled (boolean enabled)
//   {
//      if (enabled == m_bGenerateFormat)
//         return;
//      if (isUndoSupported())
//         undoableEditHappened (new SetFormatGenerationEnabledUndoable (m_bGenerateFormat, enabled));
//
//      m_bGenerateFormat = enabled;
//      fireModelChangedEvent (GENERATE_FORMATINFORMAT_CHANGED, null);
//   }
   
//stwatk Defect ID: S0266081
   
   /**
    * Sets whether or not to use FORCE option on PROC APPEND.
    * 
    * @param  enabled  true = use FORCE option (default)
    */
   public void setAppendForceEnabled (boolean enabled)
   {
      if (enabled == m_bAppendForce)
         return;
      
      if (isUndoSupported())
         undoableEditHappened (new SetAppendForceEnabledUndoable (m_bAppendForce, enabled));

      m_bAppendForce = enabled;
      fireModelChangedEvent (APPEND_FORCE_CHANGED, null);
   }
   
     /**
    * Do we use force option on proc append?
    * 
    * @return true = generate column macro variables
    */
   public boolean isAppendForceEnabled()
   {
      return m_bAppendForce;
   }
   
   /**
    * Sets whether or not to generate DIS 4.2 or 3.4-Level column macro variables.
    * 
    * @param  enabled  true = generate DIS 4.2-Level column macro variables (default)
    */

   public void setColMacroVarsEnabled (boolean enabled)
   {
      if (enabled == m_bColMacroVars)
         return;
      
      if (isUndoSupported())
         undoableEditHappened (new SetColMacroVarsEnabledUndoable (m_bColMacroVars, enabled));

      m_bColMacroVars = enabled;
      fireModelChangedEvent (COL_MACRO_VARS_CHANGED, null);
   }

   /**
    * Sets whether or not to generate external file macro variables.
    * 
    * @param  enabled  true = generate external file macro variables (default)
    */

   public void setFileMacroVarsEnabled (boolean enabled)
   {
      if (enabled == m_bFileMacroVars)
         return;
      
      if (isUndoSupported())
         undoableEditHappened (new SetFileMacroVarsEnabledUndoable (m_bFileMacroVars, enabled));

      m_bFileMacroVars = enabled;
      fireModelChangedEvent (FILE_MACRO_VARS_CHANGED, null);
   }
   
     /**
    * Do we generate DI 4.2 or DI 3.4 column macro variables?
    * 
    * @return true = generate DI 4.2 column macro variables
    */
   public boolean isColMacroVarsEnabled()
   {
      return m_bColMacroVars;
   }   

   /**
    * Do we generate external file macro variables?
    * 
    * @return true = generate external file macro variables
    */
   public boolean isFileMacroVarsEnabled()
   {
	   return m_bFileMacroVars;
   }   
 
   /**
    * Sets whether format information should be generated.
    * 
    * @param  valud values are JOB, YES, or NO
    */
   public void setFormatInformatGeneration (String value)
   {
	      if (value == m_sGenerateFormatsInformats)
	          return;
	       if (isUndoSupported())
	          undoableEditHappened (new SetFormatInformatGenerationUndoable (m_sGenerateFormatsInformats, value));

	       m_sGenerateFormatsInformats = value;
	       fireModelChangedEvent (GENERATE_FORMATINFORMAT_CHANGED, null);
   }
   
   /**
    * Return value for CONNECT USING generation.
    * Values can be:  JOB, YES, or NO
    * 
    * @return value for format generation
    */
   public String getUseConnectUsing()
   {
	   return m_sUseConnectUsing;
   }

   /**
    * Sets whether CONNECT USING should be generated.
    * 
    * @param  valud values are JOB, YES, or NO
    */
   public void setUseConnectUsing (String value)
   {
	      if (value == m_sUseConnectUsing)
	          return;
	       if (isUndoSupported())
	          undoableEditHappened (new SetUseConnectUsingUndoable (m_sUseConnectUsing, value));

	       m_sUseConnectUsing = value;
	       fireModelChangedEvent (USECONNECTUSING_CHANGED, null);
   }
   
   /**
    * Is the CONNECT USING enabled?
    * This method checks if the option is set to use the value defined in the Job
	* If so, it gets the value from the Job.
    * 
    * @return true = generate CONNECT USING syntax
    */
   public boolean isUseConnectUsingEnabled()
   {
//      return m_bGenerateFormat;
//      if (getUseConnectUsing().equalsIgnoreCase(FORMATSINFORMATS_JOB))
//      {
//    	  return getJob().isConnectUsingEnabled();    		 
//      }
//      else 
//    	  return getUseConnectUsing().equalsIgnoreCase(FORMATSINFORMATS_YES);
   	return true;
   }
   /**
    * Return value for format informat generation.
    * Values can be:  JOB, YES, or NO
    * 
    * @return value for format generation
    */
   public String getFormatInformatGeneration()
   {
	   return m_sGenerateFormatsInformats;
   }
   
   /**
    * Is the SYSLAST macro variable set to be generated?
    * 
    * @return true = generate the SYSLAST macro variable
    */
   public boolean isFormatGenerationEnabled()
   {
//      return m_bGenerateFormat;
      if (getFormatInformatGeneration().equalsIgnoreCase(FORMATSINFORMATS_JOB))
      {
    	  return getJob().isFormatInformatGenerationEnabled();    		 
      }
      else 
    	  return getFormatInformatGeneration().equalsIgnoreCase(FORMATSINFORMATS_YES);
   }

   /**
    * Sets whether the container is included in propagation operations.  The 
    * propagation operations are the actions that cause multiple propagations 
    * to occur.
    * 
    * @param bIncluded true = the container is included in propagation operations
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#setIncludedInPropagation(boolean)
    */
   public void setIncludedInPropagation( boolean bIncluded )
   {
      if (m_bIncludedInPropagation == bIncluded)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetIncludedInPropagationUndoable( m_bIncludedInPropagation, bIncluded ) );
      m_bIncludedInPropagation = bIncluded;
      fireModelChangedEvent( INCLUDE_IN_PROPAGATION_CHANGED, null );
   }

   /**
    * Is the container is included in propagation operations?  The propagation 
    * operations are the actions that cause multiple propagations to occur.
    * 
    * @return true = the container is included in propagation operations
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#isIncludedInPropagation()
    */
   public boolean isIncludedInPropagation()
   {
      return m_bIncludedInPropagation;
   }

   /**
    * Sets whether the container is included in mapping operations.  The mapping 
    * operations are the actions that cause multiple mappings to occur.
    * 
    * @param bIncluded true = the container is included in mapping operations
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#setIncludedInMapping(boolean)
    */
   public void setIncludedInMapping( boolean bIncluded )
   {
      if (m_bIncludedInMapping == bIncluded)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetIncludedInMappingUndoable( m_bIncludedInMapping, bIncluded ) );
      m_bIncludedInMapping = bIncluded;
      fireModelChangedEvent( INCLUDE_IN_MAPPING_CHANGED, null );
   }

   /**
    * Is the container is included in mapping operations?  The mapping 
    * operations are the actions that cause multiple mapping to occur.
    * 
    * @return true = the container is included in mapping operations
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#isIncludedInMapping()
    */
   public boolean isIncludedInMapping()
   {
      return m_bIncludedInMapping;
   }
   
   //---------------------------------------------------------------------------
   // Data Sources
   //---------------------------------------------------------------------------
   
   /**
    * Gets the data sources for the transform.
    * 
    * @return the data sources
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataSources()
    */
   public IDataObject[] getDataSources()
   {
      return (IDataObject[]) m_lDataSources.toArray( new IDataObject[ m_lDataSources.size() ] );
   }
   
   /**
    * Get the list of data sources for the transform
    * 
    * @return the data source list for the transform
    * @see com.sas.etl.models.job.IDataTransform#getDataSourceList()
    */
   public List getDataSourceList()
   {
      return m_lDataSources;
   }
   
   
   /**
    * Does the transform contain the specified table as a source?
    * 
    * @param source the specified source table
    * 
    * @return true = the transform contains the specified table as a source
    * @see com.sas.etl.models.job.IDataTransform#containsInDataSources(com.sas.etl.models.data.IDataObject)
    */
   public boolean containsInDataSources( IDataObject source )
   {
      return m_lDataSources.contains( source );
   }
   
   /**
    * Gets the count of data sources for the transform.
    * 
    * @return the count of data sources
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataSourcesCount()
    */
   public int getDataSourcesCount()
   {
      return m_lDataSources.size();
   }

   /**
    * Adds a data source to the transform.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#addDataSource(com.sas.etl.models.data.IDataObject)
    */
   public void addDataSource( IDataObject source )
   {
      addDataSource( m_lDataSources.size(), source );
   }

   /**
    * Adds a data source to the transform.
    * 
    * @param iSource the index of where the data source is to be added
    * @param source  the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#addDataSource(com.sas.etl.models.data.IDataObject)
    */
   public void addDataSource( int iSource, IDataObject source )
   {
      if (m_lDataSources.size() >= getMaximumDataSourceCount())
         throw new UnsupportedOperationException( "Maximum data source count will be exceeded (" + getMaximumDataSourceCount() + ")" );

      m_lDataSources.add( iSource, source );
   }
   
   /**
    * This method is called before a data source is added.  It is provided so
    * that concrete data transform classes can do any necessary preparation 
    * before the data source is added.  This method is not called during an undo 
    * or redo that involved adding or removing a data source.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataSource(com.sas.etl.models.data.IDataObject)
    */
   protected void preAddDataSource( IDataObject source )
   {
   }

   /**
    * This method is called after a data source is added.  It is provided so
    * that concrete data transform classes can do any necessary fix up after
    * the data source is added.  This method is not called during an undo or
    * redo that involved adding or removing a data source.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataSource(com.sas.etl.models.data.IDataObject)
    */
   protected void postAddDataSource( IDataObject source )
   {
      postAddTransformTableOption(source, true);
   }
   
   /**
    * Removes a data source from the transform.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataSource(com.sas.etl.models.data.IDataObject)
    */
   public void removeDataSource( IDataObject source )
   {
      m_lDataSources.remove( source );
   }

   /**
    * This method is called before a data source is removed.  It is provided so
    * that concrete data transform classes can do any necessary clean up before
    * the data source is removed.  This method is not called during an undo or
    * redo that involved adding or removing a data source.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataSource(com.sas.etl.models.data.IDataObject)
    */
   protected void preRemoveDataSource( IDataObject source )
   {
      if (source instanceof ITable)
      {
         removeSourceTableFromMappings( (ITable) source );
         preRemoveTransformTableOptions(source, true);
      }
   }
   

   /**
    * This method is called after a data source is removed.  It is provided so
    * that concrete data transform classes can do any necessary clean up after
    * the data source is removed.  This method is not called during an undo or
    * redo that involved adding or removing a data source.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param source the data source
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataSource(com.sas.etl.models.data.IDataObject)
    */
   protected void postRemoveDataSource( IDataObject source )
   {
   }
   
   /**
    * Gets the source tables for the transformation.
    * 
    * @return the source tables
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#getSourceTables()
    */
   public ITable[] getSourceTables()
   {
      List lTables = new ArrayList();
      for ( int iDataSource=0; iDataSource < m_lDataSources.size(); iDataSource++ )
      {
         Object obj = m_lDataSources.get( iDataSource );
         if (obj instanceof ITable)
            lTables.add( obj );
      }
      
      return (ITable[]) lTables.toArray( new ITable[ lTables.size() ] );
   }
   
   /**
    * Add a table to the list of sources connected via the diagram.
    * @param source the source table
    */
   public void addConnectedSource(IDataObject source)
   {
      m_lConnectedSources.add(source);
   }
   
   /**
    * Remove a table from the list of sources connected via the diagram.
    * @param source the source table
    */
   public void removeConnectedSource(IDataObject source)
   {
      m_lConnectedSources.remove(source);
   }
   
   /**
    * Get the list of sources connected via the diagram.
    * @return the list of sources connected via the diagram
    */
   public List getConnectedSources()
   {
      return m_lConnectedSources;
   }
   
   /**
    * Get the number of source tables connected via the diagram.
    * @return the number of sources connected via the diagram
    */
   public int getConnectedSourcesCount()
   {
      return m_lConnectedSources.size();
   }
   
   //---------------------------------------------------------------------------
   // Data Targets
   //---------------------------------------------------------------------------
   
   /**
    * Gets the data targets for the transform.
    * 
    * @return the data targets
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataTargets()
    */
   public IDataObject[] getDataTargets()
   {
      return (IDataObject[]) m_lDataTargets.toArray( new IDataObject[ m_lDataTargets.size() ] );
   }
   
   /**
    * Gets the data target list for the transform.
    * 
    * @return the data target list for the transform
    * @see com.sas.etl.models.job.IDataTransform#getDataTargetList()
    */
   public List getDataTargetList()
   {
      return m_lDataTargets;
   }
   
   /**
    * Does the transform contain the specified table as a target?
    * 
    * @param target the specified target table
    * 
    * @return true = the transform contains the specified table as a target
    * @see com.sas.etl.models.job.IDataTransform#containsInDataTargets(com.sas.etl.models.data.IDataObject)
    */
   public boolean containsInDataTargets( IDataObject target )
   {
      return m_lDataTargets.contains( target );
   }
   
   /**
    * Gets the count of data targets for the transform.
    * 
    * @return the count of data targets
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataTargetsCount()
    */
   public int getDataTargetsCount()
   {
      return m_lDataTargets.size();
   }

   /**
    * Adds a data target to the transform.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#addDataTarget(com.sas.etl.models.data.IDataObject)
    */
   public void addDataTarget( IDataObject target )
   {
      addDataTarget( m_lDataTargets.size(), target );
   }

   /**
    * Adds a data target to the transform.
    * 
    * @param iTarget the index of where the data target is to be added
    * @param target  the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#addDataTarget(com.sas.etl.models.data.IDataObject)
    */
   public void addDataTarget( int iTarget, IDataObject target )
   {
      if (m_lDataTargets.size() >= getMaximumDataTargetCount())
         throw new UnsupportedOperationException( "Maximum data target count will be exceeded (" + getMaximumDataTargetCount() + ")" );

      m_lDataTargets.add( iTarget, target );
   }

   /**
    * This method is called before a data target is added.  It is provided so
    * that concrete data transform classes can do any necessary preparation 
    * before the data target is added.  This method is not called during an undo 
    * or redo that involved adding or removing a data target.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   protected void preAddDataTarget( IDataObject target )
   {
   }

   /**
    * This method is called after a data target is added.  It is provided so
    * that concrete data transform classes can do any necessary fix up after
    * the data target is added.  This method is not called during an undo or
    * redo that involved adding or removing a data target.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   protected void postAddDataTarget( IDataObject target )
   {
      postAddTransformTableOption(target, false);
      if (target instanceof IWorkTable)
         updateTargetLibrary( (IWorkTable)target);
   }
   
   /**
    * Removes a data target from the transform
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   public void removeDataTarget( IDataObject target )
   {
      m_lDataTargets.remove( target );
      
      if (target != null)
         target.removeNotifyListener( this );
   }

   /**
    * This method is called before a data target is removed.  It is provided so
    * that concrete data transform classes can do any necessary clean up before
    * the data target is removed.  This method is not called during an undo or
    * redo that involved adding or removing a data target.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   protected void preRemoveDataTarget( IDataObject target )
   {
      if (target instanceof ITable)
      {
         removeTargetTableFromMappings( (ITable) target );
         preRemoveTransformTableOptions(target, false);

      }
   }

   /**
    * This method is called after a data target is removed.  It is provided so
    * that concrete data transform classes can do any necessary clean up after
    * the data target is removed.  This method is not called during an undo or
    * redo that involved adding or removing a data target.  Also this method 
    * will be called within the context of a compound undoable.
    * 
    * @param target the data target
    * 
    * @see com.sas.etl.models.job.IDataTransform#removeDataTarget(com.sas.etl.models.data.IDataObject)
    */
   protected void postRemoveDataTarget( IDataObject target )
   {
   }
   
   /**
    * Clear the list of data targets.
    */
   protected void clearDataTargetsBeforeLoad()
   {
      m_lDataTargets.clear();
   }     

   /**
    * Get the index of the target table for the transformation.
    * @param table the table to be found 
    * @return index of the table in the target list
    */
   public int getTargetTableIndex(ITable table)
   {
      IDataObject[] aTargets = getDataTargets();
      for ( int iTarget = 0; iTarget < aTargets.length; iTarget++ )
      {
    	  if ( aTargets[ iTarget ] == table)
    		  return iTarget;
      }
	   return -1;   
   }
   
   protected void updateTargetLibrary(IWorkTable table)
   {
      IJob job = getJob();
      if (job!=null && table instanceof IWorkTable)
      {
         table.setAlternateJobLibrary( job.getAlternateTemporaryLibrary() );
      }
   }
   
   /**
    * Gets the target tables for the transformation.
    * 
    * @return the target tables
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#getTargetTables()
    */
   public ITable[] getTargetTables()
   {
      List lTables = new ArrayList();
      for ( int iDataTarget=0; iDataTarget < m_lDataTargets.size(); iDataTarget++ )
      {
         Object obj = m_lDataTargets.get( iDataTarget );
         if (obj instanceof ITable)
            lTables.add( obj );
      }
      
      return (ITable[]) lTables.toArray( new ITable[ lTables.size() ] );
   }   

   /**
    * Creates a default target work table for the transform.
    * 
    * @return the default target work table
    * 
    * @see com.sas.etl.models.job.IDataTransform#addNewWorkTable()
    */
   public IWorkTable addNewWorkTable()
   {
      startCompoundUndoable();
      try
      {
         IWorkTable tbl = getObjectFactory().createNewWorkTable( getID() );
         tbl.setName( getName() );
         addDataTarget( tbl );
         return tbl;
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Gets the transform's target work tables. 
    * 
    * @return the transform's target work table
    * 
    * @see com.sas.etl.models.job.IDataTransform#getWorkTables()
    */
   public IWorkTable[] getWorkTables()
   {
      List lWorkTables = new ArrayList();
      for ( int iTable=0; iTable<getDataTargetList().size(); iTable++ )
      {
         if (getDataTargetList().get( iTable ) instanceof IWorkTable)
            lWorkTables.add( getDataTargetList().get( iTable ) );
      }
      
      return (IWorkTable[]) lWorkTables.toArray( new IWorkTable[ lWorkTables.size() ] );
   }

   /**
    * Updates source columns into the target table.  If the source column has a
    * one to one mapping to a target column in the target table, the source 
    * column's attributes are copied to the target column.  The copy performed 
    * is a deep copy which means extended attributes and the private note is 
    * copied and associations to public notes and documents are copied.  If the
    * deep copy will cause a duplicate name in the target table, the copy is 
    * aborted.
    * 
    * @param aSourceColumns the source columns
    * @param tblTarget      the target table
    * 
    * @return the columns updated
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#updateMappedColumnsToTargetTable(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.ITable)
    */
   public IColumn[] updateMappedColumnsToTargetTable( IColumn[] aSourceColumns, ITable tblTarget )
   {
      return updateMappedColumnsImpl( aSourceColumns, tblTarget, true );
   }
   
   /**
    * Updates target columns into the source table.  If the target column has a
    * one to one mapping to a source column in the source table, the target 
    * column's attributes are copied to the source column.  The copy performed 
    * is a deep copy which means extended attributes and the private note is 
    * copied and associations to public notes and documents are copied.  If the
    * deep copy will cause a duplicate name in the source table, the copy is 
    * aborted.
    * 
    * @param aTargetColumns the target columns
    * @param tblSource      the source table
    * 
    * @return the columns updated
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#updateMappedColumnsToSourceTable(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.ITable)
    */
   public IColumn[] updateMappedColumnsToSourceTable( ITable tblSource, IColumn[] aTargetColumns )
   {
      return updateMappedColumnsImpl( aTargetColumns, tblSource, false );
   }

   /**
    * Updates source columns into the target table.  If the source column has a
    * one to one mapping to a target column in the target table, the source 
    * column's attributes are copied to the target column.  The copy performed 
    * is a deep copy which means extended attributes and the private note is 
    * copied and associations to public notes and documents are copied.  If the
    * deep copy will cause a duplicate name in the target table, the copy is 
    * aborted.
    * 
    * @param aSourceColumns the source columns
    * @param tblTarget      the target table
    * 
    * @return the columns updated
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#updateMappedColumnsToTargetTable(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.ITable)
    */
   private IColumn[] updateMappedColumnsImpl( IColumn[] aSourceColumns, ITable tblTarget, boolean bForward )
   {
      startCompoundUndoable();
      try
      {
         List      lSourceColumns  = Arrays.asList( aSourceColumns );
         IColumn[] aTargetColumns  = tblTarget.getColumns();
         List      lUpdatedColumns = new ArrayList();
         boolean   bCaseSensitive  = tblTarget.isCaseSensitive();
         
         List lMappings = getMappingsList();
         
         for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
         {
            IMapping mapping = (IMapping) lMappings.get( iMapping );
            if (IMapping.ONE_TO_ONE.equals( mapping.getType() ))
            {
               IColumn[] aSources = bForward ? mapping.getSources() : mapping.getTargets();
               IColumn[] aTargets = bForward ? mapping.getTargets() : mapping.getSources();
               
               // verification
               if ((aSources.length != 1) || (aTargets.length != 1))
                  continue;
               
               // if the source is in the list of source columns and 
               //    the target is in the target table
               if (lSourceColumns.contains(       aSources[0] ) &&
                   tblTarget     .containsColumn( aTargets[0] ))
               {
                  // if the source's name equals the target's name or 
                  //    the source's name is not in the existing columns
                  // then it is OK to do the update which is a 
                  boolean bEqual = bCaseSensitive ? aSources[0].getName().equals(           aTargets[0].getName() ) 
                                                  : aSources[0].getName().equalsIgnoreCase( aTargets[0].getName() );
                  if (bEqual || !doesNameExistInColumns( aSources[0].getName(), aTargetColumns, bCaseSensitive ))
                  {
                     aSources[0].deepCopy( aTargets[0] );
                     lUpdatedColumns.add( aTargets[0] );
                  }
               }
            }
            else if (IMapping.DERIVED.equals( mapping.getType() ))
            {
               IColumn[] aSources = bForward ? mapping.getSources() : mapping.getTargets();
               IColumn[] aTargets = bForward ? mapping.getTargets() : mapping.getSources();

               // verify that the derived mapping can be used in an update
               // it can be used in an update if there is one source, one target
               // and there is no expression
               if ((aSources.length != 1) || (aTargets.length != 1) || doesMappingHaveExpression( mapping ))
                  continue;
               
               // if the source is in the list of source columns and 
               //    the target is in the target table
               if (lSourceColumns.contains(       aSources[0] ) &&
                   tblTarget     .containsColumn( aTargets[0] ))
               {
                  // if the source's name equals the target's name or 
                  //    the source's name is not in the existing columns
                  // then it is OK to do the update which is a 
                  boolean bEqual = bCaseSensitive ? aSources[0].getName().equals(           aTargets[0].getName() ) 
                                                  : aSources[0].getName().equalsIgnoreCase( aTargets[0].getName() );
                  if (bEqual || !doesNameExistInColumns( aSources[0].getName(), aTargetColumns, bCaseSensitive ))
                  {
                     aSources[0].deepCopy( aTargets[0] );
                     lUpdatedColumns.add( aTargets[0] );
                  }
               }
            }
         }
         
         return (IColumn[]) lUpdatedColumns.toArray( new IColumn[ lUpdatedColumns.size() ] );
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Does the mapping have a non-empty expression?  In other words, if there
    * is an expression, does it have any text?  This is a convenience method 
    * because of all the exceptions that expression's getText method throws.
    * 
    * @return true = the mapping has a non-empty expression
    */
   private boolean doesMappingHaveExpression( IMapping mapping )
   {
      IExpression expression = mapping.getExpression();
      if (expression == null)
         return false;

      try
      {
         return expression.getText( null, false, false ).length() > 0;
      }
      catch (RemoteException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      catch (CodegenException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      catch (MdException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      catch (BadServerDefinitionException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      catch (BadLibraryDefinitionException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      catch (ServerException ex)
      {
         ModelLogger.getDefaultLogger().error( "", ex );
      }
      
      // if an exception occurred, then there is no way to know if the mapping
      // had an expression, so pretend that it does
      return true;
   }
   
   //---------------------------------------------------------------------------
   // Mappings
   //---------------------------------------------------------------------------
   /**
    * Are expressions allowed by this transformation?  Override this method if
    * the transformation does not allow expressions. 
    * 
    * @return true = expressions are allowed
    */
   protected boolean areExpressionsAllowed()
   {
      return true;
   }
   
   /**
    * Is the proposed mapping between the specified source columns and specified
    * target columns allowed?  This method may be overridden if a transformation
    * has different requirements.  Existing mappings should not be checked 
    * because this call may be to check a change to an existing mapping.
    * <p>
    * The default implementation prevents mappings that would require an 
    * expression in transformations that do not allow expressions.
    * 
    * @param aSources the source columns of the proposed mapping
    * @param aTargets the target columns of the proposed mapping
    * 
    * @return true = the mapping is allowed
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#isMappingAllowed(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[])
    */
   public boolean isMappingAllowed( IColumn[] aSources, IColumn[] aTargets )
   {
      return !isExpressionNeeded( aSources, aTargets ) || areExpressionsAllowed();
   }
   
   /**
    * Gets the reason the proposed mapping is not allowed.  The reason can be 
    * displayed to the user as a message.
    * 
    * @param aSources the source columns of the proposed mapping
    * @param aTargets the target columns of the proposed mapping
    * 
    * @return true = the mapping is allowed
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#getReasonMappingIsNotAllowed(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[])
    */
   public String getReasonMappingIsNotAllowed( IColumn[] aSources, IColumn[] aTargets )
   {
      if (isExpressionNeeded( aSources, aTargets ) && !areExpressionsAllowed())
      {
         if ((aSources.length == 1) && (aTargets.length == 1))
         {
            if (aSources[0].getType() != aTargets[0].getType())
               return RB.getStringResource( "AbstractDataTransform.ReasonMappingNotAllowed.TypeMismatch.txt" );
//            if (aSources[0].getLength() >  aTargets[0].getLength())
//               return RB.getStringResource( "AbstractDataTransform.ReasonMappingNotAllowed.TargetTooShort.txt" );
         }
         
         // some other reason an expression is needed
         return RB.getStringResource( "AbstractDataTransform.ReasonMappingNotAllowed.ExpressionNotAllowed.txt" );
      }
      
      return null;
   }
   
   /**
    * Maps all columns between all the transform's source tables and and all the 
    * transform's target tables.  The default set of mapping rules is used.
    * 
    * @see com.sas.etl.models.job.IDataTransform#mapColumns()
    */
   public void mapColumns()
   {
      mapColumns( DefaultMappingRules.getRules() );
   }
   
   /**
    * Maps all columns between the specified source table and the specified 
    * target table.  The source table should be in the transform's sources and
    * the the target table should be in the transform's targets.  The default 
    * set of mapping rules is used.
    * 
    * @param tblSource the source table
    * @param tblTarget the target table
    * 
    * @see com.sas.etl.models.job.IDataTransform#mapColumns(com.sas.etl.models.data.ITable, com.sas.etl.models.data.ITable)
    */
   public void mapColumns( ITable tblSource, ITable tblTarget )
   {
      mapColumns( tblSource, tblTarget, DefaultMappingRules.getRules() );
   }
   
   /**
    * Maps the specified source columns to the specified target columns.  The 
    * default set of mapping rules is used.  The source columns should be 
    * columns that belong to the transform's source tables and the target 
    * columns should be columns that belong to the transform's target tables.
    * 
    * @param aSources the source columns
    * @param aTargets the target columns
    * 
    * @see com.sas.etl.models.job.IDataTransform#mapColumns(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[])
    */
   public void mapColumns( IColumn[] aSources, IColumn[] aTargets )
   {
      mapColumns( aSources, aTargets, DefaultMappingRules.getRules() );
   }
   
   /**
    * Maps all columns between all the transform's source and the transform's 
    * target tables using the mapping rules provided.
    * 
    * @param aRules the mapping rules
    */
   public void mapColumns( IMappingRule[] aRules )
   {
      startCompoundUndoable();
      try
      {
         // for all the data targets that are tables, ...
         ITable[] targetTables = getTargetTables();
         ITable[] sourceTables = getSourceTables();
         for ( int iDataTarget=0; iDataTarget<targetTables.length; iDataTarget++ )
         {
            ITable tblTarget = targetTables[ iDataTarget ];

            // for all the data sources that are tables, ...
            for ( int iDataSource=0; iDataSource<sourceTables.length; iDataSource++ )
            {
               ITable tblSource = sourceTables[ iDataSource ];
               mapColumns( tblSource, tblTarget, aRules );
            } // all sources
         } // all target tables
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Maps all columns between the specified source table and the specified 
    * target table using the mapping rules specified.  The source table should 
    * be in the transform's sources and the the target table should be in the 
    * transform's targets.  
    * 
    * @param tblSource the source table
    * @param tblTarget the target table
    * @param aRules    the mapping rules
    * 
    * @see com.sas.etl.models.job.IDataTransform#mapColumns(com.sas.etl.models.data.ITable, com.sas.etl.models.data.ITable, com.sas.etl.models.job.IMappingRule[])
    */
   public void mapColumns( ITable tblSource, ITable tblTarget, IMappingRule[] aRules )
   {
      mapColumns( tblSource.getColumns(), tblTarget.getColumns(), aRules );
   }
   
   /**
    * Maps the specified source columns to the specified target columns using 
    * the specified mapping rules.  The source columns should be columns that 
    * belong to the transform's source tables and the target columns should be 
    * columns that belong to the transform's target tables.
    * 
    * @param aSources the source columns
    * @param aTargets the target columns
    * @param aRules   the mapping rules
    * 
    * @see com.sas.etl.models.job.IDataTransform#mapColumns(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[], com.sas.etl.models.job.IMappingRule[])
    */
   public void mapColumns( IColumn[] aSources, IColumn[] aTargets, IMappingRule[] aRules )
   {
      startCompoundUndoable();
      try
      {
         IColumn[] aOneSource = new IColumn[1];
         IColumn[] aOneTarget = new IColumn[1];
         
         List lExcludedTargetColumns = getListOfColumnsExcludedFromMapping();

         // Put the source columns in a list and remove the excluded source
         // columns from the list.  The columns are put in a list so that they
         // can be removed from the list when they get mapped.  This is a
         // performance measure which assumes that a source column will not be
         // mapped to more than one target column.
         //
         // When mapping rules are created by the user, we may want to support
         // a source column being mapped to more than one target column.  If 
         // the need ever arises, we will need to change the below code to not
         // remove the source column from the list of source columns once it is
         // mapped.
         List lSources = new ArrayList( Arrays.asList( aSources ) );  // a hack to get a modifiable list
         lSources.removeAll( getSourceColumnsExcludedFromMapping() );
         
         for ( int iTarget=0; iTarget<aTargets.length; iTarget++ )
         {
            IColumn tgt = aTargets[iTarget];
            if (lExcludedTargetColumns.contains( tgt ))
               continue;

            // If the target column already has a mapping that has an 
            // expression, attempt to map the given source columns into the
            // expression.  No further attempt to map to the target column 
            // should be made because the target column already has an 
            // expression.
            IMapping mapping = findTargetMapping( tgt );
            if ((mapping != null) && (mapping.getExpression() != null))
            {
               mapUsingExpression( aSources, mapping );
               continue;
            }

            aOneTarget[0] = tgt;

            // For each of the source columns, ...
            boolean bUnmapped = (mapping == null);
            for ( int iSource=0; (iSource<lSources.size()) && bUnmapped; iSource++ )
            {
               IColumn src = (IColumn) lSources.get( iSource );
               aOneSource[0] = src;

               // If the transformation will not allow the mapping (for some
               // transformation-specific reason), skip it
               if (!isMappingAllowed( aOneSource, aOneTarget )) 
                  continue;
               
               // For each of the rules, if the rule can be used to map the 
               // the source to the target, ...
               for ( int iRule=0; (iRule<aRules.length) && bUnmapped; iRule++ )
               {
                  if (aRules[iRule].canMap( src, tgt ))
                  {
                     // If the rule creates an expression, create the expression
                     // (if the transformation supports expressions)
                     String sExpression = aRules[iRule].getExpressionText();
                     if ((sExpression != null) && (sExpression.length() > 0))
                     {
                        // punt rule if derived mappings are not supported 
                        if (!areExpressionsAllowed())
                           continue;
                        
                        ITextExpression expression = getModel().getObjectFactory().createNewTextExpression( getID() );
                        expression.setText( sExpression, new IObject[]{ src, src.getTable() } );
                        addMapping( aOneSource, aOneTarget, IMapping.DERIVED, expression );
                     }
                     
                     else
                        addMapping( aOneSource, aOneTarget, IMapping.ONE_TO_ONE, null );

                     // The target column is now mapped
                     bUnmapped = false;
                     lSources.remove( src );    // NOTE: the line of code that would need to be removed to support a source column mapping to more than one target column
                  } // can map
               } // all rules
            } // all source columns
         } // all target columns
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Maps source columns into an existing mapping that has an expression.
    * 
    * @param aSources the source columns to check to see if they can be added
    *                 to the mapping because the mapping's expression has 
    *                 remembered columns that match
    * @param mapping  the mapping that will be updated based on the expression
    */
   private void mapUsingExpression( IColumn[] aSources, IMapping mapping )
   {
      IExpression expression = mapping.getExpression(); 
      for ( int iSource=0; iSource<aSources.length; iSource++ )
      {
         if (expression.containsRememberedColumn( aSources[iSource] ))
            mapping.addSource( aSources[iSource] );
      }
   }

   //---------------------------------------------------------------------------
   // Propagate methods
   //---------------------------------------------------------------------------

   /**
    * Propagates columns to source tables.  Any column in a target 
    * table that does not exist in the source table is copied to the source 
    * table and a mapping is created between the source and target columns.  
    * Each target table is checked against each source table.  The comparison is 
    * based only on a case-insensitive compare of the column's name.  Non-work
    * tables are updated. 
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTables()
    */
   public void propagateColumnsToSourceTables()
   {
      propagateColumnsToSourceTables( PROPAGATE_INTO_NON_WORK_TABLES ); 
   }

   /**
    * Propagates columns to source tables.  Any column in a target 
    * table that does not exist in the source table is copied to the source 
    * table and a mapping is created between the source and target columns.  
    * Each target table is checked against each source table.  The comparison is 
    * based only on a case-insensitive compare of the column's name.
    * 
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTables(int)
    */
   public void propagateColumnsToSourceTables( int eNonWorkTableHandling )
   {
      propagateColumnsToSourceTables( getSourceTables(), getTargetTables(), eNonWorkTableHandling );
   }

   /**
    * Propagates columns to source tables.  Any column in one of the specificed
    * target tables that does not exist in the specified source tables is copied 
    * to the source table and a mapping is created between the source and target 
    * columns.  Each target table is checked against each source table.  The 
    * comparison is based only on a case-insensitive compare of the column's 
    * name.  Only columns from the specified target tables are candidates to be
    * propagated and only specified source tables are updated.  Non-work tables
    * are updated.
    *   
    * @param aSourceTables the specified source tables 
    * @param aTargetTables the specified target tables
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTables(com.sas.etl.models.data.ITable[], com.sas.etl.models.data.ITable[])
    */
   public void propagateColumnsToSourceTables( ITable[] aSourceTables, ITable[] aTargetTables )
   {
      propagateColumnsToSourceTables( aSourceTables, aTargetTables, PROPAGATE_INTO_NON_WORK_TABLES ); 
   }

   /**
    * Propagates columns to source tables.  Any column in one of the specificed
    * target tables that does not exist in the specified source tables is copied 
    * to the source table and a mapping is created between the source and target 
    * columns.  Each target table is checked against each source table.  The 
    * comparison is based only on a case-insensitive compare of the column's 
    * name.  Only columns from the specified target tables are candidates to be
    * propagated and only specified source tables are updated.
    * 
    * @param aSourceTables          the specified source tables 
    * @param aTargetTables          the specified target tables
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    *  
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTables(com.sas.etl.models.data.ITable[], com.sas.etl.models.data.ITable[], int)
    */
   public void propagateColumnsToSourceTables( ITable[] aSourceTables, ITable[] aTargetTables, int eNonWorkTableHandling )
   {
      propagateColumnsImpl( aTargetTables, aSourceTables, getListOfColumnsExcludedFromPropagation(), eNonWorkTableHandling, false );
   }

   /**
    * Propagates columns to target tables.  Any column in a source 
    * table that does not exist in the target table is copied to the target 
    * table and a mapping is created between the target and source columns.  
    * Each source table is checked against each target table.  The comparison is 
    * based only on a case-insensitive compare of the column's name.  Non-work
    * tables are updated.
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTables()
    */
   public void propagateColumnsToTargetTables()
   {
      propagateColumnsToTargetTables( PROPAGATE_INTO_NON_WORK_TABLES ); 
   }

   /**
    * Propagates columns to target tables.  Any column in a source 
    * table that does not exist in the target table is copied to the target 
    * table and a mapping is created between the target and source columns.  
    * Each source table is checked against each target table.  The comparison is 
    * based only on a case-insensitive compare of the column's name.
    * 
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    *  
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTables(int)
    */
   public void propagateColumnsToTargetTables( int eNonWorkTableHandling )
   {
      propagateColumnsToTargetTables( getSourceTables(), getTargetTables(), eNonWorkTableHandling );
   }

   /**
    * Propagates columns to target tables.  Any column in one of the specificed
    * source tables that does not exist in the specified target tables is copied 
    * to the target table and a mapping is created between the target and source 
    * columns.  Each source table is checked against each target table.  The 
    * comparison is based only on a case-insensitive compare of the column's 
    * name.  Only columns from the specified source tables are candidates to be
    * propagated and only specified target tables are updated.  Non-work tables
    * are updated.
    * 
    * @param aSourceTables the specified source tables 
    * @param aTargetTables the specified target tables
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTables(com.sas.etl.models.data.ITable[], com.sas.etl.models.data.ITable[])
    */
   public void propagateColumnsToTargetTables( ITable[] aSourceTables, ITable[] aTargetTables )
   {
      propagateColumnsToTargetTables( aSourceTables, aTargetTables, PROPAGATE_INTO_NON_WORK_TABLES ); 
   }

   /**
    * Propagates columns to target tables.  Any column in one of the specificed
    * source tables that does not exist in the specified target tables is copied 
    * to the target table and a mapping is created between the target and source 
    * columns.  Each source table is checked against each target table.  The 
    * comparison is based only on a case-insensitive compare of the column's 
    * name.  Only columns from the specified source tables are candidates to be
    * propagated and only specified target tables are updated.
    * 
    * @param aSourceTables          the specified source tables 
    * @param aTargetTables          the specified target tables
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    *  
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTables(com.sas.etl.models.data.ITable[], com.sas.etl.models.data.ITable[], int)
    */
   public void propagateColumnsToTargetTables( ITable[] aSourceTables, ITable[] aTargetTables, int eNonWorkTableHandling )
   {
      propagateColumnsImpl( aSourceTables, aTargetTables, getSourceColumnsExcludedFromPropagation(), eNonWorkTableHandling, true );
   }

   /**
    * Implementation of propagate columns.
    * 
    * @param aSourceTables          the tables that are the source of the columns
    * @param aTargetTables          the tables that may have columns added to them
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    * @param bForward               true = source tables are really sources to the transformation
    */
   private void propagateColumnsImpl( ITable[] aSourceTables, ITable[] aTargetTables, List lExcludedColumns, int eNonWorkTableHandling, boolean bForward )
   {
      startCompoundUndoable();
      try
      {
         // for each target table, ...
         for ( int iTargetTable=0; iTargetTable<aTargetTables.length; iTargetTable++ )
         {
            ITable tblTarget = aTargetTables[iTargetTable];
            
            // if the target table is not a work table
            if (!(tblTarget instanceof IWorkTable))
            {
               if (eNonWorkTableHandling == NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES)
                  continue;
               
               if (eNonWorkTableHandling == MAP_TO_NON_WORK_TABLES)
               {
                  for ( int iSourceTable=0; iSourceTable<aSourceTables.length; iSourceTable++ )
                  {
                     if (bForward)
                        mapColumns( aSourceTables[iSourceTable], tblTarget );
                     else
                        mapColumns( tblTarget, aSourceTables[iSourceTable] );
                  }
                  continue;
               }
            }
            
            // for each source table, ...
            for ( int iSourceTable=0; iSourceTable<aSourceTables.length; iSourceTable++ )
               propagateColumnsImpl( aSourceTables[iSourceTable].getColumns(), tblTarget, lExcludedColumns, bForward );
         } // each target table
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Implementation of propagate columns.
    * 
    * @param aSourceColumns   the tables that are the source of the columns
    * @param tblTarget        the target table that may have columns added to them
    * @param lExcludedColumns the columns to be excluded from propagating
    * @param bForward         true = source tables are really sources to the transformation
    * 
    * @return the columns created by the propagation
    */
   protected IColumn[] propagateColumnsImpl( IColumn[] aSourceColumns, ITable tblTarget, List lExcludedColumns, boolean bForward )
   {
      IObjectFactory factory = getObjectFactory();    

      IColumn[] aSource = new IColumn[1];
      IColumn[] aTarget = new IColumn[1];
      
      startCompoundUndoable();
      try
      {
         // get the columns in the target table
         List lNewTargetColumns = new ArrayList();
         IColumn[] aTargetColumns = tblTarget.getColumns();
         
         boolean bCaseSensitive = tblTarget.isCaseSensitive();
         
         // for each source column, ...
         for ( int iColumn=0; iColumn<aSourceColumns.length; iColumn++ )
         {
            IColumn colSource = aSourceColumns[iColumn];
            
            // if column is excluded from propagation, skip it
            if (lExcludedColumns.contains( colSource ))
               continue;
            
            // if the name exists in the target columns, skip it
            if (doesNameExistInColumns( colSource.getName(), aTargetColumns, bCaseSensitive ))
               continue;

            // if mapping from targets to sources and the target (colSource) 
            // already has a mapping, punt
            if (!bForward && (getOrdinaryMappingsForTargetColumn( colSource ) != null))
               continue;
            
            // create a new target column
            IColumn colTarget = factory.createNewColumn( tblTarget.getID() );
            aSourceColumns[iColumn].deepCopy( colTarget );
            tblTarget         .addColumn( colTarget );
            lNewTargetColumns .add(       colTarget );      // add new target column to list of new target columns      
            
            // create a new mapping
            aSource[0] = colSource;
            aTarget[0] = colTarget;
            if (bForward)
               addMapping( aSource, aTarget, IMapping.ONE_TO_ONE, null );
            else
               addMapping( aTarget, aSource, IMapping.ONE_TO_ONE, null );
         } // each source column
         
         return (IColumn[]) lNewTargetColumns.toArray( new IColumn[ lNewTargetColumns.size() ] );
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Propagates the specified columns to a source table.  Any specified column 
    * that does not exist in the specified source table is copied to the source 
    * table and a mapping is created between the source and target columns.  The 
    * comparison is based only on a compare of the column's name.  The 
    * comparison is case sensitive if the source table allows case sensitive 
    * column names.  Non-work tables are updated.
    *   
    * @param tblSource        the specified source table 
    * @param aTargetColumns   the specified target columns
    * 
    * @return the columns added to the source table
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTable(com.sas.etl.models.data.ITable, com.sas.etl.models.data.IColumn[])
    */
   public IColumn[] propagateColumnsToSourceTable( ITable tblSource, IColumn[] aTargetColumns )
   {
      return propagateColumnsToSourceTable( tblSource, aTargetColumns, PROPAGATE_INTO_NON_WORK_TABLES );
   }

   /**
    * Propagates the specified columns to a source table.  Any specified column 
    * that does not exist in the specified source table is copied to the source 
    * table and a mapping is created between the source and target columns.  The 
    * comparison is based only on a compare of the column's name.  The 
    * comparison is case sensitive if the source table allows case sensitive 
    * column names.
    * 
    * @param tblSource              the specified source table 
    * @param aTargetColumns         the specified target columns
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    * 
    * @return the columns added to the source table
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToSourceTable(com.sas.etl.models.data.ITable, com.sas.etl.models.data.IColumn[], int)
    */
   public IColumn[] propagateColumnsToSourceTable( ITable tblSource, IColumn[] aTargetColumns, int eNonWorkTableHandling )
   {
      // if the source table is not a work table, ...
      if (!(tblSource instanceof IWorkTable))
      {
         if (eNonWorkTableHandling == NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES)
            return new IColumn[0];
         
         if (eNonWorkTableHandling == MAP_TO_NON_WORK_TABLES)
         {
            mapColumns( tblSource.getColumns(), aTargetColumns );
            return new IColumn[0];
         }
      }
      
      return propagateColumnsImpl( aTargetColumns, tblSource, new ArrayList(), false );
   }

   /**
    * Propagates the specified columns to a target table.  Any specified column 
    * that does not exist in the specified target table is copied to the target 
    * table and a mapping is created between the source and target columns.  The 
    * comparison is based only on a compare of the column's name.  The 
    * comparison is case sensitive if the target table allows case sensitive 
    * column names.  Non-work tables are updated.
    *   
    * @param aSourceColumns   the specified source columns
    * @param tblTarget        the specified target table 
    * 
    * @return the columns added to the target table
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTable(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.ITable)
    */
   public IColumn[] propagateColumnsToTargetTable( IColumn[] aSourceColumns, ITable tblTarget )
   {
      return propagateColumnsToTargetTable( aSourceColumns, tblTarget, PROPAGATE_INTO_NON_WORK_TABLES );
   }

   /**
    * Propagates the specified columns to a target table.  Any specified column 
    * that does not exist in the specified target table is copied to the target 
    * table and a mapping is created between the source and target columns.  The 
    * comparison is based only on a compare of the column's name.  The 
    * comparison is case sensitive if the target table allows case sensitive 
    * column names.
    *   
    * @param aSourceColumns         the specified source columns
    * @param tblTarget              the specified target table 
    * @param eNonWorkTableHandling  defines how non-work tables should be 
    *                               handled.  Valid values are 
    *                               PROPAGATE_INTO_NON_WORK_TABLES,
    *                               MAP_TO_NON_WORK_TABLES, or 
    *                               NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES.
    * 
    * @return the columns added to the source table
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#propagateColumnsToTargetTable(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.ITable, int)
    */
   public IColumn[] propagateColumnsToTargetTable( IColumn[] aSourceColumns, ITable tblTarget, int eNonWorkTableHandling )
   {
      // if the target table is not a work table
      if (!(tblTarget instanceof IWorkTable))
      {
         if (eNonWorkTableHandling == NO_MAPPING_OR_PROPAGATING_TO_NON_WORK_TABLES)
            return new IColumn[0];
         
         if (eNonWorkTableHandling == MAP_TO_NON_WORK_TABLES)
         {
            mapColumns( tblTarget.getColumns(), aSourceColumns );
            return new IColumn[0];
         }
      }
      
      return propagateColumnsImpl( aSourceColumns, tblTarget, new ArrayList(), true );
   }

   /**
    * Gets a list of all the source columns that have been excluded from 
    * propagation.
    * 
    * @return the source columns that have been excluded from propagation
    */
   private List getSourceColumnsExcludedFromPropagation()
   {
      List     lColumns = new ArrayList();
      ITable[] aTables  = getSourceTables();
      for ( int iTable=0; iTable<aTables.length; iTable++ )
      {
         IDataTransform[] aProducers = aTables[iTable].getProducerTransforms();
         for ( int iProducer=0; iProducer<aProducers.length; iProducer++ )
            lColumns.addAll( aProducers[iProducer].getListOfColumnsExcludedFromPropagation() );
      }
      return lColumns;
   }
   
   /**
    * Gets a list of all the source columns that have been excluded from 
    * mapping.
    * 
    * @return the source columns that have been excluded from propagation
    */
   protected final List getSourceColumnsExcludedFromMapping()
   {
      List     lColumns = new ArrayList();
      ITable[] aTables  = getSourceTables();
      for ( int iTable=0; iTable<aTables.length; iTable++ )
      {
         IDataTransform[] aProducers = aTables[iTable].getProducerTransforms();
         for ( int iProducer=0; iProducer<aProducers.length; iProducer++ )
            lColumns.addAll( aProducers[iProducer].getListOfColumnsExcludedFromMapping() );
      }
      return lColumns;
   }
   
   /**
    * Does the name exist in the list of columns?
    * 
    * @param sName            the name
    * @param aColumns         the columns
    * @param bCaseSensitive   true = comparison is case sensitive
    * 
    * @return true = the name exists
    */
   private boolean doesNameExistInColumns( String sName, IColumn[] aColumns, boolean bCaseSensitive )
   {
      for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
      {
         String sColumnName = aColumns[iColumn].getName();
         boolean bEquals = bCaseSensitive ? sColumnName.equals(           sName ) 
                                          : sColumnName.equalsIgnoreCase( sName );
         if (bEquals)
            return true;
      }
      
      return false;
   }
   
//-------

   /**
    * Finds the mapping for the target column.
    * 
    * @param colTarget the target column
    * 
    * @return the mapping (null = no mapping found)
    */
   protected final IMapping findTargetMapping( IColumn colTarget )
   {
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping  mapping  = (IMapping) lMappings.get( iMapping );
         IColumn[] aTargets = mapping.getTargets();
         if ((aTargets.length == 1) && (aTargets[0] == colTarget))
            return mapping;
      }
      
      return null;
   }
   
   /**
    * Replaces a target table with a new target table.  Replacing the target table means
    * removing all its columns from the mappings and replacing with new target table trying to keep in place
    * as many derived mappings as possible the removing the old table and adding the new table.
    * Replacing any other column related transform specifics.
    * @param oldTable the target table to replace
    * @param newTable the new target table to replace with
    */
   public void replaceTargetTable( ITable oldTable, ITable newTable )
   {
      replaceTargetTable(oldTable, newTable, null, null);
   }
   
   /**
    * Replaces a target table with a new target table.  Replacing the target table means
    * removing all its columns from the mappings and replacing with new target table trying to keep in place
    * as many derived mappings as possible the removing the old table and adding the new table.
    * Replacing any other column related transform specifics.
    * @param oldTable the target table to replace
    * @param newTable the new target table to replace with
    */
   public void replaceTargetTable( ITable oldTable, ITable newTable, Integer[] portIndexes )
   {
	   replaceTargetTable(oldTable, newTable, null, portIndexes);
   }
   
   /**
    * Replaces a target table with a new target table.  Replacing the target table means
    * removing all its columns from the mappings and replacing with new target table trying to keep in place
    * as many derived mappings as possible then removing the table and adding the new table
    * Replacing any other column related transform specifics.
    * @param oldTable the target table to replace
    * @param newTable the new target table to replace with
    * @param columnsMap the map of oldTable column to newTable column
    */
   public void replaceTargetTable( ITable oldTable, ITable newTable, Map columnsMap, Integer[] portIndexes )
   {
      replaceTargetTable( oldTable, newTable, columnsMap );
   }
   
   /**
    * Replaces a target table with a new target table.  Replacing the target table means
    * removing all its columns from the mappings and replacing with new target table trying to keep in place
    * as many derived mappings as possible then removing the table and adding the new table
    * Replacing any other column related transform specifics.
    * @param oldTable the target table to replace
    * @param newTable the new target table to replace with
    * @param columnsMap the map of oldTable column to newTable column
    */
   public void replaceTargetTable( ITable oldTable, ITable newTable, Map columnsMap )
   {
      startCompoundUndoable();
      try
      {
      	preReplaceTargetTable( oldTable,  newTable,  columnsMap );
      	
         replaceTargetMappings( oldTable, newTable, columnsMap );

         replaceTargetTableOptions(oldTable, newTable, columnsMap );
         
         removeDataTarget(oldTable);
         addDataTarget(newTable);
         
      	postReplaceTargetTable( oldTable,  newTable,  columnsMap );

      }   
      finally
      {
         endCompoundUndoable();
      }
   }
   
   protected void preReplaceTargetTable(ITable oldTable, ITable newTable, Map columnsMap )
   {
   	
   }

   protected void postReplaceTargetTable(ITable oldTable, ITable newTable, Map columnsMap )
   {
   	
   }

   protected void replaceTargetMappings(ITable oldTable, ITable newTable, Map columnsMap)
   {
      startCompoundUndoable();
      try
      {
         List lChangedMappings = new ArrayList(); 
         IMapping[] aMappings = getMappings();
         IColumn[] aNewColumns = newTable.getColumns();
         for (int iMap=0; iMap<aMappings.length; iMap++)
         {  
            IMapping mapping = aMappings[iMap];
            IColumn[] aColumns = mapping.getTargets();
            for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
            {
               IColumn column = aColumns[iColumn];
               if (columnsMap != null && (IColumn)columnsMap.get(aColumns[ iColumn ]) != null)
               {
                  mapping.replaceTargetColumn(column, (IColumn)columnsMap.get(aColumns[ iColumn ] ));
                  lChangedMappings.add( mapping);
               }
               else
               {
                  if (!oldTable.containsColumn(column ))
                  {
                     // S0467854: do this to make sure the remove below won't remove this mapping because it belongs to a different
                     //  table
                     lChangedMappings.add(mapping);
                     continue;
                  }
                  boolean bCaseSenistive = isQuotingNeeded() || newTable.isQuoted();
                  for ( int iNewColumn=0; iNewColumn<aNewColumns.length; iNewColumn++ )
                  {
                     IColumn newColumn = aNewColumns[iNewColumn];
                     
                     if ( column.equalsName(newColumn, bCaseSenistive) &&
                              column.getLength() >= newColumn.getLength() &&
                              column.getType() == newColumn.getType())
                     {
                        mapping.replaceTargetColumn(column, newColumn);
                        lChangedMappings.add( mapping);
                        break;
                     }
                  }
               }
            }
         }
         //clean up all one to one mappings not changed
         for (int iMap=0; iMap<aMappings.length; iMap++)
         {  
            IMapping mapping = aMappings[iMap];
            if (lChangedMappings.indexOf(mapping) == -1 && mapping.getType() == IMapping.ONE_TO_ONE)
               removeMapping(mapping); 
         }
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Replaces a source table with a new source table.  Replacing the source table means
    * removing all its columns from the mappings and replacing with new source table trying to keep in place
    * as many derived mappings as possible then removing the old table and adding the new table.
    * Replacing any other column related transform specifics. 
    * @param oldTable the source table to replace
    * @param newTable the new source table to replace with
    */
   public void replaceSourceTable( ITable oldTable, ITable newTable, Integer[] portIndexes )
   {
	  replaceSourceTable(oldTable, newTable, null, portIndexes);   
   }
   
   /**
    * Replaces a source table with a new source table.  Replacing the source table means
    * removing all its columns from the mappings and replacing with new source table trying to keep in place
    * as many derived mappings as possible then removing the old table and adding the new table.
    * Replacing any other column related transform specifics.
    * @param oldTable the source table to replace
    * @param newTable the new source table to replace with
    * @param columnsMap the map of oldTable column to newTable column
    */
   public void replaceSourceTable( ITable oldTable, ITable newTable, Map columnsMap, Integer[] portIndexes)
   {
      replaceSourceTable( oldTable, newTable, columnsMap );
   }
   
   /**
    * Replaces a source table with a new source table.  Replacing the source table means
    * removing all its columns from the mappings and replacing with new source table trying to keep in place
    * as many derived mappings as possible then removing the old table and adding the new table.
    * Replacing any other column related transform specifics.
    * @param oldTable the source table to replace
    * @param newTable the new source table to replace with
    * @param columnsMap the map of oldTable column to newTable column
    */
   public void replaceSourceTable( ITable oldTable, ITable newTable, Map columnsMap)
   {
      startCompoundUndoable();
      try
      {
         replaceSourceMappings( oldTable, newTable, columnsMap );
         
         replaceSourceTableOptions(oldTable, newTable, columnsMap );
         
         removeDataSource(oldTable);
         removeConnectedSource(oldTable);
         addDataSource(newTable);
         addConnectedSource(newTable);
      }   
      finally
      {
         endCompoundUndoable();
      }
   }
   
   protected void replaceSourceTableOptions(ITable oldTable, ITable newTable, Map columnsMap)
   {
      try
      {
         IPromptModel optionModel = getOptionModel();
         optionModel.replaceSourceTable(oldTable,newTable,columnsMap, isQuotingNeeded());
      }
      catch (FileNotFoundException ex)
      {
         ModelLogger.getDefaultLogger().error( "FileNotFoundException", ex );
      }
      catch (ServiceException ex)
      {
         ModelLogger.getDefaultLogger().error( "ServiceException", ex );
      }
      catch (ServerConnectionException ex)
      {
         ModelLogger.getDefaultLogger().error( "ServerConnectionException", ex );
      }
      catch (IOException ex)
      {
         ModelLogger.getDefaultLogger().error( "IOException", ex );
      }
      catch (ParserConfigurationException ex)
      {
         ModelLogger.getDefaultLogger().error( "ParserConfigurationException", ex );
      }
      catch (SAXException ex)
      {
         ModelLogger.getDefaultLogger().error( "SAXException", ex );
      }
      catch (MdException ex)
      {
         ModelLogger.getDefaultLogger().error( "MdException", ex );
      }
   }
   
   protected void replaceTargetTableOptions(ITable oldTable, ITable newTable, Map columnsMap)
   {
      try
      {
         IPromptModel optionModel = getOptionModel();
         optionModel.replaceTargetTable(oldTable,newTable,columnsMap, isQuotingNeeded());
      }
      catch (FileNotFoundException ex)
      {
         ModelLogger.getDefaultLogger().error( "FileNotFoundException", ex );
      }
      catch (ServiceException ex)
      {
         ModelLogger.getDefaultLogger().error( "ServiceException", ex );
      }
      catch (ServerConnectionException ex)
      {
         ModelLogger.getDefaultLogger().error( "ServerConnectionException", ex );
      }
      catch (IOException ex)
      {
         ModelLogger.getDefaultLogger().error( "IOException", ex );
      }
      catch (ParserConfigurationException ex)
      {
         ModelLogger.getDefaultLogger().error( "ParserConfigurationException", ex );
      }
      catch (SAXException ex)
      {
         ModelLogger.getDefaultLogger().error( "SAXException", ex );
      }
      catch (MdException ex)
      {
         ModelLogger.getDefaultLogger().error( "MdException", ex );
      }
   }
   
   protected void replaceSourceMappings(ITable oldTable, ITable newTable, Map columnsMap)
   {
      startCompoundUndoable();
      try
      {
         List lChangedMappings = new ArrayList();
         IMapping[] aMappings = getMappings();
         IColumn[] aNewColumns = newTable.getColumns();
         for (int iMap=0; iMap<aMappings.length; iMap++)
         {  
            IMapping mapping = aMappings[iMap];
            IColumn[] aColumns = mapping.getSources();
            for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
            {
               IColumn column = aColumns[iColumn];
               if (columnsMap != null && (IColumn)columnsMap.get(aColumns[ iColumn ]) != null)
               {
                  mapping.replaceSourceColumn(column, (IColumn)columnsMap.get(aColumns[ iColumn ] ));
                  lChangedMappings.add( mapping);
               }
               else
               {
                  if (!oldTable.containsColumn(column))
                  {
                     // S0467854: do this to make sure the remove below won't remove this mapping because it belongs to a different
                     //  table
                     lChangedMappings.add(mapping);
                     continue;
                  }
                  boolean bCaseSenistive = isQuotingNeeded() || newTable.isQuoted();
                  for ( int iNewColumn=0; iNewColumn<aNewColumns.length; iNewColumn++ )
                  {
                     IColumn newColumn = aNewColumns[iNewColumn];
                     if ( column.equalsName(newColumn, bCaseSenistive) &&
                              column.getLength() >= newColumn.getLength() &&
                              column.getType() == newColumn.getType() )
                     {
                        mapping.replaceSourceColumn(column, newColumn);
                        lChangedMappings.add(mapping);
                        break;
                     }
                  }
               }
            }
         }
         //clean up all one to one mappings not changed
         for (int iMap=0; iMap<aMappings.length; iMap++)
         {  
            IMapping mapping = aMappings[iMap];
            if (lChangedMappings.indexOf(mapping) == -1 && mapping.getType() == IMapping.ONE_TO_ONE)
               removeMapping(mapping); 
         }         
      }   
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Replaces old source columns in the mappings with new source columns.  This
    * is in preparation of replacing one of the source tables with a new source
    * table.
    * 
    *  @param aOldColumns the old source columns
    *  @param aNewColumns the new source columns
    */
   public void replaceSourceColumns( IColumn[] aOldColumns, IColumn[] aNewColumns )
   {
      startCompoundUndoable();
      try
      {
         List lMappings = getMappingsList();
         for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
         {
            IMapping mapping = (IMapping) lMappings.get( iMapping );
            for ( int iColumn=0; iColumn<aOldColumns.length; iColumn++ )
            {
               int iWhere = mapping.indexInSources( aOldColumns[iColumn] );
               if (iWhere == -1)
                  continue;
   
               // replace the source column
               mapping.replaceSourceColumn( aOldColumns[iColumn], aNewColumns[iColumn]);
            }
         }
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Replaces old target columns in the mappings with new target columns.  This
    * is in preparation of replacing one of the target tables with a new target
    * table.
    * 
    *  @param aOldColumns the old target columns
    *  @param aNewColumns the new target columns
    */
   public void replaceTargetColumns( IColumn[] aOldColumns, IColumn[] aNewColumns )
   {
      startCompoundUndoable();
      try
      {
         List lMappings = getMappingsList();
         for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
         {
            IMapping mapping = (IMapping) lMappings.get( iMapping );
            for ( int iColumn=0; iColumn<aOldColumns.length; iColumn++ )
            {
               int iWhere = mapping.indexInTargets( aOldColumns[iColumn] );
               if (iWhere == -1)
                  continue;
   
               // replace the target column
               mapping.replaceTargetColumn(aOldColumns[iColumn], aNewColumns[iColumn]);
            }
         }
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Removes a source table from the mappings.  Removing the source table means
    * removing all its columns from the mappings.
    * 
    * @param tbl the source table
    */
   public void removeSourceTableFromMappings( ITable tbl )
   {
      startCompoundUndoable();
      try
      {
         IColumn[] aColumns = tbl.getColumns();
         for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
            removeSourceColumnFromMappings( aColumns[ iColumn ] );
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Removes a source column from the mappings.  If the mapping is one to one,
    * the mapping is removed.  Otherwise, the reference to the column is removed
    * from the mapping.
    * 
    * @param column the source column
    */
   public void removeSourceColumnFromMappings( IColumn column )
   {
      startCompoundUndoable();
      try
      {
         IMapping[] aMappings = getMappings();
         for ( int iMapping=0; iMapping<aMappings.length; iMapping++ )
         {
            IMapping mapping = aMappings[ iMapping ];
            if (mapping.containsInSources( column ))
            {
               mapping.removeSource( column );
//               if (mapping.getType().equals( IMapping.ONE_TO_ONE ) &&
//                   (mapping.getSourceCount() == 0))
//                  removeMapping( mapping );
            }
         }
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Removes a target table from the mappings.  Removing the target table means
    * removing all its columns from the mappings.
    * 
    * @param tbl the target table
    */
   public void removeTargetTableFromMappings( ITable tbl )
   {
      startCompoundUndoable();
      try
      {
         IColumn[] aColumns = tbl.getColumns();
         for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
            removeTargetColumnFromMappings( aColumns[ iColumn ] );
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Removes a target column from the mappings.  If the mapping is one to one,
    * the mapping is removed.  Otherwise, the reference to the column is removed
    * from the mapping.
    * 
    * @param column the target column
    */
   public void removeTargetColumnFromMappings( IColumn column )
   {
      startCompoundUndoable();
      try
      {
         IMapping[] aMappings = getMappings();
         for ( int iMapping=0; iMapping<aMappings.length; iMapping++ )
         {
            IMapping mapping = aMappings[ iMapping ];
            if (mapping.containsInTargets( column ))
            {
               if (mapping.getType() == IMapping.ONE_TO_ONE)
                  removeMapping( mapping );
               else
                  mapping.removeTarget( column );
            }
         }
      }
      finally
      {
         endCompoundUndoable();
      }
   }
   
   /**
    * Gets the mappings in the container.
    * 
    * @return the mappings
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#getMappings()
    */
   public IMapping[] getMappings()
   {
      List lMappings = getMappingsList();
      return (IMapping[]) lMappings.toArray( new IMapping[ lMappings.size() ] );
   }
   
   /**
    * Get the list of mappings
    * @return mapping list
    * @see com.sas.etl.models.job.IMappingsContainer#getMappingsList()
    */
   public List getMappingsList()
   {
      return m_lMappings;
   }
   
   /**
    * Gets the ordinary mappings in the container.
    * 
    * @return the ordinary mappings
    * 
    * @see com.sas.etl.models.job.IMapping#isOrdinary()
    * @see com.sas.etl.models.job.IMappingsContainer#getOrdinaryMappings()
    */
   public IMapping[] getOrdinaryMappings()
   {
      List lOrdinaryMappings = new ArrayList();
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (mapping.isOrdinary())
            lOrdinaryMappings.add( mapping );
      }
      
      return (IMapping[]) lOrdinaryMappings.toArray( new IMapping[ lOrdinaryMappings.size() ] );
   }
   
   /**
    * Gets the ordinary mappings in the container.
    * 
    * @return the ordinary mappings
    * 
    * @see com.sas.etl.models.job.IMapping#isSpecial()
    * @see com.sas.etl.models.job.IMappingsContainer#getSpecialMappings()
    */
   public IMapping[] getSpecialMappings()
   {
      List lSpecialMappings = new ArrayList();
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (mapping.isSpecial())
            lSpecialMappings.add( mapping );
      }
      
      return (IMapping[]) lSpecialMappings.toArray( new IMapping[ lSpecialMappings.size() ] );
   }
   
   /**
    * Gets the ordinary mappings in the mappings container that have the 
    * specified column as a source column.  
    * 
    * @param source the source column
    * 
    * @return the ordinary mappings
    * 
    * @see com.sas.etl.models.job.IMapping#isOrdinary()
    * @see com.sas.etl.models.job.IMappingsContainer#getOrdinaryMappingsForSourceColumn(IColumn)
    */
   public IMapping[] getOrdinaryMappingsForSourceColumn( IColumn source )
   {
      List lOrdinaryMappings = new ArrayList();
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (mapping.isOrdinary() && mapping.containsInSources( source ))
            lOrdinaryMappings.add( mapping );
      }
      
      return (IMapping[]) lOrdinaryMappings.toArray( new IMapping[ lOrdinaryMappings.size() ] );
   }
   
   /**
    * Gets the ordinary mapping in the mappings container that has the specified 
    * column as a target column.  It is assumed that the there is only one 
    * ordinary mapping in the container for a given target column.
    * 
    * @param target the target column
    * 
    * @return the ordinary mapping (null = no ordinary mapping
    * 
    * @see com.sas.etl.models.job.IMapping#isOrdinary()
    * @see com.sas.etl.models.job.IMappingsContainer#getOrdinaryMappingsForTargetColumn(IColumn)
    */
   public IMapping getOrdinaryMappingsForTargetColumn( IColumn target )
   {
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping < lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (mapping.isOrdinary() && mapping.containsInTargets( target ))
            return mapping;
      }
      
      return null;
   }

   /**
    * Clear out the list of mappings.
    */
   protected void clearMappingsBeforeLoad()
   {
      List lMappings = getMappingsList();
      lMappings.clear();
   }

   /**
    * Is an expression needed for a mapping between columns?  An expression is
    * needed if there is more than one source column, or if the type of the 
    * source and target columns do not match or if the target is shorter than 
    * the source.
    * 
    * @param aSources the source columns
    * @param aTargets the target columns
    * 
    * @return true = an expression is needed
    */
   protected boolean isExpressionNeeded( IColumn[] aSources, IColumn[] aTargets )
   {
      // if there is not one source or not one target, then an expression is required
      if ((aSources.length != 1) || (aTargets.length != 1))
         return true;
      
      // if the source and target are not of the same type or 
      // if the source is larger than the target, an expression is required
      return (aSources[0].getType()   != aTargets[0].getType()); //||
             //(aSources[0].getLength() >  aTargets[0].getLength());
   }
   
   /**
    * Create a new mapping.
    * 
    * @param sType the type of mapping
    * 
    * @return the new mapping
    */
   protected IMapping createNewMapping()
   {
      return getModel().getObjectFactory().createNewMapping( getID() );
   }
   
   /**
    * Adds a mapping using the default mapping type between the specified source 
    * columns and the specified target columns.  If a mapping is created between
    * one source and one target, the mapping rules are applied to find out if an
    * expression exists between the mapping rules.
    *  
    * @param aSources the source columns
    * @param aTargets the target columns
    * 
    * @return the new mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#addMapping(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[])
    */
   public IMapping addMapping( IColumn[] aSources, IColumn[] aTargets )
   {
      // if there is one source column and there is one target column, ... 
      if ((aSources.length == 1) && 
          (aTargets.length == 1))
      {
         
         // see if any of the rules apply, if they do create a mapping using the rules
         IMappingRule[] aRules = DefaultMappingRules.getRules();
         for ( int iRule=0; iRule<aRules.length; iRule++ )
         {
            if (aRules[iRule].canMap( aSources[0], aTargets[0] ))
            {
               String sExpression = aRules[iRule].getExpressionText();
               if ((sExpression != null) && (sExpression.length() > 0))
               {
                  // punt if derived mappings are not supported 
                  if (!areExpressionsAllowed())
                     continue;
                  
                  ITextExpression expression = getModel().getObjectFactory().createNewTextExpression( getID() );
                  expression.setText( sExpression, new IObject[]{ aSources[0], aSources[0].getTable() } );
                  return addMapping( aSources, aTargets, IMapping.DERIVED, expression );
               }
               
               // in case there is a rule that makes a one to one mapping before
               // a rule that creates a derived mapping, use that rule
               else
                  return addMapping( aSources, aTargets, IMapping.ONE_TO_ONE, null );
            }
         }
      }

      // no rule, applied so see if an expression is needed
      // if one is needed, then create a derived mapping
      String sType = isExpressionNeeded( aSources, aTargets ) ? IMapping.DERIVED : IMapping.ONE_TO_ONE; 
      return addMapping( aSources, aTargets, sType, null );
   }
   
   /**
    * Adds a mapping of the specified type for the specified source and 
    * target columns using the specified expresion. 
    *  
    * @param aSourceColumns the source columns
    * @param aTargetColumns the target columns
    * @param sType          the mapping type (see IMapping)
    * @param oExpression     the expression
    * 
    * @return the new mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#addMapping(com.sas.etl.models.data.IColumn[], com.sas.etl.models.data.IColumn[], String, com.sas.etl.models.job.IExpression)
    */
   public IMapping addMapping( IColumn[] aSourceColumns, IColumn[] aTargetColumns, String sType, IExpression oExpression )
   {
      startCompoundUndoable();
      try
      {
         IMapping mapping = createNewMapping();
         mapping.setName( (aTargetColumns.length==1) ? aTargetColumns[0].getName() : "newmapping" );
         mapping.setType( sType );

         mapping.setExpression( oExpression );
   
         for ( int iSourceColumn=0; iSourceColumn<aSourceColumns.length; iSourceColumn++ )
            mapping.addSource( aSourceColumns[iSourceColumn] );
         for ( int iTargetColumn=0; iTargetColumn<aTargetColumns.length; iTargetColumn++ )
            mapping.addTarget( aTargetColumns[iTargetColumn] );
   
         if (mapping.isOrdinary() && (mapping instanceof BaseMapping))
         {
            BaseMapping ordinary = (BaseMapping) mapping;
            ordinary.setAutoType( true );
            ordinary.setExpressionAllowed( areExpressionsAllowed() );
         }

         addMapping( mapping );
         return mapping;
      }
      finally
      {
         endCompoundUndoable();
      }
   }

   /**
    * Adds the mapping to the mapping container.
    * 
    * @param mapping the mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#addMapping(com.sas.etl.models.job.IMapping)
    */
   public void addMapping( IMapping mapping )
   {
      addMapping( m_lMappings.size(), mapping );
   }

   /**
    * Adds the mapping to the mapping container
    * 
    * @param iMapping the index of where the mapping is to be inserted
    * @param mapping  the mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#addMapping(int, com.sas.etl.models.job.IMapping)
    */
   public void addMapping( int iMapping, IMapping mapping )
   {
      removeFromDeletedObjects( mapping );
      m_lMappings.add( iMapping, mapping );
      fireModelChangedEvent( MAPPING_ADDED, mapping );
      if (isUndoSupported())
         undoableEditHappened( new AddMappingUndoable( iMapping, mapping ) );
      mapping.addNotifyListener( this );
   }

   /**
    * Removes a mapping from the mapping container.
    * 
    * @param mapping the mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#removeMapping(com.sas.etl.models.job.IMapping)
    */
   public void removeMapping( IMapping mapping )
   {
      int iMapping = m_lMappings.indexOf( mapping );
      if (iMapping == -1)
         return;  // mappings can be automatically removed
      addToDeletedObjects( mapping );
      m_lMappings.remove( iMapping );
      fireModelChangedEvent( MAPPING_REMOVED, mapping );
      if (isUndoSupported())
         undoableEditHappened( new RemoveMappingUndoable( iMapping, mapping ) );
      mapping.removeNotifyListener( this );
   }
   
   /**
    * Does the transform contain the mapping?
    * 
    * @param mapping the mapping
    * 
    * @return true = the transform contains the mapping
    * 
    * @see com.sas.etl.models.job.IMappingsContainer#containsMapping(com.sas.etl.models.job.IMapping)
    */
   public boolean containsMapping( IMapping mapping )
   {
      return m_lMappings.contains( mapping );
   }

   /**
    * Gets the columns that are excluded from mapping operations.  The mapping
    * operations are the actions that cause multiple columns to be mapped or
    * auto mapping.  A mapping can still be created to the column by explicitly
    * creating the mapping.
    * 
    * @return the columns excluded from mapping operations
    * 
    * @see com.sas.etl.models.job.IDataTransform#getColumnsExcludedFromMapping()
    */
   public IColumn[] getColumnsExcludedFromMapping()
   {
      return (IColumn[]) m_lColumnsExcludedFromMapping.toArray( new IColumn[ m_lColumnsExcludedFromMapping.size() ] );
   }

   /**
    * Gets the list of columns that are excluded from mapping operations.  The 
    * mapping operations are the actions that cause multiple columns to be 
    * mapped or auto mapping.  A mapping can still be created to the column by 
    * explicitly creating the mapping.  This list can be modified directly to
    * modify the columns excluded. 
    * 
    * @return the list of columns excluded from mapping operations
    * 
    * @see com.sas.etl.models.job.IDataTransform#getListOfColumnsExcludedFromMapping()
    */
   public List getListOfColumnsExcludedFromMapping()
   {
      return m_lColumnsExcludedFromMapping;
   }
   
   /**
    * Gets the columns that are excluded from propagation operations.  The 
    * propagation operations are the actions that cause multiple columns to be 
    * propagated or auto propagation.  
    * 
    * @return the columns excluded from propagation operations
    * 
    * @see com.sas.etl.models.job.IDataTransform#getColumnsExcludedFromPropagation()
    */
   public IColumn[] getColumnsExcludedFromPropagation()
   {
      return (IColumn[]) m_lColumnsExcludedFromPropagation.toArray( new IColumn[ m_lColumnsExcludedFromPropagation.size() ] );
   }
   
   /**
    * Gets the list of columns that are excluded from propagation operations.  
    * The propagation operations are the actions that cause multiple columns to 
    * be propagated or auto propagation.  This list can be modified directly to
    * modify the columns excluded.   
    * 
    * @return the list of columns excluded from propagation operations
    * 
    * @see com.sas.etl.models.job.IDataTransform#getListOfColumnsExcludedFromPropagation()
    */
   public List getListOfColumnsExcludedFromPropagation()
   {
      return m_lColumnsExcludedFromPropagation;
   }
   
   /**
    * Handles notify events from other objects in the transformation being 
    * changed.
    * 
    * @param ev the notify event
    * 
    * @see com.sas.etl.models.impl.BaseObject#notify(com.sas.etl.models.NotifyEvent)
    */
   public void notify( NotifyEvent ev )
   {
      if ((ev.getType() == NotifyEvent.OBJECT_CHANGED) &&
          (ev.getSource() instanceof IMapping))
      {
         IMapping mapping = (IMapping) ev.getSource();
         if (mapping.isReplacing())
            fireModelChangedEvent( MAPPING_CHANGED, ev.getSource() );
         
         // if the mapping is dead, remove it
         else if (mapping.isDead())
            removeMapping( mapping );
         
         // otherwise, just fire a mapping changed event
         else
            fireModelChangedEvent( MAPPING_CHANGED, ev.getSource() );

         fireModelChangedEvent( TRANSFORM_CHANGED, mapping) ;
      }
      else if (ev.getSource() instanceof IDataObject)
      {
         String sType = "";
         if (ev.getModelEvent() != null)
            sType = ev.getModelEvent().getType();
         
         if (!(sType.equals(ITable.RESPONSIBLE_PARTY_ADDED)) && !(sType.equals(ITable.RESPONSIBLE_PARTY_REMOVED) ))
         {         
              IDataObject dataObject = (IDataObject) ev.getSource();
              if (getDataSourceList().contains( dataObject ) || 
                  getDataTargetList().contains( dataObject ) ) 
              {
                 fireModelChangedEvent( TRANSFORM_CHANGED, dataObject) ;
              }
         }
      }
      else
         super.notify( ev );
   }
   
   //---------------------------------------------------------------------------
   // Inputs and Outputs
   //---------------------------------------------------------------------------
   /**
    * Is add work table available
    * @return true = the object does support adding work tables
    * and more are allowed to be added
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isAddWorkTableAvailable()
   {
      return m_lDataTargets.isEmpty();
   }

   /**
    * Is add inputs available
    * and more are allowed to be added
    * @return true = the object does support adding inputs
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isAddInputAvailable()
   {
      return false;
   }
   
   /**
    * Override this if your transform wants to do special handling for job status
    * updates.  Loader transforms override this usually, otherwise most transforms dont
    * 
    * @return Default is TRUE
    */
   public boolean isGenDefaultJobStatusUpdate()
   {
      return true;
   }

   /**
    * Is add output available
    * and more are allowed to be added
    * @return true = the object does support adding outputs
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isAddOutputAvailable()
   {
      return false;
   }

   /**
    * Is delete inputs supported
    * and more are allowed to be deleted
    * @return true = the object does support deleting inputs
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isDeleteInputAvailable()
   {
      return false;
   }

   /**
    * Is delete output supported
    * and more are allowed to be deleted
    * @return true = the object does support deleting outputs
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isDeleteOutputAvailable()
   {
     return false;
   }

   /**
    * add input to transform which is triggered by the add input port
    */
   public void addInput()
   {
      throw new UnsupportedOperationException( "add input not supported" );
   }

   /**
    * add output to transform which s triggered by the add output port
    */
   public void addOutput()
   {
      throw new UnsupportedOperationException( "add output not supported" );
   }

   /**
    * delete input to transform which is triggered by the delete input port
    */
   public void deleteInput()
   {
      throw new UnsupportedOperationException( "delete input not supported" );
   }

   /**
    * delete output to transform which is triggered by the delete output port
    */
   public void deleteOutput()
   {
      throw new UnsupportedOperationException( "delete output not supported" );
   }
   
   //---------------------------------------------------------------------------
   // Completeness
   //---------------------------------------------------------------------------

   public boolean hasWarnings()
   {
      if (super.hasWarnings())
         return true;
      
      if (isSourceTargetTablesWarning() || hasMappingWarnings())
         return true;
      
      return false;
   }
   
   public List getWarnings()
   {
      List warnings = super.getWarnings();
      
      warnings.addAll( getSourceTargetsWarnings() );
      warnings.addAll( getMappingWarnings() );
      
      return warnings;
   }
   
   protected boolean hasMappingWarnings()
   {
      IMapping[] mappings = getOrdinaryMappings();
      for (int i=0; i<mappings.length; i++)
      {
         if (mappings[i].hasWarnings())
         {
            return true;
         }
      }
      return false;
   }
   
   protected List getMappingWarnings()
   {
      List warnings = new ArrayList();
      IMapping[] mappings = getOrdinaryMappings();
      for (int i=0; i<mappings.length; i++)
      {
         if (mappings[i].hasWarnings())
         {
            warnings.addAll(mappings[i].getWarnings());
         }
      }
      return warnings;
   }
   
   
   /**
    * Is the transform complete?  This method is overridden to return true only
    * if there is a least one source, one target, and one mapping.  If this is
    * not correct for a transform, the subclass must override the method.
    * 
    * @see com.sas.etl.models.IObject#isComplete()
    */
   public boolean isComplete()
   {
      return super.isComplete() && 
             (!doesNoSourcesMeanIncomplete()    || !getDataSourceList().isEmpty()           ) &&
             (!doesNoTargetsMeanIncomplete()    || !getDataTargetList().isEmpty()           ) &&
             (!doesNoMappingsMeanIncomplete()   || doesMappingExistOnAllTargetTables() ) &&
             areMappingsComplete() &&
             isSourceTargetTablesComplete()
             ;
   }
   
   protected boolean isMappingRequiredForTargetTable(ITable table)
   {
	   return true;
   }
   
   /**
    * Checks to make sure there is at least 1 target column has a single mapping.
    * @return true, if all target tables have at least one mapping, false, otherwise
    */
   protected boolean doesMappingExistOnAllTargetTables()
   {
      List lMappings = getMappingsList();
      if (lMappings.isEmpty())
         return false;
      
      List<ITable> targets = new ArrayList();
      for(ITable table : getTargetTables())
      {
    	  if (isMappingRequiredForTargetTable(table))
    		  targets.add(table);
      }
      
      ITable[] aTargetTables = targets.toArray(new ITable[targets.size()]);

      // -----------------------------------------------------------------------
      // This algorithm performed poorly, especially when deleting source 
      // columns.  Inside the call to getOrdinaryMappingsForTargetColumn is a 
      // loop through potentially all mappings to see if the target column is a 
      // target of a mapping.  If the target column was not in any mapping, all 
      // the mappings would be visited.  The algorithm is essentially a loop
      // within a loop.
      //
      // The worse case scenario would be a transformation for which the first
      // half of the target columns had no mappings and the last half did.  
      // Assuming n is half of the target columns, the algorithm would be n*n.
      // Put this algorithm in a loop deleting the same number of source columns
      // and the algorithm is on the order of n*n*n.
      // -----------------------------------------------------------------------
      
//      // need to check each of the target tables
//      for (int iTarget=0; iTarget < aTargetTables.length; iTarget++)
//      {
//         ITable table = aTargetTables[iTarget];
//         IColumn[] columns = table.getColumns();
//         boolean mapFound = false;
//         for (int iColumn=0; iColumn < columns.length; iColumn++)
//         {
//            IMapping map = getOrdinaryMappingsForTargetColumn( columns[iColumn] );
//            if (map != null)
//            {
//               mapFound = true;
//               break;
//            }
//         }
//         if (!mapFound)
//            return false;
//      }

      // -----------------------------------------------------------------------
      // The replacement algorithm goes through the mappings looking to if the
      // target column belongs to one of the target tables still in the list.
      // If it is, the target table is removed from the list.  If the list is
      // empty, then all target tables have a mapping and the algorithm is done.
      //
      // The best case scenario for this algorithm is the most common case: one
      // target table and the first mapping is an ordinary mapping.  In this 
      // case, one mapping is visited and the algorithm is exited which is the
      // same as the above algorithm.
      //
      // The worst case for this algorithm involves multiple target tables.  So
      // the worst case applies only to splitter and generated transformations
      // that have multiple targets.  The worst case scenario is all the columns
      // for all the target tables but the last one have a mapping and the 
      // mappings are first in the list.  Then all of the mappings will be 
      // visited except for the mappings for the last target table (note: the
      // first mapping for the last target table will be visited).  In this
      // scenario, the behavior of both algorithms are the same.
      //
      // Therefore, since the first algorithm's worst case is much worse than
      // the second algorithm's, we're using the second algorithm
      // -----------------------------------------------------------------------
      
      // put the tables in a list so that the algorithm can tell when a mapping
      // has been found for each of the tables
      List lTargetTables = new ArrayList( aTargetTables.length );
      for ( int iTargetTable = 0; iTargetTable < aTargetTables.length; iTargetTable++ )
         lTargetTables.add( aTargetTables[iTargetTable] );

      // for each of the mappings, ...
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         // if the mapping is not an ordinary mapping, skip to the next mapping
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (!mapping.isOrdinary())
            continue;
         
         // check each of the target columns for the mapping
         // remove the target column's table from the list of tables
         // if the table was sucessfully removed, the list might be empty
         // if the list of tables is empty, then a mapping for each table has
         // been found, so return success.
         IColumn[] aColumns = mapping.getTargets();
         for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
            if (lTargetTables.remove( aColumns[iColumn].getTable() ))
               if (lTargetTables.isEmpty())
                  return true;
      }

      return false;
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

      if (getDataSourceList().isEmpty() && doesNoSourcesMeanIncomplete())
         lReasons.add( RB.getStringResource( "AbstractDataTransform.ReasonIncomplete.NoSources.txt"  ) );
      if (getDataTargetList().isEmpty() && doesNoTargetsMeanIncomplete())
         lReasons.add( RB.getStringResource( "AbstractDataTransform.ReasonIncomplete.NoTargets.txt"  ) );
      if (!doesMappingExistOnAllTargetTables() && doesNoMappingsMeanIncomplete())
         lReasons.add( RB.getStringResource( "AbstractDataTransform.ReasonIncomplete.NoMappings.txt" ) );
      
      lReasons.addAll( getReasonsSourceTargetsIncomplete() );
      lReasons.addAll( getReasonsMappingsIncomplete() );
      
      return lReasons;
   }
   
   protected boolean isSourceTargetTablesWarning()
   {
      IDataObject[] sources = getDataSources();
      IDataObject[] targets = getDataTargets();
      
      for (int i=0; i<sources.length; i++)
      {
         if (sources[i].hasWarnings())
            return true;
      }   

      for (int i=0; i<targets.length; i++)
      {
         if (targets[i].hasWarnings())
            return true;
      }   

      return false;
   }
   
   protected boolean isSourceTargetTablesComplete()
   {
      IDataObject[] sources = getDataSources();
      IDataObject[] targets = getDataTargets();
      
      for (int i=0; i<sources.length; i++)
      {
         if (!sources[i].isComplete())
            return false;
      }   

      for (int i=0; i<targets.length; i++)
      {
         if (!targets[i].isComplete())
            return false;
      }   

      return true;
   }
   
   protected List getSourceTargetsWarnings()
   {
      List reasons = new ArrayList();
      
      IDataObject[] sources = getDataSources();
      IDataObject[] targets = getDataTargets();
      
      for (int i=0; i<sources.length; i++)
      {
         reasons.addAll( sources[i].getWarnings() );
      }   

      for (int i=0; i<targets.length; i++)
      {
         reasons.addAll( targets[i].getWarnings() );
      }   

      return reasons;
   }
   
   protected List getReasonsSourceTargetsIncomplete()
   {
      List reasons = new ArrayList();
      
      IDataObject[] sources = getDataSources();
      IDataObject[] targets = getDataTargets();
      
      for (int i=0; i<sources.length; i++)
      {
         reasons.addAll( sources[i].getReasonsIncomplete() );
      }   

      for (int i=0; i<targets.length; i++)
      {
         reasons.addAll( targets[i].getReasonsIncomplete() );
      }   

      return reasons;
   }
   
   /**
    * Does no data sources mean this transform is incomplete?  Override this
    * method to change the default behavior which is no sources means the 
    * transform is incomplete.
    * 
    * @return true = no data sources mean incomplete
    */
   protected boolean doesNoSourcesMeanIncomplete()
   {
      return true;
   }
   
   /**
    * Does no data targets mean this transform is incomplete?  Override this
    * method to change the default behavior which is no targets means the 
    * transform is incomplete.
    * 
    * @return true = no data targets mean incomplete
    */
   protected boolean doesNoTargetsMeanIncomplete()
   {
      return true;
   }
   
   /**
    * Does no mappings mean this transform is incomplete?  Override this method
    * to change the default behavior which is no mappings means the transform
    * is incomplete.
    * 
    * @return true = no mappings means incomplete
    */
   protected boolean doesNoMappingsMeanIncomplete()
   {
      return !isUsingUserWrittenCode();
   }
   
   protected boolean areMappingsComplete()
   {
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (!mapping.isComplete())
            return false;
      }
      
      return true;
   }
   
   protected List getReasonsMappingsIncomplete()
   {
      List lMappings = getMappingsList();
      List lReasons = new ArrayList();
      
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         lReasons.addAll( mapping.getReasonsIncomplete() );
      }
      
      return lReasons;
      
   }
   
   //---------------------------------------------------------------------------
   // Persistence
   //---------------------------------------------------------------------------

   /**
    * Gets the OMR type used to represent the transform.
    * 
    * @return MetadataObjects.TRANSFORMATIONSTEP;
    * 
    * @see com.sas.etl.models.IOMRPersistable#getOMRType()
    */
   public String getOMRType()
   {
      return MetadataObjects.TRANSFORMATIONSTEP;
   }
   
   /**
    * Gets the metadata type of the object to use for the classifier map in OMR.
    * The type may be MetadataObjects.CLASSIFIERMAP or MetadataObjects.SELECT.
    * 
    * @return MetadataObjects.CLASSIFIERMAP (the default)
    */
   protected String getClassifierMapType()
   {
      return MetadataObjects.CLASSIFIERMAP;
   }

   /**
    * Get the classifier map object for this transformation.
    * 
    * @param omr the OMR Adapter
    * @return ClassifierMap metadata object
    * 
    * @throws MdException
    * @throws RemoteException
    */
   protected ClassifierMap getClassifierMapObject(OMRAdapter omr) throws MdException, RemoteException
   {
      return (ClassifierMap) omr.acquireOMRObject( m_sClassifierMapID, getClassifierMapType() );
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
      if (super.isChanged())
         return true;
      
      // TODO should source be checked too in isChanged?
      // check sources
      for ( int iSource=0; iSource<m_lDataSources.size(); iSource++ )
      {
         IDataObject data = (IDataObject) m_lDataSources.get( iSource );
         if (data.isChanged())
            return true;
      }

      // check targets
      for ( int iTarget=0; iTarget<m_lDataTargets.size(); iTarget++ )
      {
         IDataObject data = (IDataObject) m_lDataTargets.get( iTarget );
         if (data.isChanged())
            return true;
      }
      
      // check mappings
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping mapping = (IMapping) lMappings.get( iMapping );
         if (mapping.isChanged())
            return true;
      }
      
      ITransformTableOptions[] tableOpts = getTableOptionObjects();
      for (int i=0; i<tableOpts.length; i++)
         if (tableOpts[i].isChanged())
            return true;
      
      return false;
   }
   
   /**
    * Saves the transform to OMR.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (!isChanged())
         return;
      
      super.saveToOMR( omr );
      
      TransformationStep mdoStep = (TransformationStep) omr.acquireOMRObject( this ); 
      saveWorkTablesToOMR(    omr          );
      saveClassifierMapToOMR( omr, mdoStep ); 

      String sGenerateSYSLAST = isSYSLASTVariableGenerationEnabled() ? SYSLAST_MACRO_VARIABLE_YES : 
         SYSLAST_MACRO_VARIABLE_NO;
     
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, SYSLAST_MACRO_VARIABLE, SYSLAST_MACRO_VARIABLE, SYSLAST_MACRO_VARIABLE, sGenerateSYSLAST, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );   
      
//      String sGenerateFormat = isFormatGenerationEnabled() ? FORMATS_YES : 
//          FORMATS_NO;
//       savePropertyToOMR( omr, OPTIONS_PROPERTYSET, FORMATS, FORMATS, FORMATS, sGenerateFormat, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, GENERATE_FORMATSINFORMATS, GENERATE_FORMATSINFORMATS, GENERATE_FORMATSINFORMATS, m_sGenerateFormatsInformats, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, USE_CONNECTUSING, USE_CONNECTUSING, USE_CONNECTUSING, m_sUseConnectUsing, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

//stwatk S0266081
      String sAppendForce = isAppendForceEnabled() ? APPEND_FORCE_YES :  APPEND_FORCE_NO; 
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, APPEND_FORCE, APPEND_FORCE, APPEND_FORCE, sAppendForce, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );   

      String sColMacroVars = isColMacroVarsEnabled() ? COL_MACRO_VARS_YES :  COL_MACRO_VARS_NO; 
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, COL_MACRO_VARS, COL_MACRO_VARS, COL_MACRO_VARS, sColMacroVars, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

      String sFileMacroVars = isFileMacroVarsEnabled() ? FILE_MACRO_VARS_YES :  FILE_MACRO_VARS_NO; 
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, FILE_MACRO_VARS, FILE_MACRO_VARS, FILE_MACRO_VARS, sFileMacroVars, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );
   
      saveBooleanOptionToOMR( omr, INCLUDED_IN_PROPAGATION, m_bIncludedInPropagation   );
      saveBooleanOptionToOMR( omr, INCLUDED_IN_MAPPING,     m_bIncludedInMapping       );

      saveBooleanOptionToOMR( omr, GENERATE_INDEXES_ON_TARGETS, isGenerateIndexesOnTargetTables() );
      
      saveStringOptionToOMR( omr, OPTION_DBI_DIRECT_EXEC, getDBIDirectExecValue() );
      
      saveCustomListToOMR( omr, COLUMNS_EXCLUDED_FROM_MAPPING_NAME,     getColumnsExcludedFromMapping()     );
      saveCustomListToOMR( omr, COLUMNS_EXCLUDED_FROM_PROPAGATION_NAME, getColumnsExcludedFromPropagation() );

      // save list of diagram-connected sources
      saveCustomListToOMR( omr, CONNECTED_SOURCES_NAME, (IDataObject[]) m_lConnectedSources.toArray( new IDataObject[0] ) );

      saveTransformTableOptions( omr );
      
      setChanged( false );
      
   }
   
   /**
    * Saves work tables to OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    */
   protected void saveWorkTablesToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      IWorkTable[] aTables = getWorkTables();
      for ( int iTable=0; iTable<aTables.length; iTable++ )
         aTables[ iTable ].saveToOMR( omr );
   }
   
   protected void saveTransformTableOptions(OMRAdapter omr)
   throws MdException, RemoteException
   {
      ITransformTableOptions[] opts = getTableOptionObjects();
      for (int i=0; i<opts.length; i++)
      {
         opts[i].saveToOMR( omr );
      }
      
   }
   
   /**
    * Saves classifier maps to OMR.  The classifier maps are the portion of the
    * transform that map data sources to data targets.
    * 
    * @param omr     the OMR adapter
    * @param mdoStep the transformation step that represents the transform
    * 
    * @throws MdException
    * @throws RemoteException
    */
   protected void saveClassifierMapToOMR( OMRAdapter omr, TransformationStep mdoStep ) throws MdException, RemoteException
   {            
      //Get the existing tranformations attached to the tranformation step
      List lTransformations = mdoStep.getTransformations( false );
      lTransformations.clear();

      // always create at least one classifier map, if multiple classifier maps
      // are needed then this method needs to be overridden 
      String sName = getName();

      if (m_sClassifierMapID == null || m_sClassifierMapID.length() == 0)
         m_sClassifierMapID = createIDForNewObject();
      
      ClassifierMap mdoCM = (ClassifierMap) omr.acquireOMRObject( m_sClassifierMapID, getClassifierMapType() );
      mdoCM.setName( sName );
      mdoCM.getClassifierTargets( false ).clear();
      mdoCM.getClassifierSources( false ).clear();
      IDataObject[] aDataSources = getDataSources();
      for ( int iDataSource=0; iDataSource<aDataSources.length; iDataSource++ )
      {
         IDataObject objSource = aDataSources[iDataSource];
         if (!(objSource instanceof ITable))
            continue;
         
         mdoCM.getClassifierSources( false ).add( omr.acquireOMRObject( objSource ) );
      }
      
      IDataObject[] aDataTargets = getDataTargets();
      for ( int i = 0; i < aDataTargets.length; i++ )
      {
         if (aDataTargets[i] instanceof ITable )
            mdoCM.getClassifierTargets( false ).add( omr.acquireOMRObject( aDataTargets[i] ) );
      }

      ITable[] aMappingTargets = getTargetTables();
      for ( int i = 0; i < aMappingTargets.length; i++ )
      {
         saveMappingsToOMR( omr, mdoCM, aMappingTargets[i] );
      }
      
      //Add the tranform to the step
      lTransformations.add( mdoCM );          
   }

   protected AbstractTransformation getUserWrittenCodeAnchor( OMRAdapter omr ) throws MdException, RemoteException
   {
      // this code gets run before the save and load specific to this class are run
      // therefore, the classifier map ids has not been populated in the 
      // case of a load or a save of a new transformation
      // therefore if the id is there, just use it. Otherwise, 
      // try to get the value out of transformations association list
      // if the transformations association list is empty, create a new id
      TransformationStep mdoThis = (TransformationStep) omr.acquireOMRObject( this );
      List classifiers = mdoThis.getTransformations(); 
      if (!classifiers.isEmpty())
         m_sClassifierMapID = ((Root) classifiers.get(0)).getFQID();

      if (m_sClassifierMapID == null || m_sClassifierMapID.length() == 0)
      {
         m_sClassifierMapID = createIDForNewObject();
      }
      
      return (AbstractTransformation) omr.acquireOMRObject( m_sClassifierMapID, getClassifierMapType() );
   }

   /**
    * Saves column mappings to OMR.  The column mappings are saved off of the
    * classifier map.
    * 
    * @param omr       the OMR adapter
    * @param mdoCM     the classifier map that will contain the column mappings
    * @param tblTarget the target table which contains the target columns
    *                  of the mappings.  the mappings saved are subset by the
    *                  target table
    *                  
    * @throws MdException
    * @throws RemoteException
    */
   protected void saveMappingsToOMR( OMRAdapter omr, ClassifierMap mdoCM, ITable tblTarget ) throws MdException, RemoteException
   {
      List lFMs = mdoCM.getFeatureMaps( false );

      // lFMs.clear();  --- can't clear because some transforms hang other feature maps here
      
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
      {
         IMapping  mapping  = (IMapping) lMappings.get( iMapping );
         IColumn[] aTargets = mapping.getTargets();
         
         if (mapping.isDead())
         {
            removeMapping( mapping );
            continue;
         }

         // if this is an ordinary mapping and there is a target column
         // (Note: if it is an ordinary mapping, the targets length should be 1.
         //  If it was 0, it would be dead and removed.  The check is just to 
         //  be sure.)
         if (mapping.isOrdinary() && (aTargets.length != 0))
         {
            // the first target column is not in the target table, skip mapping
               
            // (this assumes all target columns are in the same target table which
            // seems reasonable because there is really only one target table per
            // classifier map.  if the target columns were in multiple target 
            // tables, how would we determine which classifier map the feature map
            // belongs to?  also, if there are no target columns, which classifier
            // map does it belong to?)
            if (aTargets[0].getTable() != tblTarget)
               continue;
         }
         
         mapping.saveToOMR( omr );
         Root mdoMapping = omr.acquireOMRObject( mapping );
         lFMs.add( mdoMapping );
      }
   }

   /**
    * Updates the new ids of the objects after a save.  This method is necessary
    * to change the new ids to their new real ids.
    *  
    * @param mapIDs the map of ids
    * 
    * @see com.sas.etl.models.IOMRPersistable#updateIDs(java.util.Map)
    */
   public void updateIDs( Map mapIDs )
   {
      super.updateIDs( mapIDs );
            
      m_sClassifierMapID = updateSubordinateID( m_sClassifierMapID, mapIDs );
      
      IMapping maps[]= getMappings();
      for (int i=0; i< maps.length; i++)
        maps[i].updateIDs ( mapIDs );
      
      ITransformTableOptions[] opts = getTableOptionObjects();
      for (int i=0; i<opts.length; i++)
      {
         opts[i].updateIDs( mapIDs );
      }
   }
   
   /**
    * Loads the transform from OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    * 
    * @see com.sas.etl.models.IOMRPersistable#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      super.loadFromOMR( omr );
      
      //setTransformRole( mdoStep.getTransformRole() );  -- transform role cannot change

      TransformationStep mdoStep = (TransformationStep) omr.acquireOMRObject( this ); 
      loadDataSourcesTargetsFromOMR( omr, mdoStep );
      String value = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, SYSLAST_MACRO_VARIABLE, SYSLAST_MACRO_VARIABLE_YES, USE_PROPERTYSET_PROPERTIES);
      setSYSLASTVariableGenerationEnabled( SYSLAST_MACRO_VARIABLE_YES.equalsIgnoreCase( value ) );

//stwatk S0266081
      String FORCE_value = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, APPEND_FORCE, APPEND_FORCE_YES, USE_PROPERTYSET_PROPERTIES);
      setAppendForceEnabled( APPEND_FORCE_YES.equalsIgnoreCase( FORCE_value ) );

      String COLMACRO_Value = loadPropertyFromOMR(omr, OPTIONS_PROPERTYSET, COL_MACRO_VARS, COL_MACRO_VARS_YES, USE_PROPERTYSET_PROPERTIES);
      setColMacroVarsEnabled(COL_MACRO_VARS_YES.equalsIgnoreCase(COLMACRO_Value));

      String FILEMACRO_Value = loadPropertyFromOMR(omr, OPTIONS_PROPERTYSET, FILE_MACRO_VARS, FILE_MACRO_VARS_YES, USE_PROPERTYSET_PROPERTIES);
      setFileMacroVarsEnabled(FILE_MACRO_VARS_YES.equalsIgnoreCase(FILEMACRO_Value));

      String formatsinformats_value = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, GENERATE_FORMATSINFORMATS, FORMATSINFORMATS_JOB, USE_PROPERTYSET_PROPERTIES);
      setFormatInformatGeneration(formatsinformats_value);
      //      setFormatGenerationEnabled(FORMATS_YES.equalsIgnoreCase( formats_value ) );
      
      String useConnectUsing_value = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, USE_CONNECTUSING, FORMATSINFORMATS_JOB, USE_PROPERTYSET_PROPERTIES);
      setUseConnectUsing(useConnectUsing_value);

      setGenerateIndexesOnTargetTables( loadBooleanOptionFromOMR( omr, GENERATE_INDEXES_ON_TARGETS, true ));
      
      setIncludedInPropagation( loadBooleanOptionFromOMR( omr, INCLUDED_IN_PROPAGATION, true ) );
      setIncludedInMapping(     loadBooleanOptionFromOMR( omr, INCLUDED_IN_MAPPING,     true ) );

      setDBIDirectExec( loadStringOptionFromOMR( omr, OPTION_DBI_DIRECT_EXEC, getDefaultDBIDirectExecValue() ) );
      
      // get columns excluded from mapping
      IPersistableObject[] aColumns = loadCustomListFromOMR( omr, COLUMNS_EXCLUDED_FROM_MAPPING_NAME );
      for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
         getListOfColumnsExcludedFromMapping().add( aColumns[iColumn] );

      // get columns excluded from propagation
      aColumns = loadCustomListFromOMR( omr, COLUMNS_EXCLUDED_FROM_PROPAGATION_NAME );
      for ( int iColumn=0; iColumn<aColumns.length; iColumn++ )
         getListOfColumnsExcludedFromPropagation().add( aColumns[iColumn] );

      // load list of diagram-connected sources
      IPersistableObject[] aConnectedSources = loadCustomListFromOMR(omr, CONNECTED_SOURCES_NAME);
      for( int i = 0; i < aConnectedSources.length; i++ )
         m_lConnectedSources.add(aConnectedSources[i]);

      try
      {
         IPromptModel optionModel = getOptionModel();
         if (optionModel != null)
         {
        	 if (optionModel.getDataProvider()!=null)
        	 {
	            optionModel.getDataProvider().addDataSources( getDataSources() );
	            optionModel.getDataProvider().addDataTargets( getDataTargets() );
        	 }
             optionModel.fixDefinitions();
         }
      }
      catch (FileNotFoundException e)
      {
         throw new MdException(e);
      }
      catch (ServiceException e)
      {
         throw new MdException(e);
      }
      catch (ServerConnectionException e)
      {
         throw new MdException(e);
      }
      catch (IOException e)
      {
         throw new MdException(e);
      }
      catch (ParserConfigurationException e)
      {
         throw new MdException(e);
      }
      catch (SAXException e)
      {
         throw new MdException(e);
      }
      
      boolean changed = loadTransformTableOptionsFromOMR( omr );

      setChanged( changed );
   }

   /**
    * Loads the data sources and targets from OMR for the transform.
    * 
    * @param omr     the OMR adapter
    * @param mdoStep the transformation step that represents the transform
    * 
    * @throws MdException
    * @throws RemoteException
    */
   protected void loadDataSourcesTargetsFromOMR( OMRAdapter omr, TransformationStep mdoStep ) throws MdException, RemoteException
   {
      m_lDataSources.clear();
      m_lDataTargets.clear();
      
      List lTransformations = mdoStep.getTransformations();
      
      if (!lTransformations.isEmpty())
      {
         //We only deal with one classifier map per transformation step, if there
         //more than on is needed then this method should be overridden by the
         //transformation
         ClassifierMap mdoCM = (ClassifierMap)lTransformations.get(0);
         m_sClassifierMapID = mdoCM.getFQID();
         omr.populateAssociations(omr.getOMRFactory(), mdoCM);
         
         // get (and load if necessary) data sources 
         List lSources = mdoCM.getClassifierSources();
         for ( int iSource=0; iSource<lSources.size(); iSource++ )
         {
            Root mdoSource = (Root) lSources.get( iSource );
            IDataObject source = (IDataObject) omr.acquireObject( mdoSource );
            
            if (!m_lDataSources.contains( source ) && iSource<getMaximumDataSourceCount())
               addDataSource( source );  
         }
         
         // get (and load if necessary) data targets 
         List lTargets = mdoCM.getClassifierTargets();
         for ( int iTarget=0; iTarget<lTargets.size(); iTarget++ )
         {
            Root mdoTarget = (Root) lTargets.get( iTarget );
            IDataObject target = (IDataObject) omr.acquireObject( mdoTarget );
            
            if (!m_lDataTargets.contains( target ) && iTarget<getMaximumDataTargetCount())
               addDataTarget( target );
            
            // S1398638  to set the correct option model on the work table if job work library location is being used
            if (target instanceof BaseWorkTable)
                ((BaseWorkTable)target).resetOptionModel(omr);
         }
         
         loadMappingsFromOMR( omr, mdoCM );        
      }     
   }
   
   protected ITransformTableOptions findTransformTableOptionsFromOMR(OMRAdapter omr, IPhysicalTable table) 
   throws MdException, RemoteException
   {
      Root mdAnchor = omr.acquireOMRObject( this );
      
      List propertySets = mdAnchor.getPropertySets();
      for (int i=0; i<propertySets.size(); i++)
      {
         PropertySet pSet = (PropertySet)propertySets.get( i );
         if (ITransformTableOptions.PROPERTY_ROLE.equals( pSet.getSetRole()))
         {
        	 ITransformTableOptions transformOpts = new BaseTransformTableOptions(pSet.getFQID(),getModel());

             IPhysicalTable associatedtable = transformOpts.loadAssociatedObject(omr, pSet.getFQID());

            if (associatedtable==table )
            {
                transformOpts.setOwner( this );
                transformOpts.loadFromOMR( omr );
                return transformOpts;
            }
         }
      }
      return null;
   }
   
   protected boolean loadTransformTableOptionsFromOMR(OMRAdapter omr) 
   throws MdException, RemoteException
   {
      Root mdAnchor = omr.acquireOMRObject( this );
      
      boolean changed = false;

      List propertySets = mdAnchor.getPropertySets();
      for (int i=0; i<propertySets.size(); i++)
      {
         PropertySet pSet = (PropertySet)propertySets.get( i );
         if (ITransformTableOptions.PROPERTY_ROLE.equals( pSet.getSetRole()))
         {
        	 ITransformTableOptions transformOpts = new BaseTransformTableOptions(pSet.getFQID(),getModel());
        	 
        	 IPhysicalTable table = transformOpts.loadAssociatedObject(omr, pSet.getFQID()); 

             List tableList = ( ITransformTableOptions.ACCESS_TYPE_INPUT.equals( pSet.getPropertySetName()) ? getDataSourceList() : getDataTargetList());
           
            
            if (tableList.contains( table ))
            {
                transformOpts.setOwner( this );
                transformOpts.loadFromOMR( omr );

               addTransformTableOption( transformOpts );
            }
            else
            {
               addToDeletedObjects(transformOpts);
               changed = true;
            }
         }
      }
      return changed;
   }

   /**
    * Loads mappings from OMR.
    * 
    * @param omr   the OMR adapter
    * @param mdoCM the classifier map that contains the mappings
    *  
    * @throws MdException
    * @throws RemoteException
    */
   protected void loadMappingsFromOMR( OMRAdapter omr, ClassifierMap mdoCM ) throws MdException, RemoteException
   {
	   omr.populateAssociations(omr.getOMRFactory(), mdoCM);
      List lFMs = mdoCM.getFeatureMaps();
      for ( int iFM=0; iFM<lFMs.size(); iFM++ )
      {
         FeatureMap mdoFM = (FeatureMap) lFMs.get( iFM );
         IMapping mapping = createMapping( omr, mdoFM );

         // if the mapping is dead, remove it
         if (mapping.isDead())
            addToDeletedObjects( mapping );
         
         // mapping is OK
         else
            addMapping( mapping );
      }
   }
   
   /**
    * Create a mapping object. 
    * 
    * @param omr   the OMRAdapter
    * @param mdoFM FeatureMap metadata object
    * @return a new mapping object
    * @throws MdException
    * @throws RemoteException
    */
   protected IMapping createMapping(OMRAdapter omr, FeatureMap mdoFM) throws MdException, RemoteException
   {
      BaseMapping mapping = (BaseMapping) omr.acquireObject( mdoFM );
      mapping.setExpressionAllowed( areExpressionsAllowed() );
      if (mapping.isOrdinary())
         mapping.setAutoType( true );
      return mapping;
   }

   public void dispose()
   {
      super.dispose();
      
      ITransformTableOptions[] tableOptions = getTableOptionObjects();
      for (int i=0; i<tableOptions.length; i++)
         tableOptions[i].dispose();
   }
   
   /**
    * Notifies the object that it will be deleted when the model is saved.  This
    * method is overridden to notify target work tables that they will be 
    * deleted.
    * 
    * @see com.sas.etl.models.impl.AbstractPersistableObject#delete()
    */
   public void delete()
   {
      IWorkTable[] aTables = getWorkTables();
      
      // return code checker and loop end dont have work tables
      if (aTables == null) return;
      
      for ( int iTable=0; iTable<aTables.length; iTable++ )
         aTables[ iTable ].delete();
      
      ITransformTableOptions[] tableOptions = getTableOptionObjects();
      for (int i=0; i<tableOptions.length; i++)
         tableOptions[i].delete();
   }
   
   /**
    * Deletes the transform from OMR.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    * 
    * @see com.sas.etl.models.IOMRPersistable#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void deleteFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (isNew())
         return;
      
      deleteWorkTablesFromOMR(    omr );
      deleteClassifierMapFromOMR( omr );
      
      super.deleteFromOMR( omr );
   }
   
   protected void deleteTransformTableOptionsFromOMR(OMRAdapter omr)
   throws MdException, RemoteException
   {
      ITransformTableOptions[] tableModels = getTableOptionObjects();
      for (int i=0; i<tableModels.length; i++)
         tableModels[i].deleteFromOMR( omr );
   }

   /**
    * Deletes work tables from OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    */
   private void deleteWorkTablesFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      IWorkTable[] aTables = getWorkTables();
      
      // rc checker and loop end dont have work tables
      if (aTables == null) return;
      
      for ( int iTable=0; iTable<aTables.length; iTable++ )
         aTables[ iTable ].deleteFromOMR( omr );
   }

   /**
    * Deletes the classifer map and mappings associated with the transform from
    * OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    */
   protected void deleteClassifierMapFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
         ((IMapping) lMappings.get( iMapping )).deleteFromOMR( omr );
      
      // delete old classifier map
      if( m_sClassifierMapID != null )
         omr.deleteOMRObject( m_sClassifierMapID, getClassifierMapType() );
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
      
      List lAssociations = (List) map.get( MetadataObjects.TRANSFORMATIONSTEP );
      if (lAssociations == null)
         ModelLogger.getDefaultLogger().error( "No entry in load map for TransformationStep" );
      else
         lAssociations.add( TransformationStep.ASSOCIATION_TRANSFORMATIONS_NAME );
      
      lAssociations = new ArrayList();
      lAssociations.add( ClassifierMap.ASSOCIATION_FEATUREMAPS_NAME           );
      lAssociations.add( ClassifierMap.ASSOCIATION_CLASSIFIERSOURCES_NAME     );
      lAssociations.add( ClassifierMap.ASSOCIATION_CLASSIFIERTARGETS_NAME     );
      lAssociations.add( ClassifierMap.ASSOCIATION_SOURCECODE_NAME            );
      lAssociations.add( ClassifierMap.ASSOCIATION_TRANSFORMATIONSOURCES_NAME );
      lAssociations.add( ClassifierMap.ASSOCIATION_TRANSFORMATIONTARGETS_NAME );
      map.put( getClassifierMapType(), lAssociations );
      
      lAssociations = new ArrayList();
      lAssociations.add( FeatureMap.ASSOCIATION_FEATURESOURCES_NAME );
      lAssociations.add( FeatureMap.ASSOCIATION_FEATURETARGETS_NAME );
      map.put( MetadataObjects.FEATUREMAP, lAssociations );

      // TODO get the template from the WorkTable and PhysicalTable classes
      lAssociations = new ArrayList();
      lAssociations.add( PhysicalTable.ASSOCIATION_COLUMNS_NAME );
      map.put( MetadataObjects.PHYSICALTABLE, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( WorkTable.ASSOCIATION_COLUMNS_NAME );
      map.put( MetadataObjects.WORKTABLE, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( StepPrecedence.ASSOCIATION_PREDECESSORS_NAME );
      lAssociations.add( StepPrecedence.ASSOCIATION_SUCCESSORS_NAME   );
      map.put( MetadataObjects.STEPPRECEDENCE, lAssociations );
      
      return map;
   }

   /**
    * Gets the OMR checkout template map for the transform.
    * 
    * @return the OMR checkout template map
    * 
    * @see com.sas.etl.models.IOMRPersistable#getOMRLoadTemplateMap()
    */
   public Map getOMRCheckOutTemplateMap()
   {
      // TODO Auto-generated method stub
      return null;
   }

      
   /**
    * Check each data sources to determine is any source table requires
    * quoting to be used.
    * @return true, a single source table requires quoting
    * @see com.sas.etl.models.job.IDataTransform#isQuotingNeeded()
    */
   public boolean isQuotingNeeded()
   {
      List tables = new ArrayList(Arrays.asList(getDataSources()));
      tables.addAll( Arrays.asList(getDataTargets()) );
      int tableSize = tables.size();
      
      for (int i=0; i<tableSize; i++)
      {
         IDataObject dObject = (IDataObject)tables.get( i );
         if (dObject instanceof ITable)
         {
            ITable table = (ITable)dObject;
            if (table.isQuoted())
               return true;
         }
      }

      return false;
   }
   
   /**
    * Check each data sources to determine is any source table requires
    * quoting to be used.
    * @return true, a single source table requires quoting
    * @see com.sas.etl.models.job.IDataTransform#isQuotingNeeded()
    */
   public void setConnectUsingFlagOnTables()
   {
      List tables = new ArrayList(Arrays.asList(getDataSources()));
      tables.addAll( Arrays.asList(getDataTargets()) );
      int tableSize = tables.size();
      
      for (int i=0; i<tableSize; i++)
      {
         IDataObject dObject = (IDataObject)tables.get( i );
         if (dObject instanceof ITable)
         {
        	boolean bUseConnectUsing = isUseConnectUsingEnabled();
            ITable table = (ITable)dObject;
            table.setUseConnectUsing(bUseConnectUsing);
         }
      }
   }

   /**
    * Returns the state of the source and target tables special characters flags.  
    * 
    * @return TRUE if the source or target tables have the flag set, FALSE otherwize
    * @see com.sas.etl.models.job.IDataTransform#isSpecialCharactersNeeded()
    */
   public final boolean isSpecialCharactersNeeded()
   {
      List tables = new ArrayList(Arrays.asList(getDataSources()));
      tables.addAll( Arrays.asList(getDataTargets()) );
      int tableSize = tables.size();
      
      for (int i=0; i<tableSize; i++)
      {
         IDataObject dObject = (IDataObject)tables.get( i );
         if (dObject instanceof ITable)
         {
            ITable table = (ITable)dObject;
            if (table.isSpecialCharacters())
               return true;
         }
      }

      return false;
   }

   /**
    * Dumps the transform to a print stream for debug purposes.
    * 
    * @param strm the print stream
    * 
    * @see com.sas.etl.models.IObject#dump(java.io.PrintStream)
    */
   public void dump( PrintStream strm )
   {
      super.dump( strm );
      
      strm.println( "<Data Sources>" );
      for ( int iDataSource=0; iDataSource<m_lDataSources.size(); iDataSource++ )
         ((IObject) m_lDataSources.get( iDataSource )).dump( strm );
      strm.println( "</Data Sources>" );
         
      strm.println( "<Data Targets>" );
      for ( int iDataTarget=0; iDataTarget<m_lDataTargets.size(); iDataTarget++ )
         ((IObject) m_lDataTargets.get( iDataTarget )).dump( strm );
      strm.println( "</Data Targets>" );
      
      strm.println( "<Mappings>" );
      List lMappings = getMappingsList();
      for ( int iMapping=0; iMapping<lMappings.size(); iMapping++ )
         ((IMapping) lMappings.get( iMapping )).dump( strm );
      strm.println( "</Mappings>" );
   }

   
   /**
    * Dump the transform to XML
    * 
    * @param stream the Printstream to use
    * @see com.sas.etl.models.job.impl.AbstractTransform#dumpXML(java.io.PrintStream)
    * 
    */
   public void dumpXML(PrintStream stream)
   {
      super.dumpXML( stream );
      
      stream.println("<sources>");
      for ( int iDataSource=0; iDataSource<m_lDataSources.size(); iDataSource++ )
      {
         IObject obj = ((IObject) m_lDataSources.get( iDataSource ));
         stream.println("<table>");
         stream.println("<name>" + obj.getName()+"</name>");
         stream.println("</table>");

      }
               
      stream.println("</sources>");
      
      stream.println("<targets>");
      for ( int iDataSource=0; iDataSource<m_lDataTargets.size(); iDataSource++ )
      {
         IObject obj = ((IObject) m_lDataTargets.get( iDataSource ));
         stream.println("<table>");
         stream.println("<name>" + obj.getName()+"</name>");
         stream.println("</table>");

      }
               
      stream.println("</targets>");

   }

   
   
   /**
    * Generate the source and target comment.
    * 
    * @param codeSegment to put the comment into 
    * 
    * @return the code segment with the comment in it
    * 
    * @see com.sas.etl.models.job.impl.AbstractTransform#getSourceTargetComment(com.sas.etl.models.job.ICodeSegment)
    */
   public ICodeSegment getSourceTargetComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
//    Source/Target Tables
      IDataObject[] sourceList = getDataSources();
      IDataObject[] targetList = getDataTargets();
         
      if (sourceList.length>0 || targetList.length>0) 
      {
         codeSegment.genCommentLine("","","");
         
         codeSegment.genCommentTableLine(sourceList, codeSegment.getCommentLabelForSource(), codeSegment.getCommentLabelForSources());
         codeSegment.genCommentTableLine(targetList, codeSegment.getCommentLabelForTarget(), codeSegment.getCommentLabelForTargets());      
      } // if - the source or target list of tables is not empty

      return codeSegment;
   }
   
   /**
    * Get columns from source tables that are not mapped.  Exclude columns
    * passed in to the method.
    * 
    * @param excludeColumns
    * @return List of unmapped source columns
    */
   public IColumn[] getOrdinaryUnmappedSourceColumns(List excludeColumns)
   {
      List unmappedColumns = new ArrayList();
      IDataObject[] sources = getDataSources();
      int size = sources.length;
      for (int i=0; i<size; i++)
      {
         if (sources[i] instanceof ITable)
         {
            ITable source = (ITable)sources[i];
            IColumn[] cols = source.getColumns();
            int cSize = cols.length;
            for (int j=0; j<cSize; j++)
            {
               IColumn col = cols[j];
               IMapping[] mapping = getOrdinaryMappingsForSourceColumn( col );
               if ( (mapping!=null && mapping.length>0)  && !unmappedColumns.contains( col ))
               {
                  if (excludeColumns==null || !excludeColumns.contains( col ))
                     unmappedColumns.add( col );
               }
            }
         }
      }
      return (IColumn[])unmappedColumns.toArray(new IColumn[unmappedColumns.size()]);
   }
   
/**
 * Get columns from source tables that are mapped.  Exclude columns passed in to the method
 * @param excludeColumns list of columns to be excluded 
 * @return list of columns that are mapped
 */
   public IColumn[] getOrdinaryMappedSourceColumns(List excludeColumns)
   {
      List mappedColumns = new ArrayList();
      IDataObject[] sources = getDataSources();
      int size = sources.length;
      for (int i=0; i<size; i++)
      {
         if (sources[i] instanceof ITable)
         {
            ITable source = (ITable)sources[i];
            IColumn[] cols = source.getColumns();
            int cSize = cols.length;
            for (int j=0; j<cSize; j++)
            {
               IColumn col = cols[j];
               IMapping[] mapping = getOrdinaryMappingsForSourceColumn( col );
               if ((mapping!=null && mapping.length>0)  && !mappedColumns.contains( col ))
               {
                  if (excludeColumns==null || !excludeColumns.contains( col ))
                     mappedColumns.add( col );
               }
            }
         }
      }
      return (IColumn[])mappedColumns.toArray(new IColumn[mappedColumns.size()]);
   }

   
   public IColumn[] getOrdinaryUnmappedTargetColumns(List excludeColumns)
   {
      List unmappedColumns = new ArrayList();
      IDataObject[] targets = getDataTargets();
      int size = targets.length;
      for (int i=0; i<size; i++)
      {
         if (targets[i] instanceof ITable)
         {
            ITable target = (ITable)targets[i];
            IColumn[] cols = target.getColumns();
            int cSize = cols.length;
            for (int j=0; j<cSize; j++)
            {
               IColumn col = cols[j];
               IMapping mapping = getOrdinaryMappingsForTargetColumn( col );
               if (mapping==null  && !unmappedColumns.contains( col ))
               {
                  if (excludeColumns==null || !excludeColumns.contains( col ))
                     unmappedColumns.add( col );
               }
            }
         }
      }
      return (IColumn[])unmappedColumns.toArray(new IColumn[unmappedColumns.size()]);
   }
   
   public IColumn[] getOrdinaryMappedTargetColumns(List excludeColumns)
   {
      List mappedColumns = new ArrayList();
      IDataObject[] targets = getDataTargets();
      int size = targets.length;
      for (int i=0; i<size; i++)
      {
         if (targets[i] instanceof ITable)
         {
            ITable target = (ITable)targets[i];
            IColumn[] cols = target.getColumns();
            int cSize = cols.length;
            for (int j=0; j<cSize; j++)
            {
               IColumn col = cols[j];
               IMapping mapping = getOrdinaryMappingsForTargetColumn( col );
               if (mapping!=null  && !mappedColumns.contains( col ))
               {
                  if (excludeColumns==null || !excludeColumns.contains( col ))
                     mappedColumns.add( col );
               }
            }
         }
      }
      return (IColumn[])mappedColumns.toArray(new IColumn[mappedColumns.size()]);
   }

   /**
    * Formerly AUTOEXTRACT's columnMapping() method... generates [typically] the view
    * "work.mapped".  Also used as the code generator for the Extract transform.
    * 
    * @param codeSegment codeSegment to add source to  
    * @param sourceTable source table (object)
    * @param targetTable target table (object)
    * @param mappingTargetTableName output table name (3.4 default: work.mapped)
    * @param inputTableName input table name (3.4 default: &syslast)
    * @return same codeSegment that was passed in
    * @throws CodegenException
    * @throws BadLibraryDefinitionException
    * @throws BadServerDefinitionException
    * @throws RemoteException
    * @throws MdException
    * @throws ServerException
    */
   public ICodeSegment getOrdinaryMappingCode(ICodeSegment codeSegment, 
            IPhysicalTable sourceTable, 
            IPhysicalTable targetTable,
            String mappingTargetTableName,
            String inputTableName
            )
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
      return getOrdinaryMappingCode( codeSegment, sourceTable, targetTable, mappingTargetTableName, inputTableName, 
                                     getTableOptionObject( sourceTable, true).getTableOptions(codeSegment.getCurrentServer()), getTableOptionObject(targetTable,false).getTableOptions(codeSegment.getCurrentServer()), true, true, true, null, null, false, null, null, null);
   }
   
   /**
    * Formerly AUTOEXTRACT's columnMapping() method... generates [typically] the view
    * "work.mapped".  Also used as the code generator for the Extract transform.
    * 
    * @param codeSegment codeSegment to add source to  
    * @param sourceTable source table (object)
    * @param targetTable target table (object)
    * @param mappingTargetTableName output table name (3.4 default: work.mapped)
    * @param inputTableName input table name (3.4 default: &syslast)
    * @param sourceTableOptions table options for source table
    * @param targetTableOptions table options for the target table
    * @param createView  flag to create a view instead of a table (3.4 default: true)
    * @param genComments flag to generate comments (3.4 default: true)
    * @param genLabelStatements flag for generating label statements 
    * @return same codeSegment that was passed in
    * @throws CodegenException
    * @throws BadLibraryDefinitionException
    * @throws BadServerDefinitionException
    * @throws RemoteException
    * @throws MdException
    * @throws ServerException
    */
   public ICodeSegment getOrdinaryMappingCode(ICodeSegment codeSegment, 
            IPhysicalTable sourceTable, 
            IPhysicalTable targetTable,
            String mappingTargetTableName,
            String inputTableName,
            String sourceTableOptions,
            String targetTableOptions,
            boolean createView,
            boolean genComments,
            boolean genLabelStatements)
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
      return getOrdinaryMappingCode( codeSegment, sourceTable, targetTable, mappingTargetTableName, inputTableName, sourceTableOptions, targetTableOptions, createView, genComments, genLabelStatements,null,null,false,null,null,null );
   }

   /**
    * Formerly AUTOEXTRACT's columnMapping() method... generates [typically] the view
    * "work.mapped".  Also used as the code generator for the Extract transform.
    * 
    * @param codeSegment codeSegment to add source to  
    * @param sourceTable source table (object)
    * @param targetTable target table (object)
    * @param mappingTargetTableName output table name (3.4 default: work.mapped)
    * @param inputTableName input table name (3.4 default: &syslast)
    * @param sourceTableOptions table options for source table
    * @param targetTableOptions table options for the target table
    * @param createView  flag to create a view instead of a table (3.4 default: true)
    * @param genComments flag to generate comments (3.4 default: true)
    * @param genLabelStatements flag for generating label statements 
    * @param passedOnlyColumns list of 'ignore' and 'extra' columns to add to select without an assignment
    * @param excludeColumns  list of column names to exclude entirely
    * @param useDistinctKeyword add DISTINCT keyword to select (3.4 default: false)
    * @param whereClause     where clause (3.4 default: null)
    * @param groupByClause  the group-by clause  (3.4 default: null)
    * @param orderByClause  the order-by clause (3.4 default: null)
    * @return same codeSegment that was passed in
    * @throws CodegenException
    * @throws BadLibraryDefinitionException
    * @throws BadServerDefinitionException
    * @throws RemoteException
    * @throws MdException
    * @throws ServerException
    */
   public ICodeSegment getOrdinaryMappingCode(ICodeSegment codeSegment, 
            ITable sourceTable, 
            ITable targetTable,
            String mappingTargetTableName,
            String inputTableName,
            String sourceTableOptions,
            String targetTableOptions,
            boolean createView,
            boolean genComments,
            boolean genLabelStatements,
            IColumn[] passedOnlyColumns,  // ignore and extra columns are just add to select statement with no assignment
            IColumn[] excludeColumns,   // columns to exclude entirely from the extract code
            boolean useDistinctKeyword,
            String whereClause,
            IGroupBy groupByClause,
            ISorting orderByClause
            
   )
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
      return getOrdinaryMappingCode(
                                    codeSegment, 
                                    sourceTable, 
                                    targetTable, 
                                    mappingTargetTableName, 
                                    inputTableName, 
                                    sourceTableOptions, 
                                    targetTableOptions, 
                                    createView, 
                                    genComments, 
                                    genLabelStatements,
                                    passedOnlyColumns,
                                    excludeColumns,
                                    useDistinctKeyword,
                                    whereClause,
                                    groupByClause,
                                    orderByClause,
                                    true,
                                    true 
                                    );
   }
   
   /**
    * Formerly AUTOEXTRACT's columnMapping() method... generates [typically] the view
    * "work.mapped".  Also used as the code generator for the Extract transform.
    * 
    * @param codeSegment codeSegment to add source to  
    * @param sourceTable source table (object)
    * @param targetTable target table (object)
    * @param mappingTargetTableName output table name (3.4 default: work.mapped)
    * @param inputTableName input table name (3.4 default: &syslast)
    * @param sourceTableOptions table options for source table
    * @param targetTableOptions table options for the target table
    * @param createView  flag to create a view instead of a table (3.4 default: true)
    * @param genComments flag to generate comments (3.4 default: true)
    * @param genLabelStatements flag for generating label statements 
    * @param passedOnlyColumns list of 'ignore' and 'extra' columns to add to select without an assignment
    * @param excludeColumns  list of column names to exclude entirely
    * @param useDistinctKeyword add DISTINCT keyword to select (3.4 default: false)
    * @param whereClause     where clause (3.4 default: null)
    * @param groupByClause  the group-by clause  (3.4 default: null)
    * @param orderByClause  the order-by clause (3.4 default: null)
    * @param bGenExcludedColumnStatement true if the code should generate a data _Null_ for any excluded columns sent to method
    * @param bGenUnMappedColumnStatement true if the code should generate a data _null_ for any unmapped columns found during mapping
    * @return same codeSegment that was passed in
    * @throws CodegenException
    * @throws BadLibraryDefinitionException
    * @throws BadServerDefinitionException
    * @throws RemoteException
    * @throws MdException
    * @throws ServerException
    */
   public ICodeSegment getOrdinaryMappingCode(ICodeSegment codeSegment, 
            ITable sourceTable, 
            ITable targetTable,
            String mappingTargetTableName,
            String inputTableName,
            String sourceTableOptions,
            String targetTableOptions,
            boolean createView,
            boolean genComments,
            boolean genLabelStatements,
            IColumn[] passedOnlyColumns,  // ignore and extra columns are just add to select statement with no assignment
            IColumn[] excludeColumns,   // columns to exclude entirely from the extract code
            boolean useDistinctKeyword,
            String whereClause,
            IGroupBy groupByClause,
            ISorting orderByClause,
            boolean bGenExcludedColumnStatement,
            boolean bGenUnMappedColumnStatement
   )
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
      return getOrdinaryMappingCode( codeSegment, sourceTable, targetTable, mappingTargetTableName, inputTableName, sourceTableOptions, targetTableOptions, createView, genComments, genLabelStatements, passedOnlyColumns, excludeColumns, useDistinctKeyword, whereClause, groupByClause, orderByClause, bGenExcludedColumnStatement, bGenUnMappedColumnStatement, false );
   }
   
   protected IPhysicalTable[] getValidateTables()
   {
      List tables = new ArrayList();
      ITable[] sources = getSourceTables();
      for (int i=0; i<sources.length; i++)
      {
         if (sources[i] instanceof IPhysicalTable)
         {
            tables.add(sources[i]);
         }
      }
      return (IPhysicalTable[])tables.toArray( new IPhysicalTable[tables.size()] );
   }
   
   private final String getValidateMacroName(IPhysicalTable table)
   {
      String tableId = table.getID().replaceAll( "\\.", "" );
      return "etls_"+tableId.replaceAll( "\\$", "_" );
   }
   
   protected ICodeSegment getPreValidateCode(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      // insert debug code for validation
      codeSegment.genDebugOptionCode();
      
      // need to generate code to create empty source tables so the procedure will not error on validation
      IPhysicalTable[] tables = getValidateTables();
      
      if (tables.length>0)
      {
         codeSegment.addSourceCode( "%macro etls_createValidateTables;\n")
         .indent();
      }
      for (int i=0; i<tables.length; i++)
      {
         IPhysicalTable table=(IPhysicalTable)tables[ i ];
         table.genAccessPath( codeSegment, false,0, null );
         String macro = getValidateMacroName( table );
         codeSegment.addSourceCode( "%global "+macro+";\n" );
         table.genTableExist( codeSegment, macro );
         table.getDBMSType().create( codeSegment, table, true, true, true, false, "", this, macro );
         
      }
      if (tables.length>0)
      {
         codeSegment.unIndent();
         codeSegment.addSourceCode( "%mend etls_createValidateTables;\n" );
         
         codeSegment.addSourceCode( "%etls_createValidateTables;\n\n" );

      }
      return codeSegment;
   }

   protected ICodeSegment getPostValidateCode(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      // need to delete tables which could have been created during validation
      IPhysicalTable[] tables = getValidateTables();
      
      if (tables.length>0)
      {
         codeSegment.addSourceCode( "%macro etls_deleteValidateTables;\n")
         .indent();
      }
      for (int i=0; i<tables.length; i++)
      {
         IPhysicalTable table=(IPhysicalTable)tables[ i ];
         String macro = getValidateMacroName( table );
         codeSegment.addSourceCode("%if (&").addSourceCode(macro).addSourceCode(" eq 0) %then\n")
         .addSourceCode( "%do;\n" ).indent();
         table.genTableDelete( codeSegment );
         codeSegment.unIndent()
         .addSourceCode( "%end;\n" );
         
      }
      if (tables.length>0)
      {
         codeSegment.unIndent();
         codeSegment.addSourceCode( "%mend etls_deleteValidateTables;\n" );
         
         codeSegment.addSourceCode( "%etls_deleteValidateTables;\n\n" );

      }
      
      
      // put out the integer value for the sql passthru macro
      codeSegment.addSourceCode( "%macro etls_pushdownMsg;\n" );
      codeSegment.addSourceCode( "%put ; %put ;\n" );
      codeSegment.addSourceCode( "%if (%symexist(").addSourceCode(ICodeSegment.ETLS_SQL_PUSHDOWN_MACRO_NAME).addSourceCode( ")) %then\n");
      codeSegment.addSourceCode( "%do;\n" );
      codeSegment.indent();
      codeSegment.addSourceCode( "%if (&").addSourceCode(ICodeSegment.ETLS_SQL_PUSHDOWN_MACRO_NAME).addSourceCode( " eq 0) %then\n" );
      codeSegment.indent()
      .genPercentPutStatement( "Transformation, " + getName() + ", will process on database.", ICodeSegment.NOTE_LABEL )
      .unIndent();
      codeSegment.addSourceCode( "%else\n" );
      codeSegment.indent()
      .genPercentPutStatement( "Transformation, " + getName() + ", will not process on database.", ICodeSegment.NOTE_LABEL )
      .unIndent();
      codeSegment.unIndent();
      codeSegment.addSourceCode( "%end;\n" );
      codeSegment.addSourceCode( "%else\n" );
      codeSegment.indent()
      .genPercentPutStatement( "Transformation, " + getName() + ", will not process on database.", ICodeSegment.NOTE_LABEL )
      .unIndent();
      codeSegment.addSourceCode( "%put ; %put ;\n" );
      codeSegment.addSourceCode( "%mend;\n" );
      codeSegment.addSourceCode("%etls_pushdownMsg;\n\n");
      
      return codeSegment;
   }
   
   /**
    * Formerly AUTOEXTRACT's columnMapping() method... generates [typically] the view
    * "work.mapped".  Also used as the code generator for the Extract transform.
    * 
    * @param codeSegment codeSegment to add source to  
    * @param sourceTable source table (object)
    * @param targetTable target table (object)
    * @param mappingTargetTableName output table name (3.4 default: work.mapped)
    * @param inputTableName input table name (3.4 default: &syslast)
    * @param sourceTableOptions table options for source table
    * @param targetTableOptions table options for the target table
    * @param createView  flag to create a view instead of a table (3.4 default: true)
    * @param genComments flag to generate comments (3.4 default: true)
    * @param genLabelStatements flag for generating label statements 
    * @param passedOnlyColumns list of 'ignore' and 'extra' columns to add to select without an assignment
    * @param excludeColumns  list of column names to exclude entirely
    * @param useDistinctKeyword add DISTINCT keyword to select (3.4 default: false)
    * @param whereClause     where clause (3.4 default: null)
    * @param groupByClause  the group-by clause  (3.4 default: null)
    * @param orderByClause  the order-by clause (3.4 default: null)
    * @param bGenExcludedColumnStatement true if the code should generate a data _Null_ for any excluded columns sent to method
    * @param bGenUnMappedColumnStatement true if the code should generate a data _null_ for any unmapped columns found during mapping
    * @return same codeSegment that was passed in
    * @throws CodegenException
    * @throws BadLibraryDefinitionException
    * @throws BadServerDefinitionException
    * @throws RemoteException
    * @throws MdException
    * @throws ServerException
    */
   public ICodeSegment getOrdinaryMappingCode(ICodeSegment codeSegment, 
            ITable sourceTable, 
            ITable targetTable,
            String mappingTargetTableName,
            String inputTableName,
            String sourceTableOptions,
            String targetTableOptions,
            boolean createView,
            boolean genComments,
            boolean genLabelStatements,
            IColumn[] passedOnlyColumns,  // ignore and extra columns are just add to select statement with no assignment
            IColumn[] excludeColumns,   // columns to exclude entirely from the extract code
            boolean useDistinctKeyword,
            String whereClause,
            IGroupBy groupByClause,
            ISorting orderByClause,
            boolean bGenExcludedColumnStatement,
            boolean bGenUnMappedColumnStatement,
            boolean bIsValidate
            
   )
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
	   boolean bGenerateFormatsInformats = true;
	   bGenerateFormatsInformats = isFormatGenerationEnabled();
      if (genComments)
         codeSegment.addSectionComment( RB.getStringResource( "AbstractDataTransform.MapColumns.msg.notrans" ) );
      
      if (!mappingTargetTableName.equalsIgnoreCase( inputTableName ))
      {
         IDBMSType dbmsType = DBMSTypeFactory.getDefaultType();
         if (targetTable!=null && targetTable instanceof IPhysicalTable)
            dbmsType = ((IPhysicalTable)targetTable).getDBMSType();
         boolean bSameTable = ObjectComparator.isEqual(sourceTable, targetTable);
         if (!bIsValidate && !(bSameTable))
         {
            // if the libref is work then no need to check the dbmstype, will assume SAS
            if (!ILibrary.WORK_LIBREF.equalsIgnoreCase( DBMSNamesUtil.getLibrefPart( mappingTargetTableName )))
               dbmsType.genTableDelete( codeSegment, false, mappingTargetTableName, targetTableOptions!=null ? codeSegment.parsePasswordOption(targetTableOptions,ICodeSegment.ALTER_PASSWORD_PATTERN) : "") ;
            else
               codeSegment.genTableDelete( mappingTargetTableName, 
                                        (targetTableOptions!=null ? codeSegment.parsePasswordOption(targetTableOptions,ICodeSegment.ALTER_PASSWORD_PATTERN) : "") );
         }
      }

      List lstPassedOnlyColumns = passedOnlyColumns!=null ? new ArrayList(Arrays.asList( passedOnlyColumns )) : new ArrayList();
      List lstExcludeColumns = excludeColumns!=null ? new ArrayList(Arrays.asList( excludeColumns )) : new ArrayList();


      IColumn[] targetColumns = targetTable.getColumns();

      // generate notes about mappings which have warnings
      for (int i=0; i<targetColumns.length; i++)
      {
         IColumn targetColumn = targetColumns[i];

         if (!lstExcludeColumns.contains( targetColumn ))
         {
             
            if (!lstPassedOnlyColumns.contains( targetColumn ))
            {
                
               IMapping mapping = getOrdinaryMappingsForTargetColumn( targetColumn );

               if (mapping!=null)
               {
                  generateMappingWarnings(codeSegment, new IMapping[]{mapping} );
               }
            }
         }
      }

      List ignoreUnmappedColumns = (new ArrayList(lstPassedOnlyColumns));
      ignoreUnmappedColumns.addAll( lstExcludeColumns );
      IColumn[] unmappedColumns = getOrdinaryUnmappedTargetColumns( ignoreUnmappedColumns );
      if (bGenUnMappedColumnStatement && unmappedColumns!=null && unmappedColumns.length>0)
      {
         codeSegment.addSourceCode("data _null_;\n")
         .indent()
         
         .genPutStatement( MessageFormat.format( RB.getStringResource( "AbstractDataTransform.UnmappedSetMissing.comment.notrans" ),new Object[]{codeSegment.makeColumnList( Arrays.asList( unmappedColumns ), ICodeSegment.INDENT, false, "," )} ))
         
         .unIndent()
         .addSourceCode( "run;\n\n");
      }
      if (bGenExcludedColumnStatement && lstExcludeColumns!=null && lstExcludeColumns.size()>0)
      {
         codeSegment.addSourceCode("data _null_;\n")
         .indent()
         
         .genPutStatement( MessageFormat.format( RB.getStringResource( "AbstractDataTransform.UnmappedExcluded.comment.notrans" ),new Object[]{codeSegment.makeColumnList( lstExcludeColumns, ICodeSegment.INDENT, false, "," )} ))
         
         .unIndent()
         .addSourceCode( "run;\n\n");
      }
      
      if (genComments)
         codeSegment.genPercentPutStatement( RB.getStringResource( "AbstractDataTransform.MappingColumnsNote.msg.sasmacro.notrans" ));
      
      codeSegment.addSourceCode( "proc sql" );
      if (bIsValidate)
         codeSegment.addSourceCode( " noexec" );
      codeSegment.addSourceCode(";\n")
      .indent()
      .addSourceCode( "create");
      if (createView)
         codeSegment.addSourceCode( " view ");
      else
         codeSegment.addSourceCode( " table " );

      if (mappingTargetTableName==null || mappingTargetTableName.length()==0)
         mappingTargetTableName = DEFAULT_MAPPING_TARGET_NAME;
      else if (mappingTargetTableName.indexOf( '.' ) == -1 && (targetTable==null || !targetTable.isWebStreamDataTarget()))
         mappingTargetTableName = ILibrary.WORK_LIBREF + "." + mappingTargetTableName;
      
      codeSegment.addSourceCode(mappingTargetTableName);
      
      if (targetTableOptions!=null && targetTableOptions.length()>0)
      {
         codeSegment.addSourceCode( "\n" )
         .indent()
         
         .addSourceCode( "(" )
         .addSourceCode(targetTableOptions)
         .addSourceCode(")")
         
         .unIndent()
         .addSourceCode( "\n" );
      }
      
      codeSegment.addSourceCode(" as\n" )
      .indent()
      .addSourceCode( "select" );
      
      if (useDistinctKeyword)
         codeSegment.addSourceCode( " distinct" );
      
      codeSegment.addSourceCode( "\n")
      .indent();
      
      // init commaNewLine to blank.  Assign to ",\n" after any valid col code gen.  Use before any [next] valid col code gen.
      String commaNewLine="";
      
      for (int i=0; i<targetColumns.length; i++)
      {
         IColumn targetColumn = targetColumns[i];

         if (!lstExcludeColumns.contains( targetColumn ))
         {
             
            if (!lstPassedOnlyColumns.contains( targetColumn ))
            {
                
               IMapping mapping = getOrdinaryMappingsForTargetColumn( targetColumn );

               if (mapping!=null)
               {
                  if (IMapping.DERIVED.equals(mapping.getType()) && mapping.getExpression()!=null)
                  {
                     IExpression exp = mapping.getExpression();
                     String expression = exp.getText(codeSegment.getCurrentServer(), codeSegment.isQuoting(), false );
                     expression = expression.replaceAll( "\\n", "\n" + CodeSegment.INDENT );
                     
                     codeSegment.addSourceCode( commaNewLine );
                     
                     codeSegment.addSourceCode("(").addSourceCode(expression.trim() ).addSourceCode( ")" );
                     codeSegment.addSourceCode(" as ");
                     codeSegment.addSourceCode(targetColumn.getAttribStatement( codeSegment.isQuoting(), false, bGenerateFormatsInformats, genLabelStatements ));
                     commaNewLine=",\n";
                  }
                  else if (IMapping.ONE_TO_ONE.equals(mapping.getType()))
                  {
                     IColumn sourceColumn = mapping.findSourceTableColumnInMapping( sourceTable );

                     // if this is MANY_TO_ONE need to make sure we find the column which belongs to the table we are searching for
                     if (sourceColumn==null)
                        continue;

                     codeSegment.addSourceCode( commaNewLine );
                     
                     if (!targetColumn.equalsName( sourceColumn, codeSegment.isQuoting() ))
                     {
                        codeSegment.addSourceCode( sourceColumn.getColumnName( codeSegment.isQuoting() ) );
                        codeSegment.addSourceCode(" as ");
                     }
                     codeSegment.addSourceCode( targetColumn.getColumnName( codeSegment.isQuoting() ) );
                     if (targetColumn.getType()!=sourceColumn.getType() || targetColumn.getLength()!=sourceColumn.getLength())
                        codeSegment.addSourceCode( " ").addSourceCode( targetColumn.getLengthStatement( false ) );
                     
                     codeSegment.indent();

                     if (bGenerateFormatsInformats)
                     {
	                     if (!targetColumn.equalsFormat( sourceColumn ) && !"".equals( targetColumn.getFormat()))
	                     {
	                        codeSegment.addSourceCode("\n").addSourceCode(targetColumn.getFormatStatement() );
	                     }

	                     if (!targetColumn.equalsInformat( sourceColumn ) && !"".equals( targetColumn.getInformat()))
	                     {
	                        codeSegment.addSourceCode("\n").addSourceCode(targetColumn.getInformatStatement() );
	                     }
                     }
                     if (genLabelStatements && !"".equals( targetColumn.getDescription() ) && !targetColumn.equalsLabel( sourceColumn ))
                     {
                        codeSegment.addSourceCode("\n").addSourceCode(targetColumn.getLabelStatement());
                     }
                     codeSegment.unIndent();
                     commaNewLine=",\n";
                  }
                  else
                  {
                     codeSegment.addSourceCode( commaNewLine );
                     if (targetColumn.getType() == IColumn.TYPE_CHARACTER)
                        codeSegment.addSourceCode( "\"\"" );
                     else
                        codeSegment.addSourceCode( "." );

                     codeSegment.addSourceCode(" as ");
                     codeSegment.addSourceCode( targetColumn.getAttribStatement( codeSegment.isQuoting(), false, bGenerateFormatsInformats, genLabelStatements ) );
                     commaNewLine=",\n";
                  }
               }
               else
               {
                  codeSegment.addSourceCode( commaNewLine );
                  if (targetColumn.getType() == IColumn.TYPE_CHARACTER)
                     codeSegment.addSourceCode( "\"\"" );
                  else
                     codeSegment.addSourceCode( "." );

                  codeSegment.addSourceCode(" as ");
                  codeSegment.addSourceCode( targetColumn.getAttribStatement( codeSegment.isQuoting(), false, bGenerateFormatsInformats, genLabelStatements ) );
                  commaNewLine=",\n";
               }
            }
            else
            {
               codeSegment.addSourceCode( commaNewLine );
               codeSegment.addSourceCode( targetColumn.getColumnName( codeSegment.isQuoting() ) );
               commaNewLine=",\n";
               
               // remove this column, because later any columns left in here will be listed in select statement
               lstPassedOnlyColumns.remove( targetColumn );
            }
         }
      }
      // go thru list again to make sure any left will be in select statement (mining results)
      for (int i=0; i<lstPassedOnlyColumns.size(); i++)
      {
         IColumn targetColumn = (IColumn)lstPassedOnlyColumns.get( i );
         codeSegment.addSourceCode( commaNewLine );
         codeSegment.addSourceCode( targetColumn.getColumnName( codeSegment.isQuoting() ) );
         commaNewLine=",\n";
      }
      
      codeSegment.addSourceCode( "\n" );
      
      if (inputTableName==null || inputTableName.length()==0)
         inputTableName = sourceTable.getFullNameQuotedAsNeeded( codeSegment );

      codeSegment.unIndent()
      .unIndent()
      .addSourceCode( "from ")
      .addSourceCode( inputTableName ).addSourceCode( "\n" );

      if (sourceTableOptions!=null && sourceTableOptions.length()>0)
      {
         codeSegment
         .indent()
         .addSourceCode( "(" ).addSourceCode(sourceTableOptions).addSourceCode(")")
         .unIndent()
         .addSourceCode( "\n" );
      }
      if (whereClause!=null && whereClause.length()>0)
      {
         whereClause = whereClause.replaceAll( "\n", "\n"+ICodeSegment.INDENT );
         
         codeSegment
         .indent()
         .addSourceCode( whereClause.trim() )
         .unIndent()
         .addSourceCode( "\n" )
         ;
      }
      if (groupByClause!=null )
      {
         StringBuffer code = groupByClause.getCode( codeSegment.isQuoting(), false );
         if (code!=null && code.length()>0)
         {
            codeSegment.addSourceCode( code );
         }
      }
      if (orderByClause!=null )
      {
         int iOldType = orderByClause.getSyntaxType();
         orderByClause.setSyntaxType( ISorting.SYNTAX_ORDERBY_CLAUSE );
         try
         {
            StringBuffer code = orderByClause.getCode( codeSegment.isQuoting(), false );
            if (code!=null && code.length()>0)
            {
               codeSegment.addSourceCode( code );
            }
         }
         finally
         {
            orderByClause.setSyntaxType( iOldType );
         }
      }
      
      codeSegment.addSourceCode( ";\n" )
      .unIndent()
      .addSourceCode( "quit;\n\n" );   

      codeSegment.addSourceCode( "%let SYSLAST = " ).addSourceCode( mappingTargetTableName ).addSourceCode(";\n\n");

      return codeSegment;
   }

   protected void generateMappingWarnings(ICodeSegment codeSegment, IMapping[] mappings)
   throws CodegenException, BadLibraryDefinitionException, RemoteException, MdException, BadServerDefinitionException, ServerException
   {
      if (mappings!=null)
      {
         for (int i=0; i<mappings.length; i++)
         {
            String warning = mappings[i].getWarning();
            if (warning!=null && warning.length()>0)
            {
               codeSegment.addSourceCode( "data _null_;\n" )
               .indent();
               codeSegment.genPutStatement( warning,getJob().isGenerateWarningOnMapping() ? ICodeSegment.WARNING_LABEL : ICodeSegment.NOTE_LABEL )
               .unIndent();
               codeSegment.addSourceCode("run;\n\n" );
               break;
            }
         }
      }
   }

   /**
    * Append the generated code for the transform either in validation mode or run mode
    * @param codeSegment code segment to append to
    * @param validateCode generate the code with no execution on to check for pushdown and valid syntax
    * @return codesegment
    * @throws CodegenException
    * @throws MdException
    * @throws RemoteException
    * @throws BadServerDefinitionException
    * @throws BadLibraryDefinitionException
    * @throws ServerException
    * @see com.sas.etl.models.job.transforms.sql.ISQLTransform#getGeneratedCode(com.sas.etl.models.job.ICodeSegment, boolean)
    */
   protected ICodeSegment getGeneratedCode( ICodeSegment codeSegment, boolean validateCode ) 
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      super.getGeneratedCode( codeSegment, validateCode );
      
      setConnectUsingFlagOnTables();
      String dbiExec = getDBIDirectExecValue();
      if (dbiExec!=null && dbiExec.length()>0)
      {
         if (DBI_DIRECT_EXEC.equals( dbiExec ))
         {
//            codeSegment.addSourceCode( "%let SYS_SQL_IP_SPEEDO = Y;\n" );
//            codeSegment.addSourceCode( "%let SYS_SQL_MAPPUTTO = sas_put;\n");
//            codeSegment.addSourceCode( "%let SYS_SQLREDUCEPUT = DBMS;\n");
            //Can take this %global out after proc sql defines it as globally automatically, probably by Oct. 13, 2008
            codeSegment.addSourceCode( "%global ").addSourceCode( ICodeSegment.ETLS_SQL_PUSHDOWN_MACRO_NAME ).addSourceCode(";\n");
            codeSegment.addSourceCode( "%let " ).addSourceCode(ICodeSegment.ETLS_SQL_PUSHDOWN_MACRO_NAME).addSourceCode(" = -1;\n");
         }
//         else
//         {
//            codeSegment.addSourceCode( "%let SYS_SQL_IP_SPEEDO = N;\n" );
//            codeSegment.addSourceCode( "%let SYS_SQL_MAPPUTTO = ;\n");
//            codeSegment.addSourceCode( "%let SYS_SQLREDUCEPUT = NONE;\n");
//         }
         codeSegment.addSourceCode( "option " ).addSourceCode( dbiExec ).addSourceCode( ";\n\n" );
      }
      
      return codeSegment;
   }

   /**
    * Get the setup codegen for the transform
    * 
    * @param codeSegment the code segment to put the generated code into
    * @param isRemote TRUE if the setup code needs to handle a remote appserver, FALSE otherwise
    * 
    * @return the code segment with the generated code added.
    * @throws MdException if the libname generation is incorrect
    * @throws RemoteException 
    * @throws BadLibraryDefinitionException 
    * @throws BadServerDefinitionException 
    * @throws ServerException 
    * @throws CodegenException 
    */
   public final ICodeSegment getTransformSetup(ICodeSegment codeSegment, boolean isRemote, boolean isValidate)
   throws MdException, RemoteException, BadLibraryDefinitionException, BadServerDefinitionException, CodegenException, ServerException
   {
      getValidVarNameCode( codeSegment, isRemote );

      // validation will assign it's own libnames
      if (!isValidate)
      {
         getGeneratedLibnameCodeForSources( codeSegment );

         getGeneratedLibnameCodeForTargets( codeSegment) ;
      }
      
      // if runtime statistics row counts are turned on
      if (codeSegment.isRunTableStatisticsEnabled())
         if (isCollectSourceTableRowCounts())
         {
            getGeneratedSourceRowCountCode( codeSegment );
         }
      
      // S0314355: Job status needs to get the first table and use it
      // since users can reorder control flow now this will be guarenteed to be the
      // right table;  in 3.4 this used to be on ClassiferMapCG;  putting it here now
      // Normally transforms wont overwrite this but loader and others do
      // If they do they should set this method to false, and then do their own thing.  AbstractLoaderTransform does
      // set it to false
      if (isGenDefaultJobStatusUpdate() && getJob().isSendJobStatusEnabled())
      {
         ITable[] targetTables = this.getTargetTables();
         if (targetTables != null)
         {
            for (int i=0;i<targetTables.length;i++)
            {
               ITable table = targetTables[i];
               if (table.getMetadataType().equalsIgnoreCase(MdFactory.PHYSICALTABLE))
               {
                  // we set this for the first one only
                  codeSegment.genJobStatusUpdateBefore((IPhysicalTable)table);
                  break;
               }
            }
         }
      }

      getSyslastCode( codeSegment );

      super.getTransformSetup( codeSegment, isRemote, isValidate );

      // S1053813
      // add code to handle ExternalTable as source or target and generated macro variables.
      // _INPUT (and _INPUTn) contain the external file location path and filename
      // _OUTPUT (and _OUTPUTn) work the same way for targets.
      // _INPUT_FILETYPE and _OUTPUT_FILETYPE contain the metadata type of the source/target.
      // Values should be either PhysicalTable or ExternalTable
      if (isUsingUserWrittenCode())
      {
         IDataObject[] sources = getDataSources();
         codeSegment.addSourceCode("%let _INPUT_count = " + sources.length + ";\n");
         for (int i=0; i<sources.length; i++)
         {
            IDataObject source = sources[i];
            if (source instanceof IPhysicalTable)
            {
               if (i==0)
               {
            	   codeSegment.addSourceCode(PromptUtils.getTablePercentLetStatements( codeSegment, "_INPUT", (IPhysicalTable)source, false,getTableOptionObject( (IPhysicalTable)source, true ),null, false,
            				  this.isColMacroVarsEnabled() ));
            	   
            	   codeSegment.addSourceCode("%let _INPUT_filetype = ").addSourceCode(source.getMetadataType())
            	   .addSourceCode( ";\n\n" );
               }
               codeSegment.addSourceCode(PromptUtils.getTablePercentLetStatements( codeSegment, "_INPUT"+ (i+1), (IPhysicalTable)source, false,getTableOptionObject( (IPhysicalTable)source, true ),null, false,
            		   this.isColMacroVarsEnabled()));
               
               codeSegment.addSourceCode("%let _INPUT" + (i + 1) + "_filetype = ").addSourceCode(source.getMetadataType())
               		.addSourceCode( ";\n\n" );
            }
            else if (source instanceof IExternalTable)
            {
            	if (this.isFileMacroVarsEnabled())
            	{
            		if (!(this instanceof FileReaderTransformModel) && !(this instanceof FileWriterTransformModel))
            			codeSegment.addSourceCode(source.getDefaultParameterCode().toString());
            		if (i==0)
            		{
            			codeSegment.addSourceCode("%let _INPUT = ").addSourceCode(((IExternalTable)source).getFullName())
            			.addSourceCode( ";\n" );
            			codeSegment.addSourceCode("%let _INPUT_filetype = ").addSourceCode(source.getMetadataType())
            			.addSourceCode( ";\n\n" );
            		}
            		codeSegment.addSourceCode("%let _INPUT" + (i+1) +" = ").addSourceCode(((IExternalTable)source).getFullName())
            		.addSourceCode( ";\n" );
            		codeSegment.addSourceCode("%let _INPUT" + (i + 1) + "_filetype = ").addSourceCode(source.getMetadataType())
            		.addSourceCode( ";\n\n" );
            	}
            }
         }

         IDataObject[] targets = getDataTargets();
         codeSegment.addSourceCode("%let _OUTPUT_count = " + targets.length + ";\n");
         for (int i=0; i<targets.length; i++)
         {
            IDataObject target = targets[i];
            if (target instanceof IPhysicalTable)
            {
               if (i==0)
               {
            	   codeSegment.addSourceCode(PromptUtils.getTablePercentLetStatements( codeSegment, "_OUTPUT", (IPhysicalTable)target, true,getTableOptionObject( (IPhysicalTable)target, false),
            			   getOrdinaryMappingsForTargetTable((IPhysicalTable) target ), true, this.isColMacroVarsEnabled()));
            			   
            	   codeSegment.addSourceCode("%let _OUTPUT_filetype = ").addSourceCode(target.getMetadataType())
                    	   .addSourceCode( ";\n\n" );
               }
               
               codeSegment.addSourceCode(PromptUtils.getTablePercentLetStatements( codeSegment, "_OUTPUT"+ (i+1), (IPhysicalTable)target, true,getTableOptionObject( (IPhysicalTable)target, false ),
            		   getOrdinaryMappingsForTargetTable((IPhysicalTable) target ), true, this.isColMacroVarsEnabled()));
               
    		   codeSegment.addSourceCode("%let _OUTPUT" + (i + 1) + "_filetype = ").addSourceCode(target.getMetadataType())
          		.addSourceCode( ";\n\n" );
            }
            else if (target instanceof IExternalTable)
            {
            	if (this.isFileMacroVarsEnabled())
            	{
            		if (!(this instanceof FileReaderTransformModel) && !(this instanceof FileWriterTransformModel))
            			codeSegment.addSourceCode(target.getDefaultParameterCode().toString());
            		if (i==0)
            		{
            			codeSegment.addSourceCode("%let _OUTPUT = ").addSourceCode(((IExternalTable)target).getFullName())
            			.addSourceCode( ";\n" );
            			codeSegment.addSourceCode("%let _OUTPUT_filetype = ").addSourceCode(target.getMetadataType())
            			.addSourceCode( ";\n\n" );
            		}
            		codeSegment.addSourceCode("%let _OUTPUT" + (i+1) +" = ").addSourceCode(((IExternalTable)target).getFullName())
            		.addSourceCode( ";\n" );
            		codeSegment.addSourceCode("%let _OUTPUT" + (i + 1) + "_filetype = ").addSourceCode(target.getMetadataType())
            		.addSourceCode( ";\n\n" );
            	}
            }
         }

      }
      return codeSegment;
   }
   
   public IMapping[] getOrdinaryMappingsForTargetTable(ITable target)
   {
      List maps = new ArrayList();
      IColumn[] columns = target.getColumns();
      for (int i=0; i<columns.length; i++)
      {
         IMapping map = getOrdinaryMappingsForTargetColumn( columns[i] );
         if (map!=null)
            maps.add(map);
      }
      return (IMapping[])maps.toArray( new IMapping[maps.size()] );
   }
   
   /**
    * Returns the default diagnostics for a transform.  The default diagnostics is 
    * a check to see if the source attached is a physicaltable, and if it is it makes sure that 
    * the physical table exists.
    * 
    * @param codeSegment the code segment to put the generated code into
    * 
    * @return the code segment with the generated code added.
    * @throws CodegenException
    */
   public ICodeSegment getPreDiagnostics(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      super.getPreDiagnostics( codeSegment );
      
      IDataObject[] sources = getDataSources();
      for (int i = 0; i<sources.length; i++)
      {
         if (sources[i] instanceof IPhysicalTable)
         {
            codeSegment.genDiagnosticTableExists( (IPhysicalTable )sources[i]);
         }
      }
      return codeSegment;
   }
   
  

    
   /**
    * Returns TRUE if the passed in table is a source of this transform
    * 
    * @param table to check to see if it is a source
    * @return TRUE if the table is a source, FALSE otherwise
    */
   public final boolean isTableSource(IPhysicalTable table)
   {
      IDataObject[] sources = getDataSources();
      for (int i=0; i<sources.length; i++)
      {
         if (sources[i]==table)
            return true;
      }
      return false;
   }
   
   /**
    * Generate the delete table code for this transform.  Delete is generated for the passed in
    * tables provided they are NOT a source, and they are a physical table.  
    * 
    * @param codeSegment  The main codesegment to use to put the generated code into
    * @param tables  Tables to generate delete for. 
    * @return the code segment with the generated code added.
    */
   public final ICodeSegment genTableDelete(ICodeSegment codeSegment, List tables)
   throws CodegenException, BadLibraryDefinitionException
   {
      List deleteTables = new ArrayList();
      
      for (int i=0; i<tables.size(); i++)
      {
         IDataObject table = (IDataObject)tables.get(i);
         if (table instanceof IPhysicalTable && !isTableSource( (IPhysicalTable)table ))
            deleteTables.add(table);
      }
      
      codeSegment.genTableDelete( deleteTables );
      
      return codeSegment;
   }
   /**
    * Gen the delete table statements for the transform.
    * 
    * 
    * @param codeSegment the code segment to put the generated code into
    * @param table the table to generate the delete statement for
    * 
    * @return the code segment with the generated code added.
    */
   public final ICodeSegment genTableDelete(ICodeSegment codeSegment, IPhysicalTable table)
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {
      if (!isTableSource( table ))      
         codeSegment.genTableDelete( table );
      
      return codeSegment;
   }
   
   /**
    * Returns the editable code for the transform
    * @param environment codegen environment
    * @return new codesegment
    * @throws CodegenException
    * @throws MdException
    * @throws RemoteException
    * @throws BadServerDefinitionException
    * @throws BadLibraryDefinitionException
    * @throws ServerException
    * @see com.sas.etl.models.job.impl.AbstractTransform#getBodyCode(com.sas.etl.models.job.ICodeGenerationEnvironment)
    */
   public ICodeSegment getBodyCode(ICodeGenerationEnvironment environment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      updateQuotingSetting( environment );
      return super.getBodyCode( environment );
   }
   
   /**
    * Updates the quoting setting for the environment based on the previous nodes in the job flow
    * @param environment environment to update
    */
   public final void updateQuotingSetting(ICodeGenerationEnvironment environment)
   {
      // need to fix the quoting setting on the environment
      if (!environment.isQuoting())
      {
         if (isQuotingNeeded())
            environment.setQuoting( true );
         else
         {
            List lTransforms = getJob().getControlOrderedTransformsList();
            for ( int iTransform=lTransforms.indexOf(this)-1; iTransform>=0; iTransform-- ) 
            {
               Object obj = lTransforms.get( iTransform );
               if (obj instanceof IDataTransform)
               {
                  IDataTransform dt = (IDataTransform) obj;
                  if (dt.isQuotingNeeded())
                  {
                     environment.setQuoting( true );
                     break;
                  }
               }
            }
         }
      }
   }
   
   /**
    * Generate the ValidVarName (handling for special characters in names of tables) code for the
    * transform.  
    * 
    * @param codeSegment the code segment to put the generated code into
    * @param isRemote TRUE if the statements have to go to a remote machine
    * 
    * @return the code segment with the generated code added.

    */
   public final ICodeSegment getValidVarNameCode(ICodeSegment codeSegment, boolean isRemote)
   {
      ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();
      
      updateQuotingSetting( environment );
      
      // need to set the special character setting for the environment so that validvarname is properly generated
      if (!environment.isSpecialCharactersNeeded())
      {
         if (isSpecialCharactersNeeded())
         {
            codeSegment.genValidvarnameOptionAny(isRemote);
            environment.setSpecialCharactersNeeded( true );
         }
         // check previous steps for quoting settings
         //  this is needed for generating the View Step Code to make sure it looks the same
         //  as the code for the job
         else
         {
            List lTransforms = getJob().getControlOrderedTransformsList();
            for ( int iTransform=lTransforms.indexOf(this)-1; iTransform>=0; iTransform-- ) 
            {
               Object obj = lTransforms.get( iTransform );
               if (obj instanceof IDataTransform)
               {
                  IDataTransform dt = (IDataTransform) obj;
                  if (dt.isSpecialCharactersNeeded())
                  {
//                     if (isRemote)
//                        codeSegment.addSourceCode("%let vvnOption = %sysfunc(getOption(VALIDVARNAME)); \n");
//                     codeSegment.addSourceCode("options VALIDVARNAME = ANY; \n\n");
                     codeSegment.genValidvarnameOptionAny(isRemote);
                     environment.setSpecialCharactersNeeded( true );
                     break;
                  }
               }
            }
         }
         
      }
      
      return codeSegment;
   }

   /**
    * Append the syslast code to the segment
    * @param codeSegment segment to append to
    * @return code segment
    */
   public final ICodeSegment getSyslastCode(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      
      IPhysicalTable prevTable = getPreviousSyslastTable();
      if (prevTable!=null)
         codeSegment.genSyslast( prevTable );
      
      return codeSegment;
   }

   private IPhysicalTable getPreviousSyslastTable()
   {
      IDataTransform transform = getSyslastDataTransform( this );
      IDataObject[] sources =  getDataSources();
      if (transform!=null )
      {
         if (isPreviousSyslastEnabled( transform ))
            sources = transform.getDataSources();
         else
            sources = null;
      }

      if (sources!=null && sources.length==1)
      {
         if (sources[0] instanceof IPhysicalTable)
            return (IPhysicalTable)sources[0];
      }
      return null;
   }
   
   private boolean isPreviousSyslastEnabled(IDataTransform transform)
   {
      IDataObject[] sources = transform.getDataSources();
      
      if (sources.length==1)
      {
         if (sources[0] instanceof IPhysicalTable)
         {
            IPhysicalTable pt = (IPhysicalTable)sources[0];
            IDataTransform[] producers = pt.getProducerTransforms(); 
           
            if (producers != null && producers.length>0)
            {
            	 for (int i=0; i < producers.length; i++)
                 {
                 	boolean isEnabled = producers[i].isSYSLASTVariableGenerationEnabled();
                 	if (isEnabled == true)
                 		return true;
                 }
            }
            else if (producers==null || producers.length==0)
               return true;                    
         }
      }
      return false;
   }
   
   private IDataTransform getSyslastDataTransform(IDataTransform transform)
   {
      IDataObject[] sources = transform.getDataSources();
      
      if (sources.length==1)
      {
         if (sources[0] instanceof IPhysicalTable)
         {
            IPhysicalTable pt = (IPhysicalTable)sources[0];
            IDataTransform[] producers = pt.getProducerTransforms(); 
            IDataTransform producer = null;
            if (producers != null && producers.length>0)
            {
               producer = producers[0];
               
               // if codegen is enabled return this one, if not search farther back
               if (!producer.isCodeGenerationEnabled())
               {
                  IDataTransform prevTransform = getSyslastDataTransform( producer );
                  // this should stop at the first one if none of the priors are active
                  if (prevTransform==null)
                     return producer;
                  else if (prevTransform.isCodeGenerationEnabled())
                  {
                     return producer;
                  }
                  else
                  {
                     return getSyslastDataTransform( prevTransform );
                  }
               }
               else
               {
                  return transform;
               }
            }
         }
      }
      return null;
   }
   

   public ICodeSegment getGeneratedLibnameCodeForSources( ICodeSegment codeSegment )
   throws MdException, 
   RemoteException, 
   BadServerDefinitionException, 
   BadLibraryDefinitionException, 
   CodegenException,
   ServerException
   {
      IDataObject[] sources = getDataSources();

      for (int i=0; i<sources.length; i++)
      {
         if (sources[i] instanceof IPhysicalTable)
         {
            IPhysicalTable table = (IPhysicalTable)sources[i];
            
            table.genAccessPath( codeSegment,codeSegment.getCodeGenerationEnvironment().isGenerateRCSetCalls(),-1, codeSegment.getRuntimeStatsConnectMacros( codeSegment ), codeSegment.isRunStatisticsEnabled(), codeSegment.isRunTableStatisticsEnabled(), getTableOptionObject( table, true ) );   
         }
      }   
      return codeSegment;
   }

   /**
    * Append the post code for the transform
    * @param codeSegment segment to append to 
    * @return segment
    * @throws CodegenException
    * @throws MdException
    * @throws RemoteException
    * @throws BadLibraryDefinitionException
    * @throws ServerException
    * @see com.sas.etl.models.job.impl.AbstractTransform#getTransformCompletion(com.sas.etl.models.job.ICodeSegment)
    */
   public final ICodeSegment getTransformCompletion(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadLibraryDefinitionException, ServerException, BadServerDefinitionException
   {     
      getGenerateTableIndexes( codeSegment );
      
      // S0314355: Job status needs to get the first table and use it
      // since users can reorder control flow now this will be guarenteed to be the
      // right table;  in 3.4 this used to be on ClassiferMapCG;  putting it here now
      // Normally transforms wont overwrite this but loader and others do
      // If a transform overwrites it, they should set isGenDefautltJobStatusUpdate to false
      // and then do their own thing in the transform
      if (isGenDefaultJobStatusUpdate() && getJob().isSendJobStatusEnabled())
      {
         ITable[] targetTables = this.getTargetTables();
         if (targetTables != null)
         {
            for (int i=0;i<targetTables.length;i++)
            {
               ITable table = targetTables[i];
               if (table.getMetadataType().equalsIgnoreCase(MdFactory.PHYSICALTABLE))
               {
                  // we set this for the first one only
                  codeSegment.genJobStatusUpdateAfter((IPhysicalTable)table);
                  break;
               }
            }
         }
      }
      
      if (codeSegment.isRunTableStatisticsEnabled())
         if (isCollectTargetTableRowCounts())
            getGeneratedTargetRowCountCode( codeSegment );
      
      return super.getTransformCompletion( codeSegment );
   }

   public ICodeSegment getGenerateTableIndexes(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      if (isGenerateIndexesOnTargetTables())
      {
         IDataObject[] targets = getDataTargets();

         IServer currentServer = codeSegment.getCurrentServer();
         ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();
         
         for (int i=0; i<targets.length; i++)
         {
            if (targets[i] instanceof IPhysicalTable && !((IPhysicalTable)targets[i]).isView())
            {
               IPhysicalTable pt = (IPhysicalTable)targets[i];
               
               boolean remote = false;
               
               ILibrary library = pt.getClientLibrary( codeSegment );
               IServer libServer = null;
               if (library!=null)
               {
                  library.genAccessPath( codeSegment );
               
                  libServer = library.getBestServer( codeSegment.getCurrentServer() );
                  
                  remote = !ObjectComparator.isEqual( libServer,codeSegment.getCurrentServer());
                  if (remote)
                  {
                     ISASClientConnection conn = libServer.getConnectClient();

                     if (conn==null)
                        throw new CodegenException(MessageFormat.format( RB.getStringResource( "Connect.MissingConnection.txt" ),new String[]{libServer.getName()}), this);

                     if (!environment.isOnSignonCache( libServer ))
                     {
                        conn.genAccessCode( codeSegment);

                        environment.addToSignonCache( libServer );
                     }
//stwatk S0523533
                     IJob job = getJob();
                     codeSegment.genReturnCodeRemoteSetup(conn, codeSegment.getRuntimeStatsConnectMacros( codeSegment ), false, job.isRCSetSYSCCEnabled());
                     codeSegment.genRemoteMacroVariablesSetup(environment.getRemoteMacroVariables(),conn.getHostName(), true).addSourceCode("\n");

                     conn.genStartSubmit(ICodeSegment.SYSRPUTSYNC_YES,codeSegment,true, codeSegment.isRunStatisticsEnabled(),codeSegment.isRunTableStatisticsEnabled());
                     
                     environment.setCurrentServer( libServer );
                     
                     codeSegment.indent().addSourceCode("\n");

                     if (codeSegment.isQuoting())
                        codeSegment.genValidvarnameOptionAny( false );

                     // wrap remote code in macro call to avoid %let statements not resolving when
                     //   rsubmit is within macro
                     codeSegment.addSourceCode("%macro ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("(); \n")
                     .indent();
                  }
               }
               pt.getDBMSType().createIndexes2( codeSegment, pt, null, "" );
               
               if (remote)
               {
                  ISASClientConnection conn = libServer.getConnectClient();
//stwatk S0523533
                  codeSegment.genReturnCodeRemoteEnding(false, false, getJob().isRCSetSYSCCEnabled());
                  codeSegment.unIndent();
                  codeSegment.addSourceCode("\n%mend ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("; \n\n")
                  .addSourceCode("%").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode(";\n\n"); //S0359689
                  codeSegment.unIndent();

                  conn.genEndSubmit(codeSegment, getJob().isRCSetSYSCCEnabled());
                 
                  environment.setCurrentServer( currentServer );
               }
            }
         }
      }
      return codeSegment;
   }

   
   public ICodeSegment getGeneratedLibnameCodeForTargets( ICodeSegment codeSegment )
   throws MdException, 
   RemoteException, 
   BadServerDefinitionException, 
   BadLibraryDefinitionException, 
   CodegenException,
   ServerException
   {
      IDataObject[] targets = getDataTargets();

      for (int i=0; i<targets.length; i++)
      {
         if (targets[i] instanceof IPhysicalTable)
         {
            IPhysicalTable table = (IPhysicalTable)targets[i];

            table.genAccessPath( codeSegment,codeSegment.getCodeGenerationEnvironment().isGenerateRCSetCalls(),-1,codeSegment.getRuntimeStatsConnectMacros(codeSegment), codeSegment.isRunStatisticsEnabled(), codeSegment.isRunTableStatisticsEnabled(), getTableOptionObject( table, false ) );
          
         }
      }   
      return codeSegment;
   }
   
   /**
    * Returns whether source table row counts should be collected
    * @return true if the source table row counts should be collected 
    */
   public boolean isCollectSourceTableRowCounts()
   {
      return m_collectSourceTableRowCounts;
   }

   /**
    * Sets whether source table row counts should be collected
    * @param collectSourceTableRowCounts true if they should be collected
    */
   public void setCollectSourceTableRowCounts(boolean collectSourceTableRowCounts)
   {
      m_collectSourceTableRowCounts = collectSourceTableRowCounts;
   }

   /**
    * Returns whether target table row counts should be collected
    * @return true if the target table row counts should be collected 
    */   
   public boolean isCollectTargetTableRowCounts()
   {
      return m_collectTargetTableRowCounts;
   }
   
   /**
    * Sets whether target table row counts should be collected
    * @param collectTargetTableRowCounts true if they should be collected
    */
   public void setCollectTargetTableRowCounts(boolean collectTargetTableRowCounts)
   {
      m_collectTargetTableRowCounts = collectTargetTableRowCounts;
   } 
   
   /**
    * Default implementation for gathering table counts for a source table
    * This method should be overridden to only return a code segment if table 
    * counts should be done for a target
    * @param codeSegment segment
    * @return
    * @throws MdException metadata exception
    * @throws RemoteException remote exception
    */
   public ICodeSegment getGeneratedSourceRowCountCode( ICodeSegment codeSegment )
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {      
      IDataObject[] sources = getDataSources();

      if (sources.length > 0)
         // the Abstract case only handles one data source
         if (sources[0] instanceof IPhysicalTable)         
            getGeneratedTableRowCountCode(codeSegment, (IPhysicalTable)sources[0], null);
            
      return codeSegment;
   }

   /**
    * Default implementation for gathering table counts for a target table
    * This method should be overridden to only return a code segment if table 
    * counts should be done for a source
    * @param codeSegment segment
    * @return
    * @throws MdException metadata exception
    * @throws RemoteException remote exception
    */
   public ICodeSegment getGeneratedTargetRowCountCode( ICodeSegment codeSegment )
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {      
      IDataObject[] targets = getDataTargets();

      if (targets.length > 0)
         // the Abstract case only handles one data source
         if (targets[0] instanceof IPhysicalTable  || targets[0] instanceof IWorkTable)         
            getGeneratedTableRowCountCode(codeSegment, (IPhysicalTable)targets[0], null);
            
      return codeSegment;
   }

   /**
    * Default implementation of getting row counts for a given physical table
    * @param codeSegment segment
    * @param table physical table
    * @param macro variable
    * @return 
    * @throws MdException metadata exception
    * @throws RemoteException remote exception
    */
   public ICodeSegment getGeneratedTableRowCountCode(ICodeSegment codeSegment,
         IPhysicalTable table, String macroVar) 
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {               
      return codeSegment.genRowsProcessedCount(table, macroVar);
   }

   public IPhysicalTable[] getTablesForTransfer(IServer defaultServer)
   throws BadServerDefinitionException, BadLibraryDefinitionException
   {
	   List<IPhysicalTable> transfers = new ArrayList<IPhysicalTable>();

	   // have to move data to next step's server
	   IDataObject[] sources = getDataSources();

	   for (int i=0; i<sources.length; i++)
	   {
		   if (sources[i] instanceof IWorkTable)
		   {
			   IWorkTable table = (IWorkTable)sources[i];

			   IDataTransform[] tableProducers = table.getProducerTransforms();
			   IDataTransform tableProducer = null;
			   if (tableProducers != null && tableProducers.length>0)
				   tableProducer = tableProducers[0];

			   if (tableProducer!=null && !tableProducer.isTargetDataAutomaticallyMoved())
			   {
				   IServer stepServer = getServerForStep(defaultServer);
				   
				   IServer previousServer = tableProducer.getServerForStep( defaultServer );

				   if (!ObjectComparator.isEqual(previousServer,stepServer ) )
				   {
					   transfers.add(table);
				   }
			   }
		   }
		   else if (sources[i] instanceof IPhysicalTable)
		   {
			   IServer stepServer = getServerForStep(defaultServer);
			   IPhysicalTable table = (IPhysicalTable)sources[i];
			   if (table.isRemoteToServer(stepServer))
				   transfers.add(table);
		   }
	   }

	   return transfers.toArray(new IPhysicalTable[transfers.size()]);
   }
   
   public boolean isTransformPerformingDataTransfer( IServer defaultServer)
   throws BadServerDefinitionException, BadLibraryDefinitionException
   {
	   IPhysicalTable[] tables = getTablesForTransfer( defaultServer);
	   return tables!=null && tables.length>0;
   }

   public ICodeSegment getGeneratedRemoteCodeStart(IServer stepServer,ICodeSegment codeSegment, IServer currentServer, boolean isValidate)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();

      // need to move source data if it was from different server than this step

      // have to move data to next step's server
      IDataObject[] sources = getDataSources();

      for (int i=0; i<sources.length; i++)
      {
         if (sources[i] instanceof IWorkTable)
         {
            IWorkTable table = (IWorkTable)sources[i];

            IDataTransform[] tableProducers = table.getProducerTransforms();
            IDataTransform tableProducer = null;
            if (tableProducers != null && tableProducers.length>0)
               tableProducer = tableProducers[0];
            
            if (tableProducer!=null && !tableProducer.isTargetDataAutomaticallyMoved())
            {
               IServer previousServer = tableProducer.getServerForStep( currentServer );
              
               if (!ObjectComparator.isEqual(previousServer,stepServer ) )
               {
                  ISASClientConnection conn = stepServer.getConnectClient();

                  if (conn==null)
                     throw new CodegenException(MessageFormat.format( RB.getStringResource( "Connect.MissingConnection.txt" ),new String[]{stepServer.getName()}), this);

                  if (conn!=null)
                  {
                     codeSegment.addSectionComment(RB.getStringResource("Transfer.ImplicitComment.msg.txt"));
                     if (!environment.isOnSignonCache( stepServer ))
                     {
                     	 //S0953027: submit quoting table options if needed
                        if (table.isQuoted())
                        	codeSegment.addSourceCode(CodeSegment.getValidvarnameOptionAny( false ) );
                        
                        conn.genAccessCode( codeSegment);

                        environment.addToSignonCache( stepServer );
                     }
//stwatk S0523533
                     IJob job = getJob();
                     codeSegment.genReturnCodeRemoteSetup(conn, codeSegment.getRuntimeStatsConnectMacros( codeSegment ), isValidate, job.isRCSetSYSCCEnabled());
                     codeSegment.genRemoteMacroVariablesSetup(environment.getRemoteMacroVariables(),conn.getHostName(), true).addSourceCode("\n");

                     conn.genStartSubmit(ICodeSegment.SYSRPUTSYNC_YES,codeSegment,true, codeSegment.isRunStatisticsEnabled(),codeSegment.isRunTableStatisticsEnabled());
                     codeSegment.indent().addSourceCode("\n");

                     // wrap remote code in macro call to avoid %let statements not resolving when
                     //   rsubmit is within macro
                     codeSegment.addSourceCode("%macro ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("(); \n")
                     .indent();

//                   cgReq.addSourceCode(nextServSupp.doReturnCodeSetup());
                     if (!isValidate)
                     table.genUploadCode(codeSegment,getTableOptionObject( table, true )).addSourceCode("\n")
                     .genRCSetCall( "&syserr" );

                     ILibrary lib = table.getCodeGenLibrary( stepServer );
                    
                     if (lib != null)
                     {
                        codeSegment.addSourceCode("libname ").addSourceCode(lib.getLibref())
                        .addSourceCode(" (").addSourceCode(ILibrary.WORK_LIBREF).addSourceCode("); \n");
                        // capture return code into macros
                        codeSegment.genRCSetCall("&syslibrc");   /*I18nOK:LINE*/
                     }
//stwatk S0523533
                     codeSegment.genReturnCodeRemoteEnding(isValidate, false, getJob().isRCSetSYSCCEnabled());
                     codeSegment.unIndent();
                     codeSegment.addSourceCode("\n%mend ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("; \n\n")
                     .addSourceCode("%").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode(";\n\n"); //S0359689
                     codeSegment.unIndent();

                    conn.genEndSubmit(codeSegment, getJob().isRCSetSYSCCEnabled());
                  }

               }
            }
         }
      }   

      return super.getGeneratedRemoteCodeStart( stepServer, codeSegment,currentServer, isValidate );
   }

   public ICodeSegment getGeneratedRemoteCodeEnd(IServer stepServer, ICodeSegment codeSegment, IServer defaultServer, boolean isValidate)
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {
      // this step was remote

      // have to move data to next step's server
      IDataObject[] targets = getDataTargets();

      for (int i=0; i<targets.length; i++)
      {
         if (targets[i] instanceof IWorkTable)
         {
            IWorkTable table = (IWorkTable)targets[i];

            IDataTransform[] tableConsumers = table.getConsumerTransforms();

            for (int j=0; j<tableConsumers.length; j++)
            {
               if (!ObjectComparator.isEqual(stepServer,tableConsumers[j].getServerForStep( defaultServer )))
               {
                  codeSegment.addSectionComment(RB.getStringResource("Transfer.ImplicitComment.msg.txt"));
                  table.genDownloadCode(codeSegment,getTableOptionObject( table, false )).addSourceCode("\n")
                  .genRCSetCall( "&syserr" );
                  
               }
            }
         }
      }   

      return super.getGeneratedRemoteCodeEnd( stepServer, codeSegment,defaultServer, isValidate );
   }

   public boolean isMappingNeeded(boolean quoting,
            IPhysicalTable sourceTable, 
            IPhysicalTable targetTable
            )
   {
      return isMappingNeeded( quoting, sourceTable, targetTable, null, true );
   }
   
   /**
    * Change to call isMappingNeeded with all 5 arguments (#5 as true), instead of using this old signature
    * @deprecated 
    */
   public boolean isMappingNeeded(boolean quoting,
            ITable sourceTable, 
            ITable targetTable,
            List ignoreColumns)
   {
      boolean needed = false;
      
      return isMappingNeeded( quoting, sourceTable, targetTable, null, true );
   }
   
   
   /**
   * Check if the a SQL Mapping step (codegen) is needed based on column differences between
   * source and target.  Returned boolean can be used to optionally call getOrdinaryMappingCode( ) to add the standard SQL mapping step. 
   * @param quoting typically: codeSegment.isQuoting()
   * @param sourceTable primary table feeding transform 
   * @param targetTable primary target table to transform
   * @param ignoreColumns columns in target that should be ignored (ex. extra tgt cols transform will set, so no extra SQL mapping step is needed to add them.
   * @param columnCountsMustMatch  Pass in true (typical) if tgt/src column count mismatch should trigger SQL mapping.  (Lookup passes a false)
   * @return true if mapping is needed, false if it is not
   */
    public boolean isMappingNeeded(boolean quoting,
             ITable sourceTable, 
             ITable targetTable,
             List ignoreColumns,
             boolean columnCountsMustMatch)
    {
      boolean needed = false;
      
      // first check column numbers
      //  if column counts are different then needed = true
      if (columnCountsMustMatch && sourceTable.getColumnCount()!=targetTable.getColumnCount())
         needed = true;
      else
      {
         int targetSize = targetTable.getColumnCount();
         for (int i=0; i<targetSize; i++)
         {
            IColumn targetColumn = targetTable.getColumns()[i];
            if (ignoreColumns==null || !ignoreColumns.contains( targetColumn ))
            {
               IMapping mapping = getOrdinaryMappingsForTargetColumn( targetColumn );
               if (mapping==null || 
                        mapping.isMappingNeeded(quoting,sourceTable  ))
               {
                  needed = true;
                  break;
               }
            }
         }
      }
      
      return needed;
   }
   
   
   public boolean isTargetDataAutomaticallyMoved()
   {
      return m_bTargetDataAutomaticallyMoved;
   }
   
   protected void setTargetDataAutomaticallyMoved(boolean bTargetDataMoved)
   {
      m_bTargetDataAutomaticallyMoved = bTargetDataMoved;
   }
   
   /**
    * update the DBMS Type for execution of source tables
    */
   public void updateDBMSExecutionType(  ) 
   {
      boolean bSameTypes = true;
      IDBMSType type = null;
      List lSources = getDataSourceList();
      for (int iSource=0; iSource<lSources.size(); iSource++)
      {
         if (lSources.get( iSource ) instanceof IPhysicalTable)
         {
            IPhysicalTable table = (IPhysicalTable)lSources.get( iSource );
            if (type == null)
               type = table.getDBMSType();
            if (type != table.getDBMSType())
            {   
               bSameTypes = false;
               break;
            }   
         }
      }
      if (bSameTypes)
      {   
         setDBMSExecutionType( type!=null ? type.getDBMSTypeID() : IDataTransform.SAS_DBMS_EXECUTION_TYPE );
         setDBMSExecutionTypeName( type!=null ? type.getDBMSTypeName() : IDBMSType.SAS_DBMS_TYPE_NAME );
      }   
      else
      {   
         setDBMSExecutionType( IDataTransform.SAS_DBMS_EXECUTION_TYPE );
         setDBMSExecutionTypeName( IDBMSType.SAS_DBMS_TYPE_NAME );
      } 
      fireModelChangedEvent( REFRESH_DBMS_TYPE, null );

   }
  /**
   * Set the DBMS type that the transform code is executed on
   * @param iDBMSType the DBMSType the code is executed on
   */
  public String getDBMSExecutionTypeName(  ) 
  {
     return m_sDBMSTypeName;
  }
  /**
   * 
   * Set the DBMS type name that the transform code is executed on
   * @param sDBMSTypeName the DBMSType the code is executed on
   */
  protected void setDBMSExecutionTypeName( String sDBMSTypeName ) 
  {
      m_sDBMSTypeName = sDBMSTypeName;
  }
   
   /**
    * Set the DBMS type that the transform code is executed on
    * @param iDBMSType the DBMSType the code is executed on
    */
   protected void setDBMSExecutionType( int iDBMSType ) 
   {
      if (m_iDBMSType == iDBMSType)
         return;

      m_iDBMSType = iDBMSType;
      fireModelChangedEvent( DBMS_EXECUTION_TYPE_CHANGED, null );
   }
   
   /**
    * Get the DBMS type that the transform code is executed on
    * @return the DBMSType the code is executed on
    */
   public int getDBMSExecutionType() 
   {
      return m_iDBMSType; 
   }
   
   /**
    * Called from the post add data source or target when adding a table to 
    * the transformation
    * table option.
    * @param table the data table
    * @param isSource true, if the table is a source table
    */
   protected void postAddTransformTableOption(IDataObject table, boolean isSource)
   {
      if (table instanceof IPhysicalTable)
      {
    	 IPhysicalTable pt = (IPhysicalTable) table;
         addTransformTableOption( createTransformTableOption( pt, isSource ) );

         pt.getDBMSType().updateTransformTableOptions(getTableOptionObject(pt, isSource), isSource);
      }
   }
   
   /**
    * Called from the pre remove data source or target when removing a table
    * from the transformation
    * @param table the table being removed
    * @param isSource true, if the table is a source table
    */
   protected void preRemoveTransformTableOptions(IDataObject table, boolean isSource)
   {
      if (table instanceof IPhysicalTable)
      {
         ITransformTableOptions tableOptions = getTableOptionObject( (IPhysicalTable)table, isSource );
         if (tableOptions!=null)
            removeTransformTableOption( tableOptions );
      }
   }
   
   
   /**
    * Does the table have a table option object?
    * @param table the data table
    * @param isSource true, if the table is a source table
    * @return true, if the table has a table option object
    * @see com.sas.etl.models.job.IDataTransform#hasTableOptionObject(com.sas.etl.models.data.IPhysicalTable, boolean)
    */
   public boolean hasTableOptionObject(IPhysicalTable table, boolean isSource)
   {
      return getTableOptionObject( table, isSource )!=null;
   }
   
   /**
    * Get the table option object for a specified table.
    * @param table the data table
    * @param isSource true, if the table is a source table
    * @return the table option object
    * @see com.sas.etl.models.job.IDataTransform#getTableOptionObject(com.sas.etl.models.data.IPhysicalTable, boolean)
    */
   public ITransformTableOptions getTableOptionObject(IPhysicalTable table, boolean isSource)
   {
	   return getTableOptionObject(table,isSource,getTableOptionObjects());
   }
   
   /**
    * Get the table option object for a specified table.
    * @param table the data table
    * @param isSource true, if the table is a source table
    * @return the table option object
    * @see com.sas.etl.models.job.IDataTransform#getTableOptionObject(com.sas.etl.models.data.IPhysicalTable, boolean)
    */
   protected ITransformTableOptions getTableOptionObject(IPhysicalTable table, boolean isSource, ITransformTableOptions[] opts)
   {
	  
      for (int i=0; i<opts.length;i++)
      {
         if (opts[i].getOptionTable()==table && (opts[i].isAccessTypeInput() == isSource))
            return opts[i];
      }
      return null;
   }
   
   /**
    * Add a new transform table option object
    * @param optionSet the option set
    */
   protected void addTransformTableOption(ITransformTableOptions optionSet)
   {
      if (m_lTableOptionModels.contains( optionSet ))
         return;
      
      if (optionSet!=null)
         removeFromDeletedObjects( optionSet );
      
      if (isUndoSupported())
         undoableEditHappened( new AddTransformTableOptionUndoable(optionSet) );
      
      // if there is already a transform option for this table remove it and use the new one
      ITransformTableOptions lddOpts = getTableOptionObject( optionSet.getOptionTable(), optionSet.isAccessTypeInput() );
      if (lddOpts!=null)
         removeTransformTableOption( lddOpts );

      m_lTableOptionModels.add( optionSet );
      
      fireModelChangedEvent( TRANSFORM_TABLE_OPTION_ADDED, optionSet );
   }
   
   /**
    * Remove the transform table option object.
    * @param optionSet the object set
    */
   protected void removeTransformTableOption(ITransformTableOptions optionSet)
   {
      if (!m_lTableOptionModels.contains( optionSet ))
         return;
      
      if (optionSet!=null)
         addToDeletedObjects( optionSet );
      
      if (isUndoSupported())
         undoableEditHappened( new RemoveTransformTableOptionUndoable(optionSet) );
      
      m_lTableOptionModels.remove(optionSet);

      fireModelChangedEvent( TRANSFORM_TABLE_OPTION_REMOVED, optionSet );
   }
   
   
   /**
    * Create the transform table option model object
    * @param table 
    * @param isSource true, if source table, otherwise false
    * @return the transform table option model
    */
   protected ITransformTableOptions createTransformTableOption(IPhysicalTable table, boolean isSource)
   {
      ITransformTableOptions optionSet = new BaseTransformTableOptions(createIDForNewObject(),getModel());
      optionSet.setOptionTable( table );
      optionSet.setAccessTypeInput( isSource );
      optionSet.setOwner( this );
      
      return optionSet;
   }
   
   /**
    * Get an array of data set option objects
    * @return an array of data set options object
    * @see com.sas.etl.models.job.IDataTransform#getTableOptionObjects()
    */
   public ITransformTableOptions[] getTableOptionObjects()
   {
      return (ITransformTableOptions[]) m_lTableOptionModels.toArray( new ITransformTableOptions[ m_lTableOptionModels.size() ] );
   }

   /**
    * Get the app server for this step, takes default server passed in as consideration for the host selection.
    * 
    * @param defaultServer the default app server, if this method is to take the default server into consideration, otherwise null
    * 
    * @return The ServerContext for this step
    * @throws BadServerDefinitionException  if the appserver is not valid 
    * @throws BadLibraryDefinitionException if the library is not valid
    * 
    */
   public IServer getServerForStep(IServer defaultServer) 
   throws BadServerDefinitionException, BadLibraryDefinitionException
   {
      IServer serverStep = null;
      
      IDataObject[] targets = getDataTargets();
      for (int i=0; i<targets.length; i++)
      {
         // this will determine if a transform is equivalent to a loader
         if (targets[i] instanceof IPhysicalTable)
         {
            IPhysicalTable target = (IPhysicalTable)targets[i];
            ILibrary library = target.getCodeGenLibrary( defaultServer );
            if (library!=null)
            {
               serverStep = library.getBestServer( defaultServer );
            }
         }
      }

      if (serverStep == null)
      {
         IServer requestedServer = getExecutionServer();
         if (requestedServer!=null)
            serverStep = requestedServer;
         else
            serverStep = defaultServer;
      }

      return serverStep;
      
      //TODO fix external tables
//      AssociationList lCMs = step.getTransformations();
//      for (int i = 0; i < lCMs.size(); i++)
//      {
//         if (lCMs.get(i) instanceof ClassifierMap)
//         {
//            ClassifierMap cm = (ClassifierMap)lCMs.get(i);
//            
//            if (CodeGenUtil.isLoaderTransformation(cm)) 
//            {
//               found = true;
//               AssociationList lTabs = cm.getClassifierTargets();
//               
//               for (int j = 0; j < lTabs.size(); j++)
//               {
//                  DataTable table = (DataTable)lTabs.get(j);
//                  RelationalSchema tablePackage = table.getTablePackage();
//                  
//                  if (tablePackage == null && table.getCMetadataType().equals(MdFactory.PHYSICALTABLE))
//                  {
//                     String tableName = table.getName();
//                     if (table instanceof PhysicalTable)
//                        tableName = ((PhysicalTable)table).getSASTableName();
//                     
//                     if (cgReq==null || cgReq.isThrowingExceptions())
//                        throw new CodegenException(bundle.formatString("PROCESS.NoLibraryForTable.msg.txt", tableName));
//                     
//                  } // if - no library
//                  
//                  if (tablePackage instanceof SASLibrary)
//                  {
//                     lHosts.addAll(LibraryUtil.getClientLibrary((SASLibrary)tablePackage, server).getDeployedComponents());
//                  } // if - SASLibrary
//                  else if (tablePackage!=null)
//                  {
//                     AssociationList libs = tablePackage.getUsedByPackages();
//                     for (int k = 0; k < libs.size(); k++)
//                     {
//                        lHosts.addAll(LibraryUtil.getClientLibrary(((SASLibrary)libs.get(k)), server).getDeployedComponents());
//                     } // for k
//                     
//                  } // else
//               } // for j
//               
//               if (defaultServer!=null )
//               {
//                  boolean localFound = false;
//                  for (int k=0; k<lHosts.size(); k++)
//                  {
//                     if (((ServerContext)lHosts.get(k)).getId().equals(defaultServer.getId()))
//                     {
//                        localFound = true;
//                        break;
//                     }
//                  }
//                  if (localFound)
//                  {
//                     lHosts.clear();
//                     lHosts.add(defaultServer);
//                  }
//               }
//               
//            } // if - this is a loader
//            
//            else
//            {
//               // find out if this is a file writer - need to treat this the same as a loader
//               AssociationList outputs = cm.getClassifierTargets();     
//               if (! outputs.isEmpty())
//               {
//                  Object output = outputs.get(0);
//                  if (output instanceof ExternalTable)
//                  {
//                     ExternalTableCG tableCG = (ExternalTableCG) CodeGenUtil.getCodeGenClass((ExternalTable)output);
//                     EFIDataModel efiModel = tableCG.getModel();
//                     
//                     ServerContext etServer = (ServerContext) efiModel.getAppServerDC();
//                     
//                     if (etServer == null)
//                        lHosts.add(server);
//                     else
//                        lHosts.add(etServer);
//                     
//                     found = true;
//                     
//                  } // if - the source to this step is an ExternalTable
//                  
//               } // if - there is an input
//               
//               AssociationList inputs = cm.getClassifierSources();     
//               if (! inputs.isEmpty())
//               {
//                  Object input = inputs.get(0);
//                  if (input instanceof ExternalTable)
//                  {
//                     ExternalTableCG tableCG = (ExternalTableCG) CodeGenUtil.getCodeGenClass((ExternalTable)input);
//                     EFIDataModel efiModel = tableCG.getModel();
//                     ServerContext etServer = (ServerContext) efiModel.getAppServerDC();
//                     if (etServer == null)
//                        lHosts.add(server);
//                     else
//                        lHosts.add(etServer);
//                     
//                     found = true;
//                  } // if - the source to this step is an ExternalTable
//                  
//               } // if - there is an input
//               
//               
//            } // else - this is not a loader
//            
//         }
//      } // for i
//
//      if (!found)
//      {
//         lHosts = step.getComputeLocations();
//      } // if
//
//      if (lHosts.size() == 0)
//         return null;
//      else
//      {
//         for (int i = 0; i < lHosts.size(); i++)
//         {
//            ServerContext iComputeServer = (ServerContext)lHosts.get(i);
//            
//            if (iComputeServer != null && server!=null && 
//                iComputeServer.getId().equals(server.getId()))
//               return iComputeServer;
//               
//         } // for i - loop through hosts
//         
//      } // else - there were some hosts found
//      
//      return (ServerContext)lHosts.get(0);
//      
   } // method: getServerForStep
   

   /**
    * DataTargetsList is the list used to maintain the data targets list.  The
    * list is responsible for generating events and generating undoable edits.
    * Data targets are in a double-link relationship with the transformation so
    * this list also maintains the double-link relationship between the data
    * target and the transform (transform's data target and data object's 
    * producer transform).
    */
   private class DataTargetsList extends AbstractPrimaryModelList
   {
      /**
       * Constructs the data targets list.
       */
      public DataTargetsList()
      {
         super( AbstractDataTransform.this, new String[] { DATA_TARGET_ADDED, DATA_TARGET_REMOVED }, SAVE_CHANGED_OBJECTS, IDataObject.class );
         setDeleteFilter( new DeleteWorkTablesFilter() );
         setOwnerNotified( true );
      }
      
      /**
       * Adds the primary object (the transform) to the secondary object 
       * (the data object).
       * 
       * @param secondary the secondary object (the data object)
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#addTo(java.lang.Object)
       */
      protected void addTo( Object secondary )
      {
         ((IDataObject) secondary).addProducerTransform( AbstractDataTransform.this );
      }

      /**
       * Removes the primary object (the transform) from the secondary object 
       * (the data object).
       * 
       * @param secondary the secondary object (the data object)
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#removeFrom(java.lang.Object)
       */
      protected void removeFrom( Object secondary )
      {
         ((IDataObject) secondary).removeProducerTransform( AbstractDataTransform.this );
      }

      /**
       * The object is about to be added, so do any preparation necessary.
       * 
       * @param obj the object to be added
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#preAdd(java.lang.Object)
       */
      protected void preAdd( Object obj )
      {
      	 if (getJob()!=null && !getJob().getDataObjectsList().contains( obj ) && !(obj instanceof IWorkTable))
     		 getJob().getDataObjectsList().add(obj);

         preAddDataTarget( (IDataObject) obj );
      }

      /**
       * The object has been add, so do any necessary work.
       * 
       * @param obj the object added
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#postAdd(java.lang.Object)
       */
      protected void postAdd( Object obj )
      {
         postAddDataTarget( (IDataObject) obj );
      }
      
      /**
       * The object is about to be removed, so do any clean up necessary.
       * 
       * @param obj the object to be removed
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#preRemove(java.lang.Object)
       */
      protected void preRemove( Object obj )
      {
         preRemoveDataTarget( (IDataObject) obj );
      }

      /**
       * The object has been removed, so do any clean up necessary.
       * 
       * @param obj the object removed
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#postRemove(java.lang.Object)
       */
      protected void postRemove( Object obj )
      {
         postRemoveDataTarget( (IDataObject) obj );
      }
   } // DataTargetsList
   
   /**
    * DeleteWorkTablesFilter is the filter that allows work tables to be deleted
    * when they are removed from the data targets list.
    */
   private static class DeleteWorkTablesFilter implements IFilter
   {
      /**
       * Does the object pass the filter?
       * 
       * @param obj the object to be tested
       * 
       * @return true = delete the object
       * 
       * @see com.sas.etl.models.IFilter#pass(java.lang.Object)
       */
      public boolean pass( Object obj )
      {
         return obj instanceof IWorkTable;
      }
   } // DeleteWorkTablesFilter

   /**
    * DataSourcesList is the list used to maintain the data sources list.  The
    * list is responsible for generating events and generating undoable edits.
    * Data sources are in a double-link relationship with the transformation so
    * this list also maintains the double-link relationship between the data
    * source and the transform (transform's data source and data object's 
    * consumer transform).
    */
   private class DataSourcesList extends AbstractPrimaryModelList
   {
      /**
       * Constructs the data sources list.
       */
      public DataSourcesList()
      {
         super( AbstractDataTransform.this, new String[] { DATA_SOURCE_ADDED, DATA_SOURCE_REMOVED }, SAVE_CHANGED_OBJECTS, IDataObject.class );
         setOwnerNotified( true );
      }
      
      /**
       * Adds the primary object (the transform) to the secondary object 
       * (the data object).
       * 
       * @param secondary the secondary object (the data object)
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#addTo(java.lang.Object)
       */
      protected void addTo( Object secondary )
      {
         ((IDataObject) secondary).addConsumerTransform( AbstractDataTransform.this );
      }

      /**
       * Removes the primary object (the transform) from the secondary object 
       * (the data object).
       * 
       * @param secondary the secondary object (the data object)
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#removeFrom(java.lang.Object)
       */
      protected void removeFrom( Object secondary )
      {
         ((IDataObject) secondary).removeConsumerTransform( AbstractDataTransform.this );
      }

      /**
       * The object is about to be added, so do any preparation necessary.
       * 
       * @param obj the object to be added
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#preAdd(java.lang.Object)
       */
      protected void preAdd( Object obj )
      {
     	 if (getJob()!=null && !getJob().getDataObjectsList().contains( obj ) && !(obj instanceof IWorkTable))
    		 getJob().getDataObjectsList().add(obj);
    	  
         preAddDataSource( (IDataObject) obj );
      }

      /**
       * The object has been add, so do any necessary work.
       * 
       * @param obj the object added
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#postAdd(java.lang.Object)
       */
      protected void postAdd( Object obj )
      {
         postAddDataSource( (IDataObject) obj );
      }
      
      /**
       * The object is about to be removed, so do any clean up necessary.
       * 
       * @param obj the object to be removed
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#preRemove(java.lang.Object)
       */
      protected void preRemove( Object obj )
      {
         preRemoveDataSource( (IDataObject) obj );
      }

      /**
       * The object has been removed, so do any clean up necessary.
       * 
       * @param obj the object removed
       * 
       * @see com.sas.etl.models.impl.AbstractPrimaryModelList#postRemove(java.lang.Object)
       */
      protected void postRemove( Object obj )
      {
         postRemoveDataSource( (IDataObject) obj );
      }
   } // DataSourcesList
   
   private class ConnectedSourcesList extends ModelList
   {
      public ConnectedSourcesList()
      {
         super( AbstractDataTransform.this, new String[] { CONNECTED_SOURCE_ADDED, CONNECTED_SOURCE_REMOVED } );
      }      
   }
   
   //---------------------------------------------------------------------------
   // Undoables
   //---------------------------------------------------------------------------
   /**
    * AddTransformTableOptionUndoable is the undoable edit for adding a TransformTableOption. 
    */
   private class AddTransformTableOptionUndoable extends AbstractUndoableEdit
   {
      private ITransformTableOptions m_TransformTableOption;
      
      /**
       * Constructs the add TransformTableOption undoable.
       * 
       * @param iTransformTableOption index of where TransformTableOption was added
       * @param TransformTableOption  the TransformTableOption that was added
       */
      public AddTransformTableOptionUndoable( ITransformTableOptions TransformTableOption )
      {
         m_TransformTableOption  = TransformTableOption;
      }
      
      /**
       * Undoes the addition of the TransformTableOption.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         removeTransformTableOption( m_TransformTableOption );
      }
      
      /**
       * Redoes the addition of the TransformTableOption.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         addTransformTableOption( m_TransformTableOption );
      }
      
      /**
       * Kills the undoable edit allowing the edit to remove any references.
       * 
       * @see javax.swing.undo.UndoableEdit#die()
       */
      public void die()
      {
         m_TransformTableOption = null;
      }
   } // AddTransformTableOptionUndoable
   
   /**
    * RemoveTransformTableOptionUndoable is the undoable edit for removing a TransformTableOption.
    */
   private class RemoveTransformTableOptionUndoable extends AbstractUndoableEdit
   {
      private ITransformTableOptions m_TransformTableOption;
      
      /**
       * Constructs the remove TransformTableOption undoable.
       * 
       * @param iTransformTableOption index of where TransformTableOption was removed
       * @param TransformTableOption  the TransformTableOption that was removed
       */
      public RemoveTransformTableOptionUndoable( ITransformTableOptions TransformTableOption )
      {
         m_TransformTableOption  = TransformTableOption;
      }
      
      /**
       * Undoes the removal of the TransformTableOption.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         addTransformTableOption( m_TransformTableOption );
      }
      
      /**
       * Redoes the removal of the TransformTableOption.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         removeTransformTableOption( m_TransformTableOption );
      }
      
      /**
       * Kills the undoable edit allowing the edit to remove any references.
       * 
       * @see javax.swing.undo.UndoableEdit#die()
       */
      public void die()
      {
         m_TransformTableOption = null;
      }
   } // RemoveTransformTableOptionUndoable
   
   
   /**
    * AddMappingUndoable is the undoable edit for adding a mapping. 
    */
   private class AddMappingUndoable extends AbstractUndoableEdit
   {
      private int      m_iMapping;
      private IMapping m_mapping;
      
      /**
       * Constructs the add mapping undoable.
       * 
       * @param iMapping index of where mapping was added
       * @param mapping  the mapping that was added
       */
      public AddMappingUndoable( int iMapping, IMapping mapping )
      {
         m_iMapping = iMapping;
         m_mapping  = mapping;
      }
      
      /**
       * Undoes the addition of the mapping.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         removeMapping( m_mapping );
      }
      
      /**
       * Redoes the addition of the mapping.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         addMapping( m_iMapping, m_mapping );
      }
      
      /**
       * Kills the undoable edit allowing the edit to remove any references.
       * 
       * @see javax.swing.undo.UndoableEdit#die()
       */
      public void die()
      {
         m_mapping = null;
      }
   } // AddMappingUndoable
   
   /**
    * RemoveMappingUndoable is the undoable edit for removing a mapping.
    */
   private class RemoveMappingUndoable extends AbstractUndoableEdit
   {
      private int      m_iMapping;
      private IMapping m_mapping;
      
      /**
       * Constructs the remove mapping undoable.
       * 
       * @param iMapping index of where mapping was removed
       * @param mapping  the mapping that was removed
       */
      public RemoveMappingUndoable( int iMapping, IMapping mapping )
      {
         m_iMapping = iMapping;
         m_mapping  = mapping;
      }
      
      /**
       * Undoes the removal of the mapping.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         addMapping( m_iMapping, m_mapping );
      }
      
      /**
       * Redoes the removal of the mapping.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         removeMapping( m_mapping );
      }
      
      /**
       * Kills the undoable edit allowing the edit to remove any references.
       * 
       * @see javax.swing.undo.UndoableEdit#die()
       */
      public void die()
      {
         m_mapping = null;
      }
   } // RemoveMappingUndoable
   
   /**
    * 
    * SetSYSLASTVariableGenerationEnabledUndoable is the undoable class for handling the option
    * for creating the SYSLAST macro varaible.
    */
   private class SetSYSLASTVariableGenerationEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldValue;
      private boolean m_newValue;
      
      /**
       * Constructs the create SYSLAST macro variable undoable
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetSYSLASTVariableGenerationEnabledUndoable (boolean oldValue, boolean newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the Create SYSLAST Macro Variable option.
       */
      public void undo()
      {
         super.undo();
         setSYSLASTVariableGenerationEnabled (m_oldValue);
      }
      
      /**
       * Redoes the setting of the Create SYSLAST Macro Variable option.
       */
      public void redo()
      {
         super.redo();
         setSYSLASTVariableGenerationEnabled (m_newValue);
      }
   } // SetSYSLASTVariableGenerationEnabledUndoable
   
   /**
    * 
    * SetSYSLASTVariableGenerationEnabledUndoable is the undoable class for handling the option
    * for creating the SYSLAST macro varaible.
    */
//   private class SetFormatGenerationEnabledUndoable extends AbstractUndoableEdit
//   {
//      private boolean m_oldValue;
//      private boolean m_newValue;
//      
//      /**
//       * Constructs the use Format undoable
//       * 
//       * @param oldValue  the old value
//       * @param newValue  the new value
//       */
//      public SetFormatGenerationEnabledUndoable (boolean oldValue, boolean newValue)
//      {
//         m_oldValue = oldValue;
//         m_newValue = newValue;
//      }
//      
//      /**
//       * Undoes the setting of the use Format option.
//       */
//      public void undo()
//      {
//         super.undo();
//         setFormatGenerationEnabled (m_oldValue);
//      }
//      
//      /**
//       * Redoes the setting of the use Formate option.
//       */
//      public void redo()
//      {
//         super.redo();
//         setFormatGenerationEnabled (m_newValue);
//      }
//   } // SetFormatGenerationEnabledUndoable
   
   /**
    * 
    * SetSYSLASTVariableGenerationEnabledUndoable is the undoable class for handling the option
    * for creating the SYSLAST macro varaible.
    */
   private class SetFormatInformatGenerationUndoable extends AbstractUndoableEdit
   {
      private String m_oldValue;
      private String m_newValue;
      
      /**
       * Constructs the use Format undoable
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetFormatInformatGenerationUndoable (String oldValue, String newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the use Format option.
       */
      public void undo()
      {
         super.undo();
         setFormatInformatGeneration (m_oldValue);
      }
      
      /**
       * Redoes the setting of the use Formate option.
       */
      public void redo()
      {
         super.redo();
         setFormatInformatGeneration(m_newValue);
      }
   } // SetFormatInformatGenerationUndoable
   
   
   /**
    * 
    * SetUseConnectUsingUndoable is the undoable class for handling the option
    * for using CONNECT USING syntax.
    */
   private class SetUseConnectUsingUndoable extends AbstractUndoableEdit
   {
      private String m_oldValue;
      private String m_newValue;
      
      /**
       * Constructs the use Format undoable
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetUseConnectUsingUndoable (String oldValue, String newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the use Format option.
       */
      public void undo()
      {
         super.undo();
         setUseConnectUsing(m_oldValue);
      }
      
      /**
       * Redoes the setting of the use Formate option.
       */
      public void redo()
      {
         super.redo();
         setUseConnectUsing(m_newValue);
      }
   } // SetUseConnectUsingUndoable

//stwatk Defect ID: S0266081
   /**
    * 
    * SetAppendForceEnabledUndoable is the undoable class for handling the option
    * for using, not using FORCE option on PROC APPEND.
    */
   private class SetAppendForceEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldValue;
      private boolean m_newValue;
      
      /**
       * Constructs the generation of column macro variables
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetAppendForceEnabledUndoable (boolean oldValue, boolean newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the column macro varibles option.
       */
      public void undo()
      {
         super.undo();
         setAppendForceEnabled (m_oldValue);
      }
      
      /**
       * Redoes the setting of the column macro variables option.
       */
      public void redo()
      {
         super.redo();
         setAppendForceEnabled (m_newValue);
      }
   } // SetAppendForceEnabledUndoable
   
   /**
    * 
    * SetColMacroVarsEnabledUncoable is the undoable class for handling the option
    * for using, not using DIS 4.2-Level column metadata macro variables.
    */
   private class SetColMacroVarsEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldValue;
      private boolean m_newValue;
      
      /**
       * Constructs the generation of column macro variables
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetColMacroVarsEnabledUndoable (boolean oldValue, boolean newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the column macro varibles option.
       */
      public void undo()
      {
         super.undo();
         setColMacroVarsEnabled(m_oldValue);
      }
      
      /**
       * Redoes the setting of the column macro variables option.
       */
      public void redo()
      {
         super.redo();
         setColMacroVarsEnabled(m_newValue);
      }
      
   } // SetColMacroVarsEnabledUndoable

   /**
    * 
    * SetFileMacroVarsEnabledUncoable is the undoable class for handling the option
    * for using, not using external file macro variables.
    */
   private class SetFileMacroVarsEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldValue;
      private boolean m_newValue;
      
      /**
       * Constructs the generation of external file macro variables
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetFileMacroVarsEnabledUndoable (boolean oldValue, boolean newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the external file macro variables option.
       */
      public void undo()
      {
         super.undo();
         setFileMacroVarsEnabled(m_oldValue);
      }
      
      /**
       * Redoes the setting of the external file macro variables option.
       */
      public void redo()
      {
         super.redo();
         setFileMacroVarsEnabled(m_newValue);
      }
      
   } // SetFileMacroVarsEnabledUndoable
   
   /**
    * SetIncludedInPropagationUndoable is the undoable for setting the 
    * transform's included in propagation attribute.
    */
   private class SetIncludedInPropagationUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldIncludedInPropagation;
      private boolean m_newIncludedInPropagation;
      
      /**
       * Constructs the set included in propagation attribute undoable
       * 
       * @param oldIncludedInPropagation the old included in propagation attribute
       * @param newIncludedInPropagation the new included in propagation attribute
       */
      public SetIncludedInPropagationUndoable( boolean oldIncludedInPropagation, boolean newIncludedInPropagation )
      {
         m_oldIncludedInPropagation = oldIncludedInPropagation;
         m_newIncludedInPropagation = newIncludedInPropagation;
      }
      
      /**
       * Undoes the setting of the included in propagation attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setIncludedInPropagation( m_oldIncludedInPropagation );
      }
      
      /**
       * Redoes the setting of the included in propagation attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setIncludedInPropagation( m_newIncludedInPropagation );
      }
   } // SetIncludedInPropagationUndoable
   
   /**
    * SetIncludedInMappingUndoable is the undoable for setting the 
    * transform's included in propagation attribute.
    */
   private class SetIncludedInMappingUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldIncludedInMapping;
      private boolean m_newIncludedInMapping;
      
      /**
       * Constructs the set included in propagation attribute undoable
       * 
       * @param oldIncludedInMapping the old included in mapping attribute
       * @param newIncludedInMapping the new included in mapping attribute
       */
      public SetIncludedInMappingUndoable( boolean oldIncludedInMapping, boolean newIncludedInMapping )
      {
         m_oldIncludedInMapping = oldIncludedInMapping;
         m_newIncludedInMapping = newIncludedInMapping;
      }
      
      /**
       * Undoes the setting of the included in mapping attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setIncludedInMapping( m_oldIncludedInMapping );
      }
      
      /**
       * Redoes the setting of the included in mapping attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setIncludedInMapping( m_newIncludedInMapping );
      }
   } // SetIncludedInMappingUndoable
   
   
   
   /**
    * SetGenerateIndexesOnTargetTables is the undoable for setting the 
    * transform's generate target table indexes
    */
   private class SetGenerateIndexesOnTargetTables extends AbstractUndoableEdit
   {
      private boolean m_oldGenerateTargetTableIndexes;
      private boolean m_newGenerateTargetTableIndexes;
      
      /**
       * Constructs the set included in propagation attribute undoable
       * 
       * @param oldGenerateTargetTableIndexes the old included generate target indexes
       * @param newGenerateTargetTableIndexes the new included generate target indexes
       */
      public SetGenerateIndexesOnTargetTables( boolean oldGenerateTargetTableIndexes, boolean newGenerateTargetTableIndexes )
      {
         m_oldGenerateTargetTableIndexes = oldGenerateTargetTableIndexes;
         m_newGenerateTargetTableIndexes = newGenerateTargetTableIndexes;
      }
      
      /**
       * Undoes the setting of the included in generate target indexes
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setGenerateIndexesOnTargetTables( m_oldGenerateTargetTableIndexes );
      }
      
      /**
       * Redoes the setting of the included in generate target indexes
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setGenerateIndexesOnTargetTables( m_newGenerateTargetTableIndexes );
      }
   } // SetGenerateTargetTableIndexesUndoable
   
      
   /**
    * SetDBIDirectExecUndoable is the undoable 
    */
   private class SetDBIDirectExecUndoable extends AbstractUndoableEdit
   {
      private String m_oldType;
      private String m_newType;
      
      /**
       * Constructs the set 
       * 
       * @param oldType the old type
       * @param newType the new type
       */
      public SetDBIDirectExecUndoable( String oldType, String newType )
      {
         m_oldType = oldType;
         m_newType = newType;
      }
      
      /**
       * Undoes the setting 
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setDBIDirectExec( m_oldType );
      }
      
      /**
       * Redoes the setting 
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setDBIDirectExec( m_newType );
      }
   } // SetDBIDirectExecUndoable

}