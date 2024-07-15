/* $Id$ */
/**
 * Title:       IComplexPersistableObject.java
 * Description: A complex persistable object that can contain properties, 
 *              extended attributes, notes, documents, etc.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.io.PrintStream;

import com.sas.etl.models.data.IFolder;
import com.sas.etl.models.other.IResponsiblePartyContainer;
import com.sas.metadata.remote.MdException;
import com.sas.util.UsageVersion;
import com.sas.workspace.models.SimpleObject;

/**
 * IComplexPersistableObject is a complex persistable object that can contain
 * properties, extended attributes, notes, documents, etc.
 */
public interface IComplexPersistableObject extends IPersistableObject, 
                                                   IExtendedAttributesContainer,
                                                   INotesAndDocumentsContainer,
                                                   IResponsiblePartyContainer,
                                                   SimpleObject
{

   /** event type for folder changed */
   public static final String FOLDER_CHANGED  = "IComplexPersistableObject:FolderChanged";
   public static final String PRIVATE_NOTES_ROLE = "Note";
   
   public static final String USAGE_VERSION_CHANGED = "IComplexPersistableObject:UsageVersionChanged";
  
   /**
    * Sets the folder on the complex persistable object.
    * 
    * @param folder the folder (null = remove the object from any folder)
    */
   void setFolder( IFolder folder );
   
   /**
    * Gets the folder in which the complex persistable object resides.
    * 
    * @return the folder (null = the object is not in a folder)
    */
   IFolder getFolder();
   
   /**
    * Get the sbip url for this object
    * @return sbip url
    */
   String getSBIPUrl() throws MdException;
   
   /**
    * Get the sbip url for the object, uses the containing folders url, then the name of the object
    * @param showType true to get the url with the type in parenthesis
    * @return sbip url /My Folder/.../New Job1
    */
   String getSBIPUrl(boolean showType) throws MdException;
   
   /**
    * Not implemented yet
    * 
    * @param stream
    */
   void dumpObjectToXML(PrintStream stream) ;
   
   /**
    * Gets the architecture version number (ex 1.01).  This is the version number of the
    * logical object in the OMR framework.  
    * 
    * @return the architecture version number
    * 
    * @see com.sas.etl.models.IComplexPersistableObject#getArchitectureVersionNumber()
    */
    UsageVersion getArchitectureVersionNumber();

   /**
    * Is the object editable?  In other words, does the user have permission to
    * edit the object?
    * 
    * @return true = editable
    */
   boolean isEditable();
   
   /**
    * Return the current version of this object.
    * 
    * @return current version of object
    */
   UsageVersion getUsageVersion();
 }

