/* $Id$ */
/**
 * Title:       AbstractDataTransformAdapter.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters.transforms;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.mutable.MutableInt;

import com.sas.etl.models.IObject;
import com.sas.etl.models.data.IDataObject;
import com.sas.etl.models.job.IDataTransform;
import com.sas.etl.models.job.ITransform;
import com.sas.wadmin.jobeditor.diagram.adapters.AbstractObjectAdapter;
import com.sas.wadmin.jobeditor.diagram.adapters.IPortDescription;
import com.sas.wadmin.jobeditor.diagram.adapters.IndicatorFactory;
import com.sas.wadmin.jobeditor.diagram.adapters.PortDescriptionFactory;
import com.sas.wadmin.visuals.common.MappingsTab;
import com.sas.wadmin.visuals.common.MappingsTab.Configuration;
import com.sas.wadmin.visuals.common.ModelUIUtilities;
import com.sas.workspace.WAPropertyTab;
import com.sas.workspace.WAdminResource;

/**
 * AbstractDataTransformAdapter is an abstract implementation of a data
 * transform adapter.  This abstract implementation provides the single input
 * data source port, the single input control port, the single output data port,
 * and the single output control port.  It also handles complete, run status,
 * enabled, user written code, pre-process code, and post-process code.
 */
public abstract class AbstractDataTransformAdapter extends AbstractObjectAdapter implements IDataTransformNodeAdapter
{
   // TODO this class may need to be split into two parts: data and no data
   private static final WAdminResource bundle = WAdminResource.getBundle( AbstractDataTransformAdapter.class );

   protected static final String[]         DEFAULT_TABLE_TYPES         = new String[]{ IPortDescription.PHYSICAL_TABLE, IPortDescription.WORK_TABLE };

   // default port descriptions
   // data input:  one physical or work table
   // data output: one physical or work table
   protected static final IPortDescription DEFAULT_INPUT_DATA_PORT     = PortDescriptionFactory.getInstance().createDataInputPortDescriptionForDataTransform(  bundle.getString("AbstractDataTransformAdapter.InputDataPort.ToolTip.txt"), 1, DEFAULT_TABLE_TYPES );
   protected static final IPortDescription DEFAULT_OUTPUT_DATA_PORT    = PortDescriptionFactory.getInstance().createDataOutputPortDescriptionForDataTransform( bundle.getString("AbstractDataTransformAdapter.OutputDataPort.ToolTip.txt"), 1, DEFAULT_TABLE_TYPES );
   protected static final IPortDescription DEFAULT_INPUT_CONTROL_PORT  = PortDescriptionFactory.getInstance().createControlInputPortDescriptionForTransform(1);
   protected static final IPortDescription DEFAULT_OUTPUT_CONTROL_PORT = PortDescriptionFactory.getInstance().createControlOutputPortDescriptionForTransform(1);

   private static final IPortDescription[] INPUT_PORTS  = new IPortDescription[] { DEFAULT_INPUT_CONTROL_PORT, DEFAULT_INPUT_DATA_PORT };
   private static final IPortDescription[] OUTPUT_PORTS = new IPortDescription[] { DEFAULT_OUTPUT_CONTROL_PORT, DEFAULT_OUTPUT_DATA_PORT };

   private IDataTransform m_transform;
   private Integer        m_iDBMSType;

   private boolean m_bDatabasePushDown;

  // private IIndicatorButton m_userWrittenIndicator;
  // private IIndicatorButton m_prePostProcessIndicator;
  // private IIndicatorButton m_diagnosticModeIndicator;

   /**
    * Constructs an abstract data transform adapter.
    *
    * @param transform the data transform
    */
   public AbstractDataTransformAdapter( IDataTransform transform )
   {
      super( transform );
      m_transform = transform;
   }
   /**
    * Returns the node type.
    *
    * @return the node type
    */
   public String getNodeType()
   {
      return getName();
   }

   /**
    * Returns the node type description.  This is the string that
    * is used to represent {@link #nodeType} in the UI.
    *
    * @return the node type description
    */
   public String getNodeTypeDescription()
   {
      return getName();
   }

