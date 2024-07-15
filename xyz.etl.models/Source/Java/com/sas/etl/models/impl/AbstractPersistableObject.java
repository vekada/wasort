/* $Id$ */
/**
 * Title:       AbstractPersistableObject.java
 * Description: An abstract implementation of a persistable object
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sas.etl.models.IModel;
import com.sas.etl.models.IPersistableObject;
import com.sas.etl.models.NotifyEvent;
import com.sas.metadata.remote.CustomAssociation;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.Property;
import com.sas.metadata.remote.PropertySet;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.Transformation;

/**
 * AbstractPersistableObject is an abstract implementation of a persistable object.
 */
public abstract class AbstractPersistableObject extends BaseObject implements IPersistableObject
{
   // constants to use when creating properties
   /**
    * put property in object's properties association
    * @deprecated
    */
   protected static final int USE_PROPERTIES_DIRECTLY       = 0x01;
   /**
    * put property in a property set's properties association
    * @deprecated
    */
   protected static final int USE_PROPERTYSET_PROPERTIES    = 0x02;
   /**
    * put property in a property set's setproperties association
    */
   protected static final int USE_PROPERTYSET_SETPROPERTIES = 0x00;
   /**
    * set the delimiter on the property
    * @deprecated
    */
   protected static final int SET_DELIMITER                 = 0x10;
   /**
    * Compare the property name (not name) when searching for the property.
    * This is the default.
    */
   protected static final int COMPARE_PROPERTY_NAME         = 0x00;
   /**
    * Compare the name (not property name) when searching for the property
    * @deprecated
    */
   protected static final int COMPARE_NAME                  = 0x20;
   /**
    * Compare the SetRole when searching for the property set.
    * This is the default.
    */
   protected static final int COMPARE_PROPERTYSET_SETROLE      = 0x00;
   /**
    * Compare the name when searching for the property set.
    * @deprecated
    */
   protected static final int COMPARE_PROPERTYSET_NAME         = 0x40;

   /**
    * Compare the property set name when searching for the property set
    */
   protected static final int COMPARE_PROPERTYSET_PROPERTYSETNAME  = 0x80;

   protected static final String OPTIONS_PROPERTYSET_NAME = "OPTIONS";
   private static final String USERPROPERTIES_PROPERTYSET_NAME = "USERPROPERTIES";

   private List m_lDeletedObjects;

   private Map m_mapProperties;      // names to PropertyTracker
   private Map m_mapPropertySets;    // names to PropertySet FQID
   private Map m_mapCustomLists;     // names to CustomAssociation FQID

   /**
    * Constructs an abstract persistable object.
    *
    * @param sID   the object id
    * @param model the model
    */
   public AbstractPersistableObject( String sID, IModel model )
   {
      super( sID, model );
      m_mapProperties   = new HashMap();
      m_mapPropertySets = new HashMap();
      m_mapCustomLists  = new HashMap();
   }

   /**
    * Saves the object as XML to the specified stream.
    *
    * @param strm the output stream
    *
    * @throws IOException
    */
   public void saveXMLToStream( OutputStream strm ) throws IOException
   {
      throw new UnsupportedOperationException( "saveXMLToStream not implemented" );
   }

   /**
    * Loads the object from XML from the specified stream.
    *
    * @param strm the input stream
    *
    * @throws IOException
    */
   public void loadXMLFromStream( InputStream  strm ) throws IOException
   {
      throw new UnsupportedOperationException( "loadXMLFromStream not implemented" );
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

      Root mdo = omr.acquireOMRObject( this );
      mdo.setName( getName()        );
      mdo.setDesc( getDescription() );

      saveUserPropertiesToOMR( omr );

      deleteDeletedObjectsFromOMR( omr );

      setChanged( false );
   }

   /**
    * Updates new object ids after the object has been saved to OMR.
    *
    * @param mapIDs the map of new object ids
    *
    * @see com.sas.etl.models.IOMRPersistable#updateIDs(java.util.Map)
    */
   public void updateIDs( Map mapIDs )
   {
      updatePropertyAndPropertySetIDs( mapIDs );
      updateCustomListIDs( mapIDs );

      // if the object is not new, nothing to do
      if (!isNew())
         return;

      // update id if there is a new id
      String sNewID = (String) mapIDs.get( getID() );
      if (sNewID != null)
         setID( sNewID );
   }

   /**
    * Updates new object ids in the properties and property sets maps after the
    * object has been saved to OMR.
    *
    * @param mapIDs
    */
   protected void updatePropertyAndPropertySetIDs( Map mapIDs )
   {
      Collection values = m_mapProperties.values();
      PropertyTracker[] aTrackers = new PropertyTracker[ values.size() ];
      values.toArray( aTrackers );

      for ( int iTracker=0; iTracker<aTrackers.length; iTracker++ )
      {
         PropertyTracker tracker  = aTrackers[ iTracker ];
         tracker.m_sFQID          = updateSubordinateID( tracker.m_sFQID,       mapIDs );
         tracker.m_sHolderFQID    = updateSubordinateID( tracker.m_sHolderFQID, mapIDs );
      }

      Set         setEntries = m_mapPropertySets.entrySet();
      Map.Entry[] aEntries   = new Map.Entry[ setEntries.size() ];
      setEntries.toArray( aEntries );

      for ( int iEntry=0; iEntry<aEntries.length; iEntry++ )
      {
         String sValue = (String) aEntries[iEntry].getValue();
         sValue = updateSubordinateID( sValue, mapIDs );
         aEntries[iEntry].setValue( sValue );
      }
   }

   /**
    * Updates new object ids in the custom lists map after the object has been
    * saved to OMR.
    *
    * @param mapIDs
    */
   protected void updateCustomListIDs( Map mapIDs )
   {
      Set         setEntries = m_mapCustomLists.entrySet();
      Map.Entry[] aEntries   = new Map.Entry[ setEntries.size() ];
      setEntries.toArray( aEntries );

      for ( int iEntry=0; iEntry<aEntries.length; iEntry++ )
      {
         String sValue = (String) aEntries[iEntry].getValue();
         sValue = updateSubordinateID( sValue, mapIDs );
         aEntries[iEntry].setValue( sValue );
      }
   }

   /**
    * Updates a subordinate object's id if the id is for a new object.  Really,
    * determines whether the subordinate object has a new id and returns it.
    * This is a convenience method.
    *
    * @param sSubordinateID the subordinate object's id
    * @param mapIDs         the map of new object ids
    *
    * @return the subordinate object's id
    */
   protected final String updateSubordinateID( String sSubordinateID, Map mapIDs )
   {
      // S0540520: don't search for a matching id, if the map of ids doesn't contain a replacement
      //   this is because on replace table and save some of the transforms ids are not being saved so there will be no replacement id
      //   returning null is not accurate
      if ((sSubordinateID != null) && isNewObjectID( sSubordinateID ) && mapIDs.containsKey(sSubordinateID ))
         return (String) mapIDs.get( sSubordinateID );
      return sSubordinateID;
   }

   /**
    * Loads the object from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @see com.sas.etl.models.IOMRPersistable#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      Root mdo = omr.acquireOMRObject( this );

      // this only happens in the case of new objects being dropped.  We
      // will give them a default name if we can.
      // This allows us to load content from the server if we need to when
      // creating objects, such as a condition action set template
      // rlr: I believe this check is completely bogus, because new objects
      // cannot be loaded from OMR
      if ((mdo.isNewObject()) &&
           ((mdo.getName() == null) || (mdo.getName().equalsIgnoreCase("")))
         )
      {
         mdo.setName(this.getDefaultName());
      }

      // some names were missing in previous releases,
      // so fix the name by changing it to NONAME
      boolean bNameChanged = false;
      String sName = mdo.getName();
      if (sName.trim().length() == 0)
      {
         sName = "NONAME";
         bNameChanged = true;
      }

      setName(        sName         );
      setDescription( mdo.getDesc() );

      clearListsBeforeLoad();

      loadUserPropertiesFromOMR( omr );

      setChanged( bNameChanged );
   }

   protected void clearListsBeforeLoad()
   {
   	 m_mapCustomLists.clear();
       m_mapProperties.clear();
       m_mapPropertySets.clear();
   }

   /**
    * Notifies the object that it will be deleted when the model is saved.
    *
    * @see com.sas.etl.models.IPersistableObject#delete()
    */
   public void delete()
   {
      fireNotifyEvent( NotifyEvent.OBJECT_DELETED, null );
   }

