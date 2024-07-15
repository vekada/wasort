/* $Id: SortColsByTreeTableSelector.java,v 1.1.2.8 2008/01/21 19:48:14 vekada Exp $ */
/**
 * Title:       SortColsByTreeTableSelector.java
 * Description: Selector panel for the tree table for selecting the columns to sort by
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Venu Kadari
 * Support:     Venu Kadari
 */
package com.sas.wadmin.transforms.sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;

import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.data.ITable;
import com.sas.etl.models.job.transforms.common.ISortColumn;
import com.sas.swing.visuals.dualselector.DualSelectorInterface;
import com.sas.swing.visuals.dualselector.DualSelectorUpDownInterface;
import com.sas.wadmin.transforms.sort.SortColsByTreeModel.cColumnTreeNode;
import com.sas.workspace.WsTreeNode;
import com.sas.workspace.visuals.dualselector.JTableDualSelectorAdapter;
import com.sas.workspace.visuals.dualselector.TreeTableSelector;

/**
  * SortColsByTreeTableSelector
  * 
  * Selector panel for the tree table for selecting the columns to order by
 */
public class SortColsByTreeTableSelector extends TreeTableSelector
{
   /**
    * Main constructor
    * 
    * @param tree  Tree to put into the panel
    * @param table Table to put into the panel
    */
   public SortColsByTreeTableSelector( JTree tree, JTable table )
   {
      super( tree, table );
   }
   
   /* ************************************************************************ *
    * Event Handler methods ...                                                *
    * ************************************************************************ */


   /**
    * Transfer all selected items from <em>fromAdapter</em> to <em>toAdapter</em>.
    * This method is called in response to the transfer items button being pressed.
    * No items are transfered if the maximum count for <em>toAdapter</em> will be
    * exceeded as a result of the transfer. This method is called by
    * <em>transferSourceItems</em> and <em>transferTargetItems</em>.  This is a subclass
    * overriding this method in BaseDualSelector, in order to generate sas code after 
    * transferring items.
    * @param fromAdapter the adapter that the items are being transferred from
    * @param toAdapter the adapter that the items are being transferred to
    * @param passedItems a collection of items programatically selected
    */
   protected synchronized void transferItems(DualSelectorInterface fromAdapter, DualSelectorInterface toAdapter, 
            List passedItems)
   {
      int maxCnt = -1;
      int selectedCount;
      int index;

      if (fromAdapter == sourceAdapter)
         maxCnt = getTargetMaximumCount();         
      if (passedItems == null)
      {
         selectedItems = fromAdapter.getSelectedItems();
         selectedCount = fromAdapter.getSelectedCount(); 
         index = fromAdapter.getLastSelectedIndex();
      }
      else
      {
         selectedItems = passedItems;
         selectedCount = passedItems.size();
         index = selectedCount;
      }

      if (selectedItems != null && selectedCount > 0)
      {
         synchronized (fromAdapter)
         {
            synchronized (toAdapter)
            {
               int count = selectedItems.size() + toAdapter.getCount();

               if ((maxCnt == -1) || (count <= maxCnt))
               {
                  // Remove the items from fromAdapter
                  if ( fromAdapter == targetAdapter )
                  {
                     fromAdapter.removeItems(selectedItems);
                     selectedItems.clear();
                  } 
                  else if (selectedItems.size() > 0 && ( fromAdapter == sourceAdapter ))
                  {
                     for (int i=0; i < selectedItems.size(); i++)
                     {
                        Object currObject = selectedItems.get(i);
                        
                        if (currObject instanceof TreePath)
                        {                           
                           TreePath currPath = (TreePath) currObject;
                           WsTreeNode node = (WsTreeNode) currPath.getLastPathComponent();
                           
                           if (node instanceof cColumnTreeNode)
                               continue;
                           else
                           {
                               selectedItems.remove(currPath);
                               i--;
                           }
                        }
                     }
                     
                     // remove any duplicates
                     List lTargetItems = toAdapter.getAllItems();
                     outer_loop : for ( Iterator iter = selectedItems.iterator(); iter.hasNext(); )
                     {
                        Object item = iter.next();
                        item = ((TreePath) item).getLastPathComponent();
                        
                        SortColsByTreeModel.cColumnTreeNode columnTreeNode = (SortColsByTreeModel.cColumnTreeNode) (item);
                        IColumn selectedColumn = columnTreeNode.getTableColumn();
                        ITable selectedTable = columnTreeNode.getTable();
                        
                        for ( int i = 0; i < lTargetItems.size(); i++ )
                        {
                           
                           ArrayList item2 = (ArrayList)lTargetItems.get(i);
                           ISortColumn sortColumn = (ISortColumn) item2.get( SortColsByTableModel.ORDERBYCOLUMN_COL );
                           IColumn targetColumn = sortColumn.getColumn();
                           if ( selectedColumn.getName().equalsIgnoreCase(targetColumn.getName() ) &&
                                    selectedTable.getName().equalsIgnoreCase(targetColumn.getTable().getName()) )
                           {
                              iter.remove();
                              continue outer_loop;
                           }
                        }
                     }
                     
                     toAdapter.addItems(selectedItems);
                  } 
                  
                  // Select the next item in the from adapter                      
                  if (fromAdapter == sourceAdapter)
                     fromAdapter.setSelectedIndex(index);
                  else
                     fromAdapter.setSelectedIndex( Math.min(index, fromAdapter.getAllItems().size() - 1));
              
                  ensureButtonsEnabled();

                  //commented out the getAllItems() call for performance reasons, it can be very, very slow, depending
                  //on the model, if a listener wants to know what is in the source or target, they
                  //can easy query themselves and take the performance hit
                  firePropertyChange("sourceItems", null, null);//fromAdapter.getAllItems());
                  firePropertyChange("targetItems", null, null);//toAdapter.getAllItems());
                  selectedItems = null;
               }
            }
         }
      }  
   }
   
