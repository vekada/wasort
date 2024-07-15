/* $Id: WAPanel.java,v 1.1.2.6 2008/09/05 16:18:26 betoml Exp $ */
/**
 * Title:        WAPanel
 * Description:  The base class for panels that are embedded in WAStandardDialog
 *               and WAStandardInternalFrames.  This class is derived from
 *               JPanel.  This class has a default implementation of the methods
 *               used by WAStandardDialog and WAStandardInternal frame to
 *               communicate with the panel and it's data.  The default
 *               implementation should be overridden by classes that derive from
 *               WAPanel.
 * Copyright:    Copyright (c) 2001
 * Company:      SAS Institute
 * Author:       Russ Robison
 * Version:      1.0
 */

package com.sas.workspace;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import com.sas.metadata.remote.MdException;

/**
 * The base class for panels that are embedded in WAStandardDialog
 * and WAStandardInternalFrames.  This class is derived from
 * JPanel.  This class has a default implementation of the methods
 * used by WAStandardDialog and WAStandardInternal frame to
 * communicate with the panel and it's data.  The default
 * implementation should be overridden by classes that derive from
 * WAPanel.
 * <p>
 * Use this class to create custom panels.
 */

public class WAPanel extends JPanel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
    * Name of DataChanged property.
    */
   public static final String DATA_CHANGED_PROPERTY = "DataChanged";    /*I18nOK:EMS*/

   /**
    * Name of Editable property.
    */
   public static final String EDITABLE_PROPERTY = "Editable";           /*I18nOK:EMS*/

