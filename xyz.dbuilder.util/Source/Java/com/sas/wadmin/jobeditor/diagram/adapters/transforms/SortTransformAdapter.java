/* $Id$ */
/**
 * Title:       SortTransformAdapter.java
 * Description: The adapter for the sort transform.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.wadmin.jobeditor.diagram.adapters.transforms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sas.etl.models.job.transforms.SortTransformModel;
import com.sas.metadata.remote.MdException;
import com.sas.services.ServiceException;
import com.sas.storage.exception.ServerConnectionException;
import com.sas.wadmin.app.AppIconFactory;
import com.sas.wadmin.transforms.sort.SASSortByTab;
import com.sas.wadmin.transforms.sort.SortColsByTreeModel;
import com.sas.wadmin.visuals.common.GeneralTab;
import com.sas.wadmin.visuals.common.MappingsTab;
import com.sas.wadmin.visuals.common.MappingsTab.Configuration;
import com.sas.wadmin.visuals.common.NotesTab;
import com.sas.wadmin.visuals.common.OptionsTab;
import com.sas.wadmin.visuals.common.ParameterTab;
import com.sas.wadmin.visuals.common.PrePostCodeTab;
import com.sas.wadmin.visuals.common.ProcessTab;
import com.sas.wadmin.visuals.common.TransformTableOptionsTab;
import com.sas.wadmin.visuals.common.TargetColumnsTableModel;
import com.sas.workspace.MessageUtil;
import com.sas.workspace.WAPropertyTab;

/**
 * SortTransformAdapter is the adapter for the sort transform.
 */
public class SortTransformAdapter extends AbstractDataTransformAdapter
{
   private static final Icon ICON_SMALL      = AppIconFactory.getAppInstance().getIconForTransformation(    SortTransformModel.getTransformTypeID(), AppIconFactory.SIZE_16 );
   private static final Icon ICON_MEDIUM     = AppIconFactory.getAppInstance().getIconForTransformation(    SortTransformModel.getTransformTypeID(), AppIconFactory.SIZE_24 );

   private SortTransformModel m_sort;

   /**
    * Constructs the sort transform adapter.
    *
    * @param transform the sort transform
    */
   public SortTransformAdapter( SortTransformModel transform )
   {
      super( transform );
      m_sort = transform;
   }


   /**
    * Gets the tabs to be used in a properties dialog.
    *
    * @return the tabs
    *
    * @see com.sas.wadmin.jobeditor.diagram.adapters.IDiagramNodeAdapter#getPropertyTabs()
    */
   public WAPropertyTab[] getPropertyTabs()
   {
      try
      {
         // create the options tab first because it may throw an exception
         // if it does throw an exception, punt
         OptionsTab tabOptions = new OptionsTab( m_sort.getOptionModel(), "sort_options", false );

         TransformTableOptionsTab tableOptionsTab = new TransformTableOptionsTab( m_sort, null, false);

         // the other tabs must be created after the options tab because they
         // add listeners to the model and they must be cleaned up
         List lTabs = new ArrayList();
         lTabs.add(new GeneralTab(            m_sort )           );
         lTabs.add(new SASSortByTab(          m_sort, SortColsByTreeModel.USE_TARGET_TABLE ));
         lTabs.add(createMappingsTab(false)                      );
         lTabs.add(tabOptions                                    );
         lTabs.add(tableOptionsTab                               );
         lTabs.add(new ProcessTab(            m_sort, true, true));
         lTabs.add(new PrePostCodeTab(        m_sort)            );
         lTabs.add(new ParameterTab(          m_sort)            );
         lTabs.add(new NotesTab(              m_sort )           );

         addAdvancedTabs( lTabs, m_sort, false );

         return (WAPropertyTab[]) lTabs.toArray( new WAPropertyTab[ lTabs.size() ] );
      }
      catch (ServiceException e)
      {
         MessageUtil.displayServiceExceptionMessage(e);
      }
      catch (ServerConnectionException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }
      catch (MdException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }
      catch (FileNotFoundException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }
      catch (IOException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }
      catch (ParserConfigurationException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }
      catch (SAXException e)
      {
         MessageUtil.displayMessage(e.getLocalizedMessage());
      }

      // an exception was thrown by OptionsTab construction, so return no tabs
      return null;
   }

   protected WAPropertyTab createMappingsTab(boolean details)
   {
      Configuration config = new Configuration();
      config.setExpressionActionsVisible( false );
      config.setHiddenColumnsInTargetColumnsTable( new int[] { TargetColumnsTableModel.EXPRESSION_COLUMN,
                                                               TargetColumnsTableModel.TABLENAME_COLUMN,
                                                               TargetColumnsTableModel.TABLEDESCR_COLUMN } );
      config.setWhereUsedSettingsKeyPrefix( details ? "Details" : "Properties" );
      config.setTargetTableSettingsKeyPrefix("Sort.TargetColumnsTable");
      return new MappingsTab( m_sort, config );
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
}

