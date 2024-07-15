/* $Id: SimpleObject.java,v 1.1.2.3 2007/06/05 20:07:49 jiholm Exp $ */
/**
 * Title:       SimpleObject.java
 * Description: A simple object interface.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Jim Holmes
 */

package com.sas.workspace.models;

/**
 * SimpleObject is a simple object interface.
 */
public interface SimpleObject
{

   /**
    * Get the name of this object.
    * 
    * @return the name
    */
   public String getName();
   
   /**
    * Get the metadata FQID of this object.
    * 
    * @return the id
    */
   public String getID();
   
   /**
    * Get the metadata type of this object.
    * 
    * @return the metadata type
    */
   public String getMetadataType();
   
   /**
    * Get the public type of this object.
    * 
    * @return the public type
    */
   public String getPublicType();
   
   /**
    * Is the object a public object?
    * 
    * @return true = the object is a public object
    */
   public boolean isPublicObject();
   
   /**
    * Is the object a new object (i.e. it hasn't been written to the server yet)?
    * 
    * @return true = the object is a new object
    */
   public boolean isNew();
}
