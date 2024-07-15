/* $Id: WsAbstractTableModel.java,v 1.1.2.3.24.1.66.1 2020/05/20 19:37:45 sasccb Exp $ */
/**
 * Title:        WsAbstractTableModel
 * Description:  WsAbstractTableModel is the model used by a WsTable.
 * Copyright:    Copyright (c) 2002
 * Company:      SAS Institute
 * Author:       Russ Robison
 * Version:      1.0
 */
package com.sas.workspace;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.sas.metadata.remote.MdException;
import com.sas.table.TableDataValidationInterface;


/**
 * WsAbstractTableModel is the abstract table model class to be used with a 
 * WsTable.  This class supports the ability to define everything about a 
 * column.  It also supports row operations such as insert row, delete row, 
 * create values for a new row, and getting a copy of values for an existing 
 * row.
 * <p>
 * This class uses a List of Lists to hold the model values and an array of 
 * column descriptions (WsAbstractTableColumns).  The class also supports the 
 * concept of permanently hidden columns.
 * <p>
 * The class also supports the concepts of populating the store and moving data
 * from the store to the view and from the view to the store.
 */
public abstract class WsAbstractTableModel extends AbstractTableModel implements TableDataValidationInterface
{
   // data
   protected List  m_lData;
   protected int   m_nRowInstances;   // used in creating names for new rows
   protected int[] m_aRowMappings;    // row mappings used for reordering rows during a save operation
   
   // attributes
   protected boolean  m_bStorePopulated;

   // columns
   protected WsAbstractTableColumn[] m_aColumns;
   protected WsAbstractTableColumn[] m_aHiddenColumns;
   
   /**
    * Constructs the abstract table model
    */
   public WsAbstractTableModel()
   {
      m_lData = new ArrayList();
   }

   //---------------------------------------------------------------------------
   // AbstractTableModel methods   
   //---------------------------------------------------------------------------

   /**
    * Gets the number of rows in the model.
    *
    * @return the number of rows in the model
    */
   public int getRowCount()
   {
      return m_lData.size();
   }

   /**
    * Gets the number of columns in the model.  If the model has hidden columns
    * this method must be overridden.
    *
    * @return the number of columns in the model
    */
   public int getColumnCount()
   {
      return m_aColumns.length;
   }

   /**
    * Gets the name of the column at the column index.  
    *
    * @param iCol the model index of the column

    * @return the name of the column
    */
   public String getColumnName( int iCol )
   {
      return m_aColumns[ iCol ].getName();
   }

   /**
    * Gets the class for all the cell values in the column.
    *
    * @param iCol the model index of the column
    *
    * @return the class of the object values in the model
    */
   public Class getColumnClass( int iCol )
   {
      return m_aColumns[ iCol ].getColumnClass();
   }

   /**
    * Is the cell at the given row and column editable?
    *
    * @param iRow the model row    of the cell
    * @param iCol the model column of the cell
    *
    * @return true = the cell is editable
    */
   public boolean isCellEditable( int iRow, int iCol )
   {
      return m_aColumns[ iCol ].isEditable();
   }

   /**
    * Gets the value of the cell at the given row and column.
    *
    * @param iRow the model row    of the cell
    * @param iCol the model column of the cell
    *
    * @return the value
    * 
    * @see #setValueAt
    */
   public Object getValueAt( int iRow, int iCol )
   {
      List lRow = (List) m_lData.get( iRow );
      return lRow.get( iCol );
   }
   
   /**
    * Sets the value of the cell at the given row and column.
    *
    * @param value the value of the cell
    * @param iRow  the model row    of the cell
    * @param iCol  the model column of the cell
    * 
    * @see #getValueAt
    */
   public void setValueAt( Object value, int iRow, int iCol )
   {
      List lRow = (List) m_lData.get( iRow );
      lRow.set( iCol, value );
      fireTableCellUpdated( iRow, iCol );
   }

   /**
    * Gets the tooltip to display for the cell at the given row and column.  The
    * default implementation returns null.
    * 
    * @param iRow  the model row    of the cell
    * @param iCol  the model column of the cell
    *
    * @return  the tooltip
    */
   public String getToolTipAt( int iRow, int iCol )
   {
      return null;
   }
   
   //---------------------------------------------------------------------------
   // Attributes
   //---------------------------------------------------------------------------

