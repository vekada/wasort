/* $Id$ */
/**
 * Title:       IExtendedAttributesContainer.java
 * Description: A container of extended attributes.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.util.List;

import com.sas.etl.models.other.IExtendedAttribute;

/**
 * IExtendedAttributesContainer is a container of extended attributes.
 */
public interface IExtendedAttributesContainer extends IObject
{
   // event types
   /** event type for an extended attribute was added */
   static final String EXTENDED_ATTRIBUTE_ADDED   = "Object:ExtendedAttributeAdded";
   /** event type for an extended attribute was removed */
   static final String EXTENDED_ATTRIBUTE_REMOVED = "Object:ExtendedAttributeRemoved";
   
   /**
    * Gets the extended attributes in the extended attributes container.
    * 
    * @return the extended attributes
    */
   IExtendedAttribute[] getExtendedAttributes();
   
   /**
    * Gets the list of extended attributes in the extended attributes container.  The list may be directly
    * modified to modify the extended attributes in the container.  The methods in the list
    * that are not implemented will throw an exception.
    * 
    * @return the list of extended attributes
    */
   List getExtendedAttributesList();
}

