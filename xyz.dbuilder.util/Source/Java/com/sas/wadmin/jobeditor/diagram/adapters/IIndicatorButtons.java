/* $Id: IIndicatorButtons.java,v 1.1.8.1 2009/03/10 16:18:14 sasblc Exp $ */
/**
 * Title:       IIndicatorButtons.java
 * Description: The interface that allows an adapter to get a
 *              it's indicator buttons.
 * Copyright:   Copyright (c) 2008
 * Company:     SAS Institute
 * Author:      Kim Lewis
 * Support:     Kim Lewis
 */
package com.sas.wadmin.jobeditor.diagram.adapters;

import java.util.List;

import com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter;

/**
 * IIndicatorButtons is the interface that allows an adapter to get a
 * it's indicator buttons.
 */
public interface IIndicatorButtons
{
	   /**
	    * get the indicator buttons used on this transform
	    * 
	    * @return the list of indicator buttons used on this transform
	    * 
	    */
	   List getIndicatorButtons();
}

