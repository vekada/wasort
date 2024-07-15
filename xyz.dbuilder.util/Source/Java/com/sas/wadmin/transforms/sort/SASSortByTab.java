/* $Id: SASSortByTab.java,v 1.1.2.11 2008/05/12 16:11:22 rogilb Exp $ */
package com.sas.wadmin.transforms.sort;

/**
 * Title:       SASSortByTab
 * Description: Tab that displays the Sort By Columns for the SASSort transformation
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Venu Kadari
 */

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTree;

import com.sas.etl.models.job.ISortingTransform;
import com.sas.metadata.remote.MdException;
import com.sas.swing.visuals.util.Util;
import com.sas.workspace.WAPropertyTab;
import com.sas.workspace.WAdminResource;
import com.sas.workspace.WsDefaultTreeCellRenderer;
import com.sas.workspace.WsTree;
import com.sas.workspace.WsUIUtilities;
import com.sas.workspace.visuals.dualselector.DefaultSelectorTable;
import com.sas.workspace.visuals.dualselector.TreeTableSelector;
/**
 * SASSortByPropertyTab allows the user to select which table columns to use
 * when sorting.
 */

public class SASSortByTab extends WAPropertyTab
{

   private static WAdminResource bundle = WAdminResource.getBundle( SASSortByTab.class );

   // model object
   private ISortingTransform        m_sortModel;  
   
   // name and description components
   private TreeTableSelector        m_treeTableSelector;
   private WsTree                   m_tree;
   private DefaultSelectorTable     m_defaultSelectorTable;
   
   private SortColsByTreeModel      m_treeModel;
   private SortColsByTableModel     m_tableModel;
   protected int                   m_iUseColsFromTable;
   private String                   m_sTargetPanelTitle;        
  
   private JCheckBox                m_ckbSortColumns;

   /**
    * Constructs a SAS Sort by tab.
    * 
    * @param oSortingTransform sorting transform object
    * @param useColsFromTable indicate whether to use the source or target table to select the sort by columns from
    * 
    */
   public SASSortByTab( ISortingTransform oSortingTransform, int useColsFromTable )
   {
      this(oSortingTransform, useColsFromTable, 
                          bundle.getString( "SASSortByTab.Title.txt" ), 
                          bundle.getString( "SASSortByTab.SelectedColumns.txt"));
   }
   
   /**
    * Constructs a SAS Sort by tab.
    * 
    * @param oSortingTransform sorting transform object
    * @param useColsFromTable indicate whether to use the source or target table to select the sort by columns from
    * @param sTabTitle title of the property tab
    * @param sTargetPanelTitle title that appears on the target panel
    */
   public SASSortByTab( ISortingTransform oSortingTransform, int useColsFromTable, String sTabTitle, String sTargetPanelTitle )
   {
      
      setName( sTabTitle );
      m_sortModel = oSortingTransform;
      m_iUseColsFromTable = useColsFromTable;
      setTargetPanelLabel(sTargetPanelTitle );
      
      initialize();
      layoutWidgets();
      //    activate help button 
      setHelpTopic( "sort_by_columns_tab" );
      setHelpProduct( "wdb");
   }
   
