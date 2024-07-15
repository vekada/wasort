/* $Id: TreeTableSelector.java,v 1.1.2.3.24.1 2011/07/14 21:22:05 dawong Exp $ */
/**
 * Title:       TreeTableSelector.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.workspace.visuals.dualselector;



import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;

import com.sas.swing.models.ViewDefaultModel;
import com.sas.swing.visuals.dualselector.BaseDualSelector;
import com.sas.swing.visuals.dualselector.JTreeDualSelectorAdapter;

/**
 * TreeTableSelector is a component that contains a tree and a table to be used 
 * to make selections.  The tree is on the left and the table is on the right.
 * There are buttons between the two components that transfer selections between 
 * the two components.  This comonent is highly customizable.  See 
 * BaseDualSelector for the customization possiblities
 */
public class TreeTableSelector extends BaseDualSelector implements TreeSelectionListener, ListSelectionListener, ViewDefaultModel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private boolean clearSelection;
   private boolean moveParentsOrChildren;
   private boolean parentNodesMoveable;
   private String rootTreeNodeName = "Root";
   transient private TreeModel sourceTreeModel;
   transient private TableModel targetTableModel;

   /**
    * the JTree component
    */
   protected JTree sourceJTree;
   /**
    * the JList component
    */
   protected JTable targetJTable;
   /**
    * a tree node used as the root of the JTree component
    */
   protected DefaultMutableTreeNode rootNode;
   

   /**
    * Constructs a TreeListSelector component.
    * 
    * @param tree  the tree  to use in the tree-table selector
    * @param table the table to use in the tree-table selector
    */
   public TreeTableSelector( JTree tree, JTable table )
   {
      setDesignTime(com.sas.beans.Util.isDesignTime());
//    setOpaque(true);
      targetMaximumCount = -1;
      dragAndDropEnabled = true;
      
      // Create components ...
      sourceAdapter = new WsTreeDualSelectorAdapter(tree, this);
      targetAdapter = new JTableDualSelectorAdapter(table, this);
      sourceJTree = (JTree) sourceAdapter.getComponent();
      targetJTable = (JTable) targetAdapter.getComponent();

      setParentNodesMoveable(false);

      initializeBaseDualSelector();

      sourceJTree.addMouseListener(sourceMouseListener);
      targetJTable.addMouseListener(targetMouseListener);

      if (getModel() == null)
         setModel(tree.getModel());

      if (getTargetModel() == null)
         setTargetModel(table.getModel());

      sourceAdapter.setSelectedIndex(1);
      initialized = true;
      ensureButtonsEnabled();
      addListeners();
      updateUI();
   }

   /**
    * Adds tree and list selection listeners.
    */
   protected void addListeners()
   {
      super.addListeners();

      sourceJTree.removeTreeSelectionListener(this);     // RLR: why?  (to make sure it's only a listener once?)
      sourceJTree.addTreeSelectionListener(this);

      targetJTable.getSelectionModel().removeListSelectionListener(this);   // RLR: why?  (to make sure it's only a listener once?)
      targetJTable.getSelectionModel().addListSelectionListener(this);
   }

   /**
    * Returns the cell renderer for the source component. The cell renderer controls
    * how the items in the source component are displayed.
    * @return cell renderer for the source component 
    * @see #setSourceCellRenderer
    */
      public TreeCellRenderer getSourceCellRenderer()
      {
         return sourceJTree.getCellRenderer();
      }

      /**
    * Sets the cell renderer for the source component.  The cell renderer controls
    * how the items in the source component are displayed.
    * @param newValue the new value for the source component cell renderer
    * @see #getSourceCellRenderer
    */
      public void setSourceCellRenderer(TreeCellRenderer newValue)
      {
         TreeCellRenderer oldValue = sourceJTree.getCellRenderer();
         if (newValue == oldValue)
            return;
         
         sourceJTree.setCellRenderer(newValue);
      
         firePropertyChange("SourceTreeCellRenderer", oldValue, newValue);
      }

