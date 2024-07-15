/* $Id: SortColsByTableModel.java,v 1.1.2.13 2008/01/18 01:06:02 vekada Exp $ */
/**
 * Title:       SortColsByTableModel.java
 * Description: Table model for Sort By Columns in SAS Sort Transformation 
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Venu Kadari   
 * Support:     Venu Kadari
 */
package com.sas.wadmin.transforms.sort;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;

import com.sas.etl.models.IModel;
import com.sas.etl.models.IModelListener;
import com.sas.etl.models.IObject;
import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.impl.ModelEvent;
import com.sas.etl.models.job.ISortingTransform;
import com.sas.etl.models.job.transforms.common.ISortColumn;
import com.sas.etl.models.job.transforms.common.ISorting;
import com.sas.etl.models.job.transforms.common.impl.BaseSorting;
import com.sas.metadata.remote.MdException;
import com.sas.wadmin.visuals.properties.AbstractPropertiesModel.DefaultListTableCellEditor;
import com.sas.wadmin.visuals.properties.AbstractPropertiesModel.DefaultListTableCellRenderer;
import com.sas.workspace.WAdminResource;
import com.sas.workspace.WsAbstractTableColumn;
import com.sas.workspace.WsEnumeratedTableColumn;
import com.sas.workspace.WsObjectTableColumn;
import com.sas.workspace.WsStringTableColumn;
import com.sas.workspace.WsTreeNode;
import com.sas.workspace.visuals.dualselector.DefaultSelectorTableModel;


/**
  * SortColsByTableModel
  * 
  * Table model that holds the selected sort by columns
 */
public class SortColsByTableModel extends DefaultSelectorTableModel
{
   private static final WAdminResource bundle = WAdminResource.getBundle(SortColsByTableModel.class);
  
   /** Default Sort Order */
   private static String DEFAULT_SORT_ORDER = ISorting.ASCENDING;   
   
   private static final String DEFAULTCOLUMNORDER = DEFAULT_SORT_ORDER;

   // column indexes for right-hand table
   /** Name column index. */
   protected final static int COLUMN_COL          =  0;
   protected final static int ASC_DESC_COL        =  1;
   protected final static int ORDERBYCOLUMN_COL   =  2;
   protected final static int COLUMNS_IN_TABLE    =  3;
   protected final static int HIDDEN_COLUMNS      =  1;

   private ISortingTransform      m_sortModel;
   private ISorting               m_sort;
   
   private ModelListener          m_lsnrModel;

   
   /**
    * Main constructor
    * 
    * @param sort sorting model object
    */
   public SortColsByTableModel(ISortingTransform sort)
   {
      m_sortModel = sort;
      m_sort = m_sortModel.getSortOrder();
      m_aColumns    = new WsAbstractTableColumn[COLUMNS_IN_TABLE];
      m_aColumns[COLUMN_COL] = new WsStringTableColumn( COLUMN_COL, bundle.getString("SortColsByTableModel.ColumnName.txt"), 100 );
      m_aColumns[ASC_DESC_COL] = new cListTableColumn( ASC_DESC_COL, bundle.getString("SortColsByTableModel.SortOrder.txt"), 75, getSortOrderTranslatedValues(), getSortOrderUnTranslatedValues(), DEFAULTCOLUMNORDER );
      m_aColumns[ORDERBYCOLUMN_COL] = new WsObjectTableColumn();
      m_aColumns[COLUMN_COL].setEditable( false );
      
      m_lsnrModel = new ModelListener();
      m_sortModel.getModel().addModelListener( m_lsnrModel );
    }
   
   /**
    * Disposes the table model.
    */
   public void dispose()
   {
      m_sortModel.getModel().removeModelListener( m_lsnrModel );
   }
   
