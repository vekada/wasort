/* $Id$ */
/**
 * Title:       IPersistableObject.java
 * Description: The interface for an object that is persistable.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.util.List;


/**
 * IPersistableObject is the interface for an object that is persistable.
 */
public interface IPersistableObject extends IObject, IOMRPersistable, IStreamPersistable
{
   /**
    * Notifies the object that it is to be deleted when the model is saved.  The
    * object should then notify all listeners that it will be deleted.  The 
    * object should also notify all dependent objects that they will be 
    * deleted as well.
    * 
    * TODO better comment
    */
   void delete();
   
   /**
    * Gets the objects that might need to be refreshed.  A refresh is required 
    * when an external force (code submitted by the application) causes an 
    * update of metadata.  Because of this update, certain objects in the model 
    * may need to be refreshed.  If an object contains or references objects 
    * that may need to be refreshed, the objects should be added to the list.  
    * The object should not add itself to the list.  It is the responsibility of 
    * the caller of the method to determine if the object should be added to the 
    * list of objects.
    * <p>
    * Currently, only transformations that generate an update table metadata 
    * will return a list of tables and physical tables will return a list of 
    * their columns, keys, foreign keys, and indexes. 
    * 
    * @return the list of refresh objects (null = no objects, also an empty list 
    *         may be returned).
    */
   List getRefreshObjects();
}

