/* $Id: AbstractConditionActionSetContainer.java,v 1.1.2.24.136.1 2021/01/29 00:31:30 sasclw Exp $ */
/**
 * Title:       AbstractConditionActionSetContainer.java
 * Description: An abstract implementation for objects that
 *              will support condition/actions
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Nancy Rausch
 * Support:     Nancy Rausch
 */

package com.sas.etl.models.job.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sas.etl.models.IModel;
import com.sas.etl.models.data.impl.AbstractParametersContainer;
import com.sas.etl.models.impl.ModelList;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.job.IConditionActionSetContainer;
import com.sas.etl.models.job.IConditionActionsTemplateJobModel;
import com.sas.etl.models.other.IConditionActionSet;
import com.sas.metadata.remote.AbstractTransformation;
import com.sas.metadata.remote.ConditionActionSet;
import com.sas.metadata.remote.Job;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MdOMIUtil;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.TransformationActivity;
import com.sas.services.information.util.SmartTypes;
import com.sas.workspace.Workspace;

/**
 * AbstractConditionActionSetTemplatesContainer is an abstract implementation for objects that
 * will support condition/actions.  This is jobs, transforms
 */
public abstract class AbstractConditionActionSetContainer extends AbstractParametersContainer implements IConditionActionSetContainer 
{

   private ModelList m_lConditionActionSets;
   private ArrayList m_lDefinedConditionActionSetUniqueIds;
   private ArrayList m_lTemplateConditionActionSets;

   
   /**
    * Construct the object
    * @param sID
    * @param model
    */
   public AbstractConditionActionSetContainer(String sID, IModel model)
   {
      super(sID, model);
      m_lConditionActionSets = new ModelList( this, new String[]{ CONDITIONACTIONSET_ADDED, CONDITIONACTIONSET_REMOVED }, ModelList.SAVE_AS_OWNER_OF_OBJECTS, IConditionActionSet.class );
      m_lDefinedConditionActionSetUniqueIds = new ArrayList();
      m_lTemplateConditionActionSets = new ArrayList();
   }
   
   
   /**
    * Return list of condition actions sets
    * 
    * @return list
    * @see com.sas.etl.models.job.IConditionActionSetContainer#getConditionActionSetsList()
    */
   public List getConditionActionSetsList()
   {
      return m_lConditionActionSets;
   }
 
   /**
    * Return array of condition actions sets
    * 
    * @return array

    * @see com.sas.etl.models.job.IConditionActionSetContainer#getConditionActionSets()
    */
   public IConditionActionSet[] getConditionActionSets()
   {
      return (IConditionActionSet[]) m_lConditionActionSets.toArray(
            new IConditionActionSet[ m_lConditionActionSets.size() ] );
   }

   /**
    * get the list of template conditionActionSets.
    * @return list of template conditionActionSets.
    */
   public List getTemplateConditionActionSetsList()
   {
      return m_lTemplateConditionActionSets;
   }
   
   /**
    * Return array of template condition actions sets
    * 
    * @return array

    * @see com.sas.etl.models.job.IConditionActionSetContainer#getConditionActionSets()
    */
   public IConditionActionSet[] getTemplateConditionActionSets()
   {
      return (IConditionActionSet[]) m_lTemplateConditionActionSets.toArray(
            new IConditionActionSet[ m_lTemplateConditionActionSets.size() ] );
   }
   
   /**
    * get the list of defined conditionActionSet template unique identifiers required.
    * @return list of defined conditionActionSet template unique ids required.
    */
   public List getDefinedConditionActionSetUniqueIds()
   {
      return m_lDefinedConditionActionSetUniqueIds;
   }
   
