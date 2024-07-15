/* $Id$ */
/**
 * Title:       IOMRPersistable.java
 * Description: The interface that defines the methods an object must implement
 *              to be persistable to OMR.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.rmi.RemoteException;
import java.util.Map;

import com.sas.etl.models.impl.OMRAdapter;
import com.sas.metadata.remote.MdException;

/**
 * IOMRPersistable is the interface that defines the methods an object must
 * implement to be persistable to OMR.
 */
public interface IOMRPersistable
{
   /**
    * Gets the id of the OMR persistable object.
    * 
    * @return the object id
    */
   String getID();
   
   /**
    * Saves the object to OMR.  It is the responsibility of the object to call
    * save on all contained objects.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    */
   void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException;
   
   /**
    * Loads the object from OMR.  It is the responsibility of the object to call
    * load on all contained objects.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    */
   void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException;
   
   /**
    * Deletes the object from OMR.  It is the responsibility of the object to call
    * delete on all contained objects.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    */
   void deleteFromOMR( OMRAdapter omr ) throws MdException, RemoteException;

   /**
    * Gets the type of OMR object used to represent this object in OMR.  This
    * value should be one constants from MetadataObjects.
    * 
    * @return the type of OMR object
    */
   String getOMRType();
   
   /**
    * Updates the new ids contained in the object.  This method should be called 
    * after an object has been saved to OMR to inform the object of the real ids
    * of newly created objects.  It is the responsibility of the object to call
    * updateIDs on all contained objects.
    * 
    * @param mapIDs the map of "new object ids" to "real object ids"  
    */
   void updateIDs( Map mapIDs );

   /**
    * Gets the template map used to populate an OMR adapter for this object.
    * 
    * @return the load template map
    */
   Map getOMRLoadTemplateMap();
   
   /**
    * Gets the template map used to copy an object.
    * 
    * @return the copy template map
    */
   Map getOMRCopyTemplateMap();
   
   /**
    * Gets the template map used to export an object.
    * 
    * @return the export template map
    */
   Map getOMRExportTemplateMap();
   
   /**
    * Gets the template map used to checkout this object.
    * 
    * @return the checkout template map
    */
   Map getOMRCheckOutTemplateMap();
   
}

