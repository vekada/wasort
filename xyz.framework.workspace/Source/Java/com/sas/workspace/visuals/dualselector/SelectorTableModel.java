/* $Id: SelectorTableModel.java,v 1.1.2.2 2007/01/15 22:32:52 sasblc Exp $ */
/**
 * Title:       SelectorTableModel.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.workspace.visuals.dualselector;

/**
 * SelectorTableModel is an interface 
 */
public interface SelectorTableModel
{
   /**
    * Gets the object that is represented by the row in the table model.
    * 
    * @param iRow the index of the row
    * 
    * @return the object
    */
   Object getRow( int iRow );

   /**
    * Sets the the row in the table model to represent the object.
    * 
    * @param row the object to be represented in the table model
    * @param iRow the index of the row
    */
   void setRow( Object row, int iRow );

   /**
    * Gets the index of the row in the table model that represents the object.
    * 
    * @param row the object
    * 
    * @return the index of the row (-1 means not found)
    */
   int indexOf( Object row );

   /**
    * Adds a row to the table model using the object to create the rows values.
    * 
    * @param row the object used to create the row
    */
   void addRow( Object row );

   /**
    * Deletes the row specified by the index from the table model.
    *  
    * @param iRow the row index
    */
   void deleteRow( int iRow );
   
   /**
    * Gets the number of rows in the table model.
    * 
    * @return the number of rows in the table model
    */
   int getRowCount();
   
   /**
    * Clears the table model which deletes all the rows in the table model.
    */
   void clear();
}

