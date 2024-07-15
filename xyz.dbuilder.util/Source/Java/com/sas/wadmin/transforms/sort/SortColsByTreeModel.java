/* $Id: SortColsByTreeModel.java,v 1.1.2.12 2007/06/08 23:48:06 dozeri Exp $ */
/**
 * Title:       SortColsByTreeModel.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Venu Kadari
 * Support:     Venu Kadari
 */
package com.sas.wadmin.transforms.sort;


import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.data.ITable;
import com.sas.etl.models.job.ISortingTransform;
import com.sas.expressions.visuals.DataSourceNodeInterface;
import com.sas.expressions.visuals.DataValuesInterface;
import com.sas.expressions.visuals.DistinctValuesInterface;
import com.sas.expressions.visuals.FormattedValuesInterface;
import com.sas.wadmin.visuals.common.ColumnUIUtilities;
import com.sas.workspace.Workspace;
import com.sas.workspace.WsTreeNode;
import com.sas.workspace.tree.WsDefaultTreeModel;

/**
 * SortColsByTreeModel is the tree model used to represent a target table,
 * source tables and their columns.
 */
public class SortColsByTreeModel extends WsDefaultTreeModel
{
   protected ISortingTransform        m_sortModel;
   protected boolean                  m_bSourcesAppearFirst = true;
   protected boolean                  m_bShowSourcesOnly = false;
   protected boolean                  m_bSortColumns = false;
   protected int                      m_iUseColsFromTable ;
   
   /** Use columns from the source table  */
   public static final int USE_SOURCE_TABLE = 0;
   /** Use columns from the target table */
   public static final int USE_TARGET_TABLE = 1;
   /** type value for a character column */
   public static final int TYPE_CHARACTER   = 0;
   /** type value for a numeric column */
   public static final int TYPE_NUMERIC     = 1;

   /**
    * Constructs the table view tree model
    *
    */
   public SortColsByTreeModel()
   {
      super( new WsTreeNode( "Root" ) ); /*I18NOK:EMS**/
   }
   
   /**
    * Constructs the table view tree model
    * 
    * @param sortTransformModel the sort transformation model
    * @param useColsFromTable should be set as 0 - for source table (e.g., rank transform, 1 - target table (e.g., sort transform)
    */
   public SortColsByTreeModel( ISortingTransform sortTransformModel, int useColsFromTable )
   {
      super( new WsTreeNode( sortTransformModel.getName() ) );
      m_sortModel = sortTransformModel;
      m_iUseColsFromTable = useColsFromTable;

   }
   
   /**
    * Gets the model.
    * 
    * @return the sort transform model
    */
   public ISortingTransform getSortModel()
   {
      return m_sortModel;
   }

   /**
    * creates listener for changes to query model
    * 
    */
   public void createModelListener()
   {
   }
   
   /**
    * Moves data from the query to the model.
    * 
    */
   public void moveDataToModel() 
   {
      cTargetTableNode  targetTableNode = getRootTableNode();
      if (targetTableNode == null) return;
      
      targetTableNode.setSorted( false );
      removeAllChildrenFromTree( targetTableNode );
      setRoot( targetTableNode );
      addColumnNodes( targetTableNode );
      
   }
   
   /**
    * 
    * @param tree the tree
    * @param expand true to expand and false to collapse
    */
   public void expandAll(JTree tree, boolean expand) 
   {
       TreeNode root = (TreeNode)tree.getModel().getRoot();
   
       // Traverse tree from root
       expandAll(tree, new TreePath(root), expand);
   }
   
