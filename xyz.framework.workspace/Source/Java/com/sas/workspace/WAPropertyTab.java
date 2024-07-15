/* $Id: WAPropertyTab.java,v 1.1.2.4 2007/06/21 14:09:40 sassem Exp $ */
/**
 * Title:        WAPropertyTab
 * Description:  This class is used to create custom property tabs and wizard panels.
 * Copyright:    Copyright (c) 2001
 * Company:      SAS Institute
 * Author:       Russ Robison
 * Version:      1.0
 */

package com.sas.workspace;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * This class is used to create custom property tabs and wizard panels.  This
 * class is derived from WAPanel.  The added support for property tabs is:
 * <ul>
 * <li>The <i>hasBeenShown</i> property which keeps track of whether the tab has
 * been shown.  This property really keeps track of whether <i>doDataExchange(false)</i>
 * has been called on the tab.</li>
 * </ul>
 * <p>
 * The added support for wizard panels is :
 * <ul>
 * <li>The <i>conditionState</i> property which is really a transition state
 * value used by the WATransitionWizardModel to determine which wizard panel
 * to move to when the Next button is pressed.</li>
 * <li>The <i>onNext</i> event handler method that is called on the currently
 * displayed tab when the Next button is pressed.  This method is called after
 * the <i>validateData</i> method is called and before the current tab is
 * changed.</li>
 * <li>The <i>onBack</i> event handler method that is called on the currently
 * displayed tab when the Back button is pressed.  This method is called before
 * the current tab is changed.</li>
 * </ul>
 */

public class WAPropertyTab extends WAPanel
   {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/** tab is an introduction wizard page */
   public static final int INTRODUCTION_PAGE = 0;
   
   /** tab is a content wizard page */
   public static final int CONTENT_PAGE = 1;
   
   /** tab is a summary wizard page */
   public static final int SUMMARY_PAGE = 2;

   /** Flag to know if the data has ever been gotten and displayed. */
   protected boolean m_bHasBeenShown = false;
   private   int     m_ePageType = CONTENT_PAGE; 

   // This field is used for transition between wizard panels
   protected String  m_conditionState = "NEXT";

   /**
    * Creates an empty tab.
    */
   public WAPropertyTab()
   {
      super();
   }

   /**
    * Creates an empty tab with a label.  This constructor is intended as a 
    * place holder for development purposes only.
    * 
    * @param sLabel the label text
    * 
    * @deprecated
    */
   public WAPropertyTab( String sLabel )
   {
      super();
      add( new JLabel( sLabel ) );
      setName( sLabel );
   }

   //---------------------------------------------------------------------------
   // Attribute accessors
   //---------------------------------------------------------------------------

   /**
    * Gets whether or not the tab has been shown.
    * <p>
    * Notes: hasBeenShown is really misnamed because the property is really has
    *        data been exchanged into the tab.  This property is actually
    *        controlled from outside of the class.
    *
    * @return true  - tab has been shown,
    *         false - tab has not been shown
    *
    * @see #setHasBeenShown
    */
   public boolean hasBeenShown()
   {
      return m_bHasBeenShown;
   }

   /**
    * Sets whether or not the tab has been shown.
    * <p>
    * Notes: hasBeenShown is really misnamed because the property is really has
    *        data been exchanged into the tab.  This property is actually
    *        controlled from outside of the class.
    *
    * @param bHasBeenShown true  - tab has been shown,
    *                      false - tab has not been shown
    *
    * @see #hasBeenShown
    */
   public void setHasBeenShown( boolean bHasBeenShown )
   {
      m_bHasBeenShown = bHasBeenShown;
   }

   /**
    * Returns the current state of the wizard condition (transition) state.
    * It is up to the property tab to set this condition so that the wizard
    * model knows which tab to show next when this property tab is used as a
    * wizard panel.
    *
    * @return the current condition (transition) state value
    */
   public String getConditionState()
   {
      return m_conditionState;
   }

   /**
    * Sets the wizard condition (transition) state.  This is the value that the
    * wizard model will use to transition between wizard panels.
    *
    * @param condition the wizard condition state.
    */
   public void setConditionState( String condition )
   {
      m_conditionState=condition;
   }

   /**
    * Sets the type of wizard page the tab is.
    * 
    * @param ePageType the wizard page type (INTRODUCTION_PAGE, CONTENT_PAGE, 
    *                  SUMMARY_PAGE)
    */
   public void setWizardPageType( int ePageType )
   {
      m_ePageType = ePageType;
   }
   
   /**
    * Gets the type of wizard page the tab is.
    * 
    * @return the wizard page type (INTRODUCTION_PAGE, CONTENT_PAGE, SUMMARY_PAGE)
    */
   public int getWizardPageType()
   {
      return m_ePageType;
   }
   
   /**
    * Gets the title of the tab that will be displayed in the tabbed pane.
    * This method is intended to be used only by plugin developers.
    *
    * @return the title of the tab.
    */
   public String getTitle()
   {
      return null;
   }

   /**
    * Gets the icon of the tab that will be displayed in the tabbed pane.
    * This method is intended to be used only by plugin developers.
    * 
    * @return the icon
    */
   public Icon getIcon()
   {
      return null;
   }

   /**
    * Gets the tooltip text of the tab in the tabbed pane.
    * This method is intended to be used only by plugin developers.
    * 
    * @return the text for the tool tip
    */
   public String getToolTipText()
   {
      return null;
   }

   //---------------------------------------------------------------------------
   // Event handler methods
   //---------------------------------------------------------------------------

   /**
    * Event handler method for the next button being pressed.
    */
   public void onNext()
   {
   }

   /**
    * Event handler method for the back button being pressed.
    */
   public void onBack()
   {
   }

   /**
    * Override this when this tab is used in a wizard to put tab specific information out about
    * what this wizard will create/modify/delete, etc.  This method is called by WAWizardDialog
    * when the last tab in the sequence calls the runFinish method in WAWizardDialog.
    *
    * @return String of specific information for this tab.  Optional formatting characters can be imbedded.
    * The string will be collected and imbedded in a JTextPane.
    */
   public String createFinishString()
   {
     return null;
   }
   
   /**
    * Create a method that will return this class name as the default;
    * Override it if you want a nicer visible name
    * 
    * @return String name of this tab
    */
   public String getTabTypeName()
   {
      return toString();
   }
}
