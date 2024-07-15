/* $Id: WATabbedPanel.java,v 1.1.2.9 2007/09/28 15:36:54 sasrlr Exp $ */
/**
 * Title:        WATabbedPanel
 * Description:  This class is derived from WAPanel and has an embedded
 *               JTabbedPane.  This class implements the methods used
 *               by WAStandardDialog and WAStandardInternal frame to
 *               communicate with the panel.  The implementation is appropriate
 *               for tabs.
 * Copyright:    Copyright (c) 2000
 * Company:      SAS
 * @author       Russ Robison
 * @version      1.0
 */
package com.sas.workspace;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import com.sas.metadata.remote.MdException;

/**
 * This class is derived from WAPanel and has an embedded
 * JTabbedPane.  This class implements the methods used
 * by WAStandardDialog and WAStandardInternal frame to
 * communicate with the panel.  The implementation is appropriate
 * for tabs.  This panel is embedded in a tabbed property sheet.
 */
public class WATabbedPanel extends WAPanel
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

// class variables
   private  static WAdminResource bundle = WAdminResource.getBundle( WATabbedPanel.class );

   // member variables
   protected WsTabbedInterface m_Tabs;
   protected WAPropertyTab     m_cmpSelectedTab       = null;
   /** @deprecated */
   protected boolean           m_bInternalFrameParent = false;
   protected boolean           m_bSkipMoveDataToView  = false;
   private   boolean           m_bActive;
   private   UndoManager       m_mgrUndo;
   private   boolean           m_bUndoing;

   
   /**
    * Constructs a default WATabbedPanel
    */
   public WATabbedPanel()
   {
      super();
      m_Tabs = createTabbedPane();

      // use border layout center so that m_Tabs grows as WATabbedPanel grows
      setLayout( new BorderLayout() );
      add( m_Tabs.getComponent(), BorderLayout.CENTER );

      // add change listener to hear when the selected tab changes
      m_Tabs.addChangeListener( createTabChangedListener() );
   }

   /**
    * Creates a tabbed pane for use in the WATabbedPanel.
    *
    * @return the tabbed pane
    */
   protected WsTabbedInterface createTabbedPane()
   {
      return new cTabbedPane();
   }

   /**
    * Gets the default focus component for the tabbed panel.  Gets the selected
    * tab's default focus component.  If the selected tab does not have a
    * default focus component, the tabbed pane is returned as the default focus
    * component.
    *
    * @return the component to receive input focus by default
    */
   public Component getDefaultFocusComponent()
   {
      Component cmp = CustomLayoutFocusTraversalPolicy.getDefaultFocusComponent( m_Tabs.getSelectedTab() );
      if (cmp == null)
         cmp = m_Tabs.getComponent();

      return cmp;
   }

   /**
    * Sets the next focusable component on the panel's last focusable component.
    * The implementation for WATabbedPanel is to set the next focusable component
    * on each of the tab's last focusable components.
    *
    * @param cmpNext the next focusable component
    */
   public void setNextFocusableComponentOnLastFocusableComponent( Component cmpNext )
   {
      int iTab;
      int nTabs = m_Tabs.getTabCount();
      for ( iTab=0; iTab < nTabs; iTab++ )
         getTabAt( iTab ).setNextFocusableComponentOnLastFocusableComponent( cmpNext );
   }

   /**
    * Sets the undo manager used by the tabbed panel.
    * 
    * @param mgrUndo the undo manager
    */
   public void setUndoManager( UndoManager mgrUndo )
   {
      m_mgrUndo = mgrUndo;
   }
   
   /**
    * Gets the undo manager used by the tabbed panel.
    * 
    * @return the undo manager
    */
   public UndoManager getUndoManager()
   {
      return m_mgrUndo;
   }
   
   //------------------------------------------------------------------------
   // tab manipulators
   //------------------------------------------------------------------------

   /**
    * Adds a tab
    *
    * @param tab the contents of the tab
    */
   public void addTab( WAPropertyTab tab )
   {
      addTab( tab.getName(), null, tab, null );
   }

   /**
    * Adds a tab
    *
    * @param sTitle the title of the tab
    * @param tab    the contents of the tab
    */
   public void addTab( String sTitle, WAPropertyTab tab )
   {
      addTab( sTitle, null, tab, null );
   }

   /**
    * Adds a tab
    *
    * @param sTitle       the title of the tab
    * @param icon         the icon of the tab
    * @param tab          the contents of the tab
    * @param sToolTipText the text of the tooltip for the tab
    */
   public void addTab( String sTitle, Icon icon, WAPropertyTab tab, String sToolTipText )
   {
      addBorderToTab( tab );
      tab.setEditable( m_bEditable );

      listenForDataChangesTo( tab );

      m_Tabs.addTab( sTitle, icon, tab, sToolTipText );
   }

   /**
    * Gets the tab at the given index
    *
    * @param  iTab the index of the tab to retrieve
    *
    * @return the tab at the given index
    */
   public WAPropertyTab getTabAt( int iTab )
   {
      return (WAPropertyTab) m_Tabs.getTabAt( iTab );
   }

   /**
    * Gets the selected tab.
    *
    * @return the selectedtab
    */
   public WAPropertyTab getSelectedTab()
   {
      return (WAPropertyTab) m_Tabs.getSelectedTab();
   }

   /**
    * Sets the selected tab.
    *
    * @param tab the tab to be selected.
    */
   public void setSelectedTab( WAPropertyTab tab )
   {
      m_Tabs.setSelectedTab( tab );
   }

   /**
    * Removes the tab at the given index
    *
    * @param iTab the index of the tab to remove
    */
   public void removeTabAt( int iTab )
   {
      removeTab( (WAPropertyTab) m_Tabs.getTabAt( iTab ) );
   }

   /**
    * Removes the given tab
    *
    * @param tab the tab to remove
    */
   public void removeTab( WAPropertyTab tab )
   {
      removeBorderFromTab( tab );

      stopListeningForDataChangesTo( tab );

      m_Tabs.removeTab( tab );
   }

   /**
    * Removes all tabs
    */
   public void removeAllTabs()
   {
      for ( int iTab=m_Tabs.getTabCount()-1; iTab>=0; iTab-- )
         m_Tabs.removeTabAt( iTab );
   }

   /**
    * Gets the number of tabs in the tabbed panel
    *
    * @return int the number of tabs
    */
   public int getTabCount()
   {
      return m_Tabs.getTabCount();
   }

   /**
    * Adds a border to the tab.  The purpose of the border is to provide some
    * space between the tabbed panel and its contents.
    *
    * @param tab the tab to which the border is added
    */
   protected void addBorderToTab( WAPropertyTab tab )
   {
      // create a new border with some white space outside the tab
      // and the tab's old border
      Border brdrOuter = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
//    Border brdrOuter = BorderFactory.createMatteBorder( 5, 5, 5, 5, Color.red );
      Border brdrInner = tab.getBorder();
      tab.setBorder( BorderFactory.createCompoundBorder( brdrOuter, brdrInner ) );
   }

   /**
    * Removes the border added to the tab.
    *
    * @param tab the tab from which the border is removed
    */
   protected void removeBorderFromTab( WAPropertyTab tab )
   {
      // restore the main panel's old border
      CompoundBorder brdrCompound = (CompoundBorder) tab.getBorder();
      tab.setBorder( brdrCompound.getOutsideBorder() );
   }

   //------------------------------------------------------------------------
   // Overrides of WA dialog interface methods
   //------------------------------------------------------------------------

   /**
    * Sets whether the contents of the panel are editable or not.  In this class,
    * the editable attribute is propagated to each tab.
    *
    * @param bEditable true = editable
    */
   public void setEditable( boolean bEditable )
   {
      if (m_bEditable != bEditable)
      {
         super.setEditable( bEditable );

         WAPropertyTab tab;
         int           iTab;
         int           nTabs = m_Tabs.getTabCount();
         for ( iTab=0; iTab<nTabs; iTab++ )
         {
            tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
            tab.setEditable( bEditable);
         }
      }
   }
   
   /**
    * Gets the help class loader for panel.  This method is overridden to get
    * the help class loader for the currently displayed tab.
    *
    * @return the help class loader
    */
   public ClassLoader getHelpClassLoader()
   {
	  return ((WAPropertyTab) m_Tabs.getSelectedTab()).getHelpClassLoader();
   }

   /**
    * Gets the help topic for the currently displayed tab.
    *
    * @return the help topic
    */
   public String getHelpTopic()
   {
      return ((WAPropertyTab) m_Tabs.getSelectedTab()).getHelpTopic();
   }

   /**
    * Gets the help product for the currently displayed tab.  The help product
    * is used to determine the help set to be used.
    *
    * @return the help product
    */
   public String getHelpProduct()
   {
      return ((WAPropertyTab) m_Tabs.getSelectedTab()).getHelpProduct();
   }

   /**
    * Sets the object store is populated on the panel.
    * <p>
    * This implementation sets the object store populated flag on all the tabs.
    *
    * @param bStorePopulated true = object store is populated
    */

   public void setStorePopulated( boolean bStorePopulated )
   {
      WAPropertyTab tab;
      int           iTab;
      int           nTabs = m_Tabs.getTabCount();
      for ( iTab=0; iTab<nTabs; iTab++ )
      {
         tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
         tab.setStorePopulated( bStorePopulated );
      }
   }

   /**
    * Validates data in tabs.  Only the exposed tab is validated under the
    * assumption that all the other tabs have not been shown or were valid when
    * the user selected this tab.
    *
    * @return true = data is valid
    */
   public boolean validateData()
   {
      return ((WAPropertyTab) m_Tabs.getSelectedTab()).validateData();
   }

   /**
    * Populates the panel's object store.  This method may run on a background
    * thread and should not update any visuals or any models used by visuals.
    * <p>
    * This implementation populates the object store on the currently displayed tab.
    */
   public void populateStore()
   {
      WAPropertyTab tab = (WAPropertyTab) m_Tabs.getSelectedTab();
      tab.setHasBeenShown( true );
      if (!tab.isStorePopulated())
      {
         m_bSkipMoveDataToView = true;
         populateStoreWrapper( tab );
      }

      m_cmpSelectedTab = (WAPropertyTab) m_Tabs.getSelectedTab();  // allow future selectionChanged events to do work
   }

   /**
    * Moves data from the panel's object store to its view (the panel).
    * <p>
    * This implementation moves data into the currently displayed tab.
    */
   public void moveDataToView()
   {
      if (m_bSkipMoveDataToView)
      {
         m_bSkipMoveDataToView = false;
         return;
      }

      moveTabDataToView( (WAPropertyTab) m_Tabs.getSelectedTab() );
   }

   /**
    * Moves data from the panel's view (the panel) to its object store.
    * <p>
    * This implementation moves data from the currently displayed tab.
    */
   public void moveDataToStore()
   {
      WAPropertyTab tab = (WAPropertyTab) m_Tabs.getSelectedTab();
      moveTabDataToStore( tab );
      setDataChanged( false );
   }

   /**
    * Updates the workspace's UI components when the panel becomes the active
    * UI component or stops being the active UI component.  This method
    * overrides the superclass's method to deliver the updateWorkspaceUIComponents
    * to the currently visible tab.
    *
    * @param bActive true = the panel is active
    */
   public void updateWorkspaceUIComponents( boolean bActive )
   {
      m_bActive = bActive;
      ((WAPropertyTab) m_Tabs.getSelectedTab()).updateWorkspaceUIComponents( bActive );
   }

   /**
    * Saves current settings of a panel and cleans up the panel before
    * termination.  This method is called after the user has completed use of
    * the panel.  Therefore, the panel should save any settings it uses as
    * defaults (such as the size of columns in a table and which columns are
    * shown).  The panel should also remove any UI components it has added to
    * the workspace frame such as hidden menu items.  This implementation calls
    * saveSettingsAndCleanUp on any tab that has been shown.
    */
   public void saveSettingsAndCleanUp()
   {
      WAPropertyTab tab;
      int           iTab;
      int           nTabs = m_Tabs.getTabCount();
      for ( iTab=0; iTab<nTabs; iTab++ )
      {
         tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
//         if (tab.hasBeenShown())
            tab.saveSettingsAndCleanUp();
      }
   }

   /**
    * Gets the current undo manager from the tabbed panel.  The current undo
    * manager is the undo manager of the current tab.
    *
    * @return the undo manager of the current tab
    */
   public WAUndoManager getWAUndoManager()
   {
      return ((WAPropertyTab) m_Tabs.getSelectedTab()).getWAUndoManager();
   }


   /**
    * Wrapper populateStore with OpRequestUI.
    *
    * @param tab the property tab
    *
    */
   protected void populateStoreWrapper( WAPropertyTab tab )
   {
      SwingUtilities.getWindowAncestor( this ).setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
      OpRequestUI opRequestUI = new OpRequestUI( Workspace.getWorkspace(),
                                                 bundle.getString( "WAPropertyTab.Label.Status" ),
                                                 bundle.getString( "WAPropertyTab.Title"        ),
                                                 bundle.getString( "WAPropertyTab.Label.Cancel" ) );
      opRequestUI.setDelayBeforeProgressCancelDialog( 1000 );  //wait before showing dialog
      opRequestUI.execute( createPopulateStoreRunnable( tab ), createMoveDataToViewRunnable( tab ) );
   }

   /**
    * Moves tab's data to view.  This is a convenience and should not be
    * overridden by subclasses.
    *
    * @param tab the property tab
    */
   protected void moveTabDataToView( WAPropertyTab tab )
   {
      Component cmp = SwingUtilities.getWindowAncestor(this);
      if (cmp != null)
         cmp.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

      try
      {
         setListeningForChanges( false );
         tab.moveDataToView();
         setListeningForChanges( true  );
      }
      catch (MdException e)
      {
         MessageUtil.displayMetadataExceptionMessage( e, MessageUtil.READING );
      }
      catch ( RemoteException re )
      {
         Workspace.handleRemoteException( re );
      }
      finally
      {
         if (cmp != null)
            cmp.setCursor( Cursor.getDefaultCursor() );
      }
   }

   /**
    * Moves tab's data to store.  This is a convenience and should not be
    * overridden by subclasses.
    *
    * @param tab the property tab
    */
   protected void moveTabDataToStore( WAPropertyTab tab )
   {
      Component cmp = SwingUtilities.getWindowAncestor(this);
      if (cmp != null)
         cmp.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

      try
      {
         tab.moveDataToStore();
      }
      catch (MdException e)
      {
         MessageUtil.displayMetadataExceptionMessage( e, MessageUtil.ACCESSING );
      }
      catch ( RemoteException re )
      {
         Workspace.handleRemoteException( re );
      }
      finally
      {
         if (cmp != null)
            cmp.setCursor( Cursor.getDefaultCursor() );
      }
   }

   /**
    * Indicates whether the panel can close by calling <code>canClose</code> on each of its
    * child tabs.  If a child tab indicates it cannot be closed, then this tabbed panel cannot
    * be closed, and will return <code>false</code>.  If all child tabs can be closed, this
    * method will return <code>true</code>.
    *
    * @return <code>true</code> if the panel's child tabs can be closed,
    *          <code>false</code> if one of the panel's child tabs cannot be closed..
    */
   public boolean canClose()
   {
      WAPropertyTab tab;
      int           iTab;
      int           nTabs = m_Tabs.getTabCount();

      for ( iTab = 0; iTab < nTabs; iTab++ )
      {
         tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
         if (!tab.canClose())
            return false;
      }

      return true;
   }

   /**
     * Called when parent's OK button is pressed.  Calls onParentOK on
    * all contained tabs.
    */
   public void onParentOK()
   {
      for ( int iTab=0; iTab<m_Tabs.getTabCount(); iTab++ )
      {
         WAPropertyTab tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
         tab.onParentOK();
      }
   }

   /**
c    * all contained tabs.
    */
   public void onParentApply()
   {
      for ( int iTab=0; iTab<m_Tabs.getTabCount(); iTab++ )
      {
         WAPropertyTab tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
         tab.onParentApply();
      }
   }

   /**
    * Called when parent's Cancel button is pressed.  Calls onParentCancel on
    * all contained tabs.
    */
   public void onParentCancel()
   {
      for ( int iTab=0; iTab<m_Tabs.getTabCount(); iTab++ )
      {
         WAPropertyTab tab = (WAPropertyTab) m_Tabs.getTabAt( iTab );
         tab.onParentCancel();
      }
   }

   //---------------------------------------------------------------------------
   // listener creators
   //---------------------------------------------------------------------------

   /**
    * Creates the data changed listener.
    *
    * @return the data changed listener
    */
   protected PropertyChangeListener createDataChangedListener()
   {
      return new cDataChangedListener();
   }

   /**
    * Creates the selected tab changed listener.
    *
    * @return the selected tab changed listener
    */
   protected ChangeListener createTabChangedListener()
   {
      return new cTabChangedListener();
   }

   //---------------------------------------------------------------------------
   // listener classes
   //---------------------------------------------------------------------------

   /**
    * cTabChangedListener listens for changes to the selected tab.
    */
   protected class cTabChangedListener implements ChangeListener
   {
      /**
       * Handles the tab selection changed event.  If the previous selected tab
       * has valid data, moveDataToStore is called on the previous tab to save
       * any changes, populateStore is called (if necessary) on the newly selected
       * tab, and moveDataToView is called on the newly selected tab.  If the
       * previous selected tab has invalid data, sets the selected tab back to
       * the previous selected tab.
       *
       * @param e the change event
       */
      public void stateChanged( ChangeEvent e )
      {
         // if no tab has ever been selected, then populateStore and
         // moveDataToStore has not been run the first time.  do not handle
         // selection change events until populateStore and moveDataToStore run
         // the first time
         if (m_cmpSelectedTab == null)
            return;

         // if newly selected tab is the same as the old selected tab,
         // then a SelectedTabChange was aborted because of invalid data.
         // therefore, do nothing
         WAPropertyTab cmpNewSelectedTab = (WAPropertyTab) m_Tabs.getSelectedTab();
         if (m_cmpSelectedTab == cmpNewSelectedTab)
            return;

         // if the tab is being set to null, something is going on that this
         // component doesn't care about.  so ignore it
         if (cmpNewSelectedTab == null)
            return;

         // make sure the previously selected tab has valid data
         // so that we can switch to the new tab
         // also tell tab to remove its workspace UI components
         if (!m_bUndoing)
         {
            if (!m_cmpSelectedTab.validateData())
            {
               m_Tabs.setSelectedTab( m_cmpSelectedTab );
               return;
            }
            moveTabDataToStore( m_cmpSelectedTab );
         }
         if (m_bActive)
            m_cmpSelectedTab.updateWorkspaceUIComponents( false );

         if ((m_mgrUndo != null) && !m_bUndoing)
            m_mgrUndo.addEdit( new ChangeTabUndoable( m_cmpSelectedTab, cmpNewSelectedTab ) );

         // previously selected tab had valid data,
         // so tell new tab to add its workspace UI components
         // and move data from model into new tab if it hasn't been shown
         m_cmpSelectedTab = cmpNewSelectedTab;

         m_cmpSelectedTab.setHasBeenShown( true );
         if (m_bActive)
            m_cmpSelectedTab.updateWorkspaceUIComponents( true );
         if (!m_bUndoing)
         {
            if (!m_cmpSelectedTab.isStorePopulated())
               populateStoreWrapper( m_cmpSelectedTab );
            else
               moveTabDataToView( m_cmpSelectedTab );
         }

//         getDefaultFocusComponent().requestFocus();
      }
   } // cTabChangedListener

   /**
    * cDataChangedListener listens for data changed events from the contained
    * tabs.
    */
   protected class cDataChangedListener implements PropertyChangeListener
   {
      /**
       * Handles the data-changed property change event by setting the data
       * changed property appropriately on the tabbed pane
       *
       * @param e the property change event
       */
      public void propertyChange( PropertyChangeEvent e )
      {
         // if the new property value is true, data has changed somewhere
         // so set the data changed property to true.
         if (((Boolean) e.getNewValue()).booleanValue())
            setDataChanged( true );
      }
   } // cDataChangedListener

   //---------------------------------------------------------------------------
   // runnable creators
   //---------------------------------------------------------------------------

   /**
    * Creates a runnable to populate the tab's object store.
    *
    * @param tab the tab that needs to populate its object store
    *
    * @return the runnable that populates a tab's object store
    */
   protected Runnable createPopulateStoreRunnable( WAPropertyTab tab )
   {
      return new cPopulateStoreRunnable( tab );
   }

   /**
    * Creates a runnable to move data from the object store to the view.
    *
    * @param tab the tab that needs to move data from its object store to the tab
    *
    * @return the runnable that moves data from the object store to the view
    */
   protected Runnable createMoveDataToViewRunnable( WAPropertyTab tab )
   {
      return new cMoveDataToViewRunnable( tab );
   }

   //---------------------------------------------------------------------------
   // runnable classes
   //---------------------------------------------------------------------------
   /**
    * cPopulateStoreRunnable is a runnable that will populate the property tab's
    * object store.
    */
   protected class cPopulateStoreRunnable implements Runnable
   {
      WAPropertyTab m_tab;

      /**
       * Constructs the runnable for the given tab.
       *
       * @param tab the tab that needs to populate the object store.
       */
      protected cPopulateStoreRunnable( WAPropertyTab tab )
      {
         m_tab = tab;
      }

      /**
       * Runs the populate store on the tab.
       */
      public void run()
      {
         try
         {
            setListeningForChanges( false );
            m_tab.populateStore();
            setListeningForChanges( true  );
         }
         catch (MdException e)
         {
            MessageUtil.displayMetadataExceptionMessage( e, MessageUtil.READING );
         }
         catch ( RemoteException re )
         {
            Workspace.handleRemoteException( re );
         }
      }
   } // cPopulateStoreRunnable

   /**
    * cMoveDataToViewRunnable is a runnable that will move data from the
    * property tab's object store to the property tab.
    */
   protected class cMoveDataToViewRunnable implements Runnable
   {
      WAPropertyTab m_tab;

      /**
       * Constructs the runnable for the given tab.
       *
       * @param tab the tab that moves data from the object store to the
       * property tab.
       */
      protected cMoveDataToViewRunnable( WAPropertyTab tab )
      {
         m_tab = tab;
      }

      /**
       * Runs the move data to view on the tab.
       */
      public void run()
      {
         moveTabDataToView( m_tab );
      }
   } // cRunMoveDataToView

   //---------------------------------------------------------------------------
   // The tabbed pane class
   //---------------------------------------------------------------------------
   /**
    * cTabbedPane is a JTabbedPane that implements the WsTabbedInterface.  The
    * purpose of the WsTabbedInterface is to make it so that WATabbedPanel doesn't
    * care whether it contains JTabbedPane or a WsSplittableTabbedPane2.
    */
   protected class cTabbedPane extends JTabbedPane implements WsTabbedInterface
   {
      /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
       * Gets the component that is implementing the interface.
       *
       * @return the component
       */
      public JComponent getComponent()
      {
         return this;
      }

      /**
       * Gets the selected tab (component).
       *
       * @return the component that is the selected tab
       */
      public Component getSelectedTab()
      {
         return getSelectedComponent();
      }

      /**
       * Sets the selected tab.
       *
       * @param cmpTab the tab's content (component)
       */
      public void setSelectedTab( Component cmpTab )
      {
         setSelectedComponent( cmpTab );
      }

      /**
       * Gets the tab (component) at the given index.
       *
       * @param iTab the tab's index
       *
       * @return the tab (component) at the given index.
       */
      public Component getTabAt( int iTab )
      {
         return getComponentAt( iTab );
      }

      /**
       * Removes the tab from the tabbed pane.
       *
       * @param cmpTab the tab's content (component)
       */
      public void removeTab( Component cmpTab )
      {
         remove( cmpTab );
      }
   } // cTabbedPane

   //---------------------------------------------------------------------------
   // Undoables
   //---------------------------------------------------------------------------
   /**
    * ChangeTabUndoable is an undoable that changes the tab back to the previous
    * tab selected.  It is not an significant undoable so it does not enable
    * undo or redo.  It simply gets executed in the path of undos and redos.
    */
   private class ChangeTabUndoable extends AbstractUndoableEdit
   {
      private static final long serialVersionUID = 1L;
      
      private WAPropertyTab m_tabOld;
      private WAPropertyTab m_tabNew;

      /**
       * Constructs the change tab undoable.
       * 
       * @param tabOld the old tab
       * @param tabNew the new tab
       */
      public ChangeTabUndoable( WAPropertyTab tabOld, WAPropertyTab tabNew )
      {
         m_tabOld = tabOld;
         m_tabNew = tabNew;
      }
      
      /**
       * Undoes the tab change.
       * 
       * @see javax.swing.undo.AbstractUndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         m_bUndoing = true;
         try
         {
            setSelectedTab( m_tabOld );
         }
         finally
         {
            m_bUndoing = false;
         }
      }
      
      /**
       * Redoes the tab change.
       * 
       * @see javax.swing.undo.AbstractUndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         m_bUndoing = true;
         try
         {
            setSelectedTab( m_tabNew );
         }
         finally
         {
            m_bUndoing = false;
         }
      }
      
      /**
       * Is the undoable edit significant?  Overridden to indicate that the tab
       * change is not a significant undoable.
       * 
       * @return false = not signficant
       * 
       * @see javax.swing.undo.AbstractUndoableEdit#isSignificant()
       */
      public boolean isSignificant()
      {
         return false;
      }
   } // ChangeTabUndoable
}