   /**
    * Gets the pushdown value for the validate of the transform.
    *
    * @return the database pushdown
    */
   public boolean isDatabasePushDown()
   {
      return m_bDatabasePushDown;
   }

   /**
    * Sets the pushdown value for the validate of the transform.
    *
    * @param the database pushdown value
    */
   public void setDatabasePushDown( boolean bDatabasePushDown)
   {
      m_bDatabasePushDown = bDatabasePushDown;
   }


   /**
    * get the indicator buttons used on this transform
    *
    * @return the list of indicator buttons used on this transform
    *
    */
   public List getIndicatorButtons()
   {
      Integer iUserWritten   = new Integer(IndicatorFactory.USER_WRITTEN);
      Integer iPrePost       = new Integer(IndicatorFactory.PRE_POST_PROCESS);
      Integer iDiagMode      = new Integer(IndicatorFactory.DIAGNOSTIC_MODE);
      Integer iDBMSType      = new Integer(m_transform.getDBMSExecutionType());
      Integer iCheckpointSet = new Integer(IndicatorFactory.CHECKPOINT_SET);
      Integer iDataTransfer  = new Integer(IndicatorFactory.DATA_TRANSFER);
      
      if (m_transform.isUsingUserWrittenCode() && !getIndicatorsMap().containsKey( iUserWritten ))
         getIndicatorsMap().put( iUserWritten , IndicatorFactory.getInstance().getIndicator( IndicatorFactory.USER_WRITTEN ) );
      else if (!m_transform.isUsingUserWrittenCode() && getIndicatorsMap().containsKey( iUserWritten ))
         getIndicatorsMap().remove( iUserWritten );

      if ((m_transform.isUsingPostProcessCode() || m_transform.isUsingPreProcessCode()) && !getIndicatorsMap().containsKey( iPrePost ))
         getIndicatorsMap().put( iPrePost, IndicatorFactory.getInstance().getIndicator( IndicatorFactory.PRE_POST_PROCESS ) );
      else if (!m_transform.isUsingPostProcessCode() && !m_transform.isUsingPreProcessCode() && getIndicatorsMap().containsKey( iPrePost ))
         getIndicatorsMap().remove( iPrePost );

      if (m_transform.isCollectingDiagnostics() && !getIndicatorsMap().containsKey( iDiagMode ))
         getIndicatorsMap().put( iDiagMode, IndicatorFactory.getInstance().getIndicator( IndicatorFactory.DIAGNOSTIC_MODE ) );
      else if (!m_transform.isCollectingDiagnostics() && getIndicatorsMap().containsKey( iDiagMode ))
         getIndicatorsMap().remove( iDiagMode );

      if (m_transform.isCheckpointEnabled() && !getIndicatorsMap().containsKey( iCheckpointSet ))
         getIndicatorsMap().put( iCheckpointSet , IndicatorFactory.getInstance().getIndicator( IndicatorFactory.CHECKPOINT_SET ) );
      else if (!m_transform.isCheckpointEnabled() && getIndicatorsMap().containsKey( iCheckpointSet ))
         getIndicatorsMap().remove( iCheckpointSet );

      boolean isTransfer = ModelUIUtilities.isTransformPerformingDataTransfer(m_transform);
      
      if (isTransfer && !getIndicatorsMap().containsKey( iDataTransfer ))
          getIndicatorsMap().put( iDataTransfer , IndicatorFactory.getInstance().getIndicator( IndicatorFactory.DATA_TRANSFER ) );
       else if (!isTransfer && getIndicatorsMap().containsKey( iDataTransfer ))
          getIndicatorsMap().remove( iDataTransfer );
      
      //add the execution dbms type for explicit type code
      //or validate ran or run happened
      //sas does not display and is the default dbms type
      if ( (isDatabasePushDown() || m_transform.isExplicitOn())  &&
            m_transform.getDBMSExecutionType() != IDataTransform.SAS_DBMS_EXECUTION_TYPE )
      {
         if (iDBMSType != m_iDBMSType)
         {
            getIndicatorsMap().remove( m_iDBMSType );
            getIndicatorsMap().put( iDBMSType, IndicatorFactory.getInstance().getIndicator( m_transform.getDBMSExecutionType() ) );
            m_iDBMSType = iDBMSType;
         }
      }
      else if ( !(isDatabasePushDown() || m_transform.isExplicitOn()) &&
                (m_iDBMSType != Integer.valueOf( IDataTransform.SAS_DBMS_EXECUTION_TYPE) ) )
           getIndicatorsMap().remove( m_iDBMSType );
      //method must return a list
      List buttons = new ArrayList();
      buttons.addAll( getIndicatorsMap().values() );
      return buttons;
   }

