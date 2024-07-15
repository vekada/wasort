/* $Id$ */
/**
 * Title:       AbstractComplexPersistableObject.java
 * Description: An abstract implementation of a complex persistable object
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.impl;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.StringHolder;

import com.sas.etl.models.IComplexPersistableObject;
import com.sas.etl.models.IModel;
import com.sas.etl.models.data.IDocument;
import com.sas.etl.models.data.IFolder;
import com.sas.etl.models.other.IExtendedAttribute;
import com.sas.etl.models.other.INote;
import com.sas.etl.models.other.IResponsibleParty;
import com.sas.iom.SASIOMDefs.GenericError;
import com.sas.meta.SASOMI.ISecurity;
import com.sas.meta.SASOMI.ISecurityPackage.InvalidCredHandle;
import com.sas.meta.SASOMI.ISecurityPackage.InvalidResourceSpec;
import com.sas.meta.SASOMI.ISecurityPackage.NotTrustedUser;
import com.sas.metadata.remote.Document;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MdOMRConnection;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.PrimaryType;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.TextStore;
import com.sas.metadata.remote.Tree;
import com.sas.services.information.metadata.PathUrl;
import com.sas.services.information.util.SmartTypes;
import com.sas.util.UsageVersion;
import com.sas.workspace.MessageUtil;
import com.sas.workspace.Workspace;
import com.sas.workspace.WsAbstractVersionedObjectManager;

/**
 * AbstractComplexPersistableObject is an abstract implementation of a 
 * complex persistable object.  Complex persistable object can contain 
 * relationships to other objects.
 */
public abstract class AbstractComplexPersistableObject extends AbstractPersistableObject implements IComplexPersistableObject
{
//   public static final String UNIQUE_ID_NAME        = "DIS_ID";     // I18NOK:EMS
//   public static final String UNIQUE_ID_DESCRIPTION = "Object identifier in Data Integration Studio";     // I18NOK:EMS        
   private ModelList m_lNotes;
   private ModelList m_lDocuments;
   private ModelList m_lExtendedAttributes;
   private ModelList m_lResponsibleParties;
   
   private String    m_sPrivateNote;
   private String    m_sPrivateNoteID;
   
   private IFolder   m_folder;
   private String    m_sPreviousFolderID;
   
   private boolean   m_bEditable;

   private UsageVersion m_usageVersion;
   
   /**
    * Constructs an abstract persistable object.
    * 
    * @param sID   the object id
    * @param model the model 
    */
   public AbstractComplexPersistableObject( String sID, IModel model )
   {
      super( sID, model );
      
      m_lNotes              = new ModelList( this, new String[]{ NOTE_ADDED,               NOTE_REMOVED               }, ModelList.SAVE_CHANGED_OBJECTS,     INote             .class );
      m_lDocuments          = new ModelList( this, new String[]{ DOCUMENT_ADDED,           DOCUMENT_REMOVED           }, ModelList.SAVE_CHANGED_OBJECTS,     IDocument         .class );
      m_lExtendedAttributes = new ModelList( this, new String[]{ EXTENDED_ATTRIBUTE_ADDED, EXTENDED_ATTRIBUTE_REMOVED }, ModelList.SAVE_AS_OWNER_OF_OBJECTS, IExtendedAttribute.class );
      m_lResponsibleParties = new ModelList( this, new String[]{ RESPONSIBLE_PARTY_ADDED,  RESPONSIBLE_PARTY_REMOVED  }, ModelList.SAVE_CHANGED_OBJECTS,     IResponsibleParty .class );
      
      m_sPrivateNoteID = "";
      m_bEditable = true;
      
      m_usageVersion = UsageVersion.decode(0);
   }
   
   /**
    * Sets the architecture version number (ex 1.02).  This method does not fire
    * any events, does not set the changed stated, and is not undoable.
    * 
    * @param version UsageVersion for the object
    *              
    * @see com.sas.etl.models.IOMRPersistable#setArchitectureVersionNumber(com.sas.util.UsageVersion)
    */
   protected void setArchitectureVersionNumber( UsageVersion version )
   {
//      if (major < 1)
//         throw new IllegalArgumentException( "The major architecture version number must be greater than or equal to 1: " + major );   // I18NOK:LINE
//      if ((minor < 1) || (minor > 99))
//         throw new IllegalArgumentException( "The minor architecture version number must be between 1 and 99 inclusive: " + minor );   // I18NOK:LINE
      
      //TODO this can prevent use from reading older objects
//      if (m_usageVersion.compareMajorMinor( version)>0)
//         throw new IllegalArgumentException( "The architecture version number must be greater than or equal to " + m_usageVersion.encode() );   // I18NOK:LINE
   }

