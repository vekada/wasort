/* $Id$ */
/**
 * Title:       IDiagramNodeAdapter.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters;


import java.awt.Window;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPopupMenu;

import com.sas.etl.models.IObject;
import com.sas.workspace.WAPropertyTab;

/**
 * IDiagramNodeAdapter is the interface that describes an adapter
 * that is appropriate for use with a process editor diagram node.  In addition
 * the normal adapter interfaces, it provides methods for getting the input 
 * and output ports that will be defined on the node.
 */
public interface IDiagramNodeAdapter extends IIndicatorButtons
{
   /**
    * Size of a small icon; also the default size.
    */
   static final int ICON_SIZE_SMALL  = 16;
   
   /**
    * Size of a medium icon.
    */
   static final int ICON_SIZE_MEDIUM = 24;
   
   /**
    * Gets the model object represented by the adapter.
    * 
    * @return the model object.
    */
   IObject getObject();
   
   /**
    * Gets the name to be used to represent the model object.
    * 
    * @return the name of the model object
    */
   String getName();
   
   /**
    * Returns the tooltip text.
    * 
    * @return the tooltip text
    */
   String getToolTipText();
   
   /**
    * Returns the title text.
    * 
    * @return the title text
    */
   String getTitleText();
   
   /**
    * Gets the decorated icon used to represent the model object in a specific size.  Objects with
    * icons of multiple sizes must override this method to specify the available icon sizes;
    * ICON_SIZE_SMALL (16x16) and ICON_SIZE_MEDIUM (24x24)-sized icons should
    * be provided.
    * @param size the size of the icon, in pixels
    * @return the decorated icon
    */
   Icon getDecoratedIcon( int size );
   
   /**
    * Gets the icon used to represent the model object.
    * 
    * @return the icon
    */
   Icon getIcon();
   
   /**
    * Gets the icon used to represent the model object in a specific size.
    * @param size the size of the icon, in pixels
    * @return the icon
    */
   Icon getIcon(int size);
   
   /**
    * Gets menu items that can be added to a popup menu on the UI component that
    * represents the model object.  If the adapter has no object specific menu
    * items, null may be returned to indicate no menu items.
    * 
    * @param mnuPopup the popup menu item for the model object (null = no menu items)
    */
   void addContextMenuItems( JPopupMenu mnuPopup );

   /**
   * is valid to connect the object to this adapter's model as input after validating that
   * the given port is an accepted port for this adapter. 
   * @param obj the object to connect
   * @param portIndex the index of the input port connecting to
   * @return true if valid
   */
   boolean isValidConnectToInput(IObject obj, int portIndex);

   /**
    * is valid to connect the object to this adapter's model as output after validating that
    * the given port is an accepted port for this adapter. 
    * @param obj the object to connect
    * @param portIndex the index of the output port connecting to, null if link connected to node
   * @return true if valid
    */
    boolean isValidConnectToOutput(IObject obj, int portIndex);
    
    /**
     * Get a single message for link tooltip on invalid connect of output 
     * @param obj object that tried to connect to output.
     * @param portIndex the index of the output port connecting from, null if link connected to node
     * @return null no message
     */
    String getInvalidConnectOutputMessage(IObject obj, int portIndex );
    
    /**
     * Get a single message for link tooltip on invalid connect of input 
     * @param obj object that tried to connect to input.
     * @param portIndex the index of the input port connecting to
     * @return null no message
     */
    String getInvalidConnectInputMessage(IObject obj, int portIndex);
   
    /**
     * Force an auto layout on the view. 
     * @return true to force layout
     */
    boolean isLayoutRequired();
    
    /**
     * Is object valid to connect on redraw of diagram. 
     * @param obj object to check
     * @return true = valid to connect object on redraw of diagram
     */
    boolean isValidToConnectInputOnRedraw(IObject obj);
    
    /**
     * Is object valid to connect on redraw of diagram. 
     * @param obj object to check
     * @return true = valid to connect object on redraw of diagram
     */
    boolean isValidToConnectOutputOnRedraw(IObject obj);
    