   /**
    * Gets the input ports that are used for a data transform object.  The input
    * ports are a data input port that allows a single connection and a control
    * input port that allows a single connection.
    *
    * @return the input ports used for the data transform object
    *
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getInputPortDescriptions()
    */
   public IPortDescription[] getInputPortDescriptions()
   {
      return INPUT_PORTS;
   }

   /**
    * Gets the output ports that are used for a data transform object.  The
    * output ports are a data output port that allows a single connection and a
    * control output port that allows a single connection.
    *
    * @return the output ports used for the data transform object
    *
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getOutputPortDescriptions()
    */
   public IPortDescription[] getOutputPortDescriptions()
   {
      return OUTPUT_PORTS;
   }

   /**
    * Is automatically added to the control flow
    * @return true if this node should be automatically added to the control flow
    */
   public boolean isAutomaticAddToControlFlow()
   {
        return false;
   }

   /**
    * Gets the index of the input port to which the object should be connected.
    * If the object is not connected to an input port, a value of -1 is
    * returned.
    *
    * @param obj the object
    *
    * @return the index of the object (-1 = the object is not an input)
    */
   public int getInputPortIndex( IObject obj )
   {
//      if (obj instanceof ITransform)
//         return m_transform.containsInControlPredecessors( (ITransform) obj ) ? 0 : -1;
      if (obj instanceof IDataObject)
         return m_transform.containsInDataSources( (IDataObject) obj ) ? 1 : -1;

      return -1;
   }

   /**
    * Gets the index of the output port to which the object should be connected.
    * If the object is not connected to an output port, a value of -1 is
    * returned.
    *
    * @param obj the object
    *
    * @return the index of the object (-1 = the object is not an output)
    */
   public int getOutputPortIndex( IObject obj )
   {
//      if (obj instanceof ITransform)
//         return m_transform.containsInControlSuccessors( (ITransform) obj ) ? 0: -1;
      if (obj instanceof IDataObject)
         return m_transform.containsInDataTargets( (IDataObject) obj ) ? 1 : -1;

      return -1;
   }

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
   public int[] getInputPortIndexes( IObject obj )
   {
      int ind = getInputPortIndex(obj);
      if (ind != -1)
         return new int[] {ind};
      else
         return null;
   }

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
   public int[] getOutputPortIndexes( IObject obj )
   {
      int ind = getOutputPortIndex(obj);
      if (ind != -1)
            return new int[] {ind};
         else
            return null;
   }

   /**
    * Connects the object to this adapter's model as input after validating that
    * the given port is an accepted port for this adapter.
    * @param obj the object to connect
    * @param portIndex the index of the input port connecting to
    * @return true if the connection was made, otherwise false
    */
    public boolean connectToInput(IObject obj, int portIndex)
    {
       if (portIndex == 0)
       {
          throw new IllegalArgumentException( "connect to control port not supported" );
//          if (obj instanceof ITransform)
//          {
//            m_transform.addControlPredecessor((ITransform)obj);
//            return true;
//          }
       }
       else if (portIndex == 1)
          if (obj instanceof IDataObject && !m_transform.containsInDataSources((IDataObject)obj))
          {
             m_transform.addDataSource((IDataObject)obj);
             m_transform.addConnectedSource((IDataObject)obj);
             
             return true;
          }
       return false;
    }

   /**
    * Connects the object to this adapter's model as output after validating that
    * the given port is an accepted port for this adapter.
    * @param obj the object to connect
    * @param portIndex the index of the output port connecting to
    * @return true if the connection was made, otherwise false
    */
    public boolean connectToOutput(IObject obj, int portIndex)
    {
       if (portIndex == 0)
       {
          throw new IllegalArgumentException( "connect to control port not supported" );
//          if (obj instanceof ITransform)
//          {
//            m_transform.addControlSuccessor((ITransform)obj);
//            return true;
//          }
       }
       else if (portIndex == 1)
          if ( obj instanceof IDataObject )
          {
             m_transform.addDataTarget((IDataObject)obj);
             return true;
          }
       return false;
    }