   /**
    * Adds a row to the table.  The object used to create the values for the row.
    * 
    * @param obj the object
    * 
    * @see com.sas.workspace.visuals.dualselector.SelectorTableModel#addRow(java.lang.Object)
    */
   public void addRow( Object obj )
   {
      if (obj instanceof TreePath)
      {
         obj = ((TreePath) obj).getLastPathComponent();
         SortColsByTreeModel.cColumnTreeNode columnTreeNode = (SortColsByTreeModel.cColumnTreeNode) ((WsTreeNode) obj);
         IColumn column = columnTreeNode.getTableColumn();
         
         //Create a new sort column and add it to the model
         ISortColumn sortColumn = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), column );
         obj = sortColumn;              
      }     
      //Insert the row into the table model
      insertRow( getRowCount()-1, createRowValues( obj ) );
      //Insert the sort column
      if (obj instanceof ISortColumn)
         m_sort.addSortColumn( getRowCount()-1, (ISortColumn)obj );
   }
   
   /**
    * Move the items around in the sort model, so the order in the table
    * matches the order in the sort model.  All selected rows should only
    * move either up one or down one.
    * @param selectedIndices the rows making the move 
    * @param bMovingUp true, if the selected items are moving up
    */
   public void moveSortModelItems(int[] selectedIndices, boolean bMovingUp)
   {
      if (selectedIndices != null && selectedIndices.length > 0)
      {
         m_sort.getModel().startCompoundUndoable();
         try
         {
               // Moving items up in the list
               if (bMovingUp)
               {
                  for ( int i = 0; i < selectedIndices.length; i++ )
                  {
                     // Use the first index and subtract 1 from it and remove the
                     // item from
                     // the last, this it will be added by at the last selected
                     // item location.
                     List lSortCols = m_sort.getSortColumnList();
                     ISortColumn sortColumn = (ISortColumn) lSortCols.get( selectedIndices[i] );
                     //printOrder(m_sort, "Before removing column");
                     m_sort.removeSortColumn( sortColumn );
                     //printOrder(m_sort, "After removing column");
                     lSortCols = m_sort.getSortColumnList();
                     m_sort.addSortColumn( selectedIndices[i] - 1, sortColumn );
                     //printOrder(m_sort, "After adding column");
                  }
               }
               else
               // Moving items down in the list
               {
                  for ( int i = selectedIndices.length-1; i >= 0; i-- )
                  {
                     List lSortCols = m_sort.getSortColumnList();
                     ISortColumn sortColumn = (ISortColumn) lSortCols.get( selectedIndices[i] );
                     //printOrder(m_sort, "Before removing column");
                     m_sort.removeSortColumn( sortColumn );
                     //printOrder(m_sort, "After removing column");
                     m_sort.addSortColumn( selectedIndices[i] + 1, sortColumn );
                     //printOrder(m_sort, "After adding column");
                  }
               }
            
         }
         finally 
         {
            m_sort.getModel().endCompoundUndoable();
         }
      }
      
   }
   
