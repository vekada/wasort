/* $Id: AbstractPrePostCodeContainer.java,v 1.1.2.16.14.1 2012/09/20 19:51:11 sasclw Exp $ */
/**
 * Title:       AbstractPrePostCodeContainer.java
 * Description: Abstract implementation of an object with pre- and post-code.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Daniel Wong
 * Support:     Daniel Wong
 */
package com.sas.etl.models.job.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sas.etl.models.IModel;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.job.ICodeSource;
import com.sas.etl.models.job.IPrePostCode;
import com.sas.etl.models.job.IUserWrittenCodeContainer;
import com.sas.metadata.remote.AbstractTransformation;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.Transformation;

/**
 * AbstractPrePostCodeContainer is an abstract implementation for objects that
 * will support pre- and post-code.
 */
public abstract class AbstractPrePostCodeContainer extends AbstractConditionActionSetContainer implements IPrePostCode
{
   
   /**
    * The code source for preprocess code.
    */
   private IUserWrittenCodeContainer m_codePreProcess;
   /**
    * The code source for postprocess code.
    */
   private IUserWrittenCodeContainer m_codePostProcess;

   private boolean m_bPreProcessEnabled;
   private boolean m_bPostProcessEnabled;
   
   /**
    * Constructor.
    * @param sID   the object id
    * @param model the model 
    */
   public AbstractPrePostCodeContainer( String sID, IModel model )
   {
      super( sID, model );
   
      m_bPreProcessEnabled = true;
      m_bPostProcessEnabled = true;
      
      m_codePreProcess = model.getObjectFactory().createUserWrittenHelper( this );
      m_codePreProcess.setContainerOMRType( MetadataObjects.TRANSFORMATION );
      m_codePreProcess.setContainerRole( PREPROCESS_ROLE );
      m_codePreProcess.setName(          PREPROCESS_ROLE );
      m_codePreProcess.setActiveFlagLocation( IUserWrittenCodeContainer.USE_ISACTIVE_ATTRIBUTE );
      m_codePreProcess.setActiveEventName( USE_PRE_PROCESS_CODE_CHANGED );
      m_codePreProcess.setCodeChangeEventName( PRE_PROCESS_CODE_CHANGED );
      
      m_codePostProcess = model.getObjectFactory().createUserWrittenHelper( this );
      m_codePostProcess.setContainerOMRType( MetadataObjects.TRANSFORMATION );
      m_codePostProcess.setContainerRole( POSTPROCESS_ROLE );
      m_codePostProcess.setName(          POSTPROCESS_ROLE );
      m_codePostProcess.setActiveFlagLocation( IUserWrittenCodeContainer.USE_ISACTIVE_ATTRIBUTE );
      m_codePostProcess.setActiveEventName( USE_POST_PROCESS_CODE_CHANGED );
      m_codePostProcess.setCodeChangeEventName( POST_PROCESS_CODE_CHANGED );
   }
   
   /**
    * Gets the template map used to populate an OMR adapter.
    * 
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssociations = (List) map.get( getOMRType() );
      if (lAssociations==null)
         lAssociations = new ArrayList();
      lAssociations.add( AbstractTransformation.ASSOCIATION_TRANSFORMATIONSOURCES_NAME );
      lAssociations.add( AbstractTransformation.ASSOCIATION_TRANSFORMATIONTARGETS_NAME );
      map.put( getOMRType(), lAssociations );
      
      m_codePreProcess.getOMRLoadTemplateMap( map );
      m_codePostProcess.getOMRLoadTemplateMap( map );
      return map;
   }
   
   /**
    * Is the object changed since it was last persisted?
    * 
    * @return true = the object has changed
    */
   public boolean isChanged()
   {
      // check to see if the pre-code or post-code contents have changed
      return super.isChanged() || 
             ((m_codePreProcess != null)  && m_codePreProcess.isChanged() ) ||
             ((m_codePostProcess != null) && m_codePostProcess.isChanged());
   }
   
