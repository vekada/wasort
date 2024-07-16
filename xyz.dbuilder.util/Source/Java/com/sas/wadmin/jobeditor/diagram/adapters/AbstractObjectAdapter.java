/* $Id$ */
/**
 * Title:       AbstractObjectAdapter.java
 * Description: An abstract default implementation of a model object adapter
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters;

import java.awt.Window;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import com.sas.etl.models.IComplexPersistableObject;
import com.sas.etl.models.IExtendedAttributesContainer;
import com.sas.etl.models.IObject;
import com.sas.etl.models.IPersistableObject;
import com.sas.etl.models.impl.BaseModel;
import com.sas.metadata.remote.Root;
import com.sas.services.ServiceException;
import com.sas.wadmin.app.AppPropertiesDialogFactory;
import com.sas.wadmin.visuals.common.ExtendedAttributesTab;
import com.sas.wadmin.visuals.common.ModelUIUtilities;
import com.sas.workspace.MessageUtil;
import com.sas.workspace.WAPropertyTab;
import com.sas.workspace.WAdminResource;
import com.sas.workspace.Workspace;
import com.sas.workspace.WsDecoratableIcon;
import com.sas.workspace.plugins.PluginViewInterface;

/**
 * AbstractObjectAdapter is an abstract default implementation of a model object
 * adapter.  This implementation returns the name and description of the object
 * to be used to represent the object.
 */
public abstract class AbstractObjectAdapter implements IDiagramNodeAdapter
{
   private static final WAdminResource bundle = WAdminResource.getBundle( AbstractObjectAdapter.class );
   
   private static final String ICON_KEY_SMALL  = "AbstractObjectAdapter.DefaultIcon.16.image";
   private static final String ICON_KEY_MEDIUM = "AbstractObjectAdapter.DefaultIcon.24.image";
   
   private static final Icon ICON_SMALL  = bundle.getIcon( ICON_KEY_SMALL  );
   private static final Icon ICON_MEDIUM = bundle.getIcon( ICON_KEY_MEDIUM );
   
   private static final URL ICON_URL_SMALL  = bundle.getIconURL( ICON_KEY_SMALL  );
   private static final URL ICON_URL_MEDIUM = bundle.getIconURL( ICON_KEY_MEDIUM );
   
   private IComplexPersistableObject   m_object;
   private Action                      m_actDefault;
   private Action                      m_actDelete;
   private WsDecoratableIcon           m_icon;
   
   //save off the types of indicators and the indicators already on the transform
   private Map m_hIndicators;
   
   /**
    * Constructs an abstract implementation of an adapter for a model object.
    * 
    * @param object the model object.
    */
   public AbstractObjectAdapter( IComplexPersistableObject object )
   {
      m_object = object;
      m_icon  = new WsDecoratableIcon();
      m_hIndicators = new HashMap();
   }

   /**
    * Gets the model object.
    * 
    * @return the model object
    * 
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getObject()
    */
   public IObject getObject()
   {
      return m_object;
   }

   /**
    * Gets the name to be used to represent the model object.
    * 
    * @return the name of the model object
    */
   public String getName()
   {
      return m_object.getName();
   }

   /**
    * Gets the description to be used to represent the model object.
    * 
    * @return the description of the model object
    */
   public String getDescription()
   {
      return m_object.getDescription();
   }

   /**
    * Returns the title text.
    * 
    * @return the title text
    */
   public String getTitleText()
   {
	   return m_object.getName();
   }
   
   /**
    * Returns the tooltip text.
    * 
    * @return the tooltip text
    */
   public String getToolTipText()
   {
       return m_object.getName();
   }
   
   /**
    * Gets the decorated icon used to represent the model object in a specific size.  Objects with
    * icons of multiple sizes must override this method to specify the available icon sizes;
    * ICON_SIZE_SMALL (16x16) and ICON_SIZE_MEDIUM (24x24)-sized icons should
    * be provided.
    * @param size the size of the icon, in pixels
    * @return the decorated icon
    */
   public Icon getDecoratedIcon( int size )
   {      
      m_icon.setIcon(getIcon( size ));
      try
      {
         DiagramNodeDecorator.getInstance().decorateIcon(m_icon, m_object);
      }
      catch ( ServiceException ex )
      {
         MessageUtil.displayMessage( ex.getLocalizedMessage(), MessageUtil.ERROR_MESSAGE );
      }
      catch ( RemoteException ex )
      {
         Workspace.handleRemoteException( ex );
      }

      return m_icon;
   }
   