   /**
    * Gets the description of the column.  The description is given in the
    * WsTableColumn object.
    * 
    * @param iColumn the model index for the column for which the information is requested.
    * 
    * @return the description of the column
    */
   public WsAbstractTableColumn getColumn( int iColumn )
   {
      return m_aColumns[ iColumn ];
   }

   /**
    * Gets the number of hidden columns.  These are permanently hidden columns.
    * If this method returns a value greater than 0, getColumnCount must be 
    * overriden as well.
    * 
    * @return the number of hidden columns.
    */
   public int getHiddenColumnCount()
   {
      return 0;
   }

   /**
    * Gets the index of the default edit column.  The default edit column is the 
    * column to be edited when a new row is created and inserted.
    * 
    * @return the index of the default edit column
    */
   public int getDefaultEditColumnIndex()
   {
      return 0;
   }
   
   /**
    * Set whether the object store has been populated for the table model.
    * 
    * @param bStorePopulated true = store is populated
    */
   public void setStorePopulated( boolean bStorePopulated )
   {
      m_bStorePopulated = bStorePopulated;
   }
   
   /**
    * Is the object store populated for the table model?
    * 
    * @return true = store is populated
    */
   public boolean isStorePopulated()
   {
      return m_bStorePopulated;
   }

   /**
    * Sets the row instance count.  This count is the number of rows when the model
    * was first populated plus all the rows ever created.  Deletions are not
    * subtracted out.  The intended purpose of this value is to be used in 
    * creating unique names.  Therefore, a user of this class may increment this
    * value while attempting to create a new name so it may actually be greater
    * than the number of rows created.
    * 
    * @param nRowInstances the row instance count
    */
   public void setRowInstanceCount( int nRowInstances )
   {
      m_nRowInstances = nRowInstances;
   }
   
   /**
    * Gets the row instance count.  This count is the number of rows when the model
    * was first populated plus all the rows ever created.  Deletions are not
    * subtracted out.  The intended purpose of this value is to be used in 
    * creating unique names.  Therefore, a user of this class may increment this
    * value while attempting to create a new name so it may actually be greater
    * than the number of rows created.
    * 
    * @return the row instance count.
    */
   public int getRowInstanceCount()
   {
      return m_nRowInstances;
   }
   
   /**
    * Sets the row mappings.  If the rows are to be reordered in saving the data,
    * set the row mappings to reorder the rows.
    * 
    * @param aRowMappings the row mappings
    */
   public void setRowMappings( int[] aRowMappings )
   {
      m_aRowMappings = aRowMappings;
   }
   
   /**
    * Gets the row mappings.
    * 
    * @return the row mappings
    */
   public int[] getRowMappings()
   {
      return m_aRowMappings;
   }
   
   //---------------------------------------------------------------------------
   // Abstract methods for populating store and moving data
   //---------------------------------------------------------------------------
   
   /**   
    * Populates object store for the table model.  This method may run on a
    * background thread.
    * 
    * @throws MdException 
    * @throws RemoteException
    */
   public abstract void populateStore()   throws MdException, RemoteException;
   
   /**
    * Moves data from object store to model.  This method should not run on a
    * background thread.
    * 
    * @throws MdException 
    * @throws RemoteException
    */
   public abstract void moveDataToModel() throws MdException, RemoteException;
   
   /**
    * Moves data from model to object store.
    * 
    * @throws MdException 
    * @throws RemoteException
    */
   public abstract void moveDataToStore() throws MdException, RemoteException;


   //---------------------------------------------------------------------------
   // Operations
   //---------------------------------------------------------------------------
   
   /**
    * Clears the model.  The rows of data are removed and an event is fired to
    * indicated that all rows were deleted.  This method can be called at the
    * beginning of moveDataToModel clear out the previous contents of the model.
    */
   public void clear()
   {
      int nRows = m_lData.size();
      if (nRows > 0)
      {
         m_lData.clear();
         fireTableRowsDeleted( 0, nRows-1 );
      }
   }
   
   /**
    * Inserts the given row of data at the given row index.
    * 
    * @param iRow       the model index of the row to be inserted after 
    *                   (if the value is the number of rows in the table, insert 
    *                   after the last row)
    * @param lRowValues the list of values for the row to be inserted (the list
    *                   must implement Cloneable).
    */   
   public void insertRow( int iRow, List lRowValues )
   {
      if (!(lRowValues instanceof Cloneable))
         throw new IllegalArgumentException( "List of values must be Cloneable" );    /* I18NOK:EMS */
      // insert the data and fire the event
      m_lData.add( iRow+1, lRowValues );
      fireTableRowsInserted( iRow+1, iRow+1 );
   }
   