   /**
    * Is Complete
    * @return true if complete
    * @see com.sas.etl.models.data.impl.AbstractParametersContainer#isComplete()
    */
   public boolean isComplete()
   {
      if (!super.isComplete())
         return false;
      
      if (isUsingPreProcessCode() && getPreProcessCode()!=null && !getPreProcessCode().isComplete())
         return false;
      if (isUsingPostProcessCode() && getPostProcessCode()!=null && !getPostProcessCode().isComplete())
         return false;
      
      return true;
   }
   
   /**
    * Gets the reasons the transform is incomplete.
    * 
    * @return a list of Strings that are the reasons.
    * 
    * @see com.sas.etl.models.job.ICodeGenerator#getReasonsIncomplete()
    */
   public List getReasonsIncomplete()
   {
      List lReasons = super.getReasonsIncomplete();

      if (isUsingPreProcessCode() && getPreProcessCode()!=null)
         lReasons.addAll( getPreProcessCode().getReasonsIncomplete() );

      if (isUsingPostProcessCode() && getPostProcessCode()!=null)
         lReasons.addAll( getPostProcessCode().getReasonsIncomplete() );
      return lReasons;
   }
   
   /**
    * Sets whether the transform should use pre-process code.
    * 
    * @param bUsePreProcessCode true = use pre-process code
    */
   public void setUsePreProcessCode( boolean bUsePreProcessCode )
   {
      if (m_codePreProcess.isActive() == bUsePreProcessCode)
         return;
      
      
         m_codePreProcess.setIsActive( bUsePreProcessCode );
         
   }
   
   /**
    * Is the transform using pre-process code instead of generated code?
    * 
    * @return true = the transform is using pre-process code
    */
   public boolean isUsingPreProcessCode()
   {
      return m_codePreProcess.isActive();
   }
   
   /**
     * Is preprocess availability is enabled
    * @return true if the pre process is available
    * @see com.sas.etl.models.job.IPrePostCode#isPreProcessEnabled()
    */
   public boolean isPreProcessEnabled()
   {
      return m_bPreProcessEnabled;
   }
   
   /**
    * Set the pre process availability
    * @param bEnabled true if enabled
    */
   public void setPreProcessEnabled(boolean bEnabled)
   {
      if (m_bPreProcessEnabled==bEnabled)
         return;
      
      m_bPreProcessEnabled = bEnabled;
      
      fireModelChangedEvent(PREPROCESS_ENABLED_CHANGED, new Boolean(m_bPreProcessEnabled ));
   }
   
   /**
    * Is postprocess availability is enabled
    * 
    * @return true if the post process is available
    * @see com.sas.etl.models.job.IPrePostCode#isPostProcessEnabled()
    */
   public boolean isPostProcessEnabled()
   {
      return m_bPostProcessEnabled;
   }
   
   /**
    * Set post process availability
    * @param bEnabled true if post process is available
    */
   public void setPostProcessEnabled(boolean bEnabled)
   {
      if (m_bPostProcessEnabled==bEnabled)
         return;
      
      m_bPostProcessEnabled = bEnabled;
      
      fireModelChangedEvent(POSTPROCESS_ENABLED_CHANGED, new Boolean(m_bPostProcessEnabled ));
   }
   
   /**
    * Updates the object's new object ids after a save has occurred.
    * 
    * @param mapIDs the map of new object ids
    * 
    * @see com.sas.etl.models.IOMRPersistable#updateIDs(java.util.Map)
    */
   public void updateIDs( Map mapIDs )
   {
      super.updateIDs( mapIDs );
      m_codePostProcess.updateIds( mapIDs );
      m_codePreProcess.updateIds( mapIDs );
   }

   /**
    * Sets the pre-process code for the transform.
    * 
    * @param code the pre-process code
    */
   public void setPreProcessCode( ICodeSource code )
   {
      m_codePreProcess.setUserWrittenSourceCode( code );
  
   }
   
   /**
    * Gets the pre-process code for the transform.
    * 
    * @return the pre-process code
    */
   public IUserWrittenCodeContainer getPreProcessCode()
   {
      return m_codePreProcess;
   }
   
