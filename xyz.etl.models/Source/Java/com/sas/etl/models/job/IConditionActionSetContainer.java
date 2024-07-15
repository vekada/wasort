/* $Id: IConditionActionSetContainer.java,v 1.1.2.6 2007/10/26 16:40:46 kilewi Exp $ */
package com.sas.etl.models.job;

import java.rmi.RemoteException;
import java.util.List;

import com.sas.etl.models.IObject;
import com.sas.etl.models.other.IConditionActionSet;
import com.sas.metadata.remote.MdException;

public interface IConditionActionSetContainer extends IObject
{
   // event types
   /** event type for a conditionActionSet was added */
   static final String CONDITIONACTIONSET_ADDED   = "Object:ConditionActionSetAdded";
   /** event type for a conditonActionSet was removed */
   static final String CONDITIONACTIONSET_REMOVED = "Object:ConditionActionSetRemoved";
   

   /**
    * Get list of condition actions sets supported 
    * 
    * @return List of conditionActionSet objects
    */
   List getConditionActionSetsList();

   /**
    * Get array of condition actions sets supported
    *  
    * @return Array of ConditionActionSet objects
    */
   IConditionActionSet[] getConditionActionSets();
   
   /**
    * get the list of defined conditionActionSet template unique identifiers required.
    * @return list of defined conditionActionSet template unique ids required.
    */
   List getDefinedConditionActionSetUniqueIds();
   
   /**
    * get the list of template conditionActionSets.
    * @return list of template conditionActionSets.
    */
   List getTemplateConditionActionSetsList();
   
   /**
    * Return array of template condition actions sets
    * 
    * @return array

    * @see com.sas.etl.models.job.impl.IConditionActionSetContainer#getConditionActionSets()
    */
   IConditionActionSet[] getTemplateConditionActionSets();

   /**
    * Load the conditionActionSet templates, this method is called in the loadFromOMR
    * method, but should also be called from the addDefaultSettings method of any
    * transform implementing condition actions.
    * @throws MdException
    * @throws RemoteException
    */
   void loadConditionActionSetTemplatesFromOMR() throws MdException, RemoteException;

}