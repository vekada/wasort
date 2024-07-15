/* $Id: WsDefaultTreeModel.java,v 1.1.2.4 2007/06/21 14:47:03 sassem Exp $ */
/**
 * Title:       WsDefaultTreeModel.java
 * Description: A default implementation of a WsTreeModel
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.workspace.tree;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MdObjectStore;
import com.sas.metadata.remote.Root;
import com.sas.workspace.Workspace;
import com.sas.workspace.WsDataTreeNode;
import com.sas.workspace.WsTreeNode;

/**
 * WsDefaultTreeModel is a default implementation of a WsTreeModel.
 */
public class WsDefaultTreeModel extends DefaultTreeModel implements WsTreeModel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
// attributes
   private WsNodeFilter    m_filter;
   private WsNodeFactory   m_factory;
   private MdObjectStore   m_store;
   private ArrayList       m_lRepositories;
   private Comparator      m_cmp;
   private String          m_sName = "DefaultTreeModel";  /*I18nOK:EMS*/

   /**
    * Constructs a default tree model.
    * 
    * @param root the root node of the model
    */
   public WsDefaultTreeModel( WsTreeNode root )
   {
      super( root, true );
      m_cmp = createDefaultComparator();
   }

   /**
    * Constructs the default comparator used by the model to sort children nodes.
    * 
    * @return the default comparator
    */
   protected Comparator createDefaultComparator()
   {
      return new DefaultComparator();
   }

   /**
    * Creates a default repository list.  Gets the list of selected repositories
    * from Workspace and removes the project repository (if there is one)
    * 
    * @deprecated at some point this is going to be the objects selector panel for 9.2
    */
   protected List createDefaultRepositoryList()
   {
      List   lKeep         = new ArrayList();
      try
      {
         List vecRepositories = Workspace.getMdFactory().getOMIUtil().getRepositories();
         ArrayList lRepositories = new ArrayList(vecRepositories);
  
         Root mdoProject    = (Root) Workspace.getWorkspace().getProjectRepository();
         if (mdoProject == null)
            return lRepositories;
      
         String sProjectFQID  = mdoProject != null ? mdoProject.getFQID() : null;
         for ( int iRepository=0; iRepository<lRepositories.size(); iRepository++ )
         {
            Root mdoRepository = (Root) lRepositories.get( iRepository );
            if (!mdoRepository.getFQID().equals( sProjectFQID ))
               lKeep.add( mdoRepository );
         }
      }
      catch ( MdException ex )
      {
         
      }
      catch ( RemoteException ex2 )
      {
         Workspace.handleRemoteException(ex2);
      }       
      
      return lKeep;
      
   }
   
   //---------------------------------------------------------------------------
   // Attributes
   //---------------------------------------------------------------------------

   /**
    * Sets the name of the tree model.  This method is used strictly for debug
    * purposes.
    *
    * @param sName the name of the model
    * @todo remove this method before we ship.
    */
   public void setName( String sName )
   {
      m_sName = sName;
   }

   /**
    * Gets the name of the tree model.
    * 
    * @return the name of the tree model
    */
   public String getName()
   {
      return m_sName;
   }

   /**
    * Sets the filter used to determine if a root folder, folder, or an object
    * should be included in the tree.  The filter should be set immediately 
    * after construction.  Setting a new filter has no affect on the current 
    * nodes in the model.  However, calling refresh on the model after the 
    *
    * @param filter the filter
    */
   public void setNodeFilter( WsNodeFilter filter )
   {
      m_filter = filter;
   }

   /**
    * Gets the filter used to determine if a metadata object should be included
    * in the tree.
    *
    * @return the node filter.
    */
   public WsNodeFilter getNodeFilter()
   {
      return m_filter;
   }

   /**
    * Sets the factory used to create nodes.  The factory is used to create all
    * nodes but the root node. The factory must support all types of objects 
    * that pass the node filter.
    *
    * @param factory the node factory.
    */
   public void setNodeFactory( WsNodeFactory factory )
   {
      m_factory = factory;
   }

   /**
    * Gets the factory used to create tree nodes.
    *
    * @return the node factory
    */
   public WsNodeFactory getNodeFactory()
   {
      return m_factory;
   }

   /**
    * Sets the store used by the custom tree model.
    *
    * @param store the store
    */
   public void setStore( MdObjectStore store )
   {
      m_store = store;
   }

   /**
    * Gets the store used by the custom tree model.
    *
    * @return the store
    */
   public MdObjectStore getStore()
   {
      return m_store;
   }

   /**
    * Sets the list of repositories to be shown under the default root node.
    * The list of repositories is a list of metadata objects with a type of
    * MdFactory.ENVIRONMENT..
    *
    * @param lRepositories the list of repositories.
    */
   public void setRepositories( List lRepositories )
   {
      m_lRepositories = new ArrayList( lRepositories );
   }

   /**
    * Gets the list of repositories to be shown under the default root node.
    *
    * @return the list of repositories.
    */
   public List getRepositories()
   {
      return (List) m_lRepositories.clone();
   }

   /**
    * Sets the comparator used for sorting children.
    *
    * @param cmp the compartor
    */
   public void setComparator( Comparator cmp )
   {
      m_cmp = cmp;
   }

   /**
    * Gets the comparator used for sorting children.
    *
    * @return the compartor
    */
   public Comparator getComparator()
   {
      return m_cmp;
   }
   
   //---------------------------------------------------------------------------
   // Overrides
   //---------------------------------------------------------------------------
   /**
    * Called when the value for a path is changed by the tree.  In other words,
    * the tree edited a value and here is the new value for the end of the path.
    * Overridden to use the rename method on a node.
    * 
    * @param path  the path of what was changed
    * @param value the value (better be a string!)
    * 
    * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
    */
   public void valueForPathChanged( TreePath path, Object value )
   {
      // the super sets the user object. no known code uses the user object, so don't bother
//    super.valueForPathChanged( path, value );
      WsTreeNode node = (WsTreeNode) path.getLastPathComponent();
      node.rename( (String) value );
   }
   
   /**
    * Sets the root node used by the model.  Overridden to set the model on the
    * root node as well.
    *
    * @param node the root node
    */
   public void setRoot( TreeNode node )
   {
      super.setRoot( node );
      if (node instanceof WsTreeNode)
         ((WsTreeNode) node).setTreeModel( this );
   }

   //---------------------------------------------------------------------------
   // Node methods
   //---------------------------------------------------------------------------
   /**
    * Adds a new child node to the parent node.  The node is inserted in the
    * child list in alphabetical order using the node's name.
    *
    * @param nodeNew    the new child node
    * @param nodeParent the parent node
    */
   public void addChild( final WsTreeNode nodeNew, final WsTreeNode nodeParent )
   {
      // if the add child is on the right thread, add it  
      if (SwingUtilities.isEventDispatchThread())
      {
         int nChildren = nodeParent.getChildCount();
         int iChild;
   
         // find where to insert the child
         if (nodeParent.isSorted())
         {
            for ( iChild=0; iChild<nChildren; iChild++ )
            {
               WsTreeNode nodeChild = (WsTreeNode) nodeParent.getChildAt( iChild );
               if (m_cmp.compare( nodeChild, nodeNew ) > 0)
                  break;
            }
         }
         else
            iChild = nChildren;
   
         // insert it
         nodeNew.setTreeModel( this );
         insertNodeInto( nodeNew, nodeParent, iChild );
      }
      
      // otherwise, move it to the right thread
      else
      {
         try
         {
            SwingUtilities.invokeAndWait( new Runnable()
                                          {
                                             public void run()
                                             {
                                                addChild( nodeNew, nodeParent );
                                             }
                                          } );
         }
         catch (InterruptedException ex)
         {
            Workspace.getDefaultLogger().error( "", ex );
         }
         catch (InvocationTargetException ex)
         {
            Workspace.getDefaultLogger().error( "", ex );
         }
      }
   } // addChild

   /**
    * Removes a child from its parent node.
    *
    * @param nodeChild the child node to remove.
    */
   public void removeChild( final WsTreeNode nodeChild )
   {
      // if the remove child is on the right thread, remove it
      if (SwingUtilities.isEventDispatchThread())
      {
         removeNodeFromParent( nodeChild );
         nodeChild.setTreeModel( null );
      }
      
      // otherwise remove it on the right thread
      else
      {
         try
         {
            SwingUtilities.invokeAndWait( new Runnable()
                                          {
                                             public void run()
                                             {
                                                removeChild( nodeChild );
                                             }
                                          } );
         }
         catch (InterruptedException ex)
         {
            Workspace.getDefaultLogger().error( "", ex );
         }
         catch (InvocationTargetException ex)
         {
            Workspace.getDefaultLogger().error( "", ex );
         }
      }
   }

   /**
    * Removes all children from a node.
    *
    * @param nodeParent the parent of the children to be removed.
    */
   public void removeAllChildren( WsTreeNode nodeParent )
   {
      int nChildren = getChildCount( nodeParent );
      int iChildren;

      for ( iChildren=nChildren-1; iChildren>=0; iChildren-- )
         removeChild( (WsTreeNode) nodeParent.getChildAt( iChildren ) );
   }

   /**
    * Resorts the specified child node.
    *
    * @param nodeChild the child node to be sorted
    */
   public void resortChild( WsTreeNode nodeChild )
   {
      WsTreeNode nodeParent = (WsTreeNode) nodeChild.getParent();
      if (nodeParent.isSorted())
      {
         // TODO use a smarter algorithm
         removeChild( nodeChild );
         addChild(    nodeChild, nodeParent );
      }
      else
         nodeChanged( nodeChild );
   }
   
   /**
    * Node changed notification method.  This method is overridden to make sure
    * the node changed event is fired on the event thread.
    * 
    * @param node the node that changed
    * 
    * @see javax.swing.tree.DefaultTreeModel#nodeChanged(javax.swing.tree.TreeNode)
    */
   public void nodeChanged( final TreeNode node )
   {
      if (SwingUtilities.isEventDispatchThread())
         super.nodeChanged( node );
      else
      {
//         try
//         {
            SwingUtilities.invokeLater( new Runnable()
                                         {
                                             public void run()
                                             {
                                                nodeChanged( node );
                                             }
                                          } );
//         }
//         catch (InterruptedException ex)
//         {
//            Workspace.getDefaultLogger().error( "", e );
//         }
//         catch (InvocationTargetException ex)
//         {
//            Workspace.getDefaultLogger().error( "", e );
//         }
      }
   }

   /**
    * Gets a breadth first enumeration of all the nodes in the model.
    * 
    * @return the breadth first enumeration of all the nodes in the model
    */
   public Enumeration getBreadthFirstEnumeration()
   {
      return ((DefaultMutableTreeNode) root).breadthFirstEnumeration();
   }
   
   /**
    * Gets a depth first enumeration of all the nodes in the model.
    * 
    * @return the depth first enumeration of all the nodes in the model
    */
   public Enumeration getDepthFirstEnumeration()
   {
      return ((DefaultMutableTreeNode) root).depthFirstEnumeration();
   }
   
   //---------------------------------------------------------------------------
   // Operations
   //---------------------------------------------------------------------------
   /**
    * Finds a node representing the specified metadata object.  If the metadata 
    * object can appear in the tree in more than one place, this method will 
    * return the first instance found (where first is model specific).  Also,
    * the tree model will be expanded appropriately to find the node.
    * <p>
    * This implementation does not do anything.  It is simply provided so that 
    * this class would not have to be an abstract class.
    * 
    * @param mdo the metadata object
    * 
    * @return the tree node (null = the metadata object is not in the tree)
    *  
    * @throws MdException
    * @throws RemoteException
    */
   public WsDataTreeNode find( Root mdo ) throws MdException, RemoteException
   {
      return null;
   }

   /**
    * Finds the node specified by the FQID in the children of the given node.  
    * If the node has not been expanded it is expanded.
    * 
    * @param node  the node
    * @param sFQID the FQID of the child node to be found
    * 
    * @return the child node (null = the node is not found among the children
    */
   protected WsDataTreeNode findInChildren( WsTreeNode node, String sFQID )
   {
      // expand node if necessary
      node.expand();

      // look through the children until the object is found
      for ( int iChild=0; iChild<node.getChildCount(); iChild++ )
      {
         WsTreeNode nodeChild = (WsTreeNode) node.getChildAt( iChild );
         if ((nodeChild instanceof WsDataTreeNode) && sFQID.equals( nodeChild.getFQID() ))
            return (WsDataTreeNode) nodeChild;
      }
      
      return null;
   }
   
   //---------------------------------------------------------------------------
   // Metadata object manipulation
   // These methods are necessary because some tree models can have stores while
   // others can not have stores.
   //---------------------------------------------------------------------------
   
   /**
    * Acquires a permanent version of the specified metadata object.  Permanent
    * refers to the life time of the tree model.  If the tree model does not 
    * have a store, a simple object will be returned.  Otherwise, a complex 
    * object will be created in the store and returned.
    * 
    * @param sName         the name of the metadata object
    * @param sType         the type of the metadata object
    * @param sFQID         the FQID of the metadata object
    * @param sChangeState  the change state attribute of the metadata object
    * @param sLockedBy     the locked by    attribute of the metadata object
    * 
    * @return the metadata object
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public Root acquirePermanentObject( String sName, String sType, String sFQID, String sChangeState, String sLockedBy ) throws MdException, RemoteException
   {
      
      Root mdoNew = (m_store != null) ? (Root) Workspace.getMdFactory().createComplexMetadataObject( m_store, sName, sType, sFQID ) :
                                        (Root) Workspace.getMdFactory().createSimpleMetadataObject(           sName, sType, sFQID );
      mdoNew.setChangeState( sChangeState );
      mdoNew.setLockedBy(    sLockedBy    );

      // now get the change state and locked by attributes and fill them in
//      List lAttrs = new ArrayList( 2 );
//      lAttrs.add( "ChangeState" );
//      lAttrs.add( "LockedBy"    );
//      
//      List lValues = Workspace.getMdFactory().getOMIUtil().getMetadataSimple( sType, sFQID, lAttrs );
//      mdoNew.setChangeState( (String) lValues.get( 0 ) );
//      mdoNew.setLockedBy(    (String) lValues.get( 1 ) );
      return mdoNew;
   }
   
   /**
    * Acquires a permanent version of the specified metadata object.  Permanent
    * refers to the life time of the tree model.  If the tree model does not 
    * have a store, a simple object will be returned.  Otherwise, it is assumed
    * that the object is in the store and the object is returned.  The object 
    * passed in is assumed to have the ChangeState and LockedBy attribute values
    * initialized correctly.
    * 
    * @param mdo the specified metadata object
    * 
    * @return the metadata object
    */
   public Root acquirePermanentObject( Root mdo )
   {
      if (m_store != null)
         return mdo;
      
      try
      {
         Root mdoNew = (Root) Workspace.getMdFactory().createSimpleMetadataObject( mdo );
         mdoNew.setChangeState( mdo.getChangeState() );
         mdoNew.setLockedBy(    mdo.getLockedBy()    );
         return mdoNew;
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
         return null;
      }
   }
   
   /**
    * Acquires a complex version of the specified metadata object.  If the tree 
    * model has a store, the object is returned because it is already a complex 
    * object.  Otherwise, a store is created and a complex object is created in 
    * the store and returned.  Therefore the complex object must be disposed so 
    * that the store is disposed.  A store will be created for each complex 
    * object created.  The complext
    *  
    * @param mdo the specified metadata object
    * 
    * @return the complex object
    */
   public Root acquireComplexObject( Root mdo )
   {
      if (m_store != null)
         return mdo;
         
      try
      {
         MdObjectStore store = Workspace.getMdFactory().createObjectStore( null, "WsDefaultTreeModel:acquireComplexObject" );
         return (Root) Workspace.getMdFactory().createComplexMetadataObject( store, mdo );
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
         return null;
      }
   }
   
   /**
    * Disposes of a complex metadata object that was acquired using 
    * acquireComplexObject.  If a store was created to hold the complex object,
    * the store is disposed.
    * 
    * @param mdo the complex metadtaa object to be disposed.
    */
   public void disposeComplexObject( Root mdo )
   {
      try
      {
      if (m_store == null)
         mdo.getObjectStore().dispose();
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
      }
   }
   
   /**
    * Acquires a store.  If the model already has a store, that store is used.
    * Otherwise, a temporary store is created.  Therefore, any store acquired 
    * must be disposed.
    * 
    * @return the store
    */
   public MdObjectStore acquireStore()
   {
      if (m_store != null)
         return m_store;
      
      try
      {
         return Workspace.getMdFactory().createObjectStore( null, "WsDefaultTreeModel:acquireStore" );
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
         return null;
      }
   }
   
   /**
    * Disposes a store that is acquired via acquireStore.  If the model does not
    * have a store, the store is disposed. 
    * 
    * @param store the store
    */
   public void disposeStore( MdObjectStore store )
   {
      if (m_store != null)
         return;
      
      try
      {
         store.dispose();
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
      }
   }

   /**
    * DefaultComparator is a comparator that compares two WsTreeNodes.  The 
    * comparison is based on the name of the nodes.  The comparison is case 
    * insensitive.  
    */
   private static class DefaultComparator implements Comparator
   {
      /**
       * Compares the two objects.
       *
       * @param  o1 the first  object
       * @param  o2 the second object
       *
       * @return the result of comparing the objects.
       */
      public int compare( Object o1, Object o2 )
      {
         return ((WsTreeNode) o1).getName().compareToIgnoreCase( ((WsTreeNode) o2).getName() );
      }
   } // DefaultComparator
}