   /**
    * Gets the architecture version number (ex 1.01).
    * 
    * @return the architecture version number
    * 
    * @see com.sas.etl.models.IComplexPersistableObject#getArchitectureVersionNumber()
    */
   public abstract UsageVersion getArchitectureVersionNumber();


   public final UsageVersion getUsageVersion()
   {
   	return m_usageVersion;
   }
   
   protected void setUsageVersion(UsageVersion version)
   {
   	if (version==null)
   		return;
   	
   	if (version.equals(m_usageVersion))
   		return;
   	
   	m_usageVersion = version;
   	
   	fireModelChangedEvent(USAGE_VERSION_CHANGED, m_usageVersion);
   }
   
   /**
    * Gets the metadata type of the object.
    * 
    * @return the metadata type (which is the same as the OMR type)
    * 
    * @see com.sas.workspace.models.SimpleObject#getMetadataType()
    */
   public String getMetadataType()
   {
      return getOMRType();
   }
   
      /**
    * Gets the metadata type of the object to use for the unique id in OMR.
    * The type may be MetadataObjects.EXTERNALIDENTITY.
    * 
    * @return MetadataObjects.CLASSIFIERMAP (the default)
    */
   protected String getExternalIdentityType()
   {
      return MetadataObjects.EXTERNALIDENTITY;
   }
   
  /**
    * Gets the notes associated to the object.
    * 
    * @return the notes
    * 
    * @see com.sas.etl.models.INotesContainer#getNotes()
    */
   public INote[] getNotes()
   {
      return (INote[]) m_lNotes.toArray( new INote[ m_lNotes.size() ] );
   }
   
   /**
    * Gets the list of notes associated to the object.  The list may be directly
    * modified in order to modify the notes associated with the object.
    * 
    * @return the notes list
    * 
    * @see com.sas.etl.models.INotesContainer#getNotesList()
    */
   public List getNotesList()
   {
      return m_lNotes;
   }
   
   /**
    * Gets the private note in the notes container. The container can only have 
    * one private note.
    * 
    * @return The private note for this container
    * 
    * @see com.sas.etl.models.INotesContainer#getPrivateNote()
    */
   public String getPrivateNote()
   {
      return m_sPrivateNote;
   }
   
   /**
    * Sets the private note in the notes container. The container can only have 
    * one private note.
    * 
    * @param sPrivateNote the private note
    * 
    * @see com.sas.etl.models.INotesContainer#setPrivateNote(com.sas.etl.models.other.IPrivateNote)
    */
   public void setPrivateNote( String sPrivateNote )
   {
      if (m_sPrivateNote == null)
      {
         if (sPrivateNote == null)
            return;
      }
      else
         if (m_sPrivateNote.equals( sPrivateNote ))
            return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetPrivateNoteUndoable( m_sPrivateNote, sPrivateNote ) );
      
      m_sPrivateNote = sPrivateNote;
      
