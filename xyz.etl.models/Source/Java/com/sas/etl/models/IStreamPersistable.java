/* $Id$ */
/**
 * Title:       IStreamPersistable.java
 * Description: The interface that defines the methods an object must implement
 *              to be persistable as XML to a stream.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * IStreamPersistable is the interface that defines the methods an object must 
 * implement to be persistable as XML to a stream.
 */
public interface IStreamPersistable
{
   /**
    * Saves the object as XML to the specified stream.
    * 
    * @param strm the output stream
    * 
    * @throws IOException
    */
   void saveXMLToStream( OutputStream strm ) throws IOException;

   /**
    * Loads the object from XML from the specified stream.
    * 
    * @param strm the input stream
    * 
    * @throws IOException
    */
   void loadXMLFromStream( InputStream  strm ) throws IOException;
}