   /**
    * Create and initialize all of the components for this tab.
    */
   protected void initialize() 
   {
      // Create tree of tables/columns from source and target tables
      m_treeModel = new SortColsByTreeModel(m_sortModel, m_iUseColsFromTable);
      m_tree =  new WsTree( m_treeModel);
      
      m_tree.setScrollsOnExpand(       true   );
      m_tree.setPopupMenuEnabled(      false  );
      m_tree.setEditable(              false  );
      m_tree.setRootVisible(           false  );
      
      m_tableModel = new SortColsByTableModel(m_sortModel);
      m_defaultSelectorTable = new DefaultSelectorTable( m_tableModel );  
      m_treeTableSelector = new SortColsByTreeTableSelector( m_tree, m_defaultSelectorTable );
      m_defaultSelectorTable.setRowHeaderVisible( true );

      boolean actionEnabled = isEditable();
 
      // override defaults
      m_treeTableSelector.setCopyModeEnabled( actionEnabled );
      m_treeTableSelector.setCopyModeDuplicatesAllowed( false );
      m_treeTableSelector.setTransferAllControlsVisible( false );
      m_treeTableSelector.setDragAndDropEnabled( true );
      m_treeTableSelector.setParentNodesMoveable( false );
      m_treeTableSelector.setTargetControlsVisible( actionEnabled );

      
      // add an expansion listener so that tree will populate when user expands table node
      m_treeModel.addListenersToTree( (JTree) m_treeTableSelector.getSourceComponent());
      
     //if necessary, modify selector to needed behavior
      m_treeTableSelector.setModel( m_treeModel );
      //     set the renderer - this is the while node including icon
      m_treeTableSelector.setSourceCellRenderer( new WsDefaultTreeCellRenderer() ); 
 
      // Set Table target labels   
      JLabel treeLbl = (JLabel) m_treeTableSelector.getTargetLabelComponent();
      treeLbl.setText( Util.getLabelWithoutMnemonic ( getTargetPanelLabel() ) );
      int targetLabelMnemonicCharacter = Util.getDisplayedMnemonic (getTargetPanelLabel());
      int targetLabelMnemonicIndex = Util.getDisplayedMnemonicIndex (getTargetPanelLabel());
      treeLbl.setDisplayedMnemonic (targetLabelMnemonicCharacter);
      treeLbl.setDisplayedMnemonicIndex (targetLabelMnemonicIndex);

      // Set Tree source labels
      JLabel tblLbl = (JLabel) m_treeTableSelector.getSourceLabelComponent();
      tblLbl.setText( Util.getLabelWithoutMnemonic( bundle.getString( "SASSortByTab.AvailableColumns.txt" ) ) );
      int sourceLabelMnemonicCharacter = Util.getDisplayedMnemonic (bundle.getString ("SASSortByTab.AvailableColumns.txt"));
      int sourceLabelMnemonicIndex = Util.getDisplayedMnemonicIndex (bundle.getString ("SASSortByTab.AvailableColumns.txt"));
      tblLbl.setDisplayedMnemonic (sourceLabelMnemonicCharacter);
      tblLbl.setDisplayedMnemonicIndex (sourceLabelMnemonicIndex);
      
      m_ckbSortColumns = WsUIUtilities.createCheckBox( bundle, "SASSortByTab.Checkbox.SortColumns" );
      m_ckbSortColumns.addActionListener( new cSortColumnsListener() );

   // layout panel 
   // (widths are two to allow other components to be added by subclasses)
      setPreferredSize( new Dimension( 450, 300 ) );
      setLayout( new GridBagLayout() );
      add( m_treeTableSelector,  new GridBagConstraints( 0, 2, 2, 1, 1, 1,     GridBagConstraints.WEST, GridBagConstraints.BOTH,       new Insets( 0, 0, 0, 0 ), 0, 0 ) );
      add( m_ckbSortColumns,     new GridBagConstraints( 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

   }

   //---------------------------------------------------------------------------
   // WAPanel overrides
   //---------------------------------------------------------------------------

   /**
    * Validates the data in the general panel.  Verifies that the name of the
    * object is not blank.  If needed, verifies that the name of the object
    * is unique among objects of its type on the server.
    *
    * @return  true if the panel data is valid
    */
   public boolean validateData()
   {
      return true;
   }

   /**
    * Populates the panel's object store.
    */
   public void populateStore()
   {
   }

   /**
    * Moves data from the panel's object store to its view (the panel).
    * @throws MdException 
    * @throws RemoteException 
    */
   public void moveDataToView() throws RemoteException, MdException
   {
      setListeningForChanges( false );
      m_tableModel.moveDataToModel();
      m_treeModel.moveDataToModel();      
      m_treeModel.expandAll( (JTree) m_treeTableSelector.getSourceComponent(), true );
      setListeningForChanges( true );
   }

   /**
    * Sets the panel's editable attribute.  If the panel is not editable, the
    * text and table components are set to not editable and the buttons and
    * comboboxes are disabled.
    *
    * @param bEditable true = the panel is editable
    */
   public void setEditable( boolean bEditable )
   {
      super.setEditable( bEditable );
      updateComponents();
   }
   
   /**
    * Updates the components enabled/disabled state based on whether the panel
    * is editable or not.
    */   
   private void updateComponents()
   {
      boolean actionEnabled = isEditable();
      m_treeTableSelector.setEnabled(actionEnabled);
      m_defaultSelectorTable.setEditable(actionEnabled);      
   }
   
   /**
    * Save settings and clean up.
    * 
    * @see com.sas.workspace.WAPanel#saveSettingsAndCleanUp()
    */
   public void saveSettingsAndCleanUp()
   {
      m_tableModel.dispose();
   }
  
   //---------------------------------------------------------------------------
   // listener classes
   //---------------------------------------------------------------------------
     
   /**
    * cSortColumnsListener listens to the sort column checkbox.  When the user
    * changes the selection, the listener sets the sort column attribute on the
    * tree model which causes the sort order of the columns to change.
    */
   protected class cSortColumnsListener implements ActionListener
   {
      /**
       * Handles the action performed event by changing the sort order of the 
       * columns on the model.
       * 
       * @param e the action performed event
       */
      public void actionPerformed( ActionEvent e )
      {
         m_treeModel.setSortColumns( m_ckbSortColumns.isSelected() );
      }
   } // cSortColumnListener

   /**
    * Get label that appears as title on the target panel
    * 
    * @return label
    */
   public String getTargetPanelLabel()
   {
      return m_sTargetPanelTitle;
   }

   /**
    * Set label that appears as title on the target panel
    * 
    * @param sTargetPanelLabel title for target panel
    */
   public void setTargetPanelLabel( String sTargetPanelLabel )
   {
      m_sTargetPanelTitle = sTargetPanelLabel;
   }
   
}
