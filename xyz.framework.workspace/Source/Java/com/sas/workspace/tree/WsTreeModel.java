/* $Id: WsTreeModel.java,v 1.1.2.2 2007/01/15 22:32:51 sasblc Exp $ */
/**
 * Title:       WsTreeModel.java
 * Description: The interface that describes a tree model
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.workspace.tree;

import java.rmi.RemoteException;
import java.util.List;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MdObjectStore;
import com.sas.metadata.remote.Root;
import com.sas.workspace.WsDataTreeNode;
import com.sas.workspace.WsTreeNode;

/**
 * WsTreeModel is the interface that describes a tree model.
 */
public interface WsTreeModel extends TreeModel
{
   //---------------------------------------------------------------------------
   // Node methods
   //---------------------------------------------------------------------------
   /**
    * Adds a child to the parent node.  This method is responsible for setting 
    * the model on the new child node and for adding the child in the 
    * appropriate place based on whether the parent is sorted or not.
    * 
    * @param nodeChild  the child node
    * @param nodeParent the parent node
    */
   void addChild( WsTreeNode nodeChild, WsTreeNode nodeParent );
   
   /**
    * Resorts a child in the parent node if the parent node has sorted children.
    * 
    * @param nodeChild the child node
    */
   void resortChild( WsTreeNode nodeChild );
   
   /**
    * Removes a child from the parent node.  This method is responsible for
    * setting the model on the child node to null.
    * 
    * @param nodeChild the child node
    */
   void removeChild( WsTreeNode nodeChild );
   
   /**
    * Removes all children of the specified node.
    * 
    * @param node the specified node
    */
   void removeAllChildren( WsTreeNode node );
   
   /**
    * Notifies the model that something about the appearance of the node has 
    * changed.  This method should not be called to indicate changes to the 
    * children of the node.  Changes to the child of the node should be 
    * done via addChild, resortChild, removeChild, or removeAllChildren.  
    * 
    * @param node the node that changed
    */
   void nodeChanged( TreeNode node );
   
   
   //---------------------------------------------------------------------------
   // Getter methods
   //---------------------------------------------------------------------------
   /**
    * Gets the factory used to create nodes for the model.
    * 
    * @return the node factory
    */
   WsNodeFactory getNodeFactory();
   
   /**
    * Gets the filter used to filter out nodes from being added to the model.
    * Before adding a node, the filter should be checked to see if the node 
    * should be added.
    * 
    * @return the node filter
    */
   WsNodeFilter getNodeFilter();
   
   /**
    * Gets the repositories shown in the model.  The list is a list of simple 
    * metadata objects with a type of ENVIRONMENT.
    * 
    * @return the list of repositories shown in the model.
    */
   List getRepositories();

   //---------------------------------------------------------------------------
   // Operations
   //---------------------------------------------------------------------------
   /**
    * Finds a node representing the specified metadata object.  If the metadata 
    * object can appear in the tree in more than one place, this method will 
    * return the first instance found (where first is model specific).  Also,
    * the tree model will be expanded appropriately to find the node.
    * 
    * @param mdo the metadata object
    * 
    * @return the tree node (null = the metadata object is not in the tree)
    *  
    * @throws MdException
    * @throws RemoteException
    */
   WsDataTreeNode find( Root mdo ) throws MdException, RemoteException;

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
   Root acquirePermanentObject( String sName, String sType, String sFQID, String sChangeState, String sLockedBy ) throws MdException, RemoteException;

   
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
   Root acquirePermanentObject( Root mdo );

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
   Root acquireComplexObject( Root mdo );
   
   /**
    * Disposes of a complex metadata object that was acquired using 
    * acquireComplexObject.  If a store was created to hold the complex object,
    * the store is disposed.
    * 
    * @param mdo the complex metadtaa object to be disposed.
    */
   void disposeComplexObject( Root mdo );

   /**
    * Acquires a store.  If the model already has a store, that store is used.
    * Otherwise, a temporary store is created.  Therefore, any store acquired 
    * must be disposed.
    * 
    * @return the store
    */
   MdObjectStore acquireStore();
   
   /**
    * Disposes a store that is acquired via acquireStore.  If the model does not
    * have a store, the store is disposed. 
    * 
    * @param store the store
    */
   void disposeStore( MdObjectStore store );
}

