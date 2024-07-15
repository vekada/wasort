/* $Id$ */
/**
 * Title:       IDocumentsContainer.java
 * Description: A container of documents.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.util.List;

import com.sas.etl.models.data.IDocument;

/**
 * IDocumentsContainer is a container of documents.
 */
public interface IDocumentsContainer extends IObject
{
   // event types
   /** event type for a document was added */
   static final String DOCUMENT_ADDED   = "Object:DocumentAdded";
   /** event type for a document was removed */
   static final String DOCUMENT_REMOVED = "Object:DocumentRemoved";
   
   /**
    * Gets the documents in the documents container.
    * 
    * @return the documents
    */
   IDocument[] getDocuments();
   
   /**
    * Gets the list of documents in the documents container.  The list may be 
    * directly modified to modify the documents in the container.  The methods 
    * in the list that are not implemented will throw an exception.
    * 
    * @return the list of documents
    */
   List getDocumentsList();
}