//   public void printOrder(ISorting m_sort, String sTitle)
//   {
//      List list = m_sort.getSortColumnList();
//      System.out.println(sTitle + "\n");
//      
//      for (int i=0; i<list.size();i++)
//      {
//         ISortColumn column = (ISortColumn) list.get(i);
//         System.out.println("No=" + i + "      Column = " + column.getColumn().getName() );
//      }
//   }

   
   /**
    * Sets the contents of a row in the table to be the value(s) specified by
    * the object.
    * 
    * @param obj  the object
    * @param iRow the index of the row to be set
    * 
    * @see com.sas.workspace.visuals.dualselector.SelectorTableModel#setRow(java.lang.Object, int)
    */
   public void setRow( Object obj, int iRow )
   {
      if (obj instanceof TreePath)
         obj = ((TreePath) obj).getLastPathComponent();
      
      //we are doing a move...
//      if (obj instanceof List)
//      {
//         List lRowValues = (ArrayList) obj;
//         setRowValues (iRow, lRowValues);
//         
//         //Update the sorting model of the move
//         ISortColumn sortColumn = (ISortColumn) lRowValues.get(ORDERBYCOLUMN_COL);
//         m_sortModel.getModel().startCompoundUndoable();
//         try
//         {
//            m_sort.removeSortColumn( sortColumn );
//            m_sort.addSortColumn( iRow, sortColumn );             
//         }
//         finally
//         {
//            m_sortModel.getModel().endCompoundUndoable();
//         }
//         
//      }
//      else 
         setRowValues( iRow, createRowValues( obj, iRow ) );
   } 
   
   /**
    * Adds a sort row to table model.  In reality, a row of values representing
    * the table's column is added to the table model.
    *  
    * @param sortCol the sort column being added back
    */
   private void addSortRow( ISortColumn sortCol )
   {
      //If it already exists in the table don't add it again.
      int iRowIndex = getRowIndex( sortCol );
      if (iRowIndex > -1)
         return;
      
      int iRow = m_sort.indexOfSortColumn( sortCol );
      insertRow( iRow-1, createRowValues( sortCol ) );   // WsAbstractTableModel inserts after indicated row
   }

   /**
    * Removes the sort column from the table model.  In reality, the row of values
    * representing the column is removed from the table model.
    * 
    * @param sortCol the sort column
    */
   private void removeSortRow( ISortColumn sortCol )
   {
      deleteRow( getRowIndex( sortCol ) );
   }
   
   /**
    * Creates row values for table on the right
    * 
    * @param obj instance of cColumnTreeNode or a List consisting of row values
    * @param iRow row from the table
    * @return row values
    */
   protected List createRowValues( Object obj, int iRow )
   {      
      if (obj instanceof ISortColumn)
      {
         ISortColumn sortColumn = (ISortColumn)obj;
         // Populate table with selected and default values
         List lRowValues = new ArrayList();
         IColumn column = sortColumn.getColumn();
            
         lRowValues.add(column.getName());
         lRowValues.add(sortColumn.getDirection());
         lRowValues.add(sortColumn);                  
         return lRowValues;                  
      } 
      if (obj instanceof List)
         return (List)obj;
      
      return null;
   } 
  

   /**
    * Creates row values for table on the right
    * 
    * @param obj instance of cColumnTreeNode or a List consisting of row values
    * @return row values
    */
   protected List createRowValues( Object obj)
   {
      return createRowValues(obj, -1);
   }  
   
   /**
    * Only here because inherited abstract WsAbstractTableModel requires it
    * @throws MdException
    * @throws java.rmi.RemoteException
    * @see com.sas.workspace.WsAbstractTableModel#populateStore()
    */
   public void populateStore() throws MdException, java.rmi.RemoteException
   {
      // 
   }

   /**
    * Moves data to the model.  This method should
    * not run on a background thread.
    */
   public void moveDataToModel() 
   {
      super.clear();
      List lSortColumns = m_sort.getSortColumnList();
      for (int index = 0; index < lSortColumns.size(); index++)
      {
         ISortColumn oSortColumn = (ISortColumn) lSortColumns.get(index);
         List row = new ArrayList();
         row.add( oSortColumn.getColumn().getName());           
         row.add( oSortColumn.getDirection());
         row.add( oSortColumn);
         m_lData.add (row);
         m_nRowInstances++;
      }
      
      fireTableModelPopulated();

   }
  

   /**
    * No Update metadata here.
    * @throws MdException
    * @throws java.rmi.RemoteException
    * @see com.sas.workspace.WsAbstractTableModel#moveDataToStore()
    */
   public void moveDataToStore() throws MdException, java.rmi.RemoteException
   {
        // Only here because base class requires it
   }
   
   /**
    * Get one row from the table that has selected order by columns
    * @param iRow the index of the row you want to return
    * @return Object the row in the table
    * @see com.sas.workspace.visuals.dualselector.SelectorTableModel#getRow(int)
    */
   public Object getRow( int iRow )
   {
      if (iRow > getRowCount()-1)
         return null;
      
      String sName  = (String) getValueAt( iRow, COLUMN_COL );
      String sOrder = (String) getValueAt( iRow, ASC_DESC_COL );
      ISortColumn sortColumn = (ISortColumn) getValueAt( iRow, ORDERBYCOLUMN_COL);

      List lRowValues  = new ArrayList();
      lRowValues.add( sName );
      lRowValues.add( sOrder );
      lRowValues.add( sortColumn );
      return lRowValues;
   }
   
   /**
    * Deletes the specified row.
    * 
    * @param iRow the model index of the row to be deleted
    */
   public void deleteRow( int iRow )
   {
      if (iRow == -1)
         return;
      
      ISortColumn sortColumn = (ISortColumn) getRowValues( iRow ).get( ORDERBYCOLUMN_COL );
      
      List lDeletedRowValues = (List) m_lData.remove( iRow );
      fireTableRowDeleted( iRow, lDeletedRowValues );      
      
      if (m_sort.containsSortColumn( sortColumn ))
         m_sort.getSortColumnList().remove( iRow );  
     
   }
   
   /**
    * Clears the model.  The rows of data are removed and an event is fired to
    * indicated that all rows were deleted.  This method can be called at the
    * beginning of moveDataToModel clear out the previous contents of the model.
    */
   public void clear()
   {
      super.clear();
      List lSortColumns = m_sort.getSortColumnList();
      int size = lSortColumns.size();
      for (int i = size - 1; i > -1; i--)
         lSortColumns.remove( lSortColumns.get(i) );      
   }

   /**
    * Sets the value of the cell at the given row and column.
    * This is used for
    * @param value the value of the cell
    * @param iRow  the model row    of the cell
    * @param iCol  the model column of the cell
    * 
    */
   public void setValueAt( Object value, int iRow, int iCol )
   {
      //make sure this is called only when an existing row is edited
      List lRow = (List) m_lData.get( iRow );
      lRow.set( iCol, value );
      
      fireTableCellUpdated( iRow, iCol );      
      
     //get the orderbycolumn value of this row
      ISortColumn oSortColumn = (ISortColumn) getValueAt( iRow, ORDERBYCOLUMN_COL);
      //set the value on orderbycolumn to this new value 
      if ( iCol == ASC_DESC_COL)
         oSortColumn.setDirection((String)value);         
            
   }

   /**
    * Gets the number of columns in the model.  If the model has hidden columns
    * this method must be overridden.
    *
    * @return the number of columns in the model
    */
   public int getColumnCount()
   {
      return m_aColumns.length - HIDDEN_COLUMNS;
   }
   
   /**
    * Get listed of translated sort order values, corresponding to untranslated values
    * 
    * @return list of translated values for sort order
    */
   public static List getSortOrderTranslatedValues()
   {
      List sortOrderTranslated = new ArrayList();
      sortOrderTranslated.add(bundle.getString("SortColsByTableModel.Ascending.txt")) ; 
      sortOrderTranslated.add(bundle.getString("SortColsByTableModel.Descending.txt"));
      return sortOrderTranslated;
   }
   

   /**
    * Get listed of untranslated sort order values, corresponding to translated values
    * 
    * @return list of untranslated values for sort order
    */
   public static List getSortOrderUnTranslatedValues()
   {
      List sortOrderUnTranslated = new ArrayList();
      sortOrderUnTranslated.add(ISorting.ASCENDING);
      sortOrderUnTranslated.add(ISorting.DESCENDING);
      return sortOrderUnTranslated;
   }
   
   /**
    * Gets the row index for the specified sort column.
    * 
    * @param sortCol the column
    * 
    * @return the row index
    */
   public int getRowIndex( ISortColumn sortCol )
   {
      for ( int iRow=0; iRow<m_lData.size(); iRow++ )
      {
         List lValues = (List) m_lData.get( iRow );
         if (lValues.get( ORDERBYCOLUMN_COL ) == sortCol)
            return iRow;
      }
      
      return -1;
   }
   
   /**
    * cListTableColumn
    */
   protected class cListTableColumn extends WsEnumeratedTableColumn
   {
      protected List m_unTranslatedValues;
      protected List m_translatedValues;
      
      /** 
       * Constructs a WsEnumeratedTableColumn.
       * 
       * @param iModelIndex the model index of the column
       * @param sName       the name        of the column
       * @param cxWidth     the width       of the column
       * @param translatedValues    the translated list of values
       * @param unTranslatedValues the untranslated list of values
       * @param sDefault    the default value of a new column value
       */
      public cListTableColumn( int iModelIndex, String sName, int cxWidth, List translatedValues, List unTranslatedValues, String sDefault )
      {
         super( iModelIndex, sName, cxWidth , (String[])translatedValues.toArray(new String[translatedValues.size()]), sDefault);
         m_translatedValues = translatedValues;
         m_unTranslatedValues = unTranslatedValues;
      }
      /**
       * Creates the cell editor used for the cell.
       * 
       * @return the cell editor
       */
      public TableCellEditor createCellEditor()
      {
         return new DefaultListTableCellEditor(m_translatedValues,m_unTranslatedValues);
      }

      /**
       * Creates the cell renderer used for the cell.
       * 
       * @return the cell renderer
       */
      protected TableCellRenderer createCellRenderer()
      {
         return new DefaultListTableCellRenderer(m_translatedValues,m_unTranslatedValues);
      }
   }
  
   //---------------------------------------------------------------------------
   // Listeners
   //---------------------------------------------------------------------------
   /**
    * ModelListener listens to the model to update values in the table model.
    */
   private class ModelListener implements IModelListener
   {
      /**
       * Handles model changed event.
       * @param ev
       * @see com.sas.etl.models.IModelListener#modelChanged(com.sas.etl.models.impl.ModelEvent)
       */
      public void modelChanged( ModelEvent ev )
      {
         String  sType = ev.getType();
         IObject obj   = ev.getModelObject();
         
         // if the event is unknown, completely reload the model
         if (sType == IModel.UNKNOWN_CHANGES)
            moveDataToModel();
         
         // if the event is related to the sort, handle sort columns added and removed
         else if (obj == m_sort)
         {
            if (sType == ISorting.SORTING_COLUMN_ADDED)
            {
               ISortColumn sortCol = (ISortColumn) ev.getData();
               int iRow = getRowIndex( sortCol );
               if (iRow == -1)
                  addSortRow( sortCol );
            }
            else if (sType == ISorting.SORTING_COLUMN_REMOVED)
               removeSortRow( (ISortColumn) ev.getData() );
         }

         // if the event is related to a column name in the table model, update the
         // appropriate value
         else if (obj instanceof IColumn)
         {
            IColumn col = (IColumn) obj;            
            if (m_sort.containsColumn( col ))
            {
               ISortColumn sortCol = m_sort.getSortColumnContainingColumn( col );
               int iRowIndex = getRowIndex( sortCol );
               
               if (sType == IColumn.NAME_CHANGED)
               {
                  SortColsByTableModel.super.setValueAt( col.getName(), iRowIndex, COLUMN_COL );
                  fireTableCellUpdated( iRowIndex, COLUMN_COL ); // repaint name
               }
               
            }
         }
         else if (obj instanceof ISortColumn)
         {
            if (sType == ISortColumn.SORTING_DIRECTION_CHANGED)
            {
               ISortColumn sortCol = (ISortColumn)obj;
               int iRowIndex = getRowIndex( sortCol );
               SortColsByTableModel.super.setValueAt( sortCol.getDirection(), iRowIndex, ASC_DESC_COL );
               fireTableCellUpdated( iRowIndex, ASC_DESC_COL ); // repaint direction
            }
         }
  
      }
   } // ModelListener


   
}