   /**
    * Get the icon used to represent the model object.  By default, the 16x16
    * icon is returned.
    * @return the icon
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getIcon()
    */
   public Icon getIcon()
   {
      return getIcon( ICON_SIZE_SMALL );
   }
   
   /**
    * Gets the icon used to represent the model object in a specific size.  If
    * an icon of the specified size cannot be found, an
    * IllegalArgumentException is thrown.  Objects with icons of multiple
    * sizes must override this method to specify the available icon sizes;
    * ICON_SIZE_SMALL (16x16) and ICON_SIZE_MEDIUM (24x24)-sized icons should
    * be provided.
    * @param size the size of the icon, in pixels
    * @return the icon
    */
   public Icon getIcon( int size )
   {
      if( size == ICON_SIZE_SMALL )
         return ICON_SMALL;
      else if( size == ICON_SIZE_MEDIUM )
         return ICON_MEDIUM;
      
      throw new IllegalArgumentException( "unable to load icon of size " + size + " for " + getName() ); /*I18nOK:LINE*/
   }
   
   /**
    * Get the URL for the icon.  By default, this method returns the URL for the
    * 16x16 icon.
    * @return the icon URL
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getIconURL()
    */
   public URL getIconURL()
   {
      return getIconURL( ICON_SIZE_SMALL );
   }
   
   /**
    * Get the URL for the icon of a specified size.  If the URL for the icon
    * of the specified size cannot be found, an IllegalArgumentException is
    * thrown.  The default is the 16x16 document icon URL; objects with icons
    * must override this method, and multiple icon sizes must be specified
    * manually as well.  ICON_SIZE_SMALL (16x16) and ICON_SIZE_MEDIUM
    * (24x24)-sized icons should be provided.
    * @param size the size of the icon, in pixels
    * @return the icon URL
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getIconURL(int)
    */
   public URL getIconURL( int size )
   {
      if( size == ICON_SIZE_SMALL )
         return ICON_URL_SMALL;
      else if( size == ICON_SIZE_MEDIUM )
         return ICON_URL_MEDIUM;      
      
      throw new IllegalArgumentException( "unable to load icon URL of size " + size + " for " + getName() ); /*I18nOK:LINE*/
   }
   
   /**
    * Gets the map of indicators for this adapter.
    * 
    * @return the map of indicators for this adapter.
    */
   public Map getIndicatorsMap()
   {
      return m_hIndicators;
   }
   
    /**
     * Sets the default action.
     * 
     * @param actDefault the default action
     * 
     * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#setDefaultAction(javax.swing.Action)
     */
    public void setDefaultAction( Action actDefault )
    {
       m_actDefault = actDefault;
    }

    /**
     * Gets the default action.
     * 
     * @return the default action
     * 
     * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#getDefaultAction()
     */
    public Action getDefaultAction()
    {
       return m_actDefault;
    }
    
    /**
     * Sets the delete action.
     * 
     * @param actDelete the delete action
     * 
     * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#setDeleteAction(javax.swing.Action)
     */
    public void setDeleteAction( Action actDelete )
    {
       m_actDelete = actDelete;
    }
    
    /**
     * Gets the delete action.
     * 
     * @return the delete action
     * 
     * @see com.sas.workspace.visuals.pfd.WsPFDPrimitive#getDeleteAction()
     */
    public Action getDeleteAction()
    {
       return m_actDelete;
    }
    
    /**
     * Force an auto layout on the view. 
     * @return true to force layout
     */
    public boolean isLayoutRequired()
    {
    	return false;
    }

    /**
     * Get a single message for link tooltip on invalid connect of output 
     * @param obj object that tried to connect to output.
     * @param portIndex the index of the output port connecting from, null if link connected to node
     * @return null no message
     */
    public String getInvalidConnectOutputMessage(IObject obj, int portIndex )
    {
       return null;
    }
    