      fireModelChangedEvent( PRIVATE_NOTE_CHANGED, m_sPrivateNote );   
   }
   
   /**
    * Get an array of any associated documents 
    * @see com.sas.etl.models.IDocumentsContainer#getDocuments()
    * 
    * @return an Array of Documents associated to this object
    */
   public IDocument[] getDocuments()
   {
      return (IDocument[]) m_lDocuments.toArray( new IDocument[ m_lDocuments.size() ] );
   }

   /**
    * Get a list of any associated documents 
    * @see com.sas.etl.models.IDocumentsContainer#getDocuments()
    * 
    * @return a List of Documents associated to this object
    */
   public List getDocumentsList()
   {
      return m_lDocuments;
   }

   /**
    * Gets the extended attributes in the extended attributes container.
    * 
    * @return the extended attributes
    * 
    * @see com.sas.etl.models.IExtendedAttributesContainer#getExtendedAttributes()
    */
   public IExtendedAttribute[] getExtendedAttributes()
   {
      return (IExtendedAttribute[]) m_lExtendedAttributes.toArray( new IExtendedAttribute[ m_lExtendedAttributes.size() ] );
   }
   
   /**
    * Gets the list of extended attributes in the extended attributes container.  The list may be directly
    * modified to modify the extended attributes in the container.  The methods in the list
    * that are not implemented will throw an exception.
    * 
    * @return the list of extended attributes
    * 
    * @see com.sas.etl.models.IExtendedAttributesContainer#getExtendedAttributesList()
    */
   public List getExtendedAttributesList()
   {
      return m_lExtendedAttributes;
   }
   
   /**
    * Gets the responsible parties for the container.
    * 
    * @return the responsible parties
    * 
    * @see com.sas.etl.models.other.IResponsiblePartyContainer#getResponsibleParties()
    */
   public IResponsibleParty[] getResponsibleParties()
   {
      return (IResponsibleParty[]) m_lResponsibleParties.toArray( new IResponsibleParty[ m_lResponsibleParties.size() ] );
   }

   /**
    * Gets the list of responsbile parties for the container.  The list may be
    * modified to modify the responsbile parties in the container.  The methods 
    * in the list that are not implemented will throw an exception.
    *  
    * @return the list of responsible parties
    * 
    * @see com.sas.etl.models.other.IResponsiblePartyContainer#getResponsiblePartiesList()
    */
   public List getResponsiblePartiesList()
   {
      return m_lResponsibleParties;
   }
   
   /**
    * Dumps the object into a stream using XML.
    * 
    * @param stream the stream
    * 
    * @see com.sas.etl.models.IComplexPersistableObject#dumpObjectToXML(java.io.PrintStream)
    */
   public void dumpObjectToXML(PrintStream stream)
   {
      stream.println( "<" + getPublicType() + 
                      " name=\"" + getName() + "\"" +
                      " description=\"" + getDescription() + "\"" +
                      " >");
      
      dumpXML(stream);
      
      stream.println( "</" + getPublicType() + ">");
   }
   
   protected void dumpXML(PrintStream stream)
   {
      stream.println("name=\"" + getName() + "\"");
   }
   
   /**
    * Get the sbip url for the object, uses the containing folders url, then the name of the object and type in parenthesis
    * @return sbip url /My Folder/.../New Job1(Job)
    * @see com.sas.etl.models.IComplexPersistableObject#getSBIPUrl()
    */
   public String getSBIPUrl()
   throws MdException
   {
      return getSBIPUrl(true);
   }
   
   /**
    * Get the sbip url for the object, uses the containing folders url, then the name of the object
    * @param showType true to get the url with the type in parenthesis
    * @return sbip url /My Folder/.../New Job1
    * @see com.sas.etl.models.IComplexPersistableObject#getSBIPUrl(boolean)
    */
   public String getSBIPUrl(boolean showType)
   throws MdException
   {
      String url = "";
      IFolder folder = getFolder();
      if (folder!=null)
      {
    	 PathUrl folderUrl = folder.getPathUrl();
    	 
    	 if (folderUrl==null)
    		 throw new MdException(MessageFormat.format(RB.getStringResource("AbstractComplexPersistableObject.FolderUrlNotFound.fmt"), getName(),getMetadataType()));
    	 
         if (!showType)
            url = folderUrl.getDisplayPath()+'/'+getName();
         else
            url = folderUrl.getDisplayPath()+'/'+getName()+'('+getPublicType()+')'; 
      }
      return url;
      
   }
   
//  /**
//   * get the unique id for this object
//   * @return the unique id
//   */
//   public String getUniqueId()
//   {
//   if (m_sUniqueId != null) return m_sUniqueId;
//   return m_sUniqueId = com.sas.entities.GUID.newGUID();
//   }
//   
   /**
    * Is the object changed since it was last persisted?
    * 
    * @return true = the object has changed
    */
   public boolean isChanged()
   {
      return super.isChanged() || m_lNotes.isChanged() 
                               || m_lDocuments.isChanged() 
                               || m_lExtendedAttributes.isChanged()
                               || m_lResponsibleParties.isChanged();
   }
   
