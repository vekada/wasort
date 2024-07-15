/* $Id$ */
/**
 * Title:       IObject.java
 * Description: The interface that describes an object.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.io.PrintStream;
import java.util.List;

/**
 * IObject is the interface that describes an object.
 */
public interface IObject
{
   /** event type for name changed */
   static final String NAME_CHANGED          = "Object:NameChanged";          // I18NOK:EMS
   /** event type for description changed */
   static final String DESCRIPTION_CHANGED   = "Object:DescriptionChanged";   // I18NOK:EMS
   /** event type for user property set */
   static final String USER_PROPERTY_SET     = "Object:UserPropertySet";      // I18NOK:EMS
   /** event type for user property removed */
   static final String USER_PROPERTY_REMOVED = "Object:UserPropertyRemoved";  // I18NOK:EMS
   
   /**
    * Gets the id of the object.
    * 
    * @return the id of the object
    */
   String getID();
   
   /**
    * Is the object a new object?
    * 
    * @return true = yes
    */
   boolean isNew();
   
   /**
    * Gets the model to which this object belongs.
    * 
    * @return the model
    */
   IModel getModel();
   
   /**
    * Sets the object's name.
    * 
    * @param sName the name
    */
   void setName( String sName );
   
   /**
    * Gets the object's name
    * 
    * @return the object's name
    */
   String getName();
   
   /**
    * Sets the object's description.
    * 
    * @param sDescription the object's description
    */
   void setDescription( String sDescription );
   
   /**
    * Gets the object's description.
    * 
    * @return the object's description
    */
   String getDescription();
   
   /**
    * Is the object changed?  If any contained objects are changed, this method
    * should return true.
    * 
    * @return true = the object has changed
    */
   boolean isChanged();
   
   /**
    * Sets whether the object has changed.  This method should be called with 
    * caution.
    * 
    * @param bChanged true = object has changed
    */
   void setChanged( boolean bChanged );
   
   /**
    * Is the object complete?  For example, a foreign key is complete only if
    * it has a partner key and its columns match the key's columns in type.
    * 
    * @return true = the object is complete
    */
   boolean isComplete();
   
   /**
    * Does the object have any warning?  A warning does not prevent the object
    * from being complete, but it may indicate a problem that should be fixed.
    * 
    * @return true = the object has warnings
    */
   boolean hasWarnings();
   
   /**
    * Sets a user property.
    * 
    * @param sName  the user property's name
    * @param sValue the user property's value
    */
   void setUserProperty( String sName, String sValue );
   
   /**
    * Gets a user property.
    * 
    * @param sName the user property's name
    * 
    * @return the user property's value
    */
   String getUserProperty( String sName );
   
   /**
    * Removes a user property.
    * 
    * @param sName the user property's name
    */
   void removeUserProperty( String sName );
   
   /**
    * Adds a notify listener to the object.  The notify listener will be called
    * when certain changes are made to the object.  Currently, the only known 
    * change is the object being deleted.
    * 
    * @param lsnr the notification listener
    */
   void addNotifyListener( INotifyListener lsnr );

   /**
    * Removes a notify listener from the object.
    * 
    * @param lsnr the notify listener.
    */
   void removeNotifyListener( INotifyListener lsnr );
   
   /**
    * Dispose of the object.
    */
   void dispose();
   
   /**
    * Dumps the object to a print stream for debugging purposes.
    * 
    * @param strm the print stream
    */
   void dump( PrintStream strm );
   
   /**
    * Gets the reasons the code generator is incomplete.
    * 
    * @return a list of Strings that are the reasons.
    * 
    */
   List getReasonsIncomplete();
   
   /**
    * Gets any warnings this object may have.
    * 
    * @return a list of Strings that are the warnings.
    */
   List getWarnings();
}