   /**
    * Load the conditionActionSet templates, this method is called in the loadFromOMR
    * method, but should also be called from the addDefaultSettings method of any
    * transform implementing condition actions.
    * @throws MdException
    * @throws RemoteException
    */
   public void loadConditionActionSetTemplatesFromOMR() throws MdException, RemoteException
   {
      List lSets = getDefinedConditionActionSetUniqueIds();
      if ( lSets.isEmpty() )
         return;
      
      OMRAdapter omr = getModel().createNewOMRAdapter(
              "loadConditionActionSetTemplatesFromOMR" );
      try
      {
    	 String sUniqueId = null;
         IConditionActionsTemplateJobModel model= null;
         if (getModel() instanceof IConditionActionsTemplateJobModel)
        	model = (IConditionActionsTemplateJobModel)getModel();
         getTemplateConditionActionSetsList().clear();
    	 for (int iTemplateSet = 0; iTemplateSet<lSets.size(); iTemplateSet++)
    	 {	 
            sUniqueId = (String)lSets.get(iTemplateSet);
            if (model != null)
            {
            	IConditionActionSet set = model.getTemplateConditionActionSet(sUniqueId);
            	if (set != null)
            	{	
                   getTemplateConditionActionSetsList().add( set );
                   continue;
            	}   
            }
            String sTemplate = "<XMLSELECT search=\"*[@PublicType=\'" + SmartTypes.TYPE_CONDITIONACTIONSET + "\' and @UniqueIdentifier='" + sUniqueId + "']\"/>" +
             "<Templates>" +
               "<ConditionActionSet>" +
                  "<AssociatedCondition/>" +
                  "<Actions/>" +
                  "<PropertySets/>" +   
               "</ConditionActionSet>" +
               "<Action>" +
                  "<PropertySets/>" +
                  "<Properties/>" +
               "</Action>" +
               "<Condition>" +
                  "<PropertySets/>" +
                  "<Properties/>" +
               "</Condition>" +
               "<PropertySet>" +
                  "<SetProperties/>" +
               "</PropertySet>" +
            "</Templates>";
      
            if (omr.getOMRFactory().getConnection().getCMRHandle()!=null)
            {
            List objects = omr.getOMRFactory().getOMIUtil().getMetadataObjectsSubset(
               omr.getOMRStore(),
               omr.getOMRFactory().getOMIUtil().getFoundationReposID(),
               MetadataObjects.CONDITIONACTIONSET,
               MdOMIUtil.OMI_TEMPLATE |
               MdOMIUtil.OMI_GET_METADATA |
               MdOMIUtil.OMI_ALL_SIMPLE |
               MdOMIUtil.OMI_XMLSELECT |
               MdOMIUtil.OMI_DEPENDENCY_USES,
               sTemplate );
            if ( !objects.isEmpty() )
            {
               for ( int iSet = 0; iSet < objects.size(); iSet++ )
               {
                  ConditionActionSet mdoSet = (ConditionActionSet) objects.get( iSet );
                  for ( int iTempSet = 0; iTempSet < lSets.size(); iTempSet++ )
                  {
                     if ( !mdoSet.getUniqueIdentifier().equals( lSets.get( iTempSet ) ) ||
                        mdoSet.getPublicType().indexOf( "Embedded" ) != -1 ||
                        mdoSet.getPublicType().indexOf( "EMBEDDED" ) != -1 ||
                        mdoSet.getPublicType() == null ||
                        mdoSet.getPublicType() == "" )
                        continue;
                     IConditionActionSet oSet = (IConditionActionSet) getModel().getObject( mdoSet.getId() );
                     if ( oSet == null )
                     {
                        oSet = getModel().getObjectFactory().createConditionActionSetTemplate(
                           mdoSet.getFQID(),
                           mdoSet.getUniqueIdentifier() );
                        omr.populateFor( oSet );
                        oSet.loadFromOMR( omr );
                     }
                     getTemplateConditionActionSetsList().add( oSet );
                     if (model != null)
                     {
                    	 model.addTemplateConditionActionSet(oSet); 
                     }
                     break;
                  }
               }
            }
            }
    	 }
      }
      finally
      {
         omr.dispose();
      }
   }
   
   /**
    * Gets the template map used to populate an OMR adapter for a job.
    * 
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssociations = (List) map.get( getOMRType() );
      lAssociations.add( AbstractTransformation.ASSOCIATION_CONDITIONACTIONSETS_NAME );
      map.put( MetadataObjects.CONDITIONACTIONSET, lAssociations );
      
      return map;
   }
   
   /**
    * Load from omr
    * 
    * @param omr adapter
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.data.impl.AbstractParametersContainer#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
     super.loadFromOMR( omr );
     loadConditionActionSetTemplatesFromOMR();
     getConditionActionSetsList().clear();
      AbstractTransformation mdoTransform = (AbstractTransformation)omr.acquireOMRObject( this ); 
  	  List lSetInstances = mdoTransform.getConditionActionSets(false);  
      for (int iInstance=0; iInstance<lSetInstances.size(); iInstance++)
      {
        ConditionActionSet mdoSet = (ConditionActionSet)lSetInstances.get(iInstance); 
        IConditionActionSet set = (IConditionActionSet)omr.acquireObject(mdoSet);
        if (set != null && getConditionActionSetsList().indexOf(set) == -1)	
           getConditionActionSetsList().add(set);
      }  
      setChanged(false);
   }
    
   /**
    * Saves the object to OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    * 
    * @see com.sas.etl.models.IOMRPersistable#saveToOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (!isChanged())
         return;

      super.saveToOMR( omr );

      for (int i=0; i<getConditionActionSetsList().size(); i++)
      {
         IConditionActionSet set = (IConditionActionSet) getConditionActionSetsList().get( i );
         AbstractTransformation mdoTransform = (AbstractTransformation)omr.acquireOMRObject( this ); 
         set.saveToOMR( omr );
         ConditionActionSet mdoSet = (ConditionActionSet)omr.acquireOMRObject( set ); 
         if (!mdoTransform.getConditionActionSets().contains(mdoSet))
            mdoTransform.getConditionActionSets().add(mdoSet);
      }        
      
      setChanged( false );
   }   
   
   /**
    * Is the table changed?  This method is overridden to check all the columns
    * to see if one has changed.
    * 
    * @return true the condition action set is changed.
    * 
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isChanged()
   {
      if (super.isChanged() || m_lConditionActionSets.isChanged())
         return true;
      
      return false;
   }
}