   /**
    * Deletes the object from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @see com.sas.etl.models.IOMRPersistable#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void deleteFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (isNew())
         return;

      deleteDeletedObjectsFromOMR( omr );
      deletePropertiesAndPropertySetsFromOMR( omr );
      deleteCustomListsFromOMR( omr );
      omr.deleteOMRObject( getID(), getOMRType() );
      if (!getModel().isCopyPaste())
    	  getModel().removeObject( this );       // remove the object from the model
      //setID( createIDForNewObject() );       // change id to a new id because the object would have to be recreated
      //getModel().putObject( this );
   }

   /**
    * Deletes the object's properties and propertysets from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @see com.sas.etl.models.IOMRPersistable#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   protected void deletePropertiesAndPropertySetsFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      // get the property ids
      Collection values = m_mapProperties.values();
      PropertyTracker[] aTrackers = new PropertyTracker[ values.size() ];
      values.toArray( aTrackers );

      // delete the properties
      for ( int iTracker=0; iTracker<aTrackers.length; iTracker++ )
         deleteProperty( omr, aTrackers[iTracker] );

      // get the property set ids
      values = m_mapPropertySets.values();
      String[] aIDs = new String[ values.size() ];
      values.toArray( aIDs );

      // delete the property sets
      for ( int iID=0; iID<aIDs.length; iID++ )
         omr.deleteOMRObject( aIDs[iID], MetadataObjects.PROPERTYSET );
   }

   /**
    * Deletes the property.  In reality, deletes the property and the holder
    * object if there is one.
    *
    * @param omr     the OMR adapter
    * @param tracker the tracker that tracks the property and its holder object
    *
    * @throws MdException
    * @throws RemoteException
    */
   private void deleteProperty( OMRAdapter omr, PropertyTracker tracker ) throws MdException, RemoteException
   {
      omr.deleteOMRObject( tracker.m_sFQID, MetadataObjects.PROPERTY );
      if (tracker.m_sHolderFQID != null)
         omr.deleteOMRObject( tracker.m_sHolderFQID, MetadataObjects.TRANSFORMATION );
      if (tracker.m_sCustomAssocFQID != null)
         omr.deleteOMRObject( tracker.m_sCustomAssocFQID, MetadataObjects.CUSTOMASSOCIATION );
   }

   /**
    * Deletes the object's custom lists from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @see com.sas.etl.models.IOMRPersistable#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   protected void deleteCustomListsFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      // get the property ids
      Collection values = m_mapCustomLists.values();
      String[] aIDs = new String[ values.size() ];
      values.toArray( aIDs );

      // delete the property sets
      for ( int iID=0; iID<aIDs.length; iID++ )
         omr.deleteOMRObject( aIDs[iID], MetadataObjects.CUSTOMASSOCIATION );

      m_mapCustomLists.clear();
   }

   /**
    * Gets the template map used to populate an OMR adapter for a table.
    *
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = new HashMap();

      List lAssociations = new ArrayList();
      lAssociations.add( Root.ASSOCIATION_PROPERTIES_NAME         );    // old style properties
      lAssociations.add( Root.ASSOCIATION_PROPERTYSETS_NAME       );
      lAssociations.add( Root.ASSOCIATION_CUSTOMASSOCIATIONS_NAME );
      map.put( getOMRType(), lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( PropertySet.ASSOCIATION_SETPROPERTIES_NAME );
      lAssociations.add( PropertySet.ASSOCIATION_PROPERTIES_NAME    );  // old style properties
      map.put( MetadataObjects.PROPERTYSET, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( Property.ASSOCIATION_SPECTARGETTRANSFORMATIONS_NAME );
      lAssociations.add( Property.ASSOCIATION_CUSTOMASSOCIATIONS_NAME );
      map.put( MetadataObjects.PROPERTY, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( Transformation.ASSOCIATION_SOURCESPECIFICATIONS_NAME );
      map.put( MetadataObjects.TRANSFORMATION, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( CustomAssociation.ASSOCIATION_ASSOCIATEDOBJECTS_NAME );
      map.put( MetadataObjects.CUSTOMASSOCIATION, lAssociations );

      return map;
   }

   /**
    * Gets the template map used to copy the column.
    *
    * @return the copy template map
    */
   public Map getOMRCopyTemplateMap()
   {
      return null;
   }

   /**
    * Gets the template map used to export the column.
    *
    * @return the export template map
    */
   public Map getOMRExportTemplateMap()
   {
      return null;
   }

   /**
    * Gets the template map used to checkout the column.
    *
    * @return the checkout template map
    */
   public Map getOMRCheckOutTemplateMap()
   {
      return null;
   }

   /**
    * Gets the objects that might need to be refreshed.  A refresh is required
    * when an external force (code submitted by the application) causes an
    * update of metadata.  Because of this update, certain objects in the model
    * may need to be refreshed.  If an object contains or references objects
    * that may need to be refreshed, the objects should be added to the list.
    * The object should not add itself to the list.  It is the responsibility of
    * the caller of the method to determine if the object should be added to the
    * list of objects.
    * <p>
    * Currently, only transformations that generate an update table metadata
    * will return a list of tables and physical tables will return a list of
    * their columns, keys, foreign keys, and indexes.
    *
    * @return the list of refresh objects (null = no objects, also an empty list
    *         may be returned).
    *
    * @see com.sas.etl.models.IPersistableObject#getRefreshObjects()
    */
   public List getRefreshObjects()
   {
      return null;
   }

   //---------------------------------------------------------------------------
   // Methods for saving and loading custom lists
   //---------------------------------------------------------------------------
   /**
    * Saves a custom list of objects to OMR.
    *
    * @param omr        the OMR adapter
    * @param sListName  the name of the list
    * @param aObjects   the objects to be saved in the list
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveCustomListToOMR( OMRAdapter omr, String sListName, IPersistableObject[] aObjects ) throws MdException, RemoteException
   {
      Root mdo = omr.acquireOMRObject( this );
      saveCustomListToOMR(omr, mdo, sListName, aObjects );
   }

   /**
    * Saves a custom list of objects to OMR.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the object that the custom assoc is saved on
    * @param sListName  the name of the list
    * @param aObjects   the objects to be saved in the list
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveCustomListToOMR( OMRAdapter omr, Root mdoAnchor, String sListName, IPersistableObject[] aObjects ) throws MdException, RemoteException
   {
      String sFQID = (String) m_mapCustomLists.get( sListName );
      if (sFQID == null)
      {
         sFQID = createIDForNewObject();
         m_mapCustomLists.put( sListName, sFQID );
      }

      CustomAssociation mdoList = (CustomAssociation) omr.acquireOMRObject( sFQID, MetadataObjects.CUSTOMASSOCIATION );
      mdoList.setName(        sListName             );
      mdoList.setPartnerName( sListName + "Partner" ); /*I18NOK:COS**/
      mdoList.setisOwned( 0 );

      List lCustAssoc = mdoAnchor.getCustomAssociations( false );

      //Had to add this check because when migrating SCD generated keys over from
      //3.4 to 4.2, we create a custom association off of the transformation step prior
      //to loading the transform model, otherwise we lose the max key if it's
      //user written.  So when the model goes to save, it's already in the
      //association and would get added a second time during the save which would
      //cause the metadata server to throw an exception. Not nice. So I added a check
      //to only add the custom association if it is not in the list.  dez 4/17/08
      //
      // S0505433: We need to ensure these are not duplicated.  This came about
      //           because we are using the same OMRAdapter in upgradeJobAction for both
      //           the load and the save.  in those usages we need to ensure uniqueness
      //           within the list.
      if (!lCustAssoc.contains(mdoList))
         lCustAssoc.add( mdoList );