   /**
    * Disconnects the object from this adapter model's input.
    * @param obj the object to disconnect
    * @param portIndex the index of the input port disconnecting from
    * @return true if the disconnection was done, otherwise false
    */
    public boolean disconnectFromInput(IObject obj, int portIndex)
    {
       if (portIndex == 0)
       {
          throw new IllegalArgumentException( "connect to control port not supported" );
//          if (obj instanceof ITransform)
//          {
//            m_transform.removeControlPredecessor((ITransform)obj);
//            return true;
//          }
       }
       else if (portIndex == 1)
          if (obj instanceof IDataObject)
          {
             m_transform.removeDataSource((IDataObject)obj);
             if (!m_transform.getModel().isUndoing())
            	 m_transform.removeConnectedSource((IDataObject)obj);

             return true;
          }
       return false;
    }

   /**
    * Disconnects the object from this adapter model's output.
    * @param obj the object to disconnect
    * @param portIndex the index of the output port disconnecting from
    * @return true if the disconnection was done, otherwise false
    */
    public boolean disconnectFromOutput(IObject obj, int portIndex)
    {
       if (portIndex == 0)
       {
          throw new IllegalArgumentException( "connect to control port not supported" );
//          if (obj instanceof ITransform)
//          {
//            m_transform.removeControlSuccessor((ITransform)obj);
//            return true;
//          }
       }
       else if (portIndex == 1)
          if (obj instanceof IDataObject)
          {
             m_transform.removeDataTarget((IDataObject)obj);
             return true;
          }
       return false;
    }

    /**
     * Is valid to connect the object to this adapter's model as input after validating that
     * the given port is an accepted port for this adapter.
     * @param obj the object to connect
     * @param portIndex the index of the input port connecting to
     * @return true if it's a valid connect to input, false otherwise
     */
    public boolean isValidConnectToInput(IObject obj, int portIndex)
    {
       if (portIndex == 0)
      {
         if (obj instanceof ITransform)
         {
           return true;
         }
      }
      else if (portIndex == 1)
         if (obj instanceof IDataObject && !m_transform.containsInDataSources((IDataObject)obj))
         {
            return true;
         }
      return false;
    }

   /**
    * Is valid to connect the object to this adapter's model as output after validating that
    * the given port is an accepted port for this adapter.
    * @param obj the object to connect
    * @param portIndex the index of the output port connecting to
    * @return true if it's a valid to connect to output
    */
   public boolean isValidConnectToOutput(IObject obj, int portIndex)
   {
      if (portIndex == 0)
      {
         if (obj instanceof ITransform)
         {
            return true;
         }
      }
      else if (portIndex == 1)
         if ( obj instanceof IDataObject && !m_transform.containsInDataTargets((IDataObject)obj) )
         {
            return true;
         }
      return false;
   }

   /**
    * Is the port mapping automatically?  Only checked if Job level mapping
    * is true
    * @param iPortIndex the index of the port to map with
    * @return true = the port maps automatically
    */
   public boolean isMappingAutomatically(int iPortIndex)
   {
      return m_transform.isIncludedInMapping();
   }

   /**
    * Is the port propagating automatically? Only checked if Job level propagation
    * is true
    * @param iPortIndex the index of the port to propagate with
    * @return true = the port propagates automatically
    */
   public boolean isPropagatingAutomatically(int iPortIndex)
   {
      return m_transform.isIncludedInPropagation();
   }

   /**
    * Add port port description
    * @param obj the object used to create the port description
    * @param inputPortIndex the port index of where to insert the port
    * @return port description
    */
   public IPortDescription addInputPortDescription(Object obj, int inputPortIndex)
   {
      return null;
   }

   /**
    * Add port port description
    * @param obj the object used to create the port description
    * @param inputPortIndex the port index of where to insert the port
    * @return port description
    */
   public IPortDescription addInputPortDescription(Object obj, MutableInt inputPortIndex)
   {
      return addInputPortDescription(obj,inputPortIndex.intValue());
   }
   
