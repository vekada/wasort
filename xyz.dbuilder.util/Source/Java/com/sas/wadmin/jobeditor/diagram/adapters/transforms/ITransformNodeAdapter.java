/* $Id$ */
/**
 * Title:       ITransformNodeAdapter.java
 * Description: The interface that describes the adapter that is used to adapt
 *              a transform for use by a process editor diagram node.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters.transforms;

import com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter;
import com.sas.wadmin.jobeditor.diagram.adapters.IIndicatorButtons;

/**
 * ITransformNodeAdapter is the interface that describes the adapter that is 
 * used to adapt a transform for use by a process editor diagram node.  The 
 * adapter contains information on whether the transform is complete, whether 
 * it is enabled, whether it is use written, where it has pre and/or post 
 * process code.
 */
public interface ITransformNodeAdapter extends IDiagramNodeAdapter
{
	   /**
	    * Is automatically added to the control flow
	    * @return true if this node should be automatically added to the control flow 
	    */
	   boolean isAutomaticAddToControlFlow();
}