      // populate the object list
      List lObjects = mdoList.getAssociatedObjects( false );
      lObjects.clear();
      for ( int iObject = 0; iObject < aObjects.length; iObject++ )
         lObjects.add( omr.acquireOMRObject( aObjects[iObject] ) );
   }


   /**
    * Loads a custom list of objects from OMR.
    *
    * @param omr        the OMR adapter
    * @param sListName  the name of the list
    *
    * @return the custom list of objects.
    *
    * @throws MdException
    * @throws RemoteException
    */
   public IPersistableObject[] loadCustomListFromOMR( OMRAdapter omr, String sListName ) throws MdException, RemoteException
   {
      Root mdoAnchor = omr.acquireOMRObject( this );
      return loadCustomListFromOMR(omr, mdoAnchor, sListName);
   }

   /**
    * Loads a custom list of objects from OMR.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the object the custom associated is saved on
    * @param sListName  the name of the list
    *
    * @return the custom list of objects.
    *
    * @throws MdException
    * @throws RemoteException
    */
   public IPersistableObject[] loadCustomListFromOMR( OMRAdapter omr, Root mdoAnchor, String sListName ) throws MdException, RemoteException
   {
      CustomAssociation mdoList = findCustomList( omr, mdoAnchor, sListName );
      if (mdoList == null)
         return new IPersistableObject[0];

      omr.populateAssociations(omr.getOMRFactory(), mdoList);
      List lObjects = mdoList.getAssociatedObjects();
      IPersistableObject[] aObjects = new IPersistableObject[ lObjects.size() ];
      for ( int iObject=0; iObject<aObjects.length; iObject++ )
         aObjects[ iObject ] = (IPersistableObject) omr.acquireObject( (Root) lObjects.get( iObject ) );

      return aObjects;
   }

   /**
    * Finds the custom list.  In reality finds the custom association object
    * that holds the custom list.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the object this custom list is anchored
    * @param sListName  the name of the list
    *
    * @return the custom list (the custom association object)
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected CustomAssociation findCustomList( OMRAdapter omr, Root mdoAnchor, String sListName ) throws MdException, RemoteException
   {
      String sFQID = (String) m_mapCustomLists.get( sListName );
      if (sFQID != null)
         return (CustomAssociation) omr.acquireOMRObject( sFQID, MetadataObjects.CUSTOMASSOCIATION );

      // search the list
//      List lLists = mdoAnchor.getCustomAssociations(false);
      //Need to search out duplicates and get rid of them as it's causing problems
      //Lots of customer problems, see defect S06549577
      List lCustAssocs = findAllCustomAssociations(omr, mdoAnchor, sListName);

      if (lCustAssocs.size() > 0)
      {
          CustomAssociation ca = (CustomAssociation)lCustAssocs.get(0);
          m_mapCustomLists.put(sListName, ca.getFQID());
           return ca;
      }
      // not found
      return null;
   }

   /**
    * Get the custom association map of the name to the metadata id
    * @return the customer association map
    */
   protected Map getCustomAssocationMap()
   {
      return m_mapCustomLists;
   }


   /**
    * Finds and returns, if multiple, all of the custom associations, there should never be
    * more than one.  But as always, it's happening out in the field!
    * @param omr the OMRAdapter class
    * @param mdoAnchor the parent of the custom association
    * @param sListName the name of the custom association
    * @return list of custom associations
    * @throws MdException
    * @throws RemoteException
    */
   protected List findAllCustomAssociations (OMRAdapter omr, Root mdoAnchor, String sListName) throws MdException, RemoteException
   {
      List lCAssocReturned = new ArrayList();
      //Search for all of the CA, should be only one if everything is working correctly
      List lCAssocs = mdoAnchor.getCustomAssociations(false);
      for ( int iList=0; iList<lCAssocs.size(); iList++ )
      {
         CustomAssociation mdo = (CustomAssociation) lCAssocs.get( iList );
         if (mdo.getName().equals( sListName ))
            lCAssocReturned.add(mdo);
      }

      return lCAssocReturned;

   }

   //------------------------------------------
   // Methods for saving options and properties
   //------------------------------------------
   /**
    * Saves a string option to OMR.  The option is saved to a Property in the
    * OPTIONS PropertySet's SetProperties association.
    *
    * @param omr    the OMR adapter
    * @param sName  the option name
    * @param sValue the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveStringOptionToOMR( OMRAdapter omr, String sName, String sValue ) throws MdException, RemoteException
   {
      saveOptionToOMR( omr, sName, sValue, Types.VARCHAR );
   }

   /**
    * Saves an integer option to OMR.  The option is saved to a Property in the
    * OPTIONS PropertySet's SetProperties association.
    *
    * @param omr    the OMR adapter
    * @param sName  the option name
    * @param iValue the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveIntOptionToOMR( OMRAdapter omr, String sName, int iValue ) throws RemoteException, MdException
   {
      saveOptionToOMR( omr, sName, Integer.toString( iValue ), Types.INTEGER );
   }
   
   /**
    * Saves an integer option to OMR.  The option is saved to a Property in the
    * OPTIONS PropertySet's SetProperties association.
    *
    * @param omr    the OMR adapter
    * @param sName  the option name
    * @param iValue the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveDoubleOptionToOMR( OMRAdapter omr, String sName, Double iValue ) throws RemoteException, MdException
   {
      saveOptionToOMR( omr, sName, iValue!=null ? Double.toString( iValue ) : null, Types.DOUBLE );
   }
   

   /**
    * Saves a boolean option to OMR.  The option is saved to a Property in the
    * OPTIONS PropertySet's SetProperties association.
    *
    * @param omr    the OMR adapter
    * @param sName  the option name
    * @param bValue the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveBooleanOptionToOMR( OMRAdapter omr, String sName, boolean bValue ) throws RemoteException, MdException
   {
      saveOptionToOMR( omr, sName, Boolean.toString( bValue ), Types.BOOLEAN );
   }

//   /**
//    * Saves an object list option to OMR.  The option is saved to a Property in
//    * the OPTIONS PropertySet's SetProperties association.
//    *
//    * @param omr      the OMR adapter
//    * @param sName    the option name
//    * @param aObjects the object list
//    *
//    * @throws MdException
//    * @throws RemoteException
//    */
//   protected void saveObjectListOptionToOMR( OMRAdapter omr, String sName, IPersistableObject[] aObjects ) throws RemoteException, MdException
//   {
//      saveObjectListPropertyToOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, sName, "OPTION", aObjects, Types.ARRAY, USE_PROPERTYSET_SETPROPERTIES );
//   }
//
   /**
    * Saves an option to OMR.  The option is saved to a Property in the OPTIONS
    * PropertySet's SetProperties association.  This method is NOT preferred but
    * is provided to allow the caller to control the SQLType set on the property.
    *
    * @param omr      the OMR adapter
    * @param sName    the option name
    * @param sValue   the option value
    * @param eSQLType the option SQLType (valid values are in java.sql.Types)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveOptionToOMR( OMRAdapter omr, String sName, String sValue, int eSQLType ) throws MdException, RemoteException
   {
      saveOptionToOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, sValue, eSQLType );
   }

   /**
    * Saves a string option to OMR.  The option is saved to a Property in the
    * specified PropertySet's SetProperties association.
    *
    * @param omr      the OMR adapter
    * @param sSetRole the option set role
    * @param sName    the option name
    * @param sValue   the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveStringOptionToOMR( OMRAdapter omr, String sSetRole, String sName, String sValue ) throws MdException, RemoteException
   {
      saveOptionToOMR( omr, sSetRole, sName, sValue, Types.VARCHAR );
   }

   /**
    * Saves an integer option to OMR.  The option is saved to a Property in the
    * specified PropertySet's SetProperties association.
    *
    * @param omr    the OMR adapter
    * @param sSetRole the option set role
    * @param sName  the option name
    * @param iValue the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveIntOptionToOMR( OMRAdapter omr, String sSetRole, String sName, int iValue ) throws RemoteException, MdException
   {
      saveOptionToOMR( omr, sSetRole, sName, Integer.toString( iValue ), Types.INTEGER );
   }

   /**
    * Saves a boolean option to OMR.  The option is saved to a Property in the
    * specified PropertySet's SetProperties association.
    *
    * @param omr      the OMR adapter
    * @param sSetRole the option set role
    * @param sName    the option name
    * @param bValue   the option value
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveBooleanOptionToOMR( OMRAdapter omr, String sSetRole, String sName, boolean bValue ) throws RemoteException, MdException
   {
      saveOptionToOMR( omr, sSetRole, sName, Boolean.toString( bValue ), Types.BOOLEAN );
   }

   /**
    * Saves an option to OMR.  The option is saved to a Property in the OPTIONS
    * PropertySet's SetProperties association.  This method is NOT preferred but
    * is provided to allow the caller to control the SQLType set on the property.
    *
    * @param omr      the OMR adapter
    * @param sSetRole the option set role
    * @param sName    the option name
    * @param sValue   the option value
    * @param eSQLType the option SQLType (valid values are in java.sql.Types)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveOptionToOMR( OMRAdapter omr, String sSetRole, String sName, String sValue, int eSQLType ) throws MdException, RemoteException
   {
      savePropertyToOMR( omr, sSetRole, sName, sName, "OPTION", sValue, eSQLType, USE_PROPERTYSET_SETPROPERTIES );
   }

   /**
    * Saves user properties to OMR.  The user properties are saved to Properties
    * in the USERPROPERTIES PropertySet's SetProperties association.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveUserPropertiesToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      String[] aKeys = getUserPropertyKeys();

      List lOldKeys = new ArrayList( m_mapProperties.keySet() );

      // save all the user properties
      for ( int iKey=0; iKey<aKeys.length; iKey++ )
      {
         String sName  = aKeys[iKey];
         String sValue = getUserProperty( sName );
         if (sValue != null)
         {
            savePropertyToOMR( omr, USERPROPERTIES_PROPERTYSET_NAME, sName, sName, "USER", sValue, Types.VARCHAR, USE_PROPERTYSET_SETPROPERTIES );
            lOldKeys.remove( USERPROPERTIES_PROPERTYSET_NAME + sName );    // I18NOK:EMS
         }
      }

      // any old user properties that were not saved, delete them
      for ( int iKey=0; iKey<lOldKeys.size(); iKey++ )
      {
         String sKey = (String) lOldKeys.get( iKey );
         if (sKey.startsWith( USERPROPERTIES_PROPERTYSET_NAME ))
         {
            deleteProperty( omr, (PropertyTracker) m_mapProperties.get( sKey ) );
            m_mapProperties.remove( sKey );
         }
      }
   }

   /**
    * Saves a property to OMR.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param sValue        the property's value
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use saveStringOptionToOMR, saveIntOptionToOMR, or
    *             saveBooleanOptionToOMR instead.
    */
   protected void savePropertyToOMR( OMRAdapter omr, String sSetRole, String sName, String sPropertyName, String sPropertyRole, String sValue, int eSQLType, int fFlags ) throws MdException, RemoteException
   {
      savePropertyToOMR( omr, omr.acquireOMRObject( this ), sSetRole, sName, sPropertyName, sPropertyRole, sValue, eSQLType, fFlags );
   }

   /**
    * Saves a property to OMR.  If a property set is specified, the property set
    * is anchored to the specified anchor object.  Otherwise, the property is
    * anchored to the specified anchor object.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the metadata anchor object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param sValue        the property's value
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use saveStringOptionToOMR, saveIntOptionToOMR, or
    *             saveBooleanOptionToOMR instead.
    */
   protected void savePropertyToOMR( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sName, String sPropertyName, String sPropertyRole, String sValue, int eSQLType, int fFlags ) throws MdException, RemoteException
   {
      Property mdoProperty = acquireProperty( omr, mdoAnchor, sSetRole, sName, sPropertyName, sPropertyRole, eSQLType, fFlags );
      mdoProperty.setDefaultValue( sValue );
   }

   /**
    * Saves an object list property to OMR.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param aObjects      the objects to be saved
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveObjectListPropertyToOMR( OMRAdapter omr,String sSetRole, String sName, String sPropertyName, String sPropertyRole, IPersistableObject[] aObjects, int eSQLType, int fFlags ) throws MdException, RemoteException
   {
      saveObjectListPropertyToOMR( omr, omr.acquireOMRObject( this ),  sSetRole,  sName,  sPropertyName,  sPropertyRole,  aObjects,  eSQLType,  fFlags, false );
   }


   /**
    * Saves an object list property to OMR.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param aObjects      the objects to be saved
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    * @param bSaveObjectList save the object list that is passed into this method
    * @throws MdException
    * @throws RemoteException
    */
   public void saveObjectListPropertyToOMR( OMRAdapter omr,String sSetRole, String sName, String sPropertyName, String sPropertyRole, IPersistableObject[] aObjects, int eSQLType, int fFlags, boolean bSaveObjectList ) throws MdException, RemoteException
   {
      saveObjectListPropertyToOMR( omr, omr.acquireOMRObject( this ),  sSetRole,  sName,  sPropertyName,  sPropertyRole,  aObjects,  eSQLType,  fFlags, bSaveObjectList );
   }

   /**
    * Saves an object list property to OMR.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the metadata anchor object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param aObjects      the objects to be saved
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use saveObjectListOptionToOMR instead.
    */
   protected void saveObjectListPropertyToOMR( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sName, String sPropertyName, String sPropertyRole, IPersistableObject[] aObjects, int eSQLType, int fFlags ) throws MdException, RemoteException
   {
      saveObjectListPropertyToOMR( omr, mdoAnchor,  sSetRole,  sName,  sPropertyName,  sPropertyRole,  aObjects,  eSQLType,  fFlags, false );
   }

   /**
    * Saves an object list property to OMR.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the metadata anchor object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param aObjects      the objects to be saved
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    * @param bSaveObjectList save the objects that are passed into this method
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use saveObjectListOptionToOMR instead.
    */
   protected void saveObjectListPropertyToOMR( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sName, String sPropertyName, String sPropertyRole, IPersistableObject[] aObjects, int eSQLType, int fFlags, boolean bSaveObjectList ) throws MdException, RemoteException
   {
      Property mdoProperty = acquireProperty( omr, mdoAnchor, sSetRole, sName, sPropertyName, sPropertyRole, eSQLType, fFlags );
      mdoProperty.setDefaultValue( sPropertyName );

      // if there is a holder transform, get it
      PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sPropertyName );
      Transformation mdoTransform;
      if (tracker.m_sHolderFQID != null)
         mdoTransform = (Transformation) omr.acquireOMRObject( tracker.m_sHolderFQID, MetadataObjects.TRANSFORMATION );
      // create the holder transform
      else
      {
         tracker.m_sHolderFQID = createIDForNewObject();
         mdoTransform = (Transformation) omr.acquireOMRObject( tracker.m_sHolderFQID, MetadataObjects.TRANSFORMATION );
      }
      mdoTransform.setName(          sPropertyName );
      mdoTransform.setTransformRole( sPropertyName );
      if (!mdoProperty.getSpecTargetTransformations().contains(mdoTransform))
      	mdoProperty.getSpecTargetTransformations().add( mdoTransform );      	
      
      // populate the object list
      List lObjects = mdoTransform.getSourceSpecifications( false );
      lObjects.clear();
      for ( int iObject = 0; iObject < aObjects.length; iObject++ )
      {
        IPersistableObject obj = aObjects[iObject];
        if (bSaveObjectList)
           obj.saveToOMR(omr);
         lObjects.add( omr.acquireOMRObject( obj ) );
      }
   }

   /**
    * Saves an option to OMR with a custom association.  The option is saved to
    * a Property in the specified PropertySet's SetProperties association.
    *
    * @param omr      the OMR adapter
    * @param sSetRole the option set role
    * @param sName    the option name
    * @param sValue   the option value
    * @param aObjects the objects to be saved on the CustomAssocation
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void saveOptionWithCustomListToOMR(OMRAdapter omr, String sSetRole, String sName, String sValue, IPersistableObject[] aObjects ) throws MdException, RemoteException
   {
      Property mdoProperty = acquireProperty(omr, omr.acquireOMRObject( this ), sSetRole, sName, sName, sName, Types.VARCHAR, USE_PROPERTYSET_SETPROPERTIES);
      PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sName );
      mdoProperty.setDefaultValue(sValue);
      CustomAssociation mdoCustAssoc;
      if (tracker.m_sCustomAssocFQID == null)
         tracker.m_sCustomAssocFQID = createIDForNewObject();

      mdoCustAssoc = (CustomAssociation) omr.acquireOMRObject( tracker.m_sCustomAssocFQID, MetadataObjects.CUSTOMASSOCIATION );
      mdoCustAssoc.setName( sName );
      mdoCustAssoc.setPartnerName( sName + "Partner" ); /*I18NOK:COS**/
      mdoCustAssoc.setisOwned( 0 );

      List lAssoc = mdoProperty.getCustomAssociations();
      lAssoc.clear();
      lAssoc.add( mdoCustAssoc );

      // populate the object list
      List lObjects = mdoCustAssoc.getAssociatedObjects( false );
      lObjects.clear();
      for ( int iObject = 0; iObject < aObjects.length; iObject++ )
         lObjects.add( omr.acquireOMRObject( aObjects[iObject] ) );
   }

   /**
    * Acquires a named property object.  If the property already exists, the
    * Property is returned.  Otherwise a new proeprty is created and is
    * associated to the object either directly or through the named property
    * set.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the metadata anchor object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    *
    * @return the acquired named property
    *
    * @throws MdException
    * @throws RemoteException
    */
   private Property acquireProperty( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sName, String sPropertyName, String sPropertyRole, int eSQLType, int fFlags )
   throws MdException, RemoteException
   {
      return acquireProperty( omr, mdoAnchor, sSetRole, sName, sPropertyName, sPropertyRole, eSQLType, fFlags, "" );
   }


   /**
    * Acquires a named property object.  If the property already exists, the
    * Property is returned.  Otherwise a new proeprty is created and is
    * associated to the object either directly or through the named property
    * set.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the metadata anchor object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to which the property
    *                      should be associated.  May be null if
    *                      USE_PROPERTIES_DIRECTLY is specified in fFlags
    * @param sName         the property's name
    * @param sPropertyName the property's PropertyName which is the value used
    *                      when searching for the property
    * @param sPropertyRole the property's role
    * @param eSQLType      the property's SQLType (valid values are in
    *                      java.sql.Types)
    * @param fFlags        flags about where to store the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is used to
    *                      track the property.  Adding COMPARE_NAME to the flags
    *                      indicates that sName should be used to track the
    *                      property.  In addition, the SET_DELIMITER flag can
    *                      be added to the flags to specify the UseValueOnly
    *                      value should be set to 0 and the Delimiter value
    *                      should be set to "=".
    *
    * @return the acquired named property
    *
    * @throws MdException
    * @throws RemoteException
    */
   private Property acquireProperty( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sName, String sPropertyName, String sPropertyRole, int eSQLType, int fFlags, String sPropertySetName ) throws MdException, RemoteException
   {
      // get the property object if there is one
      String sTestName = ((fFlags & COMPARE_NAME) == 0) ? sPropertyName : sName;
      PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sPropertySetName + sSetRole + sTestName );
      String sID = createIDForNewObject();
      if (tracker==null)
      {
          m_mapProperties.put( sPropertySetName + sSetRole + sTestName, new PropertyTracker( sID ) );  
      }
      else
    	  sID = tracker.m_sFQID;

      // if there isn't one, create and initialize the Property
      
      Property mdoProperty = (Property) omr.acquireOMRObject( sID, MetadataObjects.PROPERTY );

      boolean bDelimiter = (fFlags & SET_DELIMITER) != 0;

      mdoProperty.setName(         sName         );
      mdoProperty.setPropertyName( sPropertyName );
      mdoProperty.setPropertyRole( sPropertyRole );
      mdoProperty.setSQLType(      eSQLType      );
      mdoProperty.setUseValueOnly( bDelimiter ? 0   : 1  ); // if there is a delimiter, use value only is false
      mdoProperty.setDelimiter(    bDelimiter ? "=" : "" );

      // if properties are associated directly to the object,
      // add the property to the object's properties association
      if ((fFlags & USE_PROPERTIES_DIRECTLY) != 0)
      {
         // S0505433: We need to ensure these are not duplicated.
         if ( ! mdoAnchor.getProperties( false ).contains(mdoProperty))
         {
            mdoAnchor.getProperties( false ).add( mdoProperty );
         }
      }

      // otherwise, properties are associated to a property set
      else
      {
         PropertySet mdoSet = acquirePropertySet( mdoAnchor, omr,  sSetRole, sPropertySetName );
         if ((fFlags & USE_PROPERTYSET_PROPERTIES) != 0)
         {
            // S0505433: We need to ensure these are not duplicated.
            if ( ! mdoSet.getProperties(    false ).contains(mdoProperty))
            {
               mdoSet.getProperties(    false ).add( mdoProperty );
            }
         }
         else
         {
            // S0505433: We need to ensure these are not duplicated.
            if ( ! mdoSet.getSetProperties( false ).contains(mdoProperty))
            {
               mdoSet.getSetProperties( false ).add( mdoProperty );
            }
         }
      }

      return mdoProperty;
   }

   /**
    * Acquires a property set of the given name.  If the named property set
    * already exists, the existing PropertySet is returned.  Otherwise, a new
    * PropertySet is created and attached to the object.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the metadata anchor object for the property set
    * @param sSetRole   the set role of the property set
    *
    * @return the property set
    *
    * @throws MdException
    * @throws RemoteException
    */
   public PropertySet acquirePropertySet( OMRAdapter omr, Root mdoAnchor, String sSetRole)
   throws MdException, RemoteException
   {
      return acquirePropertySet( mdoAnchor, omr,  sSetRole, "" );
   }

   /**
    * Acquires a property set of the given name.  If the named property set
    * already exists, the existing PropertySet is returned.  Otherwise, a new
    * PropertySet is created and attached to the object.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the metadata anchor object for the property set
    * @param sSetRole   the set role of the property set
    * @param sPropertySetName the PropertySetName of the property set
    *
    * @return the property set
    *
    * @throws MdException
    * @throws RemoteException
    */
   public PropertySet acquirePropertySet( Root mdoAnchor, OMRAdapter omr,  String sSetRole, String sPropertySetName )
   throws MdException, RemoteException
   {
      String sSetID = (String) m_mapPropertySets.get(sSetRole + sPropertySetName);
      if (sSetID==null || sSetID.length()==0)
      {
          // if there isn't one, create one
          sSetID = createIDForNewObject();
          m_mapPropertySets.put(sSetRole + sPropertySetName, sSetID );
      }

      // create a property set object and initialize it
      PropertySet mdoSet = (PropertySet) omr.acquireOMRObject( sSetID, MetadataObjects.PROPERTYSET );
      mdoSet.setName(    sSetRole );
      mdoSet.setSetRole( sSetRole );
      mdoSet.setPropertySetName( sPropertySetName );

      // associate property set to the object
      // S0505433: We need to ensure these are not duplicated.
      if ( ! mdoAnchor.getPropertySets( false ).contains(mdoSet))
      {
         mdoAnchor.getPropertySets( false ).add( mdoSet );
      }

      return mdoSet;
   }

   //-------------------------------------------
   // Methods for loading options and properties
   //-------------------------------------------
   /**
    * Loads a string option from the default OPTIONS PropertySet from OMR.  If
    * the option can not be found, the default value specified by sDefaultValue
    * is returned.
    *
    * @param omr           the OMR adapter
    * @param sName         the option name
    * @param sDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (sDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public String loadStringOptionFromOMR( OMRAdapter omr, String sName, String sDefaultValue ) throws MdException, RemoteException
   {
      return loadStringOptionFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, sDefaultValue );
   }

   
   /**
    * Loads an integer option from the default OPTIONS PropertySet from OMR.
    * If the option can not be found, the default value specified by
    * iDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sName         the option name
    * @param iDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (iDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public Double loadDoubleOptionFromOMR( OMRAdapter omr, String sName, Double iDefaultValue ) throws RemoteException, MdException
   {
      return loadDoubleOptionFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, iDefaultValue );
   }

   /**
    * Loads an integer option from OMR.  If the option can not be found, the
    * default value specified by iDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the option set role
    * @param sName         the option name
    * @param iDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (iDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public Double loadDoubleOptionFromOMR( OMRAdapter omr, String sSetRole, String sName, Double iDefaultValue ) throws RemoteException, MdException
   {
      String sValue = loadOptionFromOMRImpl( omr, sSetRole, sName );
      if (sValue != null)
      {
         try
         {
            return Double.parseDouble( sValue );
         }
         catch (NumberFormatException ex)
         {
            // fall through to return default value
         }
      }
      return iDefaultValue;
   }
   /**
    * Loads an integer option from the default OPTIONS PropertySet from OMR.
    * If the option can not be found, the default value specified by
    * iDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sName         the option name
    * @param iDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (iDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public int loadIntOptionFromOMR( OMRAdapter omr, String sName, int iDefaultValue ) throws RemoteException, MdException
   {
      return loadIntOptionFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, iDefaultValue );
   }

   /**
    * Loads a boolean option from the default OPTIONS PropertySet from OMR.  If
    * the option can not be found, the default value specified by bDefaultValue
    * is returned.
    *
    * @param omr           the OMR adapter
    * @param sName         the option name
    * @param bDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (bDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public boolean loadBooleanOptionFromOMR( OMRAdapter omr, String sName, boolean bDefaultValue ) throws RemoteException, MdException
   {
      return loadBooleanOptionFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, bDefaultValue );
   }

   /**
    * Loads an object list option from the default OPTIONS PropertySet from OMR.
    * If the option can not be found, an empty array is returned.
    *
    * @param omr   the OMR adapter
    * @param sName the option name
    *
    * @return the option value which is an array of persistable objects
    *
    * @throws MdException
    * @throws RemoteException
    */