   /**
    * Moves the selected item(s) in the target component either up or down one position.
    * If the destination of an item is outside of the bounds of the component,
    * then the selected item is not moved.
    * Subclasses should override this method to modify behavior.
    * @param moveItemUp true to move the item(s) up, false to move the item(s) down
    */
   protected synchronized void moveTargetItem(boolean moveItemUp)
   {
      
      int[] selectedIndices = ((DualSelectorUpDownInterface) targetAdapter).getSelectedIndices();
      
      
     
      //make a copy of the original indices as moveItems changes it and the sort
      //model need the original ones
      int[] modelIndices = new int [selectedIndices.length];
      for (int i=0; i<selectedIndices.length; i++)
         modelIndices[i] = selectedIndices[i];
      
      if (selectedIndices != null && selectedIndices.length > 0)
      {
         int offset = moveItemUp ? -1 : 1;
         ((DualSelectorUpDownInterface) targetAdapter).moveItems( selectedIndices, offset );
      }
      ensureButtonsEnabled();   
      
      // Clear selection before the move.  This is to work around a bug in Tableview where moving
      // bottom two rows up ends up with selecting bottom three rows.
      ((JTableDualSelectorAdapter) targetAdapter).clearSelections();
      
      // do the move
      ((SortColsByTableModel)targetJTable.getModel()).moveSortModelItems(modelIndices, moveItemUp);
      
      
      // Keep row selections.
      int offset = moveItemUp ? -1 : 1;
      for (int i=0; i<modelIndices.length; i++)
         selectedIndices[i] = modelIndices[i] + offset;
      
      if (targetAdapter instanceof JTableDualSelectorAdapter)
         ((JTableDualSelectorAdapter) targetAdapter).setSelectedIndices(selectedIndices);

   }

   /**
    * Event handler for the TreeSelectionEvent's received.
    * @param ie the event to handle
    */
   public void valueChanged(TreeSelectionEvent ie)
   {
      Object source = ie.getSource();

      //Check to make sure there is a selection because removing selections causes this method to run also
      if (ie.getNewLeadSelectionPath() != null && source == sourceJTree)
         direction = RIGHT;
      else
         direction = LEFT;

      if (ie.getNewLeadSelectionPath() != null && source == sourceJTree)
      {
         if (alternatingMultipleButtonStyle == true)
               alternatingMultipleButton.setIcon(getAlternatingButtonIcon(ALTERNATING_MULTIPLE_BUTTON, RIGHT));

         if (alternatingSingleButtonStyle == true)
               alternatingSingleButton.setIcon(getAlternatingButtonIcon(ALTERNATING_SINGLE_BUTTON, RIGHT));
         else
         {
//          rightButton.setEnabled(true);
//          leftButton.setEnabled(false);
         }

         ensureButtonsEnabled();

         try
         {
            targetJTable.getSelectionModel().clearSelection();
            direction = RIGHT;
         }
         finally
         {
         }
      }
   }
   

}