//   /**
//    * Returns the cell renderer for the target component.  The cell renderer controls
//    * how the items in the target component are displayed.
//    * @return the cell renderer for the target component
//    * @see #setTargetCellRenderer
//    */
//      public TableCellRenderer getTargetCellRenderer()
//      {
//         return targetJTable.getCellRenderer();
//      }
//
//      /**
//    * Sets the cell renderer for the target component.  The cell renderer controls
//    * how the items in the target component are displayed.  
//    * @param newValue the new value for the target component cell renderer
//    * @see #getTargetCellRenderer
//    */
//      public void setTargetCellRenderer(ListCellRenderer newValue)
//      {
//         ListCellRenderer oldValue = targetJTable.getCellRenderer();
//         if (newValue == oldValue)
//            return;
//      
//         targetJTable.setCellRenderer(newValue);
//      
//         firePropertyChange("targetListCellRenderer", oldValue, newValue);
//      }
//
   /**
    * Determines whether or not the root node from the source tree is visible.
    * @param visible true if the root node of the tree is to be displayed
    */
   public void setSourceRootVisible(boolean visible)
   {
      sourceJTree.setRootVisible(visible);
      if (visible == false)
         sourceJTree.setShowsRootHandles(true);
   }

   /**
    * Returns true if the root node of the source tree is displayed.
    * @return true if the root node of the tree is displayed
    * @see #setSourceRootVisible(boolean)
    */
   public boolean isSourceRootVisible()
   {
      return sourceJTree.isRootVisible();
   }

   /**
    * Determines whether the parent nodes, in the tree, can be moved to the list portion.  
    * @param moveable Set to true if parents are allowed to be moved.  False if parents cannot be moved.
    * @see #isParentNodesMoveable()
    */
   public void setParentNodesMoveable (boolean moveable)
   {
      parentNodesMoveable = moveable;
      ((JTreeDualSelectorAdapter) sourceAdapter).setParentNodesMoveable(moveable);
   }

   /**
    * Returns the true if the parent nodes are moveable.  False if the parent nodes are not moveable. 
    * @return true if the parent nodes are moveable, false if the parent nodes are not moveable
    * @see #setParentNodesMoveable(boolean)
    */
   public boolean isParentNodesMoveable ()
   {
      return parentNodesMoveable;
   }

   /**
    * Defines whether to move only parent nodes from the tree of move only children nodes.
    * @param nodes true if the parent nodes, false if child nodes
    * @see #isParentsOrChildrenMoveable()
    */
   public void setParentOrChildrenMoveable(boolean nodes)
   {
      moveParentsOrChildren = nodes;
      if (moveParentsOrChildren == true)
         ((JTreeDualSelectorAdapter) sourceAdapter).setParentNodesOnlyMoveable(true);
      else
         ((JTreeDualSelectorAdapter) sourceAdapter).setChildNodesOnlyMoveable(true);
   }

   /**
    * Returns true if the parent nodes are to be moved and false if only the children nodes are to be moved.
    * @return true if the parent nodes are to be moved and false if only the children nodes are to be moved
    * @see #setParentOrChildrenMoveable(boolean)
    */
   public boolean isParentsOrChildrenMoveable()
   {
      return moveParentsOrChildren;
   }

   /* ************************************************************************ *
    * Model \ JTree Interaction methods ...                                    *
    * ************************************************************************ */

   /**
    * Returns the TreeModel for the source component.
    * The TreeModel for the target component can be obtained by calling getTargetModel.
    * @return the TreeModel for the source component
    * @see #setModel
    */
   public TreeModel getModel()
   {
      return sourceTreeModel;
   }
   
   /**
    * Returns the TableModel for the target component.
    * The TableModel for the source component can be obtained by calling getModel.
    * @return the TreeModel for the target component
    * @see #setTargetModel
    */
   public TableModel getTargetModel()
   {
      return targetTableModel;
   }

   /**
    * Sets the TreeModel for the source component.
    * The TreeModel for the target component can be set by calling setTargetModel.
    * @param treeModel the TreeModel to set for the source component
    * @see #getModel
    */
   public void setModel(TreeModel treeModel)
   {
      TreeModel oldModel = sourceTreeModel;
      
      if (treeModel != null)
      {
         sourceTreeModel = treeModel;
         // attach the tree to the tree box
         if (sourceJTree != null)
            sourceJTree.setModel(sourceTreeModel);    
      }
      else
      {        
         rootNode = new DefaultMutableTreeNode(rootTreeNodeName);
         sourceTreeModel = new DefaultTreeModel(rootNode);
         // attach the tree to the tree box
         if (sourceJTree != null)
            sourceJTree.setModel(sourceTreeModel);
      }
      
      //Select the first item by default
      if (sourceJTree.getRowCount() > 0)
      {
         sourceJTree.removeTreeSelectionListener(this);
         sourceJTree.setSelectionRow(0);
         sourceJTree.addTreeSelectionListener(this);
      }

      if (!(sourceTreeModel instanceof DefaultTreeModel))
         setCopyModeEnabled(true);
 

      firePropertyChange("model", oldModel, sourceTreeModel);
   }

   /**
    * Sets the TableModel for the target component.
    * The TreeeModel for the source component can be set by calling setModel.
    * @param tableModel the TableModel to set for the target component
    * @see #getTargetModel
    * @throws IllegalArgumentException
    */
   public void setTargetModel(TableModel tableModel) throws IllegalArgumentException
   {
      if (tableModel instanceof SelectorTableModel)
      {
         TableModel oldModel = targetTableModel;

         if (tableModel != null)
            targetTableModel = tableModel;
            if (targetJTable != null)
               targetJTable.setModel(targetTableModel);
         else
         {
            targetTableModel = new DefaultTableModel();
            if (targetJTable != null)
               targetJTable.setModel(targetTableModel);
         }
         firePropertyChange("targetListModel", oldModel, targetTableModel);
      }
      else
         throw new IllegalArgumentException();  //Need to add a explanation string
   }

   /**
    * Returns true if the default model is being used, false otherwise.
    * @return true if the default model is being used, false otherwise.
    * @see com.sas.swing.models.ViewDefaultModel
    **/
   public boolean isDefaultModelAttached()
   {
      if (getModel() instanceof DefaultTreeModel)
         return true;
      else
         return false;
   }

   /* ************************************************************************ *
    * Event Handler methods ...                                                *
    * ************************************************************************ */

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

         clearSelection = true;
         try
         {
            targetJTable.getSelectionModel().clearSelection();
            direction = RIGHT;
         }
         finally
         {
            clearSelection = false;
         }
      }
   }

   /**
    * Event handler for the ListSelectionEvent's received.
    * @param ie the event to handle
    */
   public void valueChanged(ListSelectionEvent ie)
   {                                 
      if (ie.getFirstIndex() >= 0)
         direction = LEFT;
      else
         direction = RIGHT;

//      Object source = ie.getSource();
      if (!clearSelection)
      {
         if (ie.getFirstIndex() >= 0)// && source == targetJTable)
         {
            if (alternatingMultipleButtonStyle == true)
               alternatingMultipleButton.setIcon(getAlternatingButtonIcon(ALTERNATING_MULTIPLE_BUTTON, LEFT));

            if (alternatingSingleButtonStyle == true)
               alternatingSingleButton.setIcon(getAlternatingButtonIcon(ALTERNATING_SINGLE_BUTTON, LEFT));
            else
            {
//             leftButton.setEnabled(true);
//             rightButton.setEnabled(false);
            }

            ensureButtonsEnabled();

            clearSelection = true;
            try
            {
               sourceJTree.getSelectionModel().clearSelection();
               direction = LEFT;
            }
            finally
            {
               clearSelection = false;
            }
         }
      }
   }
}