   /**
    * connects the object to this adapter's model as input after validating that
    * the given port is an accepted port for this adapter. 
    * @param obj the object to connect
    * @param portIndex the index of the input port connecting to
    * @return true if connected
    */
    boolean connectToInput(IObject obj, int portIndex);

   /**
    * connects the object to this adapter's model as output after validating that
    * the given port is an accepted port for this adapter. 
    * @param obj the object to connect
    * @param portIndex the index of the output port connecting to
    * @return true if connected
    */
    boolean connectToOutput(IObject obj, int portIndex);
  
   /**
    * disconnects the object from this adapter model's input
    * @param obj the object to disconnect
    * @param portIndex the index of the input port disconnecting from
    * @return true if disconnected
    */
    boolean disconnectFromInput(IObject obj, int portIndex);
   
   /**
    * disconnects the object from this adapter model's output
    * @param obj the object to disconnect
    * @param portIndex the index of the output port disconnecting from
    * @return true if disconnected
    */
    boolean disconnectFromOutput(IObject obj, int portIndex);
    
   /**
    * Gets the port descriptions for the input ports the object requires.  An 
    * input port allows another object to make a connection into the object 
    * represented by this adapter.  For example, an input into a table would be 
    * the transform that produces the table.  The descriptions are used to 
    * create the ports
    * 
    * @return the port descriptions for the input ports the object requires 
    */
   IPortDescription[] getInputPortDescriptions();
   
   /**
    * Gets the port description for the output ports the object requires.  An 
    * output port allows the object represented by the adapter to be connected
    * to another.  For example, an output from a table could be the transform 
    * that consumes the table.  The descriptions are used to create the ports.
    * 
    * @return the port descriptions for the output ports the object requires 
    */
   IPortDescription[] getOutputPortDescriptions();
   
   /**
    * Gets the index of the input port to which the object should be connected.
    * If the object is not connected to an input port, a value of -1 is 
    * returned.  This method should only be called for objects that are inputs
    * to the object represented by the adapter.
    * 
    * @param obj the object
    * 
    * @return the index of the object (-1 = the object is not an input)
    */
   int getInputPortIndex( IObject obj );

   /**
    * Gets the index of the output port to which the object should be connected.
    * If the object is not connected to an output port, a value of -1 is 
    * returned.  This method should only be called for objects that are outputs
    * to the object represented by the adapter.
    * 
    * @param obj the object
    * 
    * @return the index of the object (-1 = the object is not an input)
    */
   int getOutputPortIndex( IObject obj );

   /**
    * Shows the properties dialog for the represented object.
    * 
    * @param wdwParent the parent window for the dialog.  The parent window must
    *                  be either a java.awt.Dialog or a java.awt.Frame.
    *                  
    * @return WAStandardDialog.OK = the user OK'd the dialog, 
    *         WAStandardDialog.CANCEL = the user canceled the dialog, or 
    *         -1 = an error occurred before the dialog was displayed.
    */
   int showPropertiesDialog( Window wdwParent );
   
   /**
    * Gets the node details panel to be shown for the node.  If no node details 
    * panel exists, null will be returned.  Before the node details panel is 
    * shown, moveDataToView will be called on the panel.
    * 
    * @return the node details panel
    */
   WAPropertyTab getNodeDetailsPanel();

   /**
    * Sets the default action.
    * 
    * @param actDefault the default action
    * 
    * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#setDefaultAction(javax.swing.Action)
    */
   void setDefaultAction( Action actDefault );

   /**
    * Gets the default action.
    * 
    * @return the default action
    * 
    * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#getDefaultAction()
    */
   Action getDefaultAction();
   
   /**
    * Sets the delete action.
    * 
    * @param actDelete the delete action
    * 
    * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#setDeleteAction(javax.swing.Action)
    */
   void setDeleteAction( Action actDelete );
   
   /**
    * Gets the delete action.
    * 
    * @return the delete action
    * 
    * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#getDeleteAction()
    */
   Action getDeleteAction();

   /**
    * Returns the node type.
    *
    * @return the node type
    */
   String getNodeType();
   
   /**
    * Returns the node type description.  This is the string that
    * is used to represent {@link #nodeType} in the UI.
    * 
    * @return the node type description
    */
   String getNodeTypeDescription();
}