    /**
    * 
    * @param tree the tree
    * @param parent parent's tree path
    * @param expand true to expand and false to collapse
    */
   private void expandAll( JTree tree, TreePath parent, boolean expand )
   {
      // Traverse children
      TreeNode node = (TreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0)
      {
         for ( Enumeration e = node.children(); e.hasMoreElements(); )
         {
            TreeNode n = (TreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild( n );
            expandAll( tree, path, expand );
         }
      }

      // Expansion or collapse must be done bottom-up
      if (expand)
      {
         tree.expandPath( parent );
      }
      else
      {
         tree.collapsePath( parent );
      }
   }
   
   
   /**
    * @param tree com.sun.java.swing.JTree
    * @param start com.sun.java.swing.tree.DefaultMutableTreeNode
    */
//   private static void expandTree( JTree tree, DefaultMutableTreeNode start )
//   {
//      for ( Enumeration children = start.children(); children.hasMoreElements(); )
//      {
//         DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) children.nextElement();
//         if (!dtm.isLeaf())
//         {
//            
//            TreePath tp = new TreePath( dtm.getPath() );
//            tree.expandPath( tp );
//            
//            expandTree( tree, dtm );
//         }
//      }
//      return;
//   }
   
   /**
    * Adds target table to the model
    * 
    * @return root node is a table representation
    */
   protected cTargetTableNode getRootTableNode()
   {
      try
      {
         List l_Tables;

         if (m_iUseColsFromTable == this.USE_SOURCE_TABLE)
            l_Tables = Arrays.asList( m_sortModel.getDataSources() );
         else
            l_Tables = Arrays.asList( m_sortModel.getDataTargets() );
 
         for ( int iTables = 0; iTables < l_Tables.size(); iTables++ )
         {
            ITable oTable = (ITable) l_Tables.get( iTables );
            return (new cTargetTableNode( oTable ));
         }

      }
      catch (RemoteException ex)
      {
         Workspace.handleRemoteException( ex );
      }

      return null;
   }
   
   
   /**
    * Add columns to table node
    * @param targetTableNode Tree node representing a table
    * 
    */
   public void addColumnNodes(cTargetTableNode targetTableNode)
   {
      targetTableNode.addChildren();
      targetTableNode.setHasExpanded( true );

   }
   
   
   /**
    * cTableNode represents a DataTable and it's associated AbstractTable
    * It encapsulates information needed by both cSourceTableNode and
    * cTargetTableNode.
    */
   protected class cTableNode extends WsTreeNode
   {
      protected ITable m_table;
      
      /**
       * Constructs a node that represents a table.
       * The children of this node are the columns of the table.
       *
       * @param table the AbstractTable associated with the DataTable
       * @throws RemoteException 
       */
      public cTableNode(ITable table) throws RemoteException
      {
         
         super(table.getName());
         
         m_table = table;
         setAllowsChildren(true);
         setAsksAllowsChildren(true);
         setDragSource( false );
         setSorted(m_bSortColumns);
         setUserObject(table);
                  
 
      }
      
      /**
       * Does the default action for a node.  For this node, no action is performed.
       */
      public void doDefaultAction()
      {
         // Do nothing
      }
      
      /**
       * Retrieves the AbstractTable associated with the node
       * 
       * @return the AbstractTable
       */
      public ITable getTable()
      {
         return m_table;
      }      
   }
   

   

   
   /**
    * cTargetTableTreeNode is a node that represents a target table.  The target
    * table is either a WorkTable or PhysicalTable metadata object.
    * The children of this node are Column object nodes.
    */
   protected class cTargetTableNode extends cTableNode
   {
      /**
       * Constructs a tree node that represents a target table.
       *
       * @param targetTable the TargetTable associated with the DataTable
       * @throws RemoteException 
       */
      public cTargetTableNode( ITable targetTable ) throws RemoteException
      {
         super( targetTable);
      }
                  
      /**
       * Adds children to this node.  The children added to this node are
       * Column object nodes.
       */
      public void addChildren()
      {
        
            List l_Columns = Arrays.asList(getTable().getColumns());
            for ( int iColumn=0; iColumn<l_Columns.size(); iColumn++ )
            {
               IColumn oIColumn = (IColumn) l_Columns.get( iColumn );
               cColumnTreeNode columnNode = new cColumnTreeNode( oIColumn,  m_table);
               appendNode(columnNode, this );
            }
        
      }
   } // cTargetTableNode
   
   /**
    * Create a new column node and add it to the parent node.
    * 
    * @param column Tablecolumn to add
    * @param parent Parent node to hold the new node
    */
   public void createNewColumnNode(IColumn column, WsTreeNode parent)
   {
      if (parent.getTreeModel()==null)
         parent.setTreeModel(this);
      cColumnTreeNode colnode = new cColumnTreeNode(column, column.getTable());
      parent.addChild(colnode);
   }
   
   /**
    * cColumnTreeNode is a node that represents a metadata object of type Column.  This
    * type of node has no children
    */
   protected class cColumnTreeNode extends WsTreeNode implements DataSourceNodeInterface
   {
      private IColumn m_oIColumn;
      private ITable m_table;

      /**
       * Constructs a tree node that represents a Column metadata object
       *
       * @param oColumn Column
       * @param table   the AbstractTable associated with this column
       */
      public cColumnTreeNode( IColumn oColumn, ITable table)
      {
         //TODO: check this because the metadata column may not be created yet
         super( oColumn.getName() );
       
         m_oIColumn = oColumn;
         m_table = table;
         
         setDragSource( true );
         setAllowsChildren( false );
         
         setIcon( ColumnUIUtilities.getTypeIcon( oColumn ) );
         setUserObject(oColumn);
      }
      
      /**
       * Does the default action for a node.  For this node, no action is performed.
       */
      public void doDefaultAction()
      {
         // Do nothing
      }
      
      /**
       * Retrieves the AbstractTable associated with the node
       * 
       * @return the AbstractTable
       */
      public ITable getTable()
      {
         return m_table;
      }      
      
      /**
       * Gets the TableColumn associated with this node
       * 
       * @return the TableColumn
       */
      public IColumn getTableColumn()
      {
         return m_oIColumn;
      }
      
      /**
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getDataValuesInterface()
       */
      public DataValuesInterface getDataValuesInterface()           
      {
         return null;
      }
      /**
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getDistinctValuesInterface()
       */
      public DistinctValuesInterface getDistinctValuesInterface()   
      {
         return null;
      }
      /**
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getFormattedValuesInterface()
       */
      public FormattedValuesInterface getFormattedValuesInterface() 
      {
         return null;
      }
      
      /**
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getInsertName()
       */
      public String getInsertName()  
      {
         return m_oIColumn.getName() + " "; /*I18NOK:COS**/
      }  // null if this node cannot be inserted by reference

      /**
       * Get the description of the object
       * 
       * @return description
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getDescription()
       */
      public String getDescription()
      {
         return m_oIColumn.getDescription();
      }

      /**
       * Get the display Name
       * @return display name of object
       * @see com.sas.expressions.visuals.DataSourceItemInterface#getDisplayName()
       */
      public String getDisplayName()
      {
         return m_oIColumn.getName();
      }
     
   } // cColumnTreeNode
   
   /**
    * cExpansionListener listens for expansion/collapse events.  If the node is
    * to be expanded and has never been expanded, then its children need to be
    * added.
    */
   protected class cExpansionListener implements TreeExpansionListener
   {
      /**
       * Handles the tree expanded event by adding children to a node if its
       * children have never been expanded.
       *
       * @param e the tree expansion event
       *
       * @see javax.swing.event.TreeExpansionListener#treeExpanded(TreeExpansionEvent)
       */
      public void treeExpanded( TreeExpansionEvent e)
      {
         WsTreeNode node = (WsTreeNode) e.getPath().getLastPathComponent();
         if (!node.hasExpanded())
         {
            node.addChildren();
            node.setHasExpanded( true );
         }
      }
    
      /**
       * Handles the tree collapsed event.
       *
       * @param e the tree expansion event
       *
       * @see javax.swing.event.TreeExpansionListener#treeCollapsed(TreeExpansionEvent)
       */
      public void treeCollapsed( TreeExpansionEvent e)
      {
         // nothing to do
      }
   } // cExpansionListener
   
   /**
    * Adds an expansion listener to the tree.  The expansion listener makes the
    * tree model populate on demand when user requests tree expansion.
    *
    * @param tree the tree to which the expansion listener is added
    */
   public void addListenersToTree( JTree tree)
   {
      tree.addTreeExpansionListener( createTreeExpansionListener() );
   }
   
   protected TreeExpansionListener createTreeExpansionListener()
   {
      return new cExpansionListener();
   }
   
   /**
    * Appends a child node the end of the parent node's children.
    *  
    * @param nodeParent the parent node
    * @param nodeChild  the child node
    */
   private void appendNode( WsTreeNode nodeChild, WsTreeNode nodeParent )
   {
      if (nodeParent.isSorted())
      {
         addChild(nodeChild, nodeParent);
         return;
      }
      insertNodeInto( nodeChild, nodeParent, nodeParent.getChildCount() );
   }
   
   /**
    * Removes all children of the parent node.
    * 
    * @param nodeParent the parent node
    */
   private void removeAllChildrenFromTree( WsTreeNode nodeParent )
   {
      // build arrays of children removed
      int nChildren = nodeParent.getChildCount();
      if (nChildren <= 0)
         return;
      
      int[]      aChildIndexes = new int[      nChildren ];
      TreeNode[] aChildNodes   = new TreeNode[ nChildren ];
      for ( int iChild=0; iChild<nChildren; iChild++ )
      {
         aChildIndexes[ iChild ] = iChild;
         aChildNodes[   iChild ] = nodeParent.getChildAt( iChild );
      }

      // remove all the children and fire the event
      nodeParent.removeAllChildren();
      nodesWereRemoved( nodeParent, aChildIndexes, aChildNodes );
   }
   

   
   /**
    * Sets whether columns are sorted.
    *
    * @param bSort true = columns are sorted
    */
   public void setSortColumns( boolean bSort )
   {
      if (m_bSortColumns == bSort)
         return;

      m_bSortColumns = bSort;
      reorderTableColumns( (WsTreeNode) getRoot() );
   }
   
   /**
    * Reorder the child column nodes of a table nodes.  This method is recursive.
    * looking through all the child nodes of the given node for a table node. 
    * If a table node is found and the table node has been expanded, the child
    * column nodes are removed and readded which puts the child column nodes in 
    * the right order. 
    * 
    * @param node the parent node in which to look for child table nodes
    */
   private void reorderTableColumns( WsTreeNode node )
   {
      // Node passed in is a table node
      if (node instanceof cTableNode)
      {
         // need to set sort indicator whether node has been expanded
         // or not, so it will expand in proper order
         node.setSorted( m_bSortColumns );
         if (node.hasExpanded())
         {
            removeAllChildrenFromTree(node);
            node.setHasExpanded( false );
            node.addChildren();
            node.setHasExpanded( true );
         }
      }

      else
         reorderTableColumns( node );
      
   }
 

} // SortColsByTreeModel