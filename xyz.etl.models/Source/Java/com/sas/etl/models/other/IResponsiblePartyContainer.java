/* $Id$ */
/**
 * Title:       IResponsiblePartyContainer.java
 * Description: The interface that defines an object that contains responsible 
 *              parties.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.other;

import java.util.List;

import com.sas.etl.models.IObject;

/**
 * IResponsiblePartyContainer is the interface that defines an object that 
 * contains responsible parties.
 */
public interface IResponsiblePartyContainer extends IObject 
{
   // event types
   /** the event type for a responsible party was added */
   String RESPONSIBLE_PARTY_ADDED   = "Objecty:ResponsiblePartyAdded";
   /** the event type for a responsible party was removeed */
   String RESPONSIBLE_PARTY_REMOVED = "Objecty:ResponsiblePartyRemoved";

   /**
    * Gets the responsible parties for the container.
    * 
    * @return the responsible parties
    */
   IResponsibleParty[] getResponsibleParties();

   /**
    * Gets the list of responsbile parties for the container.  The list may be
    * modified to modify the responsbile parties in the container.  The methods 
    * in the list that are not implemented will throw an exception.
    *  
    * @return the list of responsible parties
    */
   List getResponsiblePartiesList();
}