   /**
    * Deletes the specified row.
    * 
    * @param iRow the model index of the row to be deleted
    */
   public void deleteRow( int iRow )
   {
      List lDeletedRowValues = (List) m_lData.remove( iRow );
      fireTableRowDeleted( iRow, lDeletedRowValues );
   }
   
   /**
    * Gets a copy of the column values for a row.  
    * 
    * @param iRow the row of the values
    * 
    * @return the column values for the row
    */
   public List getRowValues( int iRow )
   {
      // rlr: I don't want to cast this to ArrayList but how do I get to clone
      //      otherwise.  If someone uses a List of a different type, this will
      //      have to be fixed.  But then why would they do that?
      ArrayList alValues = (ArrayList) m_lData.get( iRow );
      return (List) alValues.clone();
   }
   
   /**
    * Sets a row's values.  
    * 
    * @param iRow       the row of the values
    * @param lRowValues the values of the row
    */
   public void setRowValues( int iRow, List lRowValues )
   {
      List lData = (List) m_lData.get( iRow );
      lData.clear();
      lData.addAll( lRowValues );
      fireTableRowsUpdated( iRow, iRow );
   }
   
   /**
    * Creates a List containing the default values for each column.  The values 
    * are suitable for inserting a new row into the table model.
    * 
    * @return the values used for the blank row
    */      
   public List createDefaultRowValues()
   {
      // count the creation of the row      
      m_nRowInstances++;
      
      // create an array list to hold all the data
      int nColumns = getColumnCount() + getHiddenColumnCount();
      List lRow = new ArrayList( nColumns );

      // create the values for the columns      
      int iColumn;
      for ( iColumn=0; iColumn<nColumns; iColumn++ )
         lRow.add( m_aColumns[ iColumn ].createDefaultValue() );

      return lRow;
   }

   /**
    * Validates the proposed value for the given cell.
    * 
    * @param oValue the proposed value
    * @param iRow   the model row index of the cell
    * @param iCol   the model column index of the cell
    * 
    * @return true = the value is valid
    */
   public boolean validateValueAt( Object oValue, int iRow, int iCol )
   {
      return m_aColumns[ iCol ].validateValue( oValue, this, iRow );
   }
   
	/**
	 * Validates a cell value.  
	 * This method should be used prior to setting cell value for the TableModel
	 * @param value -- A cell value
	 * @param row 
	 * @param col
	 * @return boolean -- A result of the validation.  The method will return false if the specified value can not be 
	 * accepted for the specified cell of the table.
	 */
	public boolean validate(Object value, int row, int col )
   {
      return validateValueAt(value, row, col);
   }

   /**
    * Fires the table model populated event.  The table model populated event is 
    * really a table rows inserted event for all the rows currently in the model.
    * This can be called at the end of moveDataToModel.
    */
   public void fireTableModelPopulated()
   {
      int nRows = m_lData.size();
      if (nRows > 0)
         fireTableRowsInserted( 0, nRows-1 );
   }

   /**
    * Fires the table row deleted event.  This version of the event preserves
    * the list of data for each column in the row.
    * 
    * @param iRow  the row  that was deleted
    * @param lData the data that was deleted
    */
   public void fireTableRowDeleted( int iRow, List lData )
   {
      fireTableChanged( createDeletedRowTableModelEvent( this, iRow, lData ) );
   }
   
   /**
    * Finds the rows that have the given value for the given column.  If the 
    * row's value at the column index is equal to the given value, the row is
    * added to the array of row indexes returned.
    * 
    * @param iColumn the column index of the column to be tested
    * @param testee the value that the column's value will be tested against
    * 
    * @return an array of row indexes for the rows that were found
    */   
   public int[] findRows( int iColumn, Object testee )
   {
      return findRows( new cFilter( iColumn, testee ) );
   }
   
   /**
    * Finds the rows that pass the given filter.  The filter's equals method
    * will be called with the list of values for the row.  If equals returns
    * true the row is found and is added to the list of rows returned.
    *
    * @param filter the filter used to test the rows
    * 
    * @return an array of row indexes for the rows that were found
    */   
   public int[] findRows( Object filter )
   {
      int   nRows = getRowCount();
      int   iRow;

      int[] aRowsFound = new int[ nRows ];
      int   nRowsFound = 0;
      
      for ( iRow=0; iRow<nRows; iRow++ )
      {
         if (filter.equals( getRowValues( iRow ) ))
         {
            aRowsFound[ nRowsFound ] = iRow;
            nRowsFound++;
         }
      }

      int[] aRowsReturned = new int[ nRowsFound ];
      int   iRowFound;
      for ( iRowFound=0; iRowFound<nRowsFound; iRowFound++ )
         aRowsReturned[ iRowFound ] = aRowsFound[ iRowFound ];
      
      return aRowsReturned;
   }