//   protected IPersistableObject[] loadObjectListOptionFromOMR( OMRAdapter omr, String sName ) throws RemoteException, MdException
//   {
//      return loadObjectListPropertyFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, USE_PROPERTYSET_SETPROPERTIES );
//   }

   /**
    * Loads an option from the default OPTIONS PropertySet from OMR.  If the
    * option can not be found, the default value specified by sDefaultValue is
    * returned.
    *
    * @param omr           the OMR adapter
    * @param sName         the option name
    * @param sDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (sDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public String loadOptionFromOMR( OMRAdapter omr, String sName, String sDefaultValue ) throws MdException, RemoteException
   {
      return loadOptionFromOMR( omr, OPTIONS_PROPERTYSET_NAME, sName, sDefaultValue );
   }

   /**
    * Loads a string option from OMR.  If the option can not be found, the
    * default value specified by sDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the option set role
    * @param sName         the option name
    * @param sDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (sDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public String loadStringOptionFromOMR( OMRAdapter omr, String sSetRole, String sName, String sDefaultValue ) throws MdException, RemoteException
   {
      String sValue = loadOptionFromOMRImpl( omr, sSetRole, sName );
      return (sValue == null) ? sDefaultValue : sValue;
   }

   /**
    * Loads an integer option from OMR.  If the option can not be found, the
    * default value specified by iDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the option set role
    * @param sName         the option name
    * @param iDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (iDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public int loadIntOptionFromOMR( OMRAdapter omr, String sSetRole, String sName, int iDefaultValue ) throws RemoteException, MdException
   {
      String sValue = loadOptionFromOMRImpl( omr, sSetRole, sName );
      if (sValue != null)
      {
         try
         {
            return Integer.parseInt( sValue );
         }
         catch (NumberFormatException ex)
         {
            // fall through to return default value
         }
      }
      return iDefaultValue;
   }

   /**
    * Loads a boolean option from OMR.  If the option can not be found, the
    * default value specified by bDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the option set role
    * @param sName         the option name
    * @param bDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (bDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public boolean loadBooleanOptionFromOMR( OMRAdapter omr, String sSetRole, String sName, boolean bDefaultValue ) throws RemoteException, MdException
   {
      String sValue = loadOptionFromOMRImpl( omr, sSetRole, sName );
      if (sValue != null)
//         return Boolean.parseBoolean( sValue );  // JRE1.5
          return Boolean.valueOf(sValue ).booleanValue();
      return bDefaultValue;
   }

   /**
    * Loads an option from OMR.  If the option can not be found, the default
    * value specified by sDefaultValue is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the option set role
    * @param sName         the option name
    * @param sDefaultValue the default value for the option if the option is not
    *                      found (this is only needed for compatibility with 3.4
    *                      although it would help in the case of corrupt
    *                      metadata)
    *
    * @return the option value (sDefaultValue if the option is not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   public String loadOptionFromOMR( OMRAdapter omr, String sSetRole, String sName, String sDefaultValue ) throws MdException, RemoteException
   {
      String sValue = loadOptionFromOMRImpl( omr, sSetRole, sName );
      return (sValue == null) ? sDefaultValue : sValue;
   }

   /**
    * Loads the option from OMR.  Convenience method.
    *
    * @param omr      the OMR adapter
    * @param sSetRole the option set role
    * @param sName    the option name
    *
    * @return the option value (null = not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   private String loadOptionFromOMRImpl( OMRAdapter omr, String sSetRole, String sName ) throws MdException, RemoteException
   {
      return loadPropertyFromOMRImpl( omr, omr.acquireOMRObject( this ), sSetRole, sName, USE_PROPERTYSET_SETPROPERTIES );
   }

   /**
    * Loads user properties from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    */
   public void loadUserPropertiesFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      PropertySet mdoSet = findPropertySet( omr, omr.acquireOMRObject( this ), USERPROPERTIES_PROPERTYSET_NAME, 0 );
      if (mdoSet == null)
         return;

      List lProperties = mdoSet.getSetProperties();
      for ( int iProperty=0; iProperty<lProperties.size(); iProperty++ )
      {
         Property mdoProperty = (Property) lProperties.get( iProperty );
         setUserProperty( mdoProperty.getPropertyName(), mdoProperty.getDefaultValue() );
         m_mapProperties.put( USERPROPERTIES_PROPERTYSET_NAME + mdoProperty.getPropertyName(), new PropertyTracker( mdoProperty.getFQID() ) );
      }
   }

   /**
    * Loads a property from OMR.  If the property is not found, the default
    * value is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    * @param sDefaultValue the default property value
    * @param eWhere        where to look for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.
    * @return the property value (sDefaultValue if no property found)
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use loadStringOptionFromOMR, loadIntOptionFromOMR, or
    *             loadBooleanOptionFromOMR instead.
    */
   protected String loadPropertyFromOMR( OMRAdapter omr, String sSetRole, String sPropertyName, String sDefaultValue, int eWhere ) throws MdException, RemoteException
   {
      String sValue = loadPropertyFromOMRImpl( omr, omr.acquireOMRObject( this ), sSetRole, sPropertyName, eWhere );
      return (sValue == null) ? sDefaultValue : sValue;
   }

   /**
    * Loads a property from OMR.  If the property is not found, the default
    * value is returned.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     The metadata object that the property is attached to
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    * @param sDefaultValue the default property value
    * @param eWhere        where to look for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.
    * @return the property value (sDefaultValue if no property found)
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use loadStringOptionFromOMR, loadIntOptionFromOMR, or
    *             loadBooleanOptionFromOMR instead.
    */
   protected String loadPropertyFromOMR( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertyName, String sDefaultValue, int eWhere ) throws MdException, RemoteException
   {
      String sValue = loadPropertyFromOMRImpl( omr, mdoAnchor, sSetRole, sPropertyName, eWhere );
      return (sValue == null) ? sDefaultValue : sValue;
   }

   /**
    * Loads a property from OMR.  If the property is not found, null is
    * returned.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     the anchor metadata object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    * @param eWhere        where to look for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.
    *
    * @return the property value (null if no property found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   private String loadPropertyFromOMRImpl( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertyName, int eWhere ) throws MdException, RemoteException
   {
      Property mdoProperty = findProperty( omr, mdoAnchor, sSetRole, sPropertyName, eWhere );
      return (mdoProperty == null) ? null : mdoProperty.getDefaultValue();
   }

   /**
    * Loads an object list property from OMR.  If the property can not be found,
    * an empty array is returned.
    *
    * @param omr           the OMR adapter
    * @param mdoAnchor     The metadata object that the property is attached to
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    * @param eWhere        where to look for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.
    *
    * @return the option value which is an array of persistable objects
    *
    * @throws MdException
    * @throws RemoteException
    */
    public IPersistableObject[] loadObjectListPropertyFromOMR( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertyName, int eWhere ) throws MdException, RemoteException
   {
      Property mdoProperty = findProperty( omr, mdoAnchor, sSetRole, sPropertyName, eWhere );
      if (mdoProperty != null)
      {
         List lTransforms = mdoProperty.getSpecTargetTransformations();
         if (!lTransforms.isEmpty())
         {
            Transformation mdoHolder = (Transformation) lTransforms.get( 0 );
            PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sPropertyName );
            tracker.m_sHolderFQID = mdoHolder.getFQID();

            omr.populateAssociations(omr.getOMRFactory(), mdoHolder);

            List lObjects = mdoHolder.getSourceSpecifications();
            IPersistableObject[] aObjects = new IPersistableObject[ lObjects.size() ];
            for ( int iObject=0; iObject<aObjects.length; iObject++ )
               aObjects[ iObject ] = (IPersistableObject) omr.acquireObject( (Root) lObjects.get( iObject ) );

            return aObjects;
         }
      }

      return new IPersistableObject[0];
   }

    /**
     * Loads a string option from OMR.  If the option can not be found, the
     * default value specified by sDefaultValue is returned.
     *
     * @param omr           the OMR adapter
     * @param sSetRole      the option set role
     * @param sName         the option name
     * @param sDefaultValue the default value for the option if the option is not
     *                      found (this is only needed for compatibility with 3.4
     *                      although it would help in the case of corrupt
     *                      metadata)
     *
     * @return the option value (sDefaultValue if the option is not found)
     *
     * @throws MdException
     * @throws RemoteException
     */
    public IPersistableObject[] loadOptionWithCustomListFromOMR( OMRAdapter omr, String sSetRole, String sName, String sDefaultValue ) throws MdException, RemoteException
    {

       Property mdoProperty = findProperty(omr, omr.acquireOMRObject(this), sSetRole, sName, USE_PROPERTYSET_SETPROPERTIES);
       if (mdoProperty != null)
       {
          List lAssocs = mdoProperty.getCustomAssociations();
          if (!lAssocs.isEmpty())
          {
             CustomAssociation mdoCustAssoc = (CustomAssociation) lAssocs.get( 0 );
             PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sName );
             tracker.m_sCustomAssocFQID = mdoCustAssoc.getFQID();
             List lObjects = mdoCustAssoc.getAssociatedObjects();
             IPersistableObject[] aObjects = new IPersistableObject[ lObjects.size() ];
             for ( int i=0; i < aObjects.length; i++ )
                aObjects [ i ] = (IPersistableObject) omr.acquireObject( (Root) lObjects.get( i ) );

             return aObjects;
          }
       }


       return new IPersistableObject[0];
    }

   /**
    * Loads an object list property from OMR.  If the property can not be found,
    * an empty array is returned.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    * @param eWhere        where to look for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.
    *
    * @return the option value which is an array of persistable objects
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated use loadObjectListOptionFromOMR
    */
   public IPersistableObject[] loadObjectListPropertyFromOMR( OMRAdapter omr, String sSetRole, String sPropertyName, int eWhere ) throws MdException, RemoteException
   {
      return (loadObjectListPropertyFromOMR( omr, omr.acquireOMRObject( this ), sSetRole, sPropertyName, eWhere ));
   }

   /**
    * Deletes a property from OMR.
    *
    * @param omr           the OMR adapter
    * @param sSetRole      the set role of the property set use
    * @param sPropertyName the property's name (actually its PropertyName)
    *
    * @throws MdException
    * @throws RemoteException
    *
    * @deprecated Use loadStringOptionFromOMR, loadIntOptionFromOMR, or
    *             loadBooleanOptionFromOMR instead.
    */
   protected void deletePropertyFromOMR( OMRAdapter omr, String sSetRole, String sPropertyName ) throws MdException, RemoteException
   {
      // get the property object if there is one
      PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sPropertyName );
      if (tracker != null)
      {
         deleteProperty( omr, tracker );
         m_mapProperties.remove( sSetRole+sPropertyName ); /*I18NOK:COS**/
      }
   }

   /**
    * Finds the specified property.
    *
    * @param omr           the omr adapter
    * @param mdoAnchor     the anchor metadata object for the property set or
    *                      property
    * @param sSetRole      the set role of the property set to use
    * @param sPropertyName the name of the property to find
    * @param fFlags        flags used to control search for the property:
    *                      USE_PROPERTIES_DIRECTLY means to look in the object's
    *                      Properties association, USE_PROPERTYSET_PROPERTIES
    *                      means look in the specified PropertySet's Properties
    *                      association, and USE_PROPERTYSET_SETPROPERTIES means
    *                      look in the specified PropertySet's SetProperties
    *                      association.  By default, sPropertyName is compared
    *                      to the property's PropertyName.  Adding COMPARE_NAME
    *                      to the flags indicates that sPropertyName should be
    *                      compared to the property's Name.
    *
    * @return the property (null = not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected Property findProperty( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertyName, int fFlags ) throws MdException, RemoteException
   {
      // if property has already been found, go directly to it
      PropertyTracker tracker = (PropertyTracker) m_mapProperties.get( sSetRole + sPropertyName );
      if (tracker != null)
         return (Property) omr.acquireOMRObject( tracker.m_sFQID, MetadataObjects.PROPERTY );

      // get a properties list to search
      List lProperties;
      if ((fFlags & USE_PROPERTIES_DIRECTLY) != 0)
         lProperties = mdoAnchor.getProperties();

      else
      {
         PropertySet mdoSet = findPropertySet( omr, mdoAnchor, sSetRole, fFlags );
         if (mdoSet == null)
            return null;

         if ((fFlags & USE_PROPERTYSET_PROPERTIES) != 0)
            lProperties = mdoSet.getProperties();
         else
            lProperties = mdoSet.getSetProperties();
      }

      // search the list
      boolean bUsePropertyName = ((fFlags & COMPARE_NAME) == 0);
      for ( int iProperty=0; iProperty<lProperties.size(); iProperty++ )
      {
         Property mdoProperty = (Property) lProperties.get( iProperty );
         //Should never return null, unless the property gets deleted but
         //not removed from the list.
         if (mdoProperty != null)
         {
            String sTestName = bUsePropertyName ? mdoProperty.getPropertyName()
                                                : mdoProperty.getName();
            if (sTestName.equalsIgnoreCase( sPropertyName ))
            {
               m_mapProperties.put( sSetRole + sPropertyName, new PropertyTracker( mdoProperty.getFQID() ) );
               return mdoProperty;
            }
         }
      }

      // not found
      return null;
   }

   /**
    * Finds a property set.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the anchor metadata object for the property set
    * @param sSetRole   the set role of the property set
    * @param fFlags     flags used to control search for the property set:
    *                   By default, sSetRole is compared to the propertyset's
    *                   SetRole.  Adding COMPARE_PROPERTYSET_NAME to the
    *                   flags indicates that sSetRole should be compared to
    *                   the propertyset's Name.
    *
    * @return the property set (null = not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected PropertySet findPropertySet( OMRAdapter omr, Root mdoAnchor, String sSetRole, int fFlags ) throws MdException, RemoteException
   {
      return findPropertySet( omr, mdoAnchor, sSetRole, "", fFlags );
   }

   /**
    * Finds a property set.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the anchor metadata object for the property set
    * @param sSetRole   the set role of the property set
    * @param fFlags     flags used to control search for the property set:
    *                   By default, sSetRole is compared to the propertyset's
    *                   SetRole.  Adding COMPARE_PROPERTYSET_NAME to the
    *                   flags indicates that sSetRole should be compared to
    *                   the propertyset's Name.
    * @param sPropertySetName the PropertySetName of the property set
    *
    * @return the property set (null = not found)
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected PropertySet findPropertySet( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertySetName, int fFlags ) throws MdException, RemoteException
   {
      // if property set has already been found, go directly to it
      String sSetID = (String) m_mapPropertySets.get(sSetRole + sPropertySetName);
      if (sSetID != null)
         return (PropertySet) omr.acquireOMRObject( sSetID, MetadataObjects.PROPERTYSET );

      // search the list of property sets for the right one
      List lSets = mdoAnchor.getPropertySets();
      for ( int iSet=0; iSet<lSets.size(); iSet++ )
      {
         PropertySet mdoSet = (PropertySet) lSets.get( iSet );

         String sCompareString;
         if ( ( fFlags & COMPARE_PROPERTYSET_NAME ) != 0 )
            sCompareString = mdoSet.getName();
         else if ( ( fFlags & COMPARE_PROPERTYSET_PROPERTYSETNAME ) != 0 )
            sCompareString = mdoSet.getPropertySetName();
         // rlr: what?  this if never passes
         else if ( ( fFlags & COMPARE_PROPERTYSET_PROPERTYSETNAME & COMPARE_PROPERTYSET_SETROLE) != 0 )
            sCompareString = mdoSet.getSetRole() + mdoSet.getPropertySetName();
         else
            sCompareString = mdoSet.getSetRole();

         if (sCompareString.equalsIgnoreCase( sSetRole + sPropertySetName))
         {
            m_mapPropertySets.put(sSetRole + sPropertySetName, mdoSet.getFQID() );
            return mdoSet;
         }
      }

      return null;
   }

   /**
    * Finds property sets with the same name and returns a list containing those objects.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the anchor metadata object for the property set
    * @param sSetRole   the set role of the property set
    * @param fFlags     flags used to control search for the property set:
    *                   By default, sSetRole is compared to the propertyset's
    *                   SetRole.  Adding COMPARE_PROPERTYSET_NAME to the
    *                   flags indicates that sSetRole should be compared to
    *                   the propertyset's Name.
    *
    * @return List of matching PropertySet objects
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected List findSameNamePropertySetsObjects( OMRAdapter omr, Root mdoAnchor, String sSetRole, int fFlags ) throws MdException, RemoteException
   {
      return findSameNamePropertySetsObjects( omr, mdoAnchor, sSetRole, "", fFlags );
   }
   
   /**
    * Finds property sets with the same name and returns a list containing those objects.
    *
    * @param omr        the OMR adapter
    * @param mdoAnchor  the anchor metadata object for the property set
    * @param sSetRole   the set role of the property set
    * @param fFlags     flags used to control search for the property set:
    *                   By default, sSetRole is compared to the propertyset's
    *                   SetRole.  Adding COMPARE_PROPERTYSET_NAME to the
    *                   flags indicates that sSetRole should be compared to
    *                   the propertyset's Name.
    * @param sPropertySetName the PropertySetName of the property set
    *
    * @return List of matching PropertySet objects
    *
    * @throws MdException
    * @throws RemoteException
    */
   protected List findSameNamePropertySetsObjects( OMRAdapter omr, Root mdoAnchor, String sSetRole, String sPropertySetName, int fFlags ) throws MdException, RemoteException
   {
	   List sameNamePropertySets = new ArrayList();
	   
      // search the list of property sets for the right one
	   omr.populateAssociations(omr.getOMRFactory(), mdoAnchor);
      List lSets = mdoAnchor.getPropertySets(false);
      for ( int iSet=0; iSet<lSets.size(); iSet++ )
      {
         PropertySet mdoSet = (PropertySet) lSets.get( iSet );

         String sCompareString;
         if ( ( fFlags & COMPARE_PROPERTYSET_NAME ) != 0 )
            sCompareString = mdoSet.getName();
         else if ( ( fFlags & COMPARE_PROPERTYSET_PROPERTYSETNAME ) != 0 )
            sCompareString = mdoSet.getPropertySetName();
         // rlr: what?  this if never passes
         else if ( ( fFlags & COMPARE_PROPERTYSET_PROPERTYSETNAME & COMPARE_PROPERTYSET_SETROLE) != 0 )
            sCompareString = mdoSet.getSetRole() + mdoSet.getPropertySetName();
         else
            sCompareString = mdoSet.getSetRole();

         if (sCompareString.equalsIgnoreCase( sSetRole + sPropertySetName))
         {
        	 sameNamePropertySets.add(mdoSet);         
         }
      }

      return sameNamePropertySets;
   }

   /**
    * Adds an object to the deleted objects list so that the object will be
    * deleted when the object is persisted.
    *
    * @param obj the object
    */
   protected void addToDeletedObjects( IPersistableObject obj )
   {
      if (m_lDeletedObjects == null)
         m_lDeletedObjects = new ArrayList();

      m_lDeletedObjects.add( obj );
      getModel().removeObject( obj );
      obj.delete();
   }

   /**
    * Removes an object from the deleted objects list.  Typically this is called
    * because a remove of the object has been undone.
    *
    * @param obj the object
    */
   protected void removeFromDeletedObjects( IPersistableObject obj )
   {
      if (obj == null)
         return;
      if (m_lDeletedObjects == null)
         // TODO throw an exception?
         return;

      m_lDeletedObjects.remove( obj );
      getModel().putObject( obj );
   }

   /**
    * Returns the list of deleted objects
    * @return deleted object list
    */
   protected List getDeletedObjects()
   {
      if (m_lDeletedObjects == null)
         m_lDeletedObjects = new ArrayList();

      return m_lDeletedObjects;
   }

   /**
    * Deletes the objects on the deleted objects list from OMR.
    *
    * @param omr the OMR adapter
    *
    * @throws MdException
    * @throws RemoteException
    */
   private void deleteDeletedObjectsFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (m_lDeletedObjects == null)
         return;

      for ( int iObject=0; iObject<m_lDeletedObjects.size(); iObject++ )
         ((IPersistableObject) m_lDeletedObjects.get( iObject )).deleteFromOMR( omr );

      m_lDeletedObjects.clear();
   }

   private static class PropertyTracker
   {
      String m_sFQID;
      String m_sHolderFQID;
      String m_sCustomAssocFQID;

      PropertyTracker( String sFQID )
      {
         m_sFQID = sFQID;
      }
   }
}