   /**
    * Add port port description
    * @param obj the object used to create the port description
    * @return port description
    */
   public IPortDescription addInputPortDescription(Object obj)
   {
      return null;
   }

   /**
    * Add output port description
    * @param obj the object used to create the port description
    * @param outputPortIndex the port index of where to delete the port
    * @return port description
    */
   public IPortDescription addOutputPortDescription(Object obj, int outputPortIndex)
   {
      return null;
   }

   /**
    * Add output port description
    * @param obj the object used to create the port description
    * @param outputPortIndex the port index of where to delete the port
    * @return port description
    */
   public IPortDescription addOutputPortDescription(Object obj, MutableInt outputPortIndex)
   {
      return addOutputPortDescription(obj, outputPortIndex.intValue());
   }
   
   /**
    * Add output port description
    * @param obj the object used to create the port description
    * @return port description
    */
   public IPortDescription addOutputPortDescription(Object obj)
   {
      return null;
   }

   /**
    * Delete input port.
    * @param obj the object used to create the port description
    * @param inputPortIndex where to insert the port
    * @return the port index to delete -1 means nothing to remove
    */
   public int deleteInputPort(Object obj, int inputPortIndex)
   {
      return -1;
   }

   /**
    * Delete input port.
    * @param obj the object used to create the port description
    * @return the port index to delete -1 means nothing to remove
    */
   public int deleteInputPort(Object obj)
   {
      return -1;
   }

   /**
    * Delete output port.
    * @param obj the object used to create the port descriptions
    * @param outputPortIndex the port index of where to delete
    * @return the port index to delete -1 means nothing to remove
    */
   public int deleteOutputPort(Object obj, int outputPortIndex)
   {
      return -1;
   }

   /**
    * Delete output port.
    * @param obj the object used to create the port descriptions
    * @return the port index to delete -1 means nothing to remove
    */
   public int deleteOutputPort(Object obj)
   {
      return -1;
   }

   /**
    * Update input port.
    * @param obj the object used to update the port
    * @return the port index should be 0 position. text should be at 1 position
    */
   public List updateInputPort(Object obj)
   {
      return null;
   }

   /**
    * Update output port.
    * @param obj the object used to update the port
    * @return the port index should be 0 position. text should be at 1 position
    */
   public List updateOutputPort(Object obj)
   {
      return null;
   }

   /**
    * Gets the menu items used for a data transform object.
    *
    * @return the menu items (null = no menu items)
    *
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getPopupMenuItems()
    */
   public final JMenuItem[] getPopupMenuItems()
   {
      return null;
   }

   public void addContextMenuItems( JPopupMenu mnuPopup )
   {
   }

   /**
    * Gets the node details panel for the transform.  The default node details
    * panel for a transform is the mappings tab in the default configuation.
    *
    * @return the node details panel (i.e. a default mappings tab)
    */
   public WAPropertyTab getNodeDetailsPanel()
   {
      return createMappingsTab(true);
   }

   protected Configuration createMappingsConfiguration(boolean details)
   {
	   Configuration configuration = new Configuration();
	   configuration.setWhereUsedSettingsKeyPrefix( details ? "Details" : "Properties" );
	   return configuration;
   }
   
   protected WAPropertyTab createMappingsTab(boolean details)
   {
      return new MappingsTab( m_transform, createMappingsConfiguration(details) );
   }

   /**
    * Get a single message for link tooltip on invalid connect of output
    * @param obj object that tried to connect to output.
    * @param portIndex the index of the output port connecting from, null if link connected to node
    * @return null no message
    */
   public String getInvalidConnectOutputMessage(IObject obj, int portIndex )
   {

      return bundle.formatString( "AbstractDataTransformAdapter.InvalidOutputMessage.txt", getName() );
   }

   /**
    * Get a single message for link tooltip on invalid connect of input
    * @param obj object that tried to connect to input.
    * @param portIndex the index of the input port connecting to, null if link connected to node
    * @return null no message
    */
   public String getInvalidConnectInputMessage(IObject obj, int portIndex)
   {
      return bundle.formatString( "AbstractDataTransformAdapter.InvalidInputMessage.txt", getName() );
   }
}