   //---------------------------------------------------------------------------
   // Event creators
   //---------------------------------------------------------------------------

   /**
    * Creates the deleted row table model event.
    * 
    * @param mdl      the table model (the source)
    * @param iRow     the original (real model) row index
    * @param lRowData the deleted row's data
    * 
    * @return the deleted row table model event
    */
   protected TableModelEvent createDeletedRowTableModelEvent( TableModel mdl, int iRow, List lRowData )
   {
      return new cDeletedRowTableModelEvent( mdl, iRow, lRowData );
   }
   
   //---------------------------------------------------------------------------
   // Event classes
   //---------------------------------------------------------------------------
      
   /**
    * DeletedRowTableModelEvent - an event for a deleted row in the table that
    *                             provides the row's data for use in handling
    *                             the event.  This event must look like any
    *                             other deleted row event.
    */
   public class cDeletedRowTableModelEvent extends TableModelEvent
   {
      /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List m_lRowData;

      /**
       * Constructs the deleted row table model event.
       *
       * @param mdl  the model generating the event
       * @param iRow the row that was deleted
       * @param lRow the array list of data for the row that was deleted.
       */
      public cDeletedRowTableModelEvent( TableModel mdl, int iRow, List lRow )
      {
         super( mdl, iRow, iRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE );
         m_lRowData = lRow;
      }

      /**
       * Gets the data for the row that was deleted.
       *
       * @return the data for the row that was deleted.
       */
      public List getRowData()
      {
         return m_lRowData;
      }
   } // cDeleteRowTableModelEvent


   //---------------------------------------------------------------------------
   // Miscellaneous classes
   //---------------------------------------------------------------------------
   
   /**
    * cFilter is a filter class for comparing a given value to a row's column 
    * value.If the given value equals the row's column value, the filter's 
    * equals method returns true.
    */
   protected class cFilter
   {
      protected int    m_iColumn;
      protected Object m_oValue; 
    
      /**
       * Constructs the filter.
       * 
       * @param iColumn the index of the column value to be tested
       * @param oValue  the value against which the row's column's value is to be tested
       */
      public cFilter( int iColumn, Object oValue )
      {
         m_iColumn = iColumn;
         m_oValue  = oValue;
      }
      
      /**
       * Returns true if the object passes the filter.  The object has to be a
       * list of a row's columns' values.
       * 
       * @param oValue the list of the row's columns' values.
       * 
       * @return true = the row passes the filter
       */
      public boolean equals( Object oValue )
      {
         List lValues = (List) oValue;
         return m_oValue.equals( lValues.get( m_iColumn ) );
      }
      