//   // class variables
//   private static boolean m_bDebug = Workspace.isDebug();
//
   // member variables
   protected boolean   m_bListeningForChanges = true;
   protected boolean   m_bStorePopulated      = false;
   protected boolean   m_bDataChanged         = false;
   protected boolean   m_bEditable            = true;
   private   Action    m_actDefault;

   //protected String    m_sHelpTopic   = "top";
   protected String    m_sHelpTopic;    // by default, use the helpTopic set in an object's constructor
   protected String    m_sHelpProduct;  // by default, use default help product

   protected WAUndoManager waum;

   private cItemListener  m_ItemListener  = new cItemListener();
   private cListListener  m_ListListener  = new cListListener();
   private cDocListener   m_DocListener   = new cDocListener();
   private cPanelListener m_PanelListener = new cPanelListener();


   //---------------------------------------------------------------------------
   // Constructors
   //---------------------------------------------------------------------------

   /**
    * Empty constructor.
    */
   public WAPanel()
   {
      waum = new WAUndoManager();
   }

   /**
    * Sets the next focusable component on the panel's last focusable component.
    * The default implementation does not know what the last focusable
    * component is and assumes the default algorithm for determining the next
    * focusable component is appropriate.
    *
    * @param cmpNext the next focusable component
    */
   public void setNextFocusableComponentOnLastFocusableComponent( Component cmpNext )
   {
   }

   //---------------------------------------------------------------------------
   // Attribute accessors
   //---------------------------------------------------------------------------

   /**
    * Sets the undo manager.
    * 
    * @param mgrUndo the undo manager
    */
   public void setWAUndoManager( WAUndoManager mgrUndo )
   {
      waum = mgrUndo;
   }

   /**
    * Gets the undo manager.
    * 
    * @return the undo manager
    */
   public WAUndoManager getWAUndoManager()
   {
      return waum;
   }

   /**
    * Gets the help class loader for panel.  The default help class loader is
    * the panel's class loader
    *
    * @return the help class loader
    */
   public ClassLoader getHelpClassLoader()
   {
	   return HelpClassLoader.getInstance(getClass());
   }

   /**
    * Sets the help topic for panel.
    *
    * @param sTopic help topic for panel
    *
    * @see #getHelpTopic
    */
   public void setHelpTopic( String sTopic )
   {
      m_sHelpTopic = sTopic;
   }

   /**
    * Gets the help topic for this panel.
    *
    * @return help topic for panel
    *
    * @see #setHelpTopic
    */
   public String getHelpTopic()
   {
      return m_sHelpTopic;
   }

   /**
    * Sets the help product for this panel.  The help product is used to find
    * the corresponding help set.
    *
    * @param sProduct help product for panel
    *
    * @see #getHelpProduct
    */
   public void setHelpProduct( String sProduct )
   {
      m_sHelpProduct = sProduct;
   }

   /**
    * Gets the help product for this panel.  The help product is used to find
    * the corresponding help set.
    *
    * @return help product for panel
    *
    * @see #setHelpProduct
    */
   public String getHelpProduct()
   {
      return m_sHelpProduct;
   }

   /**
    * Sets whether the contents of the panel are editable or not.
    * <p>
    * In this class, the editable attribute is simply maintained and the 
    * property changed event is fired.
    *
    * @param bEditable true = editable
    */
   public void setEditable( boolean bEditable )
   {
      if (m_bEditable != bEditable)
      {
         boolean bOld = m_bEditable;
         m_bEditable = bEditable;
         firePropertyChange( EDITABLE_PROPERTY, bOld, bEditable );
      }
   }

   /**
    * Gets whether the contents of the panel are editable or not.
    *
    * @return true = editable
    */
   public boolean isEditable()
   {
      return m_bEditable;
   }

   /**
    * Sets whether the panel's object store is populated or not.
    * 
    * @param bStorePopulated true = object store is populated
    */
   public void setStorePopulated( boolean bStorePopulated )
   {
      m_bStorePopulated = bStorePopulated;
   }

   /**
    * Is the panel's object store populated?
    * 
    * @return true = panel's object store is populated.
    */
   public boolean isStorePopulated()
   {
      return m_bStorePopulated;
   }
   
   /**
    * Sets whether the panel is listening for changes?.  During data movement 
    * into the view, the panel should not be listening for changes to its 
    * subcomponent.
    *
    * @param bListeningForChanges true = the panel is listening for changes
    */
   public void setListeningForChanges( boolean bListeningForChanges )
   {
      m_bListeningForChanges = bListeningForChanges;
   }
   
   /**
    * Is the panel listening for changes?
    * 
    * @return true = the panel is listening for changes
    */
   public boolean isListeningForChanges()
   {
      return m_bListeningForChanges;
   }
   
   /**
    * Sets whether data has changed or not.
    *
    * @param bDataChanged  true - data has changed
    *
    * @see #setDataChanged
    */
   public void setDataChanged( boolean bDataChanged )
   {
      if (m_bDataChanged != bDataChanged)
      {
         boolean bOld = m_bDataChanged;
         m_bDataChanged = bDataChanged;
         firePropertyChange( DATA_CHANGED_PROPERTY, bOld, bDataChanged );
      }
   }

   /**
    * Returns whether data has changed or not.
    *
    * @return false - data has not changed
    *
    * @see #setDataChanged
    */
   public boolean hasDataChanged()
   {
      return m_bDataChanged;
   }

   /**
    * Sets the default action for a panel.  The subclasser has to determined 
    * what triggers the default action.  Typically, the default action will be
    * the OK button of a dialog, so triggering the default action will cause the
    * dialog to close.
    * 
    * @param actDefault
    */
   public void setDefaultAction( Action actDefault )
   {
      m_actDefault = actDefault;
   }
   
   /**
    * Gets the default action.
    * 
    * @return the default action
    */
   public Action getDefaultAction()
   {
      return m_actDefault;
   }
   
   //---------------------------------------------------------------------------
   // Listen to components methods
   //---------------------------------------------------------------------------
   /**
    * Tells the panel to listen to a checkbox for data changes.
    *
    * @param cbx the checkbox
    */
   protected void listenForDataChangesTo( JCheckBox cbx )
   {
      cbx.addItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to listen to a radiobutton for data changes.
    *
    * @param rbn the radiobutton
    */
   protected void listenForDataChangesTo( JRadioButton rbn )
   {
      rbn.addItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to listen to a list for data changes.
    *
    * @param lst the list
    */
   protected void listenForDataChangesTo( JList lst )
   {
      lst.addListSelectionListener( m_ListListener );
   }

   /**
    * Tells the panel to listen to a combobox for data changes.
    *
    * @param cmb the combobox
    */
   protected void listenForDataChangesTo( JComboBox cmb )
   {
      cmb.addItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to listen to a text component for data changes.
    *
    * @param txt the text component
    */
   protected void listenForDataChangesTo( JTextComponent txt )
   {
      txt.getDocument().addDocumentListener( m_DocListener );
   }

   /**
    * Tells the panel to listen to a WAPanel for data changes
    *
    * @param pnl the WAPanel
    */
   protected void listenForDataChangesTo( WAPanel pnl )
   {
      pnl.addPropertyChangeListener( DATA_CHANGED_PROPERTY, m_PanelListener );
   }

   /**
    * Tells the panel to stop listening to a checkbox for data changes.
    *
    * @param cbx the checkbox
    */
   protected void stopListeningForDataChangesTo( JCheckBox cbx )
   {
      cbx.removeItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to stop listening to a radiobutton for data changes.
    *
    * @param rbn the radiobutton
    */
   protected void stopListeningForDataChangesTo( JRadioButton rbn )
   {
      rbn.removeItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to stop listening to a list for data changes.
    *
    * @param lst the list
    */
   protected void stopListeningForDataChangesTo( JList lst )
   {
      lst.removeListSelectionListener( m_ListListener );
   }

   /**
    * Tells the panel to stop listening to a combobox for data changes.
    *
    * @param cmb the combobox
    */
   protected void stopListeningForDataChangesTo( JComboBox cmb )
   {
      cmb.removeItemListener( m_ItemListener );
   }

   /**
    * Tells the panel to stop listening to a text component for data changes.
    *
    * @param txt the text component
    */
   protected void stopListeningForDataChangesTo( JTextComponent txt )
   {
      txt.getDocument().removeDocumentListener( m_DocListener );
   }

   /**
    * Tells the panel to stop listening to a WAPanel for data changes
    *
    * @param pnl the WAPanel
    */
   protected void stopListeningForDataChangesTo( WAPanel pnl )
   {
      pnl.removePropertyChangeListener( DATA_CHANGED_PROPERTY, m_PanelListener );
   }

   /**
    * Validates data on the panel before the data can be saved.
    * <p>
    * Override this method to validate panel-specific data.
    * 
    * @return false - invalid data
    *
    * @see #doDataExchange
    */
   public boolean validateData()
   {
//      // if debugging is on, display option dialog
//      if (m_bDebug)
//      {
//         // these strings do not need to be translated because they should
//         // only be shown because a deriving class did not override this
//         // method.  This method must be overridden.
//         String sTitle   = "validateData not implemented";                                      /*I18nOK:EMS*/
//         String sMessage = "This message indicates a coding error.  Pretend data is valid?";    /*I18nOK:EMS*/
//         int irc = MessageUtil.displayMessage( sMessage, sTitle, JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION );
//         return (irc == JOptionPane.OK_OPTION);
//      }

      // otherwise, just print a message and exit
      Workspace.getDefaultLogger().debug( "validateData not implemented; returning true" );
      return true;
   }

   /**
    * Populates the panel's object store.  This method may run on a background 
    * thread and should not update any visuals or any models used by visuals.
    * <p>
    * Override this method to populate the object store with panel-specific 
    * data.  Be sure to test if the store is populated before populating the
    * store and be sure to set that the store is populated after populating the
    * store.
    * <p>
    * The default implementation calls doDataExchange( false ) for backwards 
    * compatibility.
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void populateStore() throws MdException, RemoteException
   {
      if (isStorePopulated())
         return;
         
      doDataExchange( false );

      setStorePopulated( true );
   }
   
   /**
    * Moves data from the panel's object store to its view (the panel). 
    * <p>
    * Override this method to initialize panel-specific components.
    * <p>
    * The default implementation does nothing!
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void moveDataToView() throws MdException, RemoteException
   {
//      setListeningForChanges( false );
//      
//      move the data into the view
//      
//      setListeningForChanges( true  );
   }
   
   /**
    * Moves data from the panel's view (the panel) to its object store.
    * <p>
    * Override this method to move panel-specific data back into the object
    * store.
    * <p>
    * The default implementation calls doDataExchange( true ) for backwards 
    * compatibility.
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void moveDataToStore() throws MdException, RemoteException
   {
      doDataExchange( true );
   }

   /**
    * Does the model/view data exchange. (Obsolete, maintained for backwards
    * compatibility.
    *
    * @param saveToModel true  - move widget values to model values;
    *                      false - move model values to widgets values
    * 
    * @return false - invalid data, exchange to model aborted (currently unused)
    * 
    * @throws MdException
    * @throws RemoteException
    *
    * @see #validateData
    * 
    * This method will be deprecated in 2003.  Use the newer methodology
    * (populateStore, moveDataToView, moveDataToStore, etc.).
    */
   public boolean doDataExchange( boolean saveToModel ) throws MdException, RemoteException
   {
//      // if debugging is on, display message dialog
//      if (m_bDebug)
//      {
//         // these strings do not need to be translated because they should
//         // only be shown because a deriving class did not override this
//         // method.  This method must be overridden.
//         String sTitle   = "doDataExchange not implemented";            /*I18nOK:EMS*/
//         String sMessage = "This message indicates a coding error";     /*I18nOK:EMS*/
//         MessageUtil.displayMessage( sMessage, sTitle, JOptionPane.ERROR_MESSAGE );
//         return true;
//      }

      // otherwise, just print a message and exit
      Workspace.getDefaultLogger().debug( "doDataExchange not implemented; returning true" );
      return true;
   }

   /**
    * Refreshes data from the model into the view.
    * <p>
    * The default implementation of this method is to set the object store
    * as unpopulated so that data will be read from the server again.  Then the
    * object store is repopulated and data is moved from the object store to
    * the view again.
    * <p>
    * This method is obsolete and is kept for backwards compatibility.  It will 
    * be deprecated in 2003.
    * 
    * @throws MdException
    * @throws RemoteException
    * 
    * @see #doDataExchange
    */
   public void refresh() throws MdException, RemoteException
   {
      setStorePopulated( false );
      populateStore();  /** @todo run on background thread? */
      moveDataToView();
   }

   /**
    * Updates the workspace's UI components when the panel becomes the active
    * UI component or stops being the active UI component.  The panel should
    * override this method to enable/disable and show/hide workspace UI 
    * components.
    * <p>
    * The default implementation of this method has the undo manager update its
    * workspace UI components.
    * 
    * @param bActive true = the panel is the active UI component
    */
   public void updateWorkspaceUIComponents( boolean bActive )
   {
      waum.updateWorkspaceUIComponents( bActive );
   }

   /**
    * Saves current settings of a panel and cleans up the panel before
    * termination.  This method is called after the user has completed use of
    * the panel.  Therefore, the panel should override this method to save any 
    * settings it uses as defaults (such as the size of columns in a table and 
    * which columns are shown) and to clean up anything that needs cleaning up
    * (such as releasing server connections).  
    * <p>
    * The default implementation of this method does nothing.
    */
   public void saveSettingsAndCleanUp()
   {
   }

   /**
    * Loads the panel.  This method is only called by new style views 
    * (WsAbstractNewView).
    * 
    * @return true = the load was successful
    * 
    * @see com.sas.workspace.WAPanel#load()
    */
   public boolean load()
   {
      throw new UnsupportedOperationException( "load has not been implemented" );
   }
   
   /**
    * Saves the panel.  This method is only called by new style views 
    * (WsAbstractNewView).
    * 
    * @return true = the save was sucessful
    * 
    * @see com.sas.workspace.WAPanel#save()
    */
   public boolean save()
   {
      throw new UnsupportedOperationException( "save has not been implemented" );
   }
   
   /**
    * Gets the component which should receive input focus by default.
    *
    * @return the component to receive input focus by default
    */
   public Component getDefaultFocusComponent()
   {
      return null;
   }
   
   /**
    * Requests focus on the WAPanel.  This method is overriden because the WAPanel
    * does not want input focus but wants one of it's children to have input focus.
    * Also, JTabbedPane calls requestFocus on the tab's component directly.
    * <p>
    * The algorithm is simply to use CustomLayoutFocusTraversalPolicy.getDefaultFocusComponent
    * to determine which child component should have input focus.  getDefaultFocusComponent
    * can return null if there are no child components that can receive input focus.
    */
   public void requestFocus()
   {
      Component cmp = CustomLayoutFocusTraversalPolicy.getDefaultFocusComponent( this );
      if (cmp != null)
         cmp.requestFocus();
   }

   /**
    * Indicates whether the panel can close.  This method provides an opportunity for
    * the panel to approve or deny a close operation.  The default return value for
    * this method is <code>true</code>.
    * 
    * @return <code>true</code> if the panel can be closed,
    *          <code>false</code> if the panel should not be closed.
    */
   public boolean canClose()
   {
      return true;
   }
   
   /**
    * Sets a property on a tab.  This method provides a generic way to set 
    * property values on tabs.  The default implementation does nothing.
    * 
    * @param sPropertyName the name of the property
    * @param value         the new value of the property
    */
   public void setTabProperty( String sPropertyName, Object value )
   {
   }

   /**
    * Called when parent's OK button is pressed.
    */
   public void onParentOK()
   {
   }

   /**
    * Called when parent's Apply button is pressed.
    */
   public void onParentApply()
   {
   }

   /**
    * Called when parent's Cancel button is pressed.
    */
   public void onParentCancel()
   {
   }

   //---------------------------------------------------------------------------
   // Data Changed Listeners
   //    cItemListener           - for checkboxes, radiobuttons, and comboboxes
   //       itemStateChanged
   //    cListListener           - for lists
   //       valueChanged
   //    cDocListener            - for text components
   //       changedUpdate
   //       insertUpdate
   //       removeUpdate
   //---------------------------------------------------------------------------

   /**
    * Listens to data changes on checkboxes, radiobuttons, and comboboxes.
    */
   private class cItemListener implements ItemListener
   {
      /**
       * Handles the item state changed event.
       * 
       * @param e the item listener event
       */
      public void itemStateChanged( ItemEvent e )
      {
         if (isListeningForChanges())
            setDataChanged( true );
      }
   }

   /**
    * Listens to data changes on lists.
    */
   private class cListListener implements ListSelectionListener
   {
      /**
       * Handles the value changed event.
       * 
       * @param e the check tree selection event
       */
      public void valueChanged( ListSelectionEvent e )
      {
         if (isListeningForChanges())
            setDataChanged( true );
      }
   }

   /**
    * Listens to data changes on text components.
    */
   private class cDocListener implements DocumentListener
   {
      /**
       * Handles the change update event
       * 
       * @param e
       * 
       * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
       */
      public void changedUpdate( DocumentEvent e )
      {
         if (isListeningForChanges())
            setDataChanged( true );
      }

      /**
       * Handles the insert update event
       * 
       * @param e
       * 
       * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
       */
      public void insertUpdate( DocumentEvent e )
      {
         if (isListeningForChanges())
            setDataChanged( true );
      }

      /**
       * Handles the remove update event
       * 
       * @param e
       * 
       * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
       */
      public void removeUpdate( DocumentEvent e )
      {
         if (isListeningForChanges())
            setDataChanged( true );
      }
   }

   /**
    * Listens to data changes on a WAPanel
    */
   private class cPanelListener implements PropertyChangeListener
   {
      /**
       * Handle the property change event
       * 
       * @param e
       * 
       * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
       */
      public void propertyChange( PropertyChangeEvent e )
      {
         // if the new property value is true, data has changed somewhere
         // so set data changed on the panel.  if the new property value is
         // false, nothing is really known
         if (isListeningForChanges())
         {
            if (((Boolean) e.getNewValue()).booleanValue())
               setDataChanged( true );
         }
      }
   }

   //---------------------------------------------------------------------------
   // Initialization methods
   //---------------------------------------------------------------------------

   /**
    * Initializes panel and creates/initializes panel's widgets.
    */
   protected void initialize()
   {
   }

   /**
    * Lays out panel's widgets.
    */
   protected void layoutWidgets()
   {
   }

}





