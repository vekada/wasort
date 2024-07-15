/* $Id: DefaultSelectorTableModel.java,v 1.1.2.2 2007/01/15 22:32:52 sasblc Exp $ */
/**
 * Title:       DefaultSelectorTableModel.java
 * Description: An abstract table model that can be used in a DefaultSelectorTable.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.workspace.visuals.dualselector;

import java.util.List;

import javax.swing.tree.TreePath;

import com.sas.workspace.WsAbstractTableModel;

/**
 * DefaultSelectorTableModel is an abstract table model that can be used in a 
 * DefaultSelectorTable.
 */
public abstract class DefaultSelectorTableModel extends WsAbstractTableModel implements SelectorTableModel
{

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
      
      setRowValues( iRow, createRowValues( obj ) );
   }
   
   /**
    * Adds a row to the able.  The object used to create the values for the row.
    * 
    * @param obj the object
    * 
    * @see com.sas.workspace.visuals.dualselector.SelectorTableModel#addRow(java.lang.Object)
    */
   public void addRow( Object obj )
   {
      if (obj instanceof TreePath)
         obj = ((TreePath) obj).getLastPathComponent();
      
      insertRow( getRowCount()-1, createRowValues( obj ) );
   }
   
   /**
    * Gets the row index of the object in the table.
    * 
    * @param obj the object
    * 
    * @return the row index of the object (-1 = not found)
    * 
    * @see com.sas.workspace.visuals.dualselector.SelectorTableModel#indexOf(java.lang.Object)
    */
   public int indexOf( Object obj )
   {
      if (obj == null)
         return -1;
      
      for ( int iRow=0; iRow<getRowCount(); iRow++ )
         if (obj.equals( getRow( iRow ) ))
            return iRow;
      
      return -1;
   }

   /**
    * Creates a row of values used to represent the object in the table.
    * 
    * @param obj
    * 
    * @return the row of values
    */
   protected abstract List createRowValues( Object obj );
}