//   /**
//    * Saves unique id to OMR.  The unique id is saved off of the
//    * metadata object.
//    * 
//    * @param omr the OMR adapter
//    * @param mdo the metadata object that will contain the unique id
//    *                  
//    * @throws MdException
//    * @throws RemoteException
//    */
//   protected void saveUniqueIdToOMR( OMRAdapter omr, Root mdo ) throws MdException, RemoteException
//   {
//       String sFQID = createIDForNewObject();
//    ExternalIdentity ident = (ExternalIdentity) omr.acquireOMRObject( sFQID, getExternalIdentityType() );
//    ident.setName(UNIQUE_ID_NAME);
//       ident.setDesc(UNIQUE_ID_DESCRIPTION);
//    ident.setContext("GUID");
//    ident.setIdentifier(m_sUniqueId);
//    mdo.getExternalIdentities().add(ident);   
//   }
   
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
      PrimaryType mdo = (PrimaryType) omr.acquireOMRObject( this );
      mdo.setPublicType(   isPublicObject() ? getPublicType()  : "" );    // clear out is for early 4.2, can be removed at any time
      mdo.setUsageVersion( getArchitectureVersionNumber().encode() );
      
      saveNotesAndDocumentsToOMR( omr, mdo );
      
      // TODO need changes to not delete text stores that aren't really notes