   /**
    * Sets whether the transform should use pre-process code.
    * 
    * @param bUsePostProcessCode true = use post-process code
    */
   public void setUsePostProcessCode( boolean bUsePostProcessCode )
   {
      if (m_codePostProcess.isActive() == bUsePostProcessCode)
         return;
      
     
         m_codePostProcess.setIsActive( bUsePostProcessCode );
   }
   
   /**
    * Is the transform using post-process code instead of generated code?
    * 
    * @return true = the transform is using post-process code
    */
   public boolean isUsingPostProcessCode()
   {
      return m_codePostProcess.isActive();
   }
   
   /**
    * Sets the post-process code for the transform.
    * 
    * @param code the post-process code
    */
   public void setPostProcessCode( ICodeSource code )
   {
      m_codePostProcess.setUserWrittenSourceCode( code );
   }
   
   /**
    * Gets the post-process code for the transform.
    * 
    * @return the post-process code
    */
   public IUserWrittenCodeContainer getPostProcessCode()
   {
      return m_codePostProcess;
   }

   /**
    * Deletes the transform from OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void deleteFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (isNew())
         return;
      
      m_codePreProcess.deleteFromOMR( omr );
      m_codePostProcess.deleteFromOMR( omr );
      
      super.deleteFromOMR( omr );
   }

   /**
    * Load from omr  
    * @param omr adapter
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.data.impl.AbstractParametersContainer#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      super.loadFromOMR( omr );

      AbstractTransformation mdObject = (AbstractTransformation)omr.acquireOMRObject( this );

      // get pre process code settings
      Transformation preHolder = findPreCodeContainer( mdObject );
      if (preHolder!=null)
      {
         m_codePreProcess.setContainer( preHolder );
         m_codePreProcess.loadFromOMR( omr );
      }
      
      // get post process code settings
      Transformation postHolder = findPostCodeContainer( mdObject );
      if (postHolder!=null)
      {
         m_codePostProcess.setContainer( postHolder );
         m_codePostProcess.loadFromOMR( omr );
      }
      
      setChanged(false);
   }

   /**
    * Save to omr
    * @param omr adapter
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.data.impl.AbstractParametersContainer#saveToOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (!isChanged())
         return;

      super.saveToOMR( omr );

      AbstractTransformation mdObject = (AbstractTransformation)omr.acquireOMRObject( this );
      
      // if use pre-process code is true, or there is pre-process code, or there is a pre-process holder 
      // the pre-process holder has to be created or updated
      IUserWrittenCodeContainer preCode = getPreProcessCode();
      preCode.saveToOMR( omr );
      
      Transformation preHolder = (Transformation)m_codePreProcess.getOMRContainer( omr );
      if (preHolder!=null)
      {
         mdObject.getTransformationSources( false ).add( preHolder );
      }
      
      // if use post-process code is true, or there is post-process code, or there is a post-process holder 
      // the post-process holder has to be created or updated
      IUserWrittenCodeContainer postCode = getPostProcessCode();
      postCode.saveToOMR( omr );
      Transformation postHolder = (Transformation) m_codePostProcess.getOMRContainer( omr );
      if (postHolder!=null)
      {
         mdObject.getTransformationTargets( false ).add( postHolder );
      }

      setChanged( false );
   }


   private Transformation findPreCodeContainer(AbstractTransformation mdObject)
   throws RemoteException, MdException
   {
      Transformation trans = null;
      
      List sources = mdObject.getTransformationSources();
      int size = sources.size();
      for (int i=0; i<size; i++)
      {
         Root target = (Root)sources.get( i );
         if (target instanceof Transformation && ((Transformation)target).getTransformRole().equals( PREPROCESS_ROLE ))
         {
            return (Transformation)target;
         }
      }
      
      return trans;
   }


   private Transformation findPostCodeContainer(AbstractTransformation mdObject)
   throws RemoteException, MdException
   {
      Transformation trans = null;
      
      List targets = mdObject.getTransformationTargets();
      int size = targets.size();
      for (int i=0; i<size; i++)
      {
         Root target = (Root)targets.get( i );
         if (target instanceof Transformation && ((Transformation)target).getTransformRole().equals( POSTPROCESS_ROLE ))
         {
            return (Transformation)target;
         }
      }
      
      return trans;
   }

  

}