    /**
     * Get a single message for link tooltip on invalid connect of input 
     * @param obj object that tried to connect to input.
     * @param portIndex the index of the input port connecting to, null if link connected to node
     * @return null no message
     */
    public String getInvalidConnectInputMessage(IObject obj, int portIndex)
    {
       return null;
    }
    
    /**
     * Is object valid to connect on redraw of diagram. 
     * @param obj object to check
     * @return true = valid to connect object on redraw of diagram
     */
    public boolean isValidToConnectInputOnRedraw(IObject obj)
    {
    	return true;
    }
    
    /**
     * Is object valid to connect on redraw of diagram. 
     * @param obj object to check
     * @return true = valid to connect object on redraw of diagram
     */
    public boolean isValidToConnectOutputOnRedraw(IObject obj)
    {
      return true;
    }
    
    /**
     * Shows the properties dialog for the object represented by the adapter.
     * Override this method to change how the property dialog is created and
     * shown.
     * 
    * @param wdwParent the parent window for the dialog.  The parent window must
    *                  be either a java.awt.Dialog or a java.awt.Frame.
    *                  
    * @return WAStandardDialog.OK = the user OK'd the dialog, 
    *         WAStandardDialog.CANCEL = the user canceled the dialog, or 
    *         -1 = an error occurred before the dialog was displayed.
    *         
     * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#showPropertiesDialog(java.awt.Window)
     */
    public int showPropertiesDialog( Window wdwParent )
    {
       WAPropertyTab[] aTabs = getPropertyTabs();
       if (aTabs == null)
          return -1;  // something bad happened creating the tabs, so punt

       return ModelUIUtilities.showPropertiesDialog( m_object, aTabs );
    }
    
    /**
     * Gets the tabs used in the properties dialog.
     * 
     * @return the tabs used in the properties dialog (null = an error occurred
     *         during the creation of the tabs)
     */
    protected abstract WAPropertyTab[] getPropertyTabs();

    /**
     * Adds the advanced tabs to the list of tabs if the user's preference is to
     * show advanced tabs.
     * 
     * @param lTabs                 the list of tabs
     * @param obj                   the object
     * @param bAddAuthorizationTab  true = add authorization tab
     */
    protected void addAdvancedTabs( List lTabs, IPersistableObject obj, boolean bAddAuthorizationTab )
    {
       if (!AppPropertiesDialogFactory.isUserPreferenceToShowAdvancedTabs())
          return;
       
       if (obj instanceof IExtendedAttributesContainer)
          lTabs.add( new ExtendedAttributesTab( (IExtendedAttributesContainer) obj ) );
    
       if (bAddAuthorizationTab)
          addAuthorizationPluginTab( lTabs, obj );
    }

    /**
     * Adds the authorization tab to the list of tabs if the plugin is present.
     * This method is written as if there were the possibility of multiple 
     * authorization plugins and they could have more than one tab.  In reality,
     * there is one plugin and it has one tab.
     *  
     * @param lTabs the list of tabs
     * @param obj   the object for which the authorization is to be set 
     */
    private void addAuthorizationPluginTab( List lTabs, IPersistableObject obj )
    {
       try
       {
          Root mdo = ((BaseModel) obj.getModel()).getOMRObject( obj, Workspace.getMdFactory() );

          // for all the plugins that support the AuthorizationTabPluginInterface, ..
          List lPlugins = Workspace.getWorkspace().getPluginLoader().getPlugins( "com.sas.wadmin.plugins.authtab.AuthorizationTabPluginInterface", true );
          for ( int iPlugin=0; iPlugin<lPlugins.size(); iPlugin++ )
          {
             // get the plugin's property tabs for the metadata object
             PluginViewInterface plugin = (PluginViewInterface) lPlugins.get( iPlugin );
             List lPluginTabs = plugin.getPropertyTabs( mdo );
             if (lPluginTabs == null)
                continue;

             for ( int iTab = 0; iTab < lPluginTabs.size(); iTab++ )
                lTabs.add( lPluginTabs.get( iTab ) ); 
          }
       }
       catch (RemoteException ex)
       {
          Workspace.getDefaultLogger().error( "", ex );
       }
    }
    
    /**
     * get the indicator buttons used on this transform
     * 
     * @return the list of indicator buttons used on this transform
     * 
     */
    public List getIndicatorButtons()
    {
       return null;
    }
}

