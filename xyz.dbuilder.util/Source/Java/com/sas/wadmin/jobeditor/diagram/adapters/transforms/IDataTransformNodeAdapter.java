/* $Id$ */
/**
 * Title:       IDataTransformNodeAdapter.java
 * Description: The interface that describes an adapter for a data transform.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters.transforms;

import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;

import com.sas.etl.models.IObject;
import com.sas.wadmin.jobeditor.diagram.adapters.IPortDescription;

/**
 * IDataTransformNodeAdapter is the interface that describes an adapter for a 
 * data transform.
 */
public interface IDataTransformNodeAdapter extends ITransformNodeAdapter
{
   /**
    * delete input 
    * @param obj the object needed to determine the port to delete
    * @return the index of the port to delete
    */
   int deleteInputPort(Object obj);
   
   /**
    * delete output 
    * @param obj the object needed to determine the port to delete
    * @return the index of the port to delete
    */
   int deleteOutputPort(Object obj);
   
   /**
    * Delete output port.
    * @param obj the object used to create the port descriptions
    * @param outputPortIndex the port index of where to delete
    * @return the port index to delete -1 means nothing to remove
    */
   int deleteOutputPort(Object obj, int outputPortIndex);
   
   /**
    * Delete input port. 
    * @param obj the object used to create the port description
    * @param inputPortIndex where to insert the port
    * @return the port index to delete -1 means nothing to remove
    */
   int deleteInputPort(Object obj, int inputPortIndex);
   
   /**
    * add port deescription
    * @param obj the object needed to determine the port to add
    * @return port description added
    */
   IPortDescription addInputPortDescription(Object obj);
   
   /**
    * Add port port description
    * @param obj the object used to create the port description
    * @param inputPortIndex the port index of where to insert the port
    * @return port description
    */
   IPortDescription addInputPortDescription(Object obj, int inputPortIndex);

   /**
    * Add port port description
    * @param obj the object used to create the port description
    * @param inputPortIndex the port index of where to insert the port
    * @return port description
    */
   IPortDescription addInputPortDescription(Object obj, MutableInt inputPortIndex);
   
   /**
    * Add output port description
    * @param obj the object used to create the port description
    * @param outputPortIndex the port index of where to delete the port
    * @return port description
    */
   IPortDescription addOutputPortDescription(Object obj, int outputPortIndex);
   
   /**
    * Add output port description
    * @param obj the object used to create the port description
    * @param outputPortIndex the port index of where to delete the port
    * @return port description
    */
   IPortDescription addOutputPortDescription(Object obj, MutableInt outputPortIndex);
   
   /**
    * add output Description
    * @param obj the object needed to determine the port to add
    * @return port description added
    */
   IPortDescription addOutputPortDescription(Object obj);
   /**
    * Update input port. 
    * @param obj the object used to update the port
    * @return the port index should be 0 position. text should be at 1 position
    */
   List updateInputPort(Object obj);
    
   /**
    * Update output port.
    * @param obj the object used to update the port 
    * @return the port index should be 0 position. text should be at 1 position
    */
   List updateOutputPort(Object obj);
   
   /**
    * Is the port mapping automatically?  Only checked if Job level mapping
    * is true
    * @param iPortIndex the index of the port to map with
    * @return true = the port maps automatically
    */
   boolean isMappingAutomatically(int iPortIndex);
   
   /**
    * Is the port propagating automatically? Only checked if Job level propagation 
    * is true
    * @param iPortIndex the index of the port to propagate with
    * @return true = the port propagates automatically
    */
   boolean isPropagatingAutomatically(int iPortIndex);
   
   /**
    * Gets the indexes of the input ports to which the object should be connected.
    * If the object is not connected to an input port, if null is returned
    * This method should only be called for objects that are inputs
    * to the object represented by the adapter.
    * 
    * @param obj the object
    * 
    * @return the array of indexes of the object
    */
   int[] getInputPortIndexes( IObject obj );

   /**
    * Gets the indexes of the output ports to which the object should be connected.
    * If the object is not connected to an output port, if null is returned
    * This method should only be called for objects that are outputs
    * to the object represented by the adapter.
    * 
    * @param obj the object
    * 
    * @return the array of indexes of the object
    */
   int[] getOutputPortIndexes( IObject obj );
   
   /**
    * Gets the pushdown value for the validate of the transform.
    *  
    * @return the database pushdown
    */
   boolean isDatabasePushDown();
   
   /**
    * Sets the pushdown value for the validate of the transform.
    *  
    * @param the database pushdown value
    */
   void setDatabasePushDown( boolean bDatabasePushDown);

}

