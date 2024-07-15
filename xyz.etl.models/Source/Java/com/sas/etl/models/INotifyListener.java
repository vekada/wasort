/* $Id$ */
/**
 * Title:       INotifyListener.java
 * Description: A listener associated with an object that allows changes to 
 *              another object to update the first object.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;


/**
 * INotifyListener describes the methods needed by a notification listener.  A
 * notification listener will be associated with an object in the model.  The
 * object will want to hear when another object is changed in such a way that it
 * affects the object.  Currently, the changes are limited to objects being 
 * removed from their owning object which effectively deletes the object.
 * <p>
 * The notify listener is intended for use by model objects only.  Client usage
 * of the notify listener is strongly discouraged and not supported.
 */
public interface INotifyListener
{
   /**
    * Notifies the listener of the notification event.
    * 
    * @param ev the notification event
    */
   void notify( NotifyEvent ev ); 
   
}