//      m_lNotes             .saveToOMR( omr, mdo.getNotes(      false ) );
//      m_lDocuments         .saveToOMR( omr, mdo.getDocuments(  false ) );
      m_lExtendedAttributes.saveToOMR( omr, mdo.getExtensions(         false ) );
      m_lResponsibleParties.saveToOMR( omr, mdo.getResponsibleParties( false ) );

      // if there is a folder and its id is not the previous folder's id,
      // add the new folder to the list of trees and remove the previous folder
      // if there was one
      List lTrees = mdo.getTrees( false );
      if ((m_folder != null) && !m_folder.getID().equals( m_sPreviousFolderID ))
      {
         lTrees.add( omr.acquireOMRObject( m_folder ) );
         if (m_sPreviousFolderID != null)
            lTrees.remove( omr.acquireOMRObject( m_sPreviousFolderID, MetadataObjects.TREE ) );
      }
      
      // if there is no folder but there was a previous folder
      // remove the previous folder from the list of trees
      else if ((m_folder == null) && (m_sPreviousFolderID != null))
         lTrees.remove( omr.acquireOMRObject( m_sPreviousFolderID, MetadataObjects.TREE ) );
      
      setChanged( false );
   }
   
   /**
    * Saves the notes (both public and private) and the documents for this 
    * object. 
    * @param omr The omr adapter
    * @param mdo The metadata object to save the note/documents under.
    * @throws MdException
    * @throws RemoteException
    */
   private void saveNotesAndDocumentsToOMR( OMRAdapter omr, Root mdo ) throws MdException, RemoteException
   {
      List lDocs = mdo.getDocuments( false );
      List lNotes = mdo.getNotes( false );
      
      //TODO: Do these need to be cleared?
      lDocs.clear();
      
      // NOT THIS ONE;  OTHER FOLKS ARE PUTTING STUFF IN HERE AND WE WILL DELETE THEIRS!!!
      // See S0505375
      // lNotes.clear();
      
      //First save the private note as a textstore off the notes association
      if (m_sPrivateNote != null && m_sPrivateNote.length() > 0)
      {
         String sID = (m_sPrivateNoteID.length() == 0) ? createIDForNewObject() : m_sPrivateNoteID;
         TextStore mdoText = (TextStore) omr.acquireOMRObject( sID, MetadataObjects.TEXTSTORE );
         mdoText.setName( PRIVATE_NOTE_NAME );
         mdoText.setStoredText( getPrivateNote() );
         mdoText.setTextRole(PRIVATE_NOTES_ROLE); // YES I want this to be a constant.
         
         // only add it if it is a new object, otherwise it will just get changed
         if (mdoText.isNewObject())
            lNotes.add( mdoText );
      }
      else
      {
         //Check if private note had existed at one point
         if (m_sPrivateNoteID.length() > 0)
         {
            omr.deleteOMRObject( m_sPrivateNoteID, MetadataObjects.TEXTSTORE );
         }
      }
      
      //Now save the public notes off of the documents association
      for (int iNote = 0; iNote < m_lNotes.size(); iNote++)
      {
         INote oNote = (INote) m_lNotes.get( iNote );
         oNote.saveToOMR( omr );
         lDocs.add( omr.acquireOMRObject( oNote ) );
      }
      
      //Now save the documents off of the documents association
      for (int iDoc = 0; iDoc < m_lDocuments.size(); iDoc++)
      {
         IDocument oDoc = (IDocument) m_lDocuments.get( iDoc );
         oDoc.saveToOMR( omr );
         lDocs.add( omr.acquireOMRObject( oDoc ) );
      }
   }

   /**
    * Updates the ids of the objects contained.  This method is overridden to
    * deal with the peculiarities of responsible party objects.
    * 
    * @param mapIDs the map of ids to be updated
    * 
    * @see com.sas.etl.models.impl.AbstractPersistableObject#updateIDs(java.util.Map)
    */
   public void updateIDs( Map mapIDs )
   {
      super.updateIDs( mapIDs );

      // save the current folder as the previous folder's id
      m_sPreviousFolderID = (m_folder != null) ? m_folder.getID() : null;
      
      // Because only one responsible party object exist per identity name and
      // responsible party role, the model can end up with multiple responsible
      // party objects with the same id.  This can happen if the user creates
      // multiple repsonsible parties for the same user with the same role.
      // Indeed, this can happen within the responsible parties of a single 
      // object.
      // The model will really work better if there is only one object in the
      // model with a given id.  So during an update ids, make sure the 
      // responsible party objects in the list are the ones the model is 
      // tracking in its object map.  Also make sure no duplicates exist in the
      // list.
      
      IResponsibleParty[] aOldRPs = getResponsibleParties();
      
      boolean bChanged       = isChanged();
      boolean bUndoSupported = isUndoSupported();  // undo supported should be false
      getModel().setUndoSupported( false );
      try
      {
         m_lResponsibleParties.clear();
         for ( int iOldRP=0; iOldRP<aOldRPs.length; iOldRP++ )
         {
            IResponsibleParty party = (IResponsibleParty) getModel().getObject( aOldRPs[iOldRP].getID() );
            if (!m_lResponsibleParties.contains( party ))
               m_lResponsibleParties.add( party );
         }
      }
      finally
      {
         getModel().setUndoSupported( bUndoSupported );
         setChanged( bChanged );
      }
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
      super.loadFromOMR( omr );
      Root mdo = omr.acquireOMRObject( this );

      loadEditableFromOMR( omr, mdo );
      
      loadNotesAndDocumentsFromOMR( omr, mdo );
      m_lExtendedAttributes.loadFromOMR( omr, mdo.getExtensions()         );
      m_lResponsibleParties.loadFromOMR( omr, mdo.getResponsibleParties() );
      
      // turn this object into a UsageVersion object
      setArchitectureVersionNumber( UsageVersion.decode(mdo.getUsageVersion()));

      // find the folder the object is in
      m_sPreviousFolderID = null;
      List lTrees = mdo.getTrees();
      for ( int iTree = 0; iTree<lTrees.size(); iTree++ )
      {
         Tree mdoTree = (Tree) lTrees.get( iTree );
         if (SmartTypes.TYPE_FOLDER.equals( mdoTree.getPublicType() ))
         {
            setFolder( (IFolder) omr.acquireObject( mdoTree ) );
            m_sPreviousFolderID = m_folder.getID();  // save the current folder as the old folder's id
         }
      }
      
      setUsageVersion(UsageVersion.decode(mdo.getUsageVersion()));
      
      setChanged( false );
   }
   
   /**
    * Loads the notes (both public and private) and the documents for this 
    * object. 
    * @param omr The omr adapter
    * @param mdo The metadata object that the notes/documents are being loaded from
    * @throws MdException
    * @throws RemoteException
    */
   private void loadNotesAndDocumentsFromOMR( OMRAdapter omr, Root mdo ) throws MdException, RemoteException
   {
      List lDocs  = mdo.getDocuments();
      List lNotes = mdo.getNotes();
      
      //First load the private note
      if (!lNotes.isEmpty())
      {
         // find ours in this list if it exists:
         for (int t=0;t<lNotes.size();t++)
         {
            TextStore oOurPossiblePrivateNote = (TextStore)lNotes.get(t);
            if (oOurPossiblePrivateNote.getTextRole().equalsIgnoreCase(PRIVATE_NOTES_ROLE))
            {
               TextStore mdoPrivateNote = (TextStore) lNotes.get(t);
               m_sPrivateNoteID = mdoPrivateNote.getFQID();
               setPrivateNote( mdoPrivateNote.getStoredText() );
               break;
            }
         }
      }
      
      //Now load the documents
      m_lNotes.clear();
      m_lDocuments.clear();
      for (int iDoc = 0; iDoc < lDocs.size(); iDoc++)
      {
         Document mdoDoc = (Document) lDocs.get( iDoc );
         // Check if this is a public note document
         if (mdoDoc.getPublicType().equals( NOTE_ROLE ))
         {
            //Add it as a note
            INote oNote = (INote) omr.acquireObject( mdoDoc );
            m_lNotes.add( oNote );
         }
         else
         {
            //Add it as a document
            IDocument oDoc = (IDocument) omr.acquireObject( mdoDoc );
            m_lDocuments.add( oDoc );
         }
      }
   }

   /**
    * Should this complex persistable object load whether it is editable?  This
    * method may be overridden to change whether the editable state of the 
    * object should be loaded.
    * 
    * @return true = load the editable state
    */
   protected boolean shouldLoadEditable()
   {
      return isPublicObject() && !isNew();
   }
   
   /**
    * Loads the editable state of the object from OMR.  The editable state is
    * only loaded for public primary objects.  If the object is not a public 
    * primary object, it is assumed to be editable because it's owning object
    * will control whether it is edited or not.
    * 
    * @param omr  the OMR adapter
    * @param mdo  the metadata object
    * 
    * @throws MdException
    * @throws RemoteException
    */
   private void loadEditableFromOMR( OMRAdapter omr, Root mdo ) throws MdException, RemoteException
   {
      if (!shouldLoadEditable())
         return;
      
      try
      {     
         // if object is checked out to a project, it can not be written
         if (mdo.getChangeState().startsWith( "Checked" ))     // I18NOK:EMS
         {
            m_bEditable = false;
            return;
         }
         
         // check for permission to write metadata
         if (Workspace.getMdFactory()!=null)
         {
	         MdOMRConnection connection = Workspace.getMdFactory().getConnection();
	         ISecurity security = connection.MakeISecurityConnection( connection.getCMRHandle() );
	
	         String resource   = "OMSOBJ:" + getOMRType() + "/" + getID(); /*I18nOK:LINE*/
	         String permission = "WriteMetadata";  /*I18nOK:LINE*/
	
	         StringHolder  permissionCondition = new StringHolder();
	         BooleanHolder authorized          = new BooleanHolder();
	         
	         security.IsAuthorized( "",resource,permission,permissionCondition,authorized );  /*I18nOK:LINE*/
	         m_bEditable = authorized.value;
         }
      }
      catch (NotTrustedUser ex)
      {
         ModelLogger.getDefaultLogger().error( "isObjectEditable exception", ex );
      }
      catch (InvalidCredHandle ex)
      {
         ModelLogger.getDefaultLogger().error( "isObjectEditable exception", ex );
      }
      catch (InvalidResourceSpec ex)
      {
         ModelLogger.getDefaultLogger().error( "isObjectEditable exception", ex );
      }
      catch (GenericError ex)
      {
         ModelLogger.getDefaultLogger().error( "isObjectEditable exception", ex );
      }
   }
   
   /**
    * Is the object editable?  If the object is not editable, outside forces 
    * should not change the object.  A non-editable object will not prevent
    * changes made to the object.
    * 
    * @return true = the object is editable
    */
   public boolean isEditable()
   {
      return m_bEditable;
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

      m_lExtendedAttributes.deleteFromOMR( omr );
      
      //delete the private note textstore if it exists
      if (m_sPrivateNoteID.length() > 0)
      {
         omr.deleteOMRObject( m_sPrivateNoteID, MetadataObjects.TEXTSTORE );
         m_sPrivateNoteID = "";
      }
      
      super.deleteFromOMR( omr );
   }

   /**
    * Gets the template map used to populate an OMR adapter for a table.
    * 
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssociations = (List) map.get( getOMRType() );
      lAssociations.add( Root.ASSOCIATION_TREES_NAME              );
      lAssociations.add( Root.ASSOCIATION_DOCUMENTS_NAME          );
      lAssociations.add( Root.ASSOCIATION_NOTES_NAME              );
      lAssociations.add( Root.ASSOCIATION_EXTENSIONS_NAME         );
      lAssociations.add( Root.ASSOCIATION_RESPONSIBLEPARTIES_NAME );
      
      return map;
   }
   
   /**
    * Is the object a public object?
    * 
    * @return true = the object is a public object 
    * 
    * @see com.sas.workspace.models.SimpleObject#isPublicObject()
    */
   public boolean isPublicObject()
   {
      return true;
   }

   /**
    * Sets the folder on the complex persistable object.
    * 
    * @param folder the folder (null = remove the object from any folder)
    * 
    * @see com.sas.etl.models.IComplexPersistableObject#setFolder(com.sas.etl.models.data.IFolder)
    */
   public void setFolder( IFolder folder )
   {
      if (m_folder == folder)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetFolderUndoable(m_folder, folder) );
      m_folder = folder;
      fireModelChangedEvent( FOLDER_CHANGED, m_folder );
   }

   /**
    * Gets the folder in which the complex persistable object resides.
    * 
    * @return the folder (null = the object is not in a folder)
    * 
    * @see com.sas.etl.models.IComplexPersistableObject#getFolder()
    */
   public IFolder getFolder()
   {
      return m_folder;
   }
   

   /**
    * SetFolderUndoable is the undoable for setting the folder
    */
   private class SetFolderUndoable extends AbstractUndoableEdit
   {
      private IFolder m_bOldFolder;
      private IFolder m_bNewFolder;
      
      /**
       * Constructs the set folder undoable.
       * 
       * @param bOldFolder the old folder attribute
       * @param bNewFolder the new folder attribute
       */
      public SetFolderUndoable( IFolder bOldFolder, IFolder bNewFolder )
      {
         m_bOldFolder = bOldFolder;
         m_bNewFolder = bNewFolder;
      }
      
      /**
       * Undoes the setting of the folder attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setFolder( m_bOldFolder );
      }
      
      /**
       * Redoes the setting of the folder attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setFolder( m_bNewFolder );
      }
   } // SetFolderUndoable
   
   /**
    * SetPrivateNoteUndoable is the undoable for setting the folder
    */
   private class SetPrivateNoteUndoable extends AbstractUndoableEdit
   {
      private String m_sOldPrivateNote;
      private String m_sNewPrivateNote;
      
      /**
       * Constructs the set folder undoable.
       * 
       * @param sOldPrivateNote the old folder attribute
       * @param sNewPrivateNote the new folder attribute
       */
      public SetPrivateNoteUndoable( String sOldPrivateNote, String sNewPrivateNote )
      {
         m_sOldPrivateNote = sOldPrivateNote;
         m_sNewPrivateNote = sNewPrivateNote;
      }
      
      /**
       * Undoes the setting of the folder attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setPrivateNote( m_sOldPrivateNote );
      }
      
      /**
       * Redoes the setting of the folder attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setPrivateNote( m_sNewPrivateNote );
      }
   } // SetPrivateNoteUndoable
   
   /**
    * Is the version of the object valid for the current application?
    * 
    * @param publicType
    * @param instanceVersion
    * @param ti
    * @param objInst
    * @return true if the version is valid
    */
   private boolean isValidVersion (WsAbstractVersionedObjectManager objInst)
   {
      boolean returnValue = true;
      if ( objInst != null && !objInst.isVersionSupported(getPublicType(), getUsageVersion()))
      {
         returnValue = false;
//         MessageUtil.displayDetailsMessage(bundle.getString("OpenSelectedObjectsAction.OpenNotSupported.Message.txt"), details);
      }
      return returnValue;
   }
   
   public boolean isComplete()
   {
   	return super.isComplete() && isValidVersion(WsAbstractVersionedObjectManager.getInstance());
   }
   
   public List getReasonsIncomplete()
   {
   	List lst = super.getReasonsIncomplete();
   	
   	if (!isValidVersion(WsAbstractVersionedObjectManager.getInstance()))
   	{
   		lst.add(MessageFormat.format(RB.getStringResource("AbstractComplexPersistableObject.VersionNotSupported.Message.txt"), getUsageVersion().toString(), getName()));
   	}
   	
   	return lst;
   }
}