      public int hashcode() 
      {
         // NOTE: this hashcode() implementation violates the contract that hashcode return the same value when equals() would return true
         // HOWEVER, code inspection has revealed this object is not being used as a hash key AND changing the hashcode method to meet
         // the contract is overly risky.
         return super.hashCode();
      }
      
   } // cFilter
   
//------------------------------------------------------------------------------
// Code graveyard
//------------------------------------------------------------------------------
//   /**
//    * cRowConverter maintains the row indexes.  It is used to provide conversion
//    * between the original (real model) row indexes and the model indexes.  It 
//    * also provide the sort algorithm.
//    */
//   protected class cRowConverter
//   {
//      int   m_mRows;
//      int   m_nRows;
//      int[] m_aOriginalIndexes;   // given a model     index, what is the original index
//      int[] m_aModelIndexes;      // given an original index, what is the model index
//
//      // these are temporary variables so that they don't have to be passed to each
//      // of the sort and compare method
//      int[]   m_aColumns;     // columns to be compared
//      int[]   m_aDirections;  // directions of comparison
//      
//      protected static final int BLOCKSIZE = 10;
//
//      /**
//       * Converts a row index from an original (real model) index to a model 
//       * index.
//       * 
//       * @param iOriginalIndex the original row index
//       * 
//       * @param the model row index
//       */
//      public int toModel( int iOriginalIndex )
//      {
//         return m_aModelIndexes[ iOriginalIndex ];
//      }
//   
//      /**
//       * Converts a row index from a model index to the original (real model) 
//       * index.
//       * 
//       * @param iModelIndex the model row index
//       * 
//       * @param the original row index
//       */
//      public int toOriginal( int iModelIndex )
//      {
//         return m_aOriginalIndexes[ iModelIndex ];
//      }
//
//      /**
//       * Initializes the row converter to the given size.  Used when data is 
//       * first loaded into the model.
//       * 
//       * @param nRows the number of rows in the data model.
//       */
//      public void initialize( int nRows )
//      {
//         if (nRows > m_mRows)
//         {
//            m_mRows = nRows;
//            m_aOriginalIndexes = new int[ nRows ];
//            m_aModelIndexes    = new int[ nRows ];
//         }
//
//         m_nRows = nRows;
//         reset();
//      }      
//
//      /**
//       * Resets the row converter to the original order of rows.
//       */
//      public void reset()
//      {
//         int iRow;
//         for ( iRow=0; iRow<m_nRows; iRow++ )
//         {
//            m_aModelIndexes   [ iRow ] = iRow;
//            m_aOriginalIndexes[ iRow ] = iRow;
//         }
//      }
//      
//      /**
//       * Removes a row from the row converter.
//       * 
//       * @param iOriginalRow the original row index of the row
//       */      
//      public void remove( int iOriginalRow )
//      {
//         // 0 1 2 3 4 5 6 7
//         // d e h f a c b g
//         // 3 4 7 5 0 2 1 6   given original, get model index    = m_aModelIndexes
//         // a b c d e f g h
//         // 4 6 5 0 1 3 7 2   given model,    get original index = m_aOriginalIndexes
//         //
//         // delete e, original=1, model=4
//         //
//         // 0 1 2 3 4 5 6 
//         // d h f a c b g
//         // 3 6 4 0 2 1 5   given original, get model index    = m_aModelIndexes
//         // a b c d f g h
//         // 3 5 4 0 2 6 1   given model,    get original index = m_aOriginalIndexes
//         int iModelRow = m_aModelIndexes[ iOriginalRow ];
//
//         // remove the row from the original indexes
//         int nRowsMoved = m_nRows - iModelRow - 1;
//         if (nRowsMoved > 0)
//            System.arraycopy( m_aOriginalIndexes, iModelRow+1,    m_aOriginalIndexes, iModelRow,    nRowsMoved );
//
//         // remove the row from the model indexes            
//         nRowsMoved = m_nRows - iOriginalRow - 1;
//         if (nRowsMoved > 0)
//            System.arraycopy( m_aModelIndexes,    iOriginalRow+1, m_aModelIndexes,    iOriginalRow, nRowsMoved );
//
//         // decrement the number of rows
//         m_nRows--;
//         
//         // in the original indexes, decrement any original index that was 
//         // greater than the deleted original index.  in the model indexes,
//         // decrement any model index that was greater than the deleted 
//         // model index.
//         int iRow;
//         for ( iRow=0; iRow<m_nRows; iRow++ )
//         {
//            if (m_aOriginalIndexes[ iRow ] > iOriginalRow)
//               m_aOriginalIndexes[ iRow ]--;
//            if (m_aModelIndexes[ iRow ] > iModelRow)
//               m_aModelIndexes[ iRow ]--;
//         }
//      }
//
//      /**
//       * Inserts a row into the row converter.
//       * 
//       * @param iOriginalRow the original row index
//       */      
//      public void insert( int iOriginalRow )
//      {
//         // 0 1 2 3 4 5 6 7
//         // d e h f a c b g
//         // 3 4 7 5 0 2 1 6   given original, get model index    = m_aModelIndexes
//         // a b c d e f g h
//         // 4 6 5 0 1 3 7 2   given model,    get original index = m_aOriginalIndexes
//         //
//         // insert after e, original=1, model=4
//         //
//         // 0 1 2 3 4 5 6 7 8
//         // d e n h f a c b g
//         // 3 4 5 8 6 0 2 1 7   given original, get model index    = m_aModelIndexes
//         // a b c d e n f g h
//         // 5 7 6 0 1 2 4 8 3   given model,    get original index = m_aOriginalIndexes
//         checkCapacity( 1 );
//         
//         int iModelRow = m_aModelIndexes[ iOriginalRow ];
//
//         // make room for the row in the original indexes
//         int nRowsMoved = m_nRows - iModelRow - 1;
//         if (nRowsMoved > 0)
//            System.arraycopy( m_aOriginalIndexes, iModelRow+1,    m_aOriginalIndexes, iModelRow+2,    nRowsMoved );
//
//         // make room for the row in the model indexes
//         nRowsMoved = m_nRows - iOriginalRow - 1;
//         if (nRowsMoved > 0)
//            System.arraycopy( m_aModelIndexes,    iOriginalRow+1, m_aModelIndexes,    iOriginalRow+2, nRowsMoved );
//
//         // increment the number of rows
//         m_nRows++;
//         
//         // in the original indexes, increment any original index that was 
//         // greater than the deleted original index.  in the model indexes,
//         // increment any model index that was greater than the deleted 
//         // model index.
//         int iRow;
//         for ( iRow=0; iRow<m_nRows; iRow++ )
//         {
//            if (m_aOriginalIndexes[ iRow ] > iOriginalRow)
//               m_aOriginalIndexes[ iRow ]++;
//            if (m_aModelIndexes[ iRow ] > iModelRow)
//               m_aModelIndexes[ iRow ]++;
//         }
//         
//         // insert the new rows
//         m_aModelIndexes[    iOriginalRow+1 ] = iModelRow   +1;
//         m_aOriginalIndexes[ iModelRow   +1 ] = iOriginalRow+1;
//      }
//      
//      /**
//       * Moves a row from the from index to the to index.  Since this method
//       * is really only changing the "model" rows, the indexes are model indexes
//       * as opposed to the usual original indexes.
//       * 
//       * @param iFrom the from model index
//       * @param iTo   the to   model index
//       */
//      public void moveRow( int iFrom, int iTo )
//      {
//         // 0 1 2 3 4 5 6 7
//         // d e h f a c b g
//         // 3 4 7 5 0 2 1 6   given original, get model index    = m_aModelIndexes
//         // a b c d e f g h
//         // 4 6 5 0 1 3 7 2   given model,    get original index = m_aOriginalIndexes
//         //
//         // move c, original=5, model=2 to model=6
//         //
//         // 0 1 2 3 4 5 6 7
//         // d e h f a c b g
//         // 2 3 7 4 0 6 1 5   given original, get model index    = m_aModelIndexes
//         // a b d e f g c h
//         // 4 6 0 1 3 7 5 2   given model,    get original index = m_aOriginalIndexes
//         //
//         // move f, original=3, model=5 to model=2
//         //
//         // 0 1 2 3 4 5 6 7
//         // d e h f a c b g
//         // 4 5 7 2 0 3 1 6   given original, get model index    = m_aModelIndexes
//         // a b f c d e g h
//         // 4 6 3 5 0 1 7 2   given model,    get original index = m_aOriginalIndexes
//
//         // save the row's original index
//         int iOriginalFrom = m_aOriginalIndexes[ iFrom ];
//
//         // if move indexes up, ...
//         if (iTo > iFrom)
//         {
//            // move indexes up, filling gap at iFrom and creating gap iTo
//            int nMove = iTo - iFrom;
//            System.arraycopy( m_aOriginalIndexes, iFrom+1, m_aOriginalIndexes, iFrom, nMove );
//
//            // decrement the model indexes for all the rows that moved up
//            int i;
//            for ( i=iFrom; i<iTo; i++ )
//               m_aModelIndexes[ m_aOriginalIndexes[ i ] ]--;
//         }
//
//         // otherwise, ...
//         else
//         {
//            // move indexes down, filling gap at iFrom and creating gap at iTo
//            int nMove  = iFrom - iTo;
//            System.arraycopy( m_aOriginalIndexes, iTo, m_aOriginalIndexes, iTo+1, nMove );
//
//            // increment the model indexes for all the rows that moved down
//            int i;
//            for ( i=iTo+1; i<=iFrom; i++ )
//               m_aModelIndexes[ m_aOriginalIndexes[ i ] ]++;
//         }
//
//         // put the row back into place
//         m_aOriginalIndexes[ iTo           ] = iOriginalFrom;
//         m_aModelIndexes[    iOriginalFrom ] = iTo;
//      }
//
//      /**
//       * Checks the capacity to make sure there is enough room for the number of
//       * rows being added.
//       * 
//       * @param nRows the number of rows being added
//       */
//      public void checkCapacity( int nRows )
//      {
//         // if there is enough room, done
//         if (m_nRows + nRows <= m_mRows)
//            return;
//         
//         // calculate the new size (increase by the larger of 1/2 the current
//         // size or by the requested size)
//         int mRows = m_mRows + Math.max( m_mRows / 2, nRows );
//         
//         // create new arrays
//         int[] aOriginalIndexes = new int[ mRows ];
//         int[] aModelIndexes    = new int[ mRows ];
//         
//         // copy the existing arrays to the new arrays
//         System.arraycopy( m_aOriginalIndexes, 0, aOriginalIndexes, 0, m_nRows );
//         System.arraycopy( m_aModelIndexes,    0, aModelIndexes,    0, m_nRows );
//         
//         // save the new max and the new arrays
//         m_mRows = mRows;
//         m_aOriginalIndexes = aOriginalIndexes;
//         m_aModelIndexes    = aModelIndexes;
//      }
//
//
//      //------------------------------------------------------------------------
//      // sort methods
//      //------------------------------------------------------------------------
//
//      /**
//       * Sorts the rows of the table model.
//       * 
//       * @param aColumns the list of column indexes of the columns to be used in
//       *                  sorting the rows.
//       * @param aOrder   the order in which the rows are to be sorted:
//       *                  <ul>
//       *                  <li>ORIGINAL   - the rows are to be returned to their 
//       *                                   original order.</li>
//       *                  <li>ASCENDING  - the rows are to be sorted based on
//       *                                   ascending values in the given 
//       *                                   columns.</li>
//       *                  <li>DESCENDING - the rows are to be sorted based on
//       *                                   descending values in the given 
//       *                                   columns.</li>
//       *                  </ul>
//       */
//      public void sort( int[] aColumns, int[] aDirections )
//      {
//         m_aColumns    = aColumns;
//         m_aDirections = aDirections;
//
//         if ((aColumns           == null) ||
//             (aColumns.length    == 0   ) ||
//             (aDirections        == null) ||
//             (aDirections.length == 0   ))    
//            reset();
//
//         else
//         {
//            // Tricky code alert!!!
//            // It doesn't matter that the rows are not in the original order
//            // initially.  So the indexes are not reset.  Also, the shuttle
//            // sort requires two arrays to move the indexes between and the
//            // desired result is to have the "sorted" indexes in the original
//            // indexes array.  Also, the model indexes array will get rebuilt
//            // after the sort is complete.
//            //
//            // Therefore, sort the model indexes array into the original indexes
//            // array.
//            shuttlesort( m_aModelIndexes, m_aOriginalIndexes, 0, m_nRows );
//            
//            // now build the model indexes
//            int iRow;
//            for ( iRow=0; iRow<m_nRows; iRow++ )
//               m_aModelIndexes[ m_aOriginalIndexes[ iRow ] ] = iRow;
//         }
//      }
//      
//      /**
//       * Compares two rows by a given column.
//       * 
//       * @param iRow1 the row index of the first  row
//       * @param iRow2 the row index of the second row
//       * @param iCol  the column index of the value to be compared for comparing
//       *               the rows
//       * 
//       * @return the result of comparing the rows
//       *          <ul>
//       *          <li>&lt 0, first row &lt second row </li>
//       *          <li>= 0, first row = second row</li>
//       *          <li>> 0, first row > second row</li>
//       *          </ul>
//       */
//      protected int compareRowsByColumn( int iRow1, int iRow2, int iCol )
//      {
//         // get the column's class
//         Class clsColumn = getColumnClass( iCol );
//
//         // get the row's column values
//         Object o1 = getValueAt( iRow1, iCol );
//         Object o2 = getValueAt( iRow2, iCol );
//
//         // compare strings
//         if (clsColumn == String.class)
//         {
//            String s1 = (String) o1;
//            String s2 = (String) o2;
//            return s1.compareToIgnoreCase( s2 );
//         }
//         
//         // compare integers
//         else if (clsColumn == Integer.class)
//         {
//            Integer i1 = (Integer) o1;
//            Integer i2 = (Integer) o2;
//            return i1.compareTo( i2 );
//         }
//   
//         // compare bytes
//         else if (clsColumn == Byte.class)
//         {
//            Byte b1 = (Byte) o1;
//            Byte b2 = (Byte) o2;
//            return b1.compareTo( b2 );
//         }
//
//         // compare doubles
//         else if (clsColumn == Double.class)
//         {
//            Double d1 = (Double) o1;
//            Double d2 = (Double) o2;
//            return d1.compareTo( d2 );
//         }
//
//         // compare floats
//         else if (clsColumn == Float.class)
//         {
//            Float f1 = (Float) o1;
//            Float f2 = (Float) o2;
//            return f1.compareTo( f2 );
//         }
//
//         // compare longs
//         else if (clsColumn == Long.class)
//         {
//            Long l1 = (Long) o1;
//            Long l2 = (Long) o2;
//            return l1.compareTo( l2 );
//         }
//
//         // compare shorts
//         else if (clsColumn == Short.class)
//         {
//            Short s1 = (Short) o1;
//            Short s2 = (Short) o2;
//            return s1.compareTo( s2 );
//         }
//
//         // compare characters
//         else if (clsColumn == Character.class)
//         {
//            Character c1 = (Character) o1;
//            Character c2 = (Character) o2;
//            return c1.compareTo( c2 );
//         }
//
//         // compare characters
//         else if (clsColumn == Boolean.class)
//         {
//            Boolean b1 = (Boolean) o1;
//            Boolean b2 = (Boolean) o2;
//            if (b1.equals( b2 ))
//               return 0;
//            else if (b1.booleanValue())
//               return 1;
//            else
//               return -1;
//         }
//
//         return 0;
//      }
//   
//      /**
//       * Compares two rows using the columns and the order previously saved
//       * in member variables.
//       * 
//       * @param iRow1 the first  row to be compared
//       * @param iRow2 the second row to be compared
//       * 
//       * @return the result of comparing the rows
//       *          <ul>
//       *          <li>&lt 0, first row &lt second row </li>
//       *          <li>= 0, first row = second row</li>
//       *          <li>> 0, first row > second row</li>
//       *          </ul>
//       */
//      protected int compareRows( int iRow1, int iRow2 )
//      {
//         int iColumn;
//         int nColumns = m_aColumns.length;
//         for ( iColumn=0; iColumn<nColumns; iColumn++ )
//         {
//            int result = compareRowsByColumn( iRow1, iRow2, m_aColumns[ iColumn ] );
//            if (result != 0)
//               return m_aDirections[ iColumn ] == SortableInterface.ASCENDING ? result : -result;
//         }
//   
//         return 0;
//      }
//   
//
//      // This is a home-grown implementation which we have not had time
//      // to research - it may perform poorly in some circumstances. It
//      // requires twice the space of an in-place algorithm and makes
//      // NlogN assigments shuttling the values between the two
//      // arrays. The number of compares appears to vary between N-1 and
//      // NlogN depending on the initial order but the main reason for
//      // using it here is that, unlike qsort, it is stable.
//
//      /**
//       * Shuttle sorts the rows.  The comparison criteria are assumed to have
//       * been already saved in member variables from the compareRows method.
//       * The rows to be sorted are in the arrays from low (inclusive) to 
//       * high (exclusive).
//       * 
//       * @param from the array of indexes to be sorted
//       * @param to   the sorted array of indexes
//       * @param low  the first index to be sorted
//       * @param high the first index to not be sorted
//       */
//      protected void shuttlesort( int[] from, int[] to, int low, int high )
//      {
//         // if there is nothing to sort, done!
//         if (high - low < 2)
//            return;
//   
//         // divide and conquer
//         // divide the array of indexes into two parts and sort them
//         int middle = (low + high)/2;
//         shuttlesort(to, from, low, middle);
//         shuttlesort(to, from, middle, high);
//   
//         // This is an optional short-cut; at each recursive call,
//         // check to see if the elements in this subset are already
//         // ordered.  If so, no further comparisons are needed; the
//         // sub-array can just be copied.  When the number of elements is 
//         // three, the elements are partitioned so that the first set, [low, mid), 
//         // has one element and and the second, [mid, high), has two. The 
//         // optimization is skipped when the number of elements is three or less 
//         // since the check for the optimization will not save any comparisons
//         // from the normal algorithm.
//         if (high - low >= 4 && compareRows( from[middle - 1], from[middle]) <= 0 )
//         {
//            for (int i = low; i < high; i++)
//               to[i] = from[i];
//            return;
//         }
//   
//         // merge the two result sets
//         int p = low;
//         int q = middle;
//         for (int i = low; i < high; i++)
//         {
//            if ((q >= high) || (p < middle && compareRows( from[p], from[q] ) <= 0))
//               to[i] =  from[p++];
//            else
//               to[i] =  from[q++];
//         }
//      }
//      
//   } // cRowConverter
//      

}