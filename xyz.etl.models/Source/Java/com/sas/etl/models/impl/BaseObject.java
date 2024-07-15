/* $Id$ */
/**
 * Title:       BaseObject.java
 * Description: A base implementation of an object.
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import com.sas.etl.models.IModel;
import com.sas.etl.models.INotifyListener;
import com.sas.etl.models.IObject;
import com.sas.etl.models.IObjectFactory;
import com.sas.etl.models.NotifyEvent;

/**
 * BaseObject is a base implementation of an object.
 */
public class BaseObject implements IObject, INotifyListener
{
	
   private String  m_sID;
   private String  m_sName;
   private String  m_sDescription;
   private boolean m_bChanged;
   private Map     m_mapUserProperties;

   private IModel  m_model;

   private List    m_lListeners;
   
   /**
    * Constructs the object.
    * 
    * @param sID   the object's id
    * @param model the object's model
    */
   public BaseObject( String sID, IModel model )
   {
      m_sID          = sID;
      m_sName        = getDefaultName();
      m_sDescription = "";
      m_model        = model;
      m_bChanged     = model.isNewObjectID( sID );
      
      m_model.putObject( this );
   }

   /**
    * Gets the id of the object
    * 
    * @return true = the id of the object
    * 
    * @see com.sas.etl.models.IObject#getID()
    */
   public String getID()
   {
      return m_sID;
   }
   
   /**
    * Sets the id of the object.  This is intended to only be used by OMR to 
    * update the object's id after a new object has been created.
    * 
    * @param sID the object's id
    */
   protected void setID( String sID )
   {
      m_model.removeObject( this );
      m_sID = sID;
      m_model.putObject(   this );
   }
   
   /**
    * Sets the object's name.
    * 
    * @param sName the object's name
    * 
    * @see com.sas.etl.models.IObject#setName(java.lang.String)
    */
   public void setName( String sName )
   {
      if (sName == null)
         throw new NullPointerException( "name can not be set to null" );     // I18NOK:EMS
      if (sName.trim().length() == 0)
         throw new IllegalArgumentException( "name must have at least one non-blank character" );  //I18NOK:EMS
      
      if (sName.equals( m_sName ))
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetNameUndoable( m_sName, sName ) );
      m_sName = sName;
      fireModelChangedEvent( NAME_CHANGED, m_sName );
   }
   
   /**
    * Gets the object's name.
    * 
    * @return the object's name
    * 
    * @see com.sas.etl.models.IObject#getName()
    */
   public String getName()
   {
      return m_sName;
   }

   /**
    * Sets the object's description
    * 
    * @param sDescription the object's description
    * 
    * @see com.sas.etl.models.IObject#setDescription(java.lang.String)
    */
   public void setDescription( String sDescription )
   {
      if (ObjectComparator.isEqual(sDescription, m_sDescription ))
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetDescriptionUndoable( m_sDescription, sDescription ) );
      m_sDescription = sDescription;
      fireModelChangedEvent( DESCRIPTION_CHANGED, m_sDescription );
   }

   /**
    * Gets the object's description
    * 
    * @return the object's description
    * @see com.sas.etl.models.IObject#getDescription()
    */
   public String getDescription()
   {
      return m_sDescription;
   }
   
   /**
    * Sets whether the object has changed since it was last persisted.  Other
    * than <code>this</code>, this method should only be used during load by
    * other objects to reset the object's changed state.
    *  
    * @param bChanged true = the object has changed
    */
   public void setChanged( boolean bChanged )
   {
	  if (getModel().isCopyPaste())
		  return;
	  
      m_bChanged = bChanged;
   }
   
   /**
    * Is the object changed since it was last persisted?
    * 
    * @return true = the object has changed
    */
   public boolean isChanged()
   {
      return m_bChanged || (getModel()!=null && getModel().isCopyPaste());
   }
 
   /**
    * Get the list of reasons this object is incomplete
    * @return empty list
    * @see com.sas.etl.models.IObject#getReasonsIncomplete()
    */
   public List getReasonsIncomplete()
   {
      return new ArrayList();
   }
   
   /**
    * Get the list of warnings for this object
    * @return empty list
    * @see com.sas.etl.models.IObject#getWarnings()
    */
   public List getWarnings()
   {
      return new ArrayList();
   }
   
   /**
    * Is the object a new object?
    * 
    * @return true = yes
    */
   public boolean isNew()
   {
      return isNewObjectID( m_sID );
   }
   
   /**
    * Is the object complete?  The default implementation of this method is 
    * intended to be overridden or extended to check more details on the object.
    * The default implementation returns true.
    * 
    * @return true = object is complete
    * 
    * @see com.sas.etl.models.IObject#isComplete()
    */
   public boolean isComplete()
   {
      return true;
   }
   
   /**
    * Does the object have any warnings?  The default implementation of this
    * method is intended to be overridden or extended to check more details
    * on the object.  The default implementation returns false.
    * 
    * @return true = the object has warnings
    * 
    * @see com.sas.etl.models.IObject#hasWarnings()
    */
   public boolean hasWarnings()
   {
      return false;
   }
   
   /**
    * Gets the model of which this object is a part.
    * 
    * @return the model
    */
   public IModel getModel()
   {
      return m_model;
   }
   
   public void setModel(IModel model)
   {
	   if (m_model==model)
		   return;
	   
	   m_model = model;
	   
	   m_model.putObject(this);
   }
   
   //---------------------------------------------------------------------------
   // User Property methods
   //---------------------------------------------------------------------------

   /**
    * Sets a user property specified by the name to the specified value.
    *  
    * @param sName  the name of the property
    * @param sValue the value of the property
    * 
    * @see com.sas.etl.models.IObject#setUserProperty(java.lang.String, java.lang.String)
    */
   public void setUserProperty( String sName, String sValue )
   {
      if (m_mapUserProperties == null)
         m_mapUserProperties = new HashMap();
      
      // if the old value equals the new value, done
      String sOldValue = (String) m_mapUserProperties.get( sName );
      if ((sOldValue != null) && sOldValue.equals( sValue ))
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetUserPropertyUndoable( sName, getUserProperty( sName ), sValue ) );
      m_mapUserProperties.put( sName, sValue );
      fireModelChangedEvent( USER_PROPERTY_SET, sName );
   }

   /**
    * Gets the user property specified by the property name.
    * 
    * @param sName the property name
    * 
    * @return the property value
    * 
    * @see com.sas.etl.models.IObject#getUserProperty(java.lang.String)
    */
   public String getUserProperty( String sName )
   {
      return (m_mapUserProperties == null) ? null : (String) m_mapUserProperties.get( sName );
   }

   /**
    * Removes the user property specified by the property name.
    * 
    * @param sName the property name
    * 
    * @see com.sas.etl.models.IObject#removeUserProperty(java.lang.String)
    */
   public void removeUserProperty( String sName )
   {
      if (m_mapUserProperties == null)
         return;
      
      String sValue = getUserProperty( sName );
      if (sValue == null)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new RemoveUserPropertyUndoable( sName, sValue ) );
      m_mapUserProperties.remove( sName );      
      fireModelChangedEvent( USER_PROPERTY_REMOVED, sName );
   }
   
   /**
    * Gets an array of the user property keys.  These values are needed by the 
    * persistence code in AbstractPersistableObject in order to know what 
    * properties need to be retrieved and saved to the persistence mechansim.
    * 
    * @return the user property keys
    */
   protected String[] getUserPropertyKeys()
   {
      if (m_mapUserProperties == null)
         return new String[0];
      
      Set keys = m_mapUserProperties.keySet();
      return (String[]) keys.toArray( new String[ keys.size() ] );
   }
   
   /**
    * Gets the object's default name.  Override this method to change the 
    * object's default name.  The default name is only used when the object is 
    * created.
    * 
    * @return the name of the object
    */
   protected String getDefaultName()
   {
	   return "unnamed";
   }

   //---------------------------------------------------------------------------
   // convenience methods for using the model
   //---------------------------------------------------------------------------

   /**
    * Fires a model changed event.
    * 
    * @param ev the model event
    */
   protected void fireModelChangedEvent( ModelEvent ev )
   {
      setChanged( true );
      m_model.fireModelEvent( ev );
      fireNotifyEvent( NotifyEvent.OBJECT_CHANGED, ev );
   }
   
   /**
    * Fires a model changed event.
    * 
    * @param sType the type of model change
    * @param data  data related to the model change
    */
   protected void fireModelChangedEvent( String sType, Object data )
   {
      fireModelChangedEvent( new ModelEvent(m_model,this, sType, data) );
   }
   
   /**
    * Fires a model changed event.
    * 
    * @param sType the type of model change
    * @param data  data related to the model change
    * @param additionalData additional data related to the model change
    */
   protected void fireModelChangedEvent( String sType, Object data, Object additionalData )
   {
      fireModelChangedEvent( new ModelEvent(m_model, this, sType, data, additionalData) );
   }
   
   /**
    * Is undo supported by the model?
    * 
    * @return true = undo is supported
    */
   protected boolean isUndoSupported()
   {
      return m_model.isUndoSupported();
   }
   
   /**
    * Tells the model that an undoable edit occurred.
    * 
    * @param edit the undoable edit that occurred
    */
   protected void undoableEditHappened( UndoableEdit edit )
   {
      m_model.undoableEditHappened( edit );
   }
   
   /**
    * Tells the model to start a compound undoable.
    */
   protected void startCompoundUndoable()
   {
      m_model.startCompoundUndoable();
   }

   /**
    * Tells the model to end a compound undoable.
    */
   protected void endCompoundUndoable()
   {
      m_model.endCompoundUndoable();
   }
   
   /**
    * Gets the object factory used by the model.
    * 
    * @return the object factory
    */
   protected IObjectFactory getObjectFactory()
   {
      return m_model.getObjectFactory();
   }
   
   /**
    * Creates an id for a new object.
    * 
    * @return the id for the new object
    */
   protected String createIDForNewObject()
   {
      return m_model.createIDForNewObject( getID() );
   }
   
   /**
    * Is object id for a new object?
    * 
    * @param sID the object id
    * 
    * @return true = new object
    * 
    */
   protected boolean isNewObjectID( String sID )
   {
      return m_model.isNewObjectID( sID );
   }
   
   //---------------------------------------------------------------------------
   // Notify Listener Management Methods
   //---------------------------------------------------------------------------
   /**
    * Adds a notify listener to the object.  The notify listener will be called
    * when certain changes are made to the object.  Currently, the only known 
    * change is the object being deleted.
    * 
    * @param lsnr the notification listener
    * 
    * @see com.sas.etl.models.IPersistableObject#addNotifyListener(com.sas.etl.models.INotifyListener)
    */
   public void addNotifyListener( INotifyListener lsnr )
   {
      if (m_lListeners == null)
         m_lListeners = new ArrayList();
      
      // don't add the same listener to the listener list more than once
      //  this will avoid duplicate notification to the same listener for the same event
      if (m_lListeners.contains(lsnr))
         return;
      
      m_lListeners.add( lsnr );
   }
   
   /**
    * Removes a notify listener from the object.
    * 
    * @param lsnr the notify listener.
    * 
    * @see com.sas.etl.models.IPersistableObject#removeNotifyListener(com.sas.etl.models.INotifyListener)
    */
   public void removeNotifyListener( INotifyListener lsnr )
   {
      if (m_lListeners!=null)
         m_lListeners.remove( lsnr );
   }
   
   /**
    * Fires a notify event with no model event.
    * 
    * @param eNotifyType the notify event type (see NotifyEvent)
    * 
    */
   protected final void fireNotifyEvent( int eNotifyType)
   {
      fireNotifyEvent( eNotifyType, null);
   }
   
   /**
    * Fires a notify event.
    * 
    * @param eNotifyType the notify event type (see NotifyEvent)
    * @param modelEvent a model event
    */
   protected final void fireNotifyEvent( int eNotifyType, ModelEvent modelEvent )
   {
      if ((m_lListeners == null) || m_lListeners.isEmpty())
         return;
      
      fireNotifyEventImpl( new NotifyEvent( this, eNotifyType, modelEvent ) );
   }
   
   /**
    * Fires a notify event.
    * 
    * @param ev the notify event
    */
   protected final void fireNotifyEvent( NotifyEvent ev )
   {
      if ((m_lListeners == null) || m_lListeners.isEmpty())
         return;

      fireNotifyEventImpl( ev );
   }


   /**
    * Fires a notify event.
    * 
    * @param ev the notify event
    */
   private void fireNotifyEventImpl( NotifyEvent ev )
   {
      INotifyListener[] aListeners = (INotifyListener[]) m_lListeners.toArray( new INotifyListener[ m_lListeners.size() ] );
      for ( int iListener=0; iListener<aListeners.length; iListener++ )
         aListeners[ iListener ].notify( ev );
   }

   /**
    * Handles the notify event by forwarding any events up to any listeners.
    * 
    * @param ev the notify event
    * 
    * @see com.sas.etl.models.INotifyListener#notify(com.sas.etl.models.NotifyEvent)
    */
   public void notify( NotifyEvent ev )
   {
      // ripple the event up to any listeners
      fireNotifyEvent( ev );
   }
   
      
   /**
    * Dumps this object to the print stream.
    * 
    * @param strm the print stream
    * 
    * @see com.sas.etl.models.IObject#dump(java.io.PrintStream)
    */
   public void dump( PrintStream strm )
   {
      strm.println( this );
   }

   /**
    * Dispose of the object
    *
    */
   public void dispose()
   {
      
   }
   
   /**
    * Converts the object to a string.  The string can not be used to recreate 
    * the object.  The string is intended for debug purposes only.
    * 
    * @return the string
    * 
    * @see java.lang.Object#toString()
    */ 
   public String toString()
   {
      StringBuffer sb = new StringBuffer( m_sName.length() + 17 );
      sb.append( m_sName );
      sb.append( ":"     );
      sb.append( m_sID   );
      return sb.toString();   // I18NOK:LSM
   }
   
   /**
    * Is the object specified equal to this object?  
    * 
    * @param object the specified object to be tested
    * 
    * @return true = yes
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals( Object object )
   {
      // if the object is this object, then yes
      if (object == this)
         return true;
      
      // otherwise, if the object is a model object, compare ids
      if (object instanceof IObject) 
         return m_sID.equals( ((IObject) object).getID() );
      
      // otherwise, nope
      return false;
   }
   
   public int hashcode() 
   {
      // NOTE: this hashcode() implementation violates the contract that hashcode return the same value when equals() would return true
      // HOWEVER, code inspection has revealed this object is not being used as a hash key AND changing the hashcode method to meet
      // the contract is overly risky.
      return super.hashCode();
   }
   
   /**
    * SetNameUndoable is the undoable for setting the object's name.
    */
   private class SetNameUndoable extends AbstractUndoableEdit
   {
      private String m_sOldName;
      private String m_sNewName;
      
      /**
       * Constructs the set name undoable.
       * 
       * @param sOldName the old name
       * @param sNewName the new name
       */
      public SetNameUndoable( String sOldName, String sNewName )
      {
         m_sOldName = sOldName;
         m_sNewName = sNewName;
      }
      
      /**
       * Undoes the setting of the name.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setName( m_sOldName );
      }
      
      /**
       * Redoes the setting of the name.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setName( m_sNewName );
      }
   } // SetNameUndoable

   /**
    * SetDescriptionUndoable is the undoable for setting the object's 
    * description.
    */
   private class SetDescriptionUndoable extends AbstractUndoableEdit
   {
      private String m_sOldDescription;
      private String m_sNewDescription;
      
      /**
       * Constructs the set description undoable.
       * 
       * @param sOldDescription the old description
       * @param sNewDescription the new description
       */
      public SetDescriptionUndoable( String sOldDescription, String sNewDescription )
      {
         m_sOldDescription = sOldDescription;
         m_sNewDescription = sNewDescription;
      }
      
      /**
       * Undoes the setting of the description.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setDescription( m_sOldDescription );
      }
      
      /**
       * Redoes the setting of the description.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setDescription( m_sNewDescription );
      }
   } // SetDescriptionUndoable

   /**
    * SetUserPropertyUndoable is the undoable for setting a user property.
    */
   private class SetUserPropertyUndoable extends AbstractUndoableEdit
   {
      private String m_sPropertyName;
      private String m_sOldPropertyValue;
      private String m_sNewPropertyValue;
      
      /**
       * Constructs the set user property undoable.
       * 
       * @param sPropertyName     the user property name
       * @param sOldPropertyValue the old user property value
       * @param sNewPropertyValue the new user property value
       */
      public SetUserPropertyUndoable( String sPropertyName, String sOldPropertyValue, String sNewPropertyValue )
      {
         m_sPropertyName     = sPropertyName;
         m_sOldPropertyValue = sOldPropertyValue;
         m_sNewPropertyValue = sNewPropertyValue;
      }
      
      /**
       * Undoes the setting of the user property.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         if (m_sOldPropertyValue == null)
            removeUserProperty( m_sPropertyName );
         else
            setUserProperty( m_sPropertyName, m_sOldPropertyValue );
      }
      
      /**
       * Redoes the setting of the user property.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setUserProperty( m_sPropertyName, m_sNewPropertyValue );
      }
   } // SetUserPropertyUndoable

   /**
    * RemoveUserPropertyUndoable is the undoable for removing a user property.
    */
   private class RemoveUserPropertyUndoable extends AbstractUndoableEdit
   {
      private String m_sPropertyName;
      private String m_sPropertyValue;
      
      /**
       * Constructs the set user property undoable.
       * 
       * @param sPropertyName  the user property name
       * @param sPropertyValue the user property value
       */
      public RemoveUserPropertyUndoable( String sPropertyName, String sPropertyValue )
      {
         m_sPropertyName  = sPropertyName;
         m_sPropertyValue = sPropertyValue;
      }
      
      /**
       * Undoes the removal of the user property.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setUserProperty( m_sPropertyName, m_sPropertyValue );
      }
      
      /**
       * Redoes the removal of the user property.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         removeUserProperty( m_sPropertyName );
      }
   } // RemoveUserPropertyUndoable
}

