/* $Id$ */
/**
 * Title:       AbstractTransform.java
 * Description:
 * Copyright:   Copyright (c) 2006
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models.job.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sas.entities.GUID;
import com.sas.etl.models.IModel;
import com.sas.etl.models.IObject;
import com.sas.etl.models.IPasteModify;
import com.sas.etl.models.ServerException;
import com.sas.etl.models.data.BadLibraryDefinitionException;
import com.sas.etl.models.data.ILibrary;
import com.sas.etl.models.impl.AbstractSecondaryAttributeHelper;
import com.sas.etl.models.impl.ModelLogger;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.impl.ObjectComparator;
import com.sas.etl.models.job.ICheckpointRestart;
import com.sas.etl.models.job.ICodeGenerationEnvironment;
import com.sas.etl.models.job.ICodeSegment;
import com.sas.etl.models.job.ICodeSource;
import com.sas.etl.models.job.IJob;
import com.sas.etl.models.job.ITransform;
import com.sas.etl.models.job.IUIPlacement;
import com.sas.etl.models.job.IUserWrittenCodeContainer;
import com.sas.etl.models.other.BadServerDefinitionException;
import com.sas.etl.models.other.ISASClientConnection;
import com.sas.etl.models.other.IServer;
import com.sas.etl.models.prompts.IPromptDefinitionValue;
import com.sas.etl.models.prompts.IPromptModel;
import com.sas.etl.models.prompts.impl.BaseTransformPromptModel;
import com.sas.metadata.remote.AbstractTransformation;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.StepPrecedence;
import com.sas.metadata.remote.Transformation;
import com.sas.metadata.remote.TransformationStep;
import com.sas.services.ServiceException;
import com.sas.storage.exception.ServerConnectionException;
import com.sas.util.UsageVersion;
import com.sas.workspace.Workspace;

/**
 * AbstractTransform is an abstract transform class.  It provides support for 
 * tracking control predecessors, control successors, and source code.
 */
public abstract class AbstractTransform extends AbstractPrePostCodeContainer implements ITransform
{
   protected static final String OPTIONS_PROPERTYSET = "OPTIONS";
   protected static final String UI_PROPERTY = "UI_PLACEMENT";

   private IPromptModel m_optionModel;
   
//   private List m_lPredecessors;
//   private List m_lSuccessors;
//
//   private String  m_sStepPrecedenceID;
//   private boolean m_bUseStepPrecedence;
   
   private boolean m_bCodeGenerationEnabled;
   
//   private boolean m_bUseUserWrittenCode;
   
   //TODO this needs to be moved to base object and needs to be used in comment header for transforms
//   private String m_sModelVersion;
   private int    m_iInstanceVersion = 1;
   
   private String      m_sSystemOptions;
   private IUserWrittenCodeContainer m_codeUserWritten;

   private IServer m_executionServer;
   
   private boolean m_bCollectDiagnostics;

   private boolean m_bCleanUpRequired;
   
   private SetJobHelper m_helper; 
   
   private boolean m_bGenerateDISHeaderAndFooters;
   
   private boolean m_bCheckpointEnabled;
//   private boolean m_bRequiredForRestart;
   private boolean m_bRunAlways;
   
   private boolean m_bExplicitOn;
       
   private String m_sClearLibrefs;
      
   private IUIPlacement m_UIPlacement;
   
   /**
    * Constructs an AbstractTransform.
    * 
    * @param sID   the id associated with the transform
    * @param model the model that owns the transform
    */
   public AbstractTransform( String sID, IModel model )
   {
      super( sID, model );
//      m_lPredecessors = new ArrayList();
//      m_lSuccessors   = new ArrayList();
      
      m_bCodeGenerationEnabled = true;
      m_sSystemOptions         = null;
      m_bGenerateDISHeaderAndFooters = true;
      m_bCleanUpRequired       = false;
      
      m_helper = new SetJobHelper();
      m_codeUserWritten = model.getObjectFactory().createUserWrittenHelper( this );
      m_codeUserWritten.setActiveFlagLocation( IUserWrittenCodeContainer.USE_ISUSERDEFINED_ATTRIBUTE );
      
      m_bCheckpointEnabled = false;
      m_bRunAlways = false;
      
      m_sClearLibrefs = CLEARLIBREFS_JOB;
            
      try
      {
         getOptionModel();
      }
      catch (FileNotFoundException e)
      {
         ModelLogger.getDefaultLogger().debug( "FileNotFoundException",e );
      }
      catch (RemoteException e)
      {
         ModelLogger.getDefaultLogger().debug( "RemoteException",e );
      }
      catch (ServiceException e)
      {
         ModelLogger.getDefaultLogger().debug( "ServiceException",e );
      }
      catch (ServerConnectionException e)
      {
         ModelLogger.getDefaultLogger().debug( "ServerConnectionException",e );
      }
      catch (IOException e)
      {
         ModelLogger.getDefaultLogger().debug( "IOException",e );
      }
      catch (ParserConfigurationException e)
      {
         ModelLogger.getDefaultLogger().debug( "ParserConfigurationException",e );
      }
      catch (SAXException e)
      {
         ModelLogger.getDefaultLogger().debug( "SAXException",e );
      }
      catch (MdException e)
      {
         ModelLogger.getDefaultLogger().debug( "MdException",e );
      }
   }

   /**
    * Set the job that owns the transform.
    * 
    * @param job the job
    * 
    * @see com.sas.etl.models.job.ITransform#setJob(com.sas.etl.models.job.IJob)
    */
   public void setJob( IJob job )
   {
      m_helper.set( job );
   }
   
   /**
    * Use this for backwards compatibilty with 9.1 style transforms.  This will
    * instantiate a proxy transform in the new model to handle the transform.
    * 
    * @return false by default
    */
   protected boolean is91StyleTransform()
   {
      return false;
   }
   
   /**
    * This is normally never used, and should not be used by new transforms.  
    * This is used by the proxy transform to allow it to assume the identity of
    * older style transforms. 
    * 
    * @deprecated
    * @param sDisplayType the display type of the transform
    */
   protected void setDisplayType(String sDisplayType)
   {
      
   }

   
   /**
    * This is normally never used, and should not be used by new transforms.  
    * This is used by the proxy transform to allow it to assume the identity of
    * older style transforms. 
    * 
    * @deprecated
    * @param sType the type of the transform
    */   
    protected void setType(String sType)
    {
       
    }
   
   /**
    * Gets the job that owns the transform.
    * 
    * @return the job
    * 
    * @see com.sas.etl.models.job.ITransform#getJob()
    */
   public IJob getJob()
   {
      return (IJob) m_helper.get();
   }
   
   protected abstract String getTransformRole();
   
   protected abstract String getTransformClass();
   
   public boolean isCorrect()
   {
      return true;
   }
   
   /**
    * Sets whether explicit passthru code is being used in the transform code
    * @param bExplicitOn the explicit passthru the code is executed on
    */
   protected void setExplicitOn( boolean bExplicitOn ) 
   {
      if (m_bExplicitOn == bExplicitOn)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetExplicitOnUndoable( m_bExplicitOn, bExplicitOn ) );
      m_bExplicitOn = bExplicitOn;
      fireModelChangedEvent( EXPLICIT_ON_CHANGED, null );
   }
   
   /**
    * Get the explicit on value used to indicate explicit passthru is being used in the
    * transform code
    * @return the explicit on value
    */
   public boolean isExplicitOn() 
   {
      return m_bExplicitOn; 
   }
   
   
   /**
    * Is clean up required for this transform?  If this transform is corrupt 
    * in some way then it needs to be clean up out of the job
    * 
    * @return true = transform requires cleaning up
    */
   public boolean isCleanUpRequired()
   {
	   return m_bCleanUpRequired;
   }
   
   /**
    * Sets whether clean up of the model is required.  This means
    * the model has problems beyond repair.
    * 
    * @param bRequired true = this model requires cleanup
    */
   protected void setCleanUpRequired( boolean bRequired )
   {
      if (m_bCleanUpRequired == bRequired)
    	  return;
      m_bCleanUpRequired = bRequired;
   }   
   
   /**
    * try to recover by cleaning up the model's objects.
    * 
    * @return true = recovery was successful
    */
   protected boolean doCleanUpRecovery( )
   {
	   return true;
   }

   public boolean isComplete()
   {
      if (!super.isComplete())
         return false;

      if (!m_codeUserWritten.isComplete())
         return false;

      if (m_optionModel!=null)
      {
         if (!isUsingUserWrittenCode() && !m_optionModel.isComplete())
            return false;
      }

      return true;
   }
   
   public boolean isCompleteWithUserWritten()
   {
	   if (isUsingUserWrittenCode() && m_codeUserWritten.isComplete())
		   return true;
	   else
	   {
		   return isComplete();
	   }
   }

   /**
    * Gets the reasons the transform is incomplete.
    * 
    * @return a list of Strings that are the reasons.
    * 
    * @see com.sas.etl.models.IObject#getReasonsIncomplete()
    */
   public List getReasonsIncomplete()
   {
      List lReasons = new ArrayList();

      lReasons.addAll( super.getReasonsIncomplete() );
      
      lReasons.addAll( m_codeUserWritten.getReasonsIncomplete() );

      if (m_optionModel!=null)
      {
         lReasons.addAll( m_optionModel.getReasonsIncomplete() );
      }

      return lReasons;
   }
   
   /**
    * Is the object changed since it was last persisted?
    * 
    * @return true = the object has changed
    */
   public boolean isChanged()
   {
      // check to see if the user written contents have changed
      return super.isChanged() || 
    		 ((m_codeUserWritten != null) && m_codeUserWritten.isChanged() ||
    		  getUIPlacement().isChanged());
   }
   
   /**
    * Is the object a public object?
    * 
    * @return false = instances of transforms are not public objects 
    * 
    * @see com.sas.workspace.models.SimpleObject#isPublicObject()
    */
   public boolean isPublicObject()
   {
      return false;
   }

   public final IPromptModel getOptionModel()
   throws IOException, ParserConfigurationException, SAXException, FileNotFoundException, ServiceException, MdException, ServerConnectionException
   {
      if (m_optionModel==null)
         m_optionModel = createOptionModel();
     
      if (m_optionModel!=null)
         m_optionModel.updateModelValues();
      
      return m_optionModel;
   }
   
   protected IPromptModel createOptionModel()
   throws IOException, ParserConfigurationException, SAXException, FileNotFoundException,
   ServerConnectionException, ServiceException, MdException
   {
      return new BaseTransformPromptModel(getModel(),this);
   }
   
   /**
    * Clean up the transform
    * 
    * @see com.sas.etl.models.impl.BaseObject#dispose()
    */
   public void dispose()
   {
      if (m_optionModel!=null)
         m_optionModel.dispose();
      
      super.dispose();
   }
   
   /**
    * Gets the type of the transform.
    * 
    * @return the type of the transform
    */
   public String getType()
   {
      return getTransformClass();
   }

   /**
    * Gets the maximum count of predecessors.  Most transforms will have only 
    * one predecessor.
    * 
    * @return the maximum count of predecessors
    */
   protected int getMaximumPredecessorCount()
   {
      return 1;
   }
   
   /**
    * Gets the maximum count of successors.  Most transforms will have only one
    * successor.  Only a conditional transform that has one of several succesor
    * control paths should return more than one.
    * 
    * @return the maximum successor count
    */
   protected int getMaximumSuccessorCount()
   {
      return 1;
   }

   /**
    * Gets the model version of the transform.
    * 
    * @return true = the model version
    * 
    * @see com.sas.etl.models.job.ITransform#getModelVersion()
    */
   public UsageVersion getModelVersion()
   {
      return DEFAULT_TRANSFORM_MODEL_VERSION;
   }
   
  
   /**
    * Gets the instance version of the transform.
    * 
    * @return the instance version
    */
   public int getInstanceVersion()
   {
      return m_iInstanceVersion;
   }
   
   public boolean isRunAlways()
   {
      return m_bRunAlways;
   }
   
   public void setRunAlways(boolean bRunAlways)
   {
      if (m_bRunAlways==bRunAlways)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetRunAlwaysUndoable(m_bRunAlways, bRunAlways));
      
      m_bRunAlways = bRunAlways;
      
      fireModelChangedEvent( RUN_ALWAYS_CHANGED, null );
   }
   
   
   /**
    * Is diagnostics set for this transformation
    * @return true if diagnostics is turned on
    */
   public boolean isCollectingDiagnostics()
   {
      return m_bCollectDiagnostics;
   }
   

   
   /**
    * Set diagnostics for this object
    * @param collectDiagnostics true to turn on diagnostics
    */
   public void setCollectDiagnostics(boolean collectDiagnostics)
   {
      if (collectDiagnostics==m_bCollectDiagnostics)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetCollectDiagnosticsUndoable( m_bCollectDiagnostics, collectDiagnostics ) );
         
      m_bCollectDiagnostics = collectDiagnostics;
      
      fireModelChangedEvent( COLLECT_DIAGNOSTICS_CHANGED, null );
   }

   public boolean isCPRStepSetupComplete()
   {
       return isCheckpointEnabled() && isCodeGenerationEnabled();
   }
   
   /* (non-Javadoc)
    * CheckointRestart code is needed by the transform depending on options in the trasnform and
    * the JOB!  When Job calls this, it already knows whether its criteria is met, so it can call this
    * with parm1=true to prevent this from calling back to the job. 
    * @see com.sas.etl.models.job.ITransform#isCPRStepCodeNeeded(boolean)
    */
   public boolean isCPRStepCodeNeeded()
   {
	  return isCPRStepCodeNeeded(false);
   }
   
   public boolean isCPRStepCodeNeeded(boolean bIKnowJobHasCheckpointOn)
   {
	 boolean neededByStep=isCPRStepSetupComplete();
	 if (bIKnowJobHasCheckpointOn)  
	    return neededByStep;
	 else
	    return neededByStep && getJob().isCPRJobCodeNeeded(neededByStep);
   }


   public boolean isCheckpointEnabled()
   {
      return m_bCheckpointEnabled;
   }
  
   public boolean isCustomRestartSupported()
   {
      return false; 
   }
  
  
   /* (non-Javadoc)
    * @see com.sas.etl.models.job.ITransform#getCPRCustomPreStepCode(com.sas.etl.models.job.ICodeSegment)
    */
   public ICodeSegment getCPRCustomPreStepCode(ICodeSegment codeSegment)
   {
      if (isCustomRestartSupported()) 
         codeSegment.addSourceCode("Subclass override should have code to load info from job's save-state library (from prior run).");
	  return codeSegment;
   }

   
   /* (non-Javadoc)
    * @see com.sas.etl.models.job.ITransform#getCPRCustomPostStepCode(com.sas.etl.models.job.ICodeSegment)
    */
   public ICodeSegment getCPRCustomPostStepCode(ICodeSegment codeSegment)
   {
     if (isCustomRestartSupported()) 
        codeSegment.addSourceCode("Subclass override should have code to save info to jobs's save-state library.");
     return codeSegment;
   }
   
   
   public void setCheckpointEnabled(boolean enabled)
   {
      if (m_bCheckpointEnabled==enabled)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetCheckpointEnabledUndoable(m_bCheckpointEnabled, enabled) );
      
      m_bCheckpointEnabled = enabled;
      
      fireModelChangedEvent( CHECKPOINT_ENABLED_CHANGED, null );
   }
   
   //---------------------------------------------------------------------------
   // Predecessors
   //---------------------------------------------------------------------------
//   /**
//    * Gets the count of control predecessors.
//    * 
//    * @return the count of control predecessors.
//    * 
//    * @see com.sas.etl.models.job.ITransform#getControlPredecessorCount()
//    */
//   public int getControlPredecessorCount()
//   {
//      return m_lPredecessors.size();
//   }
//
//   /**
//    * Gets the control predecessors.  Typically there is only one control 
//    * predecssor.
//    * 
//    * @return the control predecessors.
//    * 
//    * @see com.sas.etl.models.job.ITransform#getControlPredecessors()
//    */
//   public ITransform[] getControlPredecessors()
//   {
//      return (ITransform[]) m_lPredecessors.toArray( new ITransform[ m_lPredecessors.size() ] );
//   }
//
//   /**
//    * Is the specified transform a control predecessor?
//    * 
//    * @param transform the specified transform 
//    * 
//    * @return true = the specified transform is a control predecessor
//    * 
//    * @see com.sas.etl.models.job.ITransform#containsInControlPredecessors(com.sas.etl.models.job.ITransform)
//    */
//   public boolean containsInControlPredecessors( ITransform transform )
//   {
//      return m_lPredecessors.contains( transform );
//   }
//
//   /**
//    * Adds a control predecesssor.  A control predecessor is the transform that
//    * executes prior to the this transform.
//    * 
//    * @param transform the control predecessor.
//    * 
//    * @see com.sas.etl.models.job.ITransform#addControlPredecessor(com.sas.etl.models.job.ITransform)
//    */
//   public void addControlPredecessor( ITransform transform )
//   {
//      addControlPredecessor( m_lPredecessors.size(), transform );
//   }
//
//   /**
//    * Adds a control predecesssor.  A control predecessor is the transform that
//    * executes prior to the this transform.
//    *
//    * @param iTransform the index of where the control predecessor is added
//    * @param transform the control predecessor
//    */
//   public void addControlPredecessor( int iTransform, ITransform transform )
//   {
//      if (m_lPredecessors.size() >= getMaximumPredecessorCount())
//         throw new UnsupportedOperationException( "Maximum predecessor count will be exceeded (" + getMaximumPredecessorCount() + ")" );
//
//      m_lPredecessors.add( iTransform, transform );
//      fireModelChangedEvent( PREDECESSOR_ADDED, transform );
//      if (isUndoSupported())
//         undoableEditHappened( new AddPredecessorUndoable( iTransform, transform ) );
//   }
//
//   /**
//    * Removes a control predecessor.  A control predecessor is the transform that
//    * executes prior to the this transform.
//    * 
//    * @param transform the control predecessor.
//    * 
//    * @see com.sas.etl.models.job.ITransform#removeControlPredecessor(com.sas.etl.models.job.ITransform)
//    */
//   public void removeControlPredecessor( ITransform transform )
//   {
//      int iTransform = m_lPredecessors.indexOf( transform );
//      m_lPredecessors.remove( iTransform );
//      fireModelChangedEvent( PREDECESSOR_REMOVED, transform );
//      if (isUndoSupported())
//         undoableEditHappened( new RemovePredecessorUndoable( iTransform, transform ) );
//   }
//
//   //---------------------------------------------------------------------------
//   // Successors
//   //---------------------------------------------------------------------------
//   
//   /**
//    * Gets the count of control successors.
//    * 
//    * @return the count of control successors
//    * 
//    * @see com.sas.etl.models.job.ITransform#getControlSuccessorCount()
//    */
//   public int getControlSuccessorCount()
//   {
//      return m_lSuccessors.size();
//   }
//
//   /**
//    * Gets the control successors.  Typically there is only one control 
//    * succssor.
//    * 
//    * @return the control successors
//    * 
//    * @see com.sas.etl.models.job.ITransform#getControlSuccessors()
//    */
//   public ITransform[] getControlSuccessors()
//   {
//      return (ITransform[]) m_lSuccessors.toArray( new ITransform[ m_lSuccessors.size() ] );
//   }
//
//   /**
//    * Is the specified transform a control successor?
//    * 
//    * @param transform the specified transform 
//    * 
//    * @return true = the specified transform is a control successor
//    * 
//    * @see com.sas.etl.models.job.ITransform#containsInControlSuccessors(com.sas.etl.models.job.ITransform)
//    */
//   public boolean containsInControlSuccessors( ITransform transform )
//   {
//      return m_lSuccessors.contains( transform );
//   }
//
//   /**
//    * Adds a control successsor.  A control successor is the transform that
//    * executes after this transform.
//    * 
//    * @param transform the control successor
//    * 
//    * @see com.sas.etl.models.job.ITransform#addControlSuccessor(com.sas.etl.models.job.ITransform)
//    */
//   public void addControlSuccessor( ITransform transform )
//   {
//      addControlSuccessor( m_lSuccessors.size(), transform );
//   }
//
//   /**
//    * Adds a control successsor.  A control successor is the transform that
//    * executes after this transform.
//    *
//    * @param iTransform the index of where the control predecessor is added
//    * @param transform the control predecessor
//    */
//   public void addControlSuccessor( int iTransform, ITransform transform )
//   {
//      if (m_lSuccessors.size() >= getMaximumSuccessorCount())
//         throw new UnsupportedOperationException( "Maximum successor count will be exceeded (" + getMaximumSuccessorCount() + ")" );
//
//      m_lSuccessors.add( iTransform, transform );
//      fireModelChangedEvent( SUCCESSOR_ADDED, transform );
//      if (isUndoSupported())
//         undoableEditHappened( new AddSuccessorUndoable( iTransform, transform ) );
//   }
//
//   /**
//    * Removes a control successor.  A control successor is the transform that
//    * executes prior to the this transform.
//    * 
//    * @param transform the control successor
//    * 
//    * @see com.sas.etl.models.job.ITransform#removeControlSuccessor(com.sas.etl.models.job.ITransform)
//    */
//   public void removeControlSuccessor( ITransform transform )
//   {
//      int iTransform = m_lSuccessors.indexOf( transform );
//      m_lSuccessors.remove( iTransform );
//      m_bUseStepPrecedence = false;
//      fireModelChangedEvent( SUCCESSOR_REMOVED, transform );
//      if (isUndoSupported())
//         undoableEditHappened( new RemoveSuccessorUndoable( iTransform, transform ) );
//   }
//
   //---------------------------------------------------------------------------
   // Code generation
   //---------------------------------------------------------------------------
   /**
    * Sets whether code generation is enabled.
    * 
    * @param bEnabled true = code generation is enabled
    */
   public void setCodeGenerationEnabled( boolean bEnabled )
   {
      if (m_bCodeGenerationEnabled == bEnabled)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetCodeGenerationEnabledUndoable( m_bCodeGenerationEnabled, bEnabled ) );
      m_bCodeGenerationEnabled = bEnabled;
      fireModelChangedEvent( CODE_GENERATION_ENABLED_CHANGED, null );
   }
   
   /**
    * Is code generation enabled?
    * 
    * @return true = code generation is enabled
    */
   public boolean isCodeGenerationEnabled()
   {
      return m_bCodeGenerationEnabled;
   }
   
   public IPromptDefinitionValue[] getParameters(boolean includeSubComponents)
   {
      return super.getParameters();
   }
   
   /**
    * Generates code into the code string buffer.
    * 
    * @param environment the code generation environment
    * 
    * @return the string buffer that contains the code
    * @throws CodegenException
    * @throws ServerException 
    */
   public final ICodeSegment getCompleteCode( ICodeGenerationEnvironment environment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      return getCompleteCode( environment.createNewCodeSegment( this ) );
   }
   
   

   public final ICodeSegment getGeneratedCodeHeader(ICodeSegment codeSegment, IServer defaultServer, boolean isRemote, boolean isValidate)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
//      ICodeSegment codeSegment = environment.createNewCodeSegment( getPreProcessCode() );

      //getStepComment( codeSegment );
      
      if (isCodeGenerationEnabled())
      {
         if (isCompleteWithUserWritten())
         {
            // get environment quoting flag set (and validvarname code gen'd) before genRemoteCodeStart needs flag
            getValidVarNameCode(codeSegment,false);
            
            getGeneratedCodeHeaderMacrovars(codeSegment);
           
            getGeneratedDefaultParameterCode( codeSegment );

            IServer stepServer = getServerForStep( codeSegment.getCurrentServer() );
            
            if (isRemote)
               getGeneratedRemoteCodeStart( stepServer, codeSegment, defaultServer , isValidate);

            getTransformSetup( codeSegment, isRemote, isValidate );
         }
      }

      return codeSegment;
   }
   
   public final ICodeSegment getGeneratedCodeFooter(ICodeSegment codeSegment, IServer defaultServer, boolean isRemote, boolean isValidate)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      if (isCodeGenerationEnabled())
      {
         if (isCompleteWithUserWritten())
         {
        	// for transform keep the default parameters local, so that if a transform down stream
        	// has a parameter by the same name, it will generate its default value instead of using
        	// the already generated default by this one
        	removeGeneratedDefaultParameters(codeSegment);
        	
            getTransformCompletion( codeSegment );
            if (isRemote)
               getGeneratedRemoteCodeEnd( codeSegment.getCurrentServer(), codeSegment, defaultServer, isValidate );

            getGeneratedCodeEnd( codeSegment );
         }
      }

      return codeSegment;
   }

   /**
    * Generates complete validate code into the code string buffer.
    * 
    * @param environment the code generation environment
    * 
    * @return the string buffer that contains the code
    * @throws CodegenException
    * @throws ServerException 
    */
   public ICodeSegment getCompleteValidateCode( ICodeGenerationEnvironment environment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      return getCompleteValidateCode( environment.createNewCodeSegment( this ) );  
   }
   
   protected ICodeSegment getPreValidateCode(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {

      return codeSegment;
   }

   protected ICodeSegment getPostValidateCode(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {

      return codeSegment;
   }

   /**
    * Generates complete validate code into the code string buffer.
    * 
    * @param the code generation segment
    * 
    * @return the string buffer that contains the code
    * @throws CodegenException
    * @throws ServerException 
    */
   public ICodeSegment getCompleteValidateCode( ICodeSegment codeSegment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();
      IServer previousServer = environment.getCurrentServer();

      try
      {
         if(!isValidateAvailable())
         {
            return getValidateUnsupportedComment( codeSegment );
         }
         
         if(isExplicitOn())
         {
            return getValidateExplicitOnComment( codeSegment );
         }
         
         IServer stepServer = getServerForStep( previousServer );

         boolean isRemote = !ObjectComparator.isEqual(previousServer,stepServer);
         
         if (!isUsingUserWrittenCode())
         {               
            if (isGenerateDISHeaderAndFooters() )
            {
               ICodeSegment header = codeSegment.createNewCodeSegment( getPreProcessCode() ); 
               header.setIndent(codeSegment.getIndents());
               getGeneratedCodeHeader( header, previousServer,  isRemote, true );
               codeSegment.setIndent( header.getIndents() );
            }
   
            if (isCodeGenerationEnabled())
            {
               if (isCompleteWithUserWritten())
               {
                  getPreValidateCode( codeSegment );
                  getGeneratedCode( codeSegment, true );
                  getPostValidateCode( codeSegment );
               }
               else
               {
                  // TODO fix this because this is redundant and should not have to
                  // call in both cases
                  List inc = getReasonsIncomplete();
                  for ( int i = 0; i < inc.size(); i++ )
                     codeSegment.addCommentLine( inc.get( i ).toString() );
                  codeSegment.addSourceCode( "\n" );
               }
            }
            
            if (isGenerateDISHeaderAndFooters())
            {
               ICodeSegment footer = codeSegment.createNewCodeSegment( getPostProcessCode() );
               footer.setIndent( codeSegment.getIndents() );
               getGeneratedCodeFooter( footer,previousServer, isRemote, true );
            }
         }
      }
      catch (CodegenException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (MdException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (RemoteException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (BadServerDefinitionException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (BadLibraryDefinitionException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (ServerException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch(Exception e)
      {
         codeSegment.handleException( e );
         throw new CodegenException(e,this);
      }
      finally
      {
         codeSegment.getCodeGenerationEnvironment().setCurrentServer(previousServer);
      }
      
      return codeSegment;
   }

   
   /**
    * Generates code into the code string buffer.
    * 
    * @param environment the code generation environment
    * 
    * @return the string buffer that contains the code
    * @throws CodegenException
    * @throws ServerException 
    */
   protected ICheckpointRestart createNewCheckpointRestart()
   {
	   return new CheckpointRestart(this);
   }
   
   public ICodeSegment getCompleteCode( ICodeSegment codeSegment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();
      IServer previousServer = environment.getCurrentServer();
      try
      {
         IServer stepServer = getServerForStep( previousServer );

         boolean isRemote = previousServer!=null ? !ObjectComparator.isEqual(previousServer,stepServer) : false;

         boolean bIsComplete = isCompleteWithUserWritten();
         ICheckpointRestart cpr = createNewCheckpointRestart();

        cpr.getCheckpointRestartPreStepCode(codeSegment);   
         
         if (isGenerateDISHeaderAndFooters())
         {
            ICodeSegment header = codeSegment.createNewCodeSegment( getPreProcessCode() ); 
            header.setIndent(codeSegment.getIndents());
            getGeneratedCodeHeader( header, previousServer,  isRemote, false );
            codeSegment.setIndent( header.getIndents() );
         }

         if (isCodeGenerationEnabled())
         {
            if (bIsComplete)
            {

               if (!isUsingUserWrittenCode())
               {
                  getGeneratedCode( codeSegment );
               }
               else
               {
                  codeSegment.genUserWrittenCode( this, false );
               }
               
            }
            else
            {
               // TODO fix this because this is redundant and should not have to
               // call in both cases
               List inc = getReasonsIncomplete();
               for ( int i = 0; i < inc.size(); i++ )
                  codeSegment.addCommentLine( inc.get( i ).toString() );
               codeSegment.addSourceCode( "\n" );
            }
         }
         
         if (isGenerateDISHeaderAndFooters())
         {
            ICodeSegment footer = codeSegment.createNewCodeSegment( getPostProcessCode() );
            footer.setIndent( codeSegment.getIndents() );
            getGeneratedCodeFooter( footer,previousServer, isRemote, false );
         }

         cpr.getCheckpointRestartPostStepCode(codeSegment);         

      }
      catch (CodegenException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (MdException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (RemoteException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (BadServerDefinitionException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (BadLibraryDefinitionException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (ServerException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch(Exception e)
      {
         codeSegment.handleException( e );
         throw new CodegenException(e,this);
      }
      finally
      {
         codeSegment.getCodeGenerationEnvironment().setCurrentServer(previousServer);
      }
      
      return codeSegment;
   }  // end method getCompleteCode
   
   /**
    * Generate the ValidVarName (handling for special characters) code for the transform
    * Override this as needed (ex: AbstractDataTransform)
    * @param codeSegment the code segment to put the generated code into
    * @param isRemote TRUE if the statements have to go to a remote machine
    * @return the code segment with the generated code added.

    */
   public ICodeSegment getValidVarNameCode(ICodeSegment codeSegment, boolean isRemote)
   {
      return codeSegment;
   }

   /**
    * Checks the setting for whether or not LIBNAME CLEAR statements should be generated,
    * and if true, generates LIBNAME CLEAR statements for all libraries  allocated .  
    * This should not deallocate libraries that are preassigned or an alternate temporary library.
    * 
    * @param codeSegment the code segment
    * return codeSegment
    * @throws CodegenException
    */
   public ICodeSegment getClearLibraryCode(ICodeSegment codeSegment)
   throws CodegenException
   {
	   IJob currentJob = getJob();
	   if (currentJob != null)
	   {
		   if (!isClearLibrefEnabled())
			   return codeSegment;
	   }
	   // if job is null, just return
	   else
		   return codeSegment;
	   
	   ILibrary altTempLib = currentJob.getAlternateTemporaryLibrary();

	   List localLiblist = codeSegment.getCodeGenerationEnvironment().getLibrariesGeneratedList();

	   if (localLiblist != null && localLiblist.size() > 0)
	   {
		   for (int i=localLiblist.size()-1; i > -1; i--)
		   {
			   ILibrary lib = null;
			   lib = (ILibrary) localLiblist.get(i);
			   if (altTempLib != null && currentJob.isDeletingAlternateTemporaryTables()
					   && altTempLib.getID().equals(lib.getID()))
				   continue;
			   if (lib.isPreAssigned())
				   continue;
			   codeSegment.addSourceCode("/* ").addSourceCode("libname " + lib.getLibref() + " clear; */\n");
			   if (codeSegment.getCodeGenerationEnvironment().isGenerateRCSetCalls())
				   codeSegment.genRCSetCall("&syslibrc");    /*I18nOK:LINE*/
			   localLiblist.remove(lib);
		   }
       }
	   return codeSegment;
   }
   
   /**
    * Return value for clear librefs.
    * Values can be:  JOB, YES, or NO
    * 
    * @return value for clearing librefs
    */
   public String getClearLibrefGeneration()
   {
	   return m_sClearLibrefs;
   }
   
   /**
    * Sets whether librefs are cleared.
    * 
    * @param  valud values are JOB, YES, or NO
    */
   public void setClearLibrefGeneration (String value)
   {
	      if (value == m_sClearLibrefs)
	          return;
	       if (isUndoSupported())
	          undoableEditHappened (new SetClearLibrefsGenerationUndoable(m_sClearLibrefs, value));

	       m_sClearLibrefs = value;
	       fireModelChangedEvent (CLEARLIBREFSTRANSFORM_CHANGED, null);
   }
   
   /**
    * Is clearing libref code enabled?
    * 
    * @return true = generate code to clear librefs
    */
   public boolean isClearLibrefEnabled()
   {
      if (getClearLibrefGeneration().equalsIgnoreCase(CLEARLIBREFS_JOB))
      {
    	  return getJob().isClearLibrefEnabled();    		 
      }
      else 
    	  return getClearLibrefGeneration().equalsIgnoreCase(CLEARLIBREFS_YES);
   }
      
   public ICodeSegment getBodyCode(ICodeGenerationEnvironment environment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      ICodeSegment codeSegment = environment.createNewCodeSegment( this );

      codeSegment.setIsBody(true);
      
      try
      {
         
         if (isCodeGenerationEnabled())
         {
            if (!isUsingUserWrittenCode())
            {
               if (isCompleteWithUserWritten())
               {
                  getGeneratedCode( codeSegment );
               }
               else
               {
                  List inc = getReasonsIncomplete();
                  for (int i=0; i<inc.size(); i++)
                     codeSegment.addCommentLine( inc.get(i).toString() );
                  codeSegment.addSourceCode( "\n" );
               }
            }
            else
            {
               IUserWrittenCodeContainer uw = getUserWrittenCode();
               if (uw!=null)
                  codeSegment.addSourceCode( uw.getCode() );
            }
         }
      }
      catch (CodegenException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      catch (ServerException e)
      {
         codeSegment.handleException( e );
         throw e;
      }
      return codeSegment;
   }
   
   public ICodeSegment getPreDiagnostics(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      return codeSegment;
   }

   public ICodeSegment getPostDiagnostics(ICodeSegment codeSegment)
   throws CodegenException
   {
      return codeSegment;
   }   
   
   public ICodeSegment getRuntimeStatistics(ICodeSegment codeSegment)
   throws CodegenException
   {
      codeSegment.addCommentLine(RB.getStringResource( "AbstractTransform.RunTimeStatistics.Comment.txt" ));  // TODO update property bundle
      codeSegment.addSourceCode("%etls_setPerfInit;\n");
      codeSegment.addSourceCode("%perfstrt(txnname=%BQUOTE(_DISARM|");      
      codeSegment.addSourceCode("&transformID|");
      codeSegment.addSourceCode("&syshostname");
      codeSegment.addSourceCode("|");
      
      codeSegment.addSourceCode(getAbsoluteName());      
      if (codeSegment.getLoopCount()>0)
         codeSegment.addSourceCode( "_&etls_filePrefix" );
      
      codeSegment.addSourceCode(")");           
      if (codeSegment.isRunTableStatisticsEnabled())
         // handles passing in the metric for table row count
         codeSegment.addSourceCode(", metrNam6=_DISROWCNT, metrDef6=Count32");         

      codeSegment.addSourceCode(")   ;\n\n");
      return codeSegment;
   }

   public ICodeSegment getRuntimeStatisticsComplete(ICodeSegment codeSegment)
   throws CodegenException
   {
      boolean isCollectingTableCounts = codeSegment.isRunTableStatisticsEnabled();
      if (!isCollectingTableCounts)
         codeSegment.addSourceCode("%perfstop;\n\n");
      else    	  
      {    	 
    	 codeSegment.addSourceCode("%perfstop(metrVal6=%sysfunc(max(&etls_recnt,-1)));\n");
         codeSegment.addSourceCode("%let etls_recnt=-1;\n\n");
      }
      return codeSegment;
   }

   
   public ICodeSegment getTransformCompletion(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadLibraryDefinitionException, ServerException, BadServerDefinitionException
   {
      if (isUsingUserWrittenCode())
      {
         // capture return code into macros
         codeSegment.genRCSetCall("&syserr", false);   /*I18nOK:LINE*/
         codeSegment.genRCSetCall("&sqlrc", false);           /*I18nOK:LINE*/
      }

      if (isUsingPostProcessCode() && getPostProcessCode()!=null)
      {
         codeSegment.genCodeSource( getPostProcessCode().getUserWrittenSourceCode(), ICodeSegment.POSTPROCESS_START_COMMENT, ICodeSegment.POSTPROCESS_END_COMMENT );
      }

      if (isCollectingDiagnostics())
         getPostDiagnostics( codeSegment );
      
      if (codeSegment.isRunStatisticsEnabled())
         getRuntimeStatisticsComplete( codeSegment );
      
    //stwatk S0523533 
      if (getJob().isRCSetSYSCCEnabled())
      {
    	  codeSegment.genRCSetCall("&syscc",false)
    	             .addSourceCode("\n");
      }


      return codeSegment;
   }

   public ICodeSegment getTransformSetup(ICodeSegment codeSegment, boolean isRemote, boolean isValidate) 
   throws MdException, RemoteException, BadLibraryDefinitionException, BadServerDefinitionException, CodegenException, ServerException
   {
      if (isCollectingDiagnostics())
      {
         getPreDiagnostics( codeSegment );
      }
      
      if (codeSegment.isRunStatisticsEnabled())
          getRuntimeStatistics( codeSegment );
      
      if (isUsingPreProcessCode() && getPreProcessCode()!=null)
      {
         codeSegment.genSaveSyslast();  // addSourceCode("%let ETLS_SYSLAST = &SYSLAST;/n" );
         codeSegment.genCodeSource( getPreProcessCode().getUserWrittenSourceCode(), ICodeSegment.PREPROCESS_START_COMMENT, ICodeSegment.PREPROCESS_END_COMMENT );
         codeSegment.genRestoreSyslast(); // addSourceCode("%let SYSLAST = &ETLS_SYSLAST;/n" );
      }
      
      return codeSegment;
   }

   public ICodeSegment getGeneratedRemoteCodeStart(IServer stepServer,ICodeSegment codeSegment, IServer currentServer, boolean isValidate)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();

      //TODO if null and needed throw exception
      ISASClientConnection conn = stepServer.getConnectClient();

      if (conn==null)
         throw new CodegenException(MessageFormat.format(RB.getStringResource( "Connect.MissingConnection.txt" ),new String[]{stepServer.getName()}), this);

      // this step is remote, need to generate rsubmits

      if (!environment.isOnSignonCache( stepServer ))
      {
         environment.addToSignonCache( stepServer );

         conn.genAccessCode( codeSegment);
         
         codeSegment.addSourceCode("\n");
      }         
      // put the job_rc, trans_rc, and sqlrc macros to remote
//stwatk S0523533
      IJob job = getJob();
      codeSegment.genReturnCodeRemoteSetup(conn, codeSegment.getRuntimeStatsConnectMacros( codeSegment ), isValidate, job.isRCSetSYSCCEnabled());
      codeSegment.genRemoteMacroVariablesSetup(environment.getRemoteMacroVariables(),conn.getHostName(),true);
      
      conn.genStartSubmit(ICodeSegment.SYSRPUTSYNC_YES,codeSegment,true, codeSegment.isRunStatisticsEnabled(), codeSegment.isRunTableStatisticsEnabled());

      environment.setCurrentServer( stepServer );
      codeSegment.indent();
      // wrap remote code in macro call to avoid %let statements not resolving when
      //   rsubmit is within macro
      codeSegment.addSourceCode("%macro ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("(); \n") /*I18NOK:EMS**/
      .indent();
      if (environment.isQuoting())
      {
         codeSegment.genValidvarnameOptionAny(true);
      }

      codeSegment.addSourceCode("\n");
      codeSegment.getRCSetMacro();

      return codeSegment;
   }

   public ICodeSegment getGeneratedRemoteCodeEnd(IServer stepServer, ICodeSegment codeSegment, IServer currentServer, boolean isValidate)
   throws RemoteException, MdException, BadLibraryDefinitionException, BadServerDefinitionException,
   CodegenException, ServerException
   {

      ISASClientConnection conn = stepServer.getConnectClient();

      if (conn==null)
         throw new CodegenException(MessageFormat.format( RB.getStringResource( "Connect.MissingConnection.txt" ), new String[]{stepServer.getName()}), this);

      // this step was remote

      //TODO what here??
      
      if (codeSegment.isQuoting())
      {
         codeSegment.genValidvarnameOptionReset();
      }
      codeSegment.genReturnCodeRemoteEnding(isValidate, isValidateAvailable(), getJob().isRCSetSYSCCEnabled() );
      codeSegment.unIndent();
      codeSegment.addSourceCode("\n%mend ").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode("; \n\n")
      .addSourceCode("%").addSourceCode(TRANSFORM_MACRO_NAME).addSourceCode(";\n\n"); //S0359689
      codeSegment.unIndent();

      getClearLibraryCode(codeSegment);
      conn.genEndSubmit(codeSegment, getJob().isRCSetSYSCCEnabled());

      return codeSegment;
   }

   /**
    * Create Header for explicit on validate comment for the beginning of a step
    *
    * @param step - The transformationStep object
    * 
    * @return this CodegenRequest object
    */
   public final ICodeSegment getValidateExplicitOnComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      // header line
      String divider = codeSegment.repeat(ICodeSegment.STEP_DIVIDER, ICodeSegment.LINE_LENGTH - 4);
      
      codeSegment.addSourceCode("/*").addSourceCode(divider).addSourceCode("* \n");
      
      // step name and transformation type
      codeSegment.genCommentLine(codeSegment.getCommentLabelForStep(), getName(), getID());

      StringBuffer transformName = new StringBuffer();

      transformName.append( getDefaultName());
      //TODO fix version
      if (!DEFAULT_TRANSFORM_MODEL_VERSION.equals( getModelVersion() ))
      {
         transformName.append(" (").append(RB.getStringResource( "AbstractTransform.Version.txt")).append(" ").append(getModelVersion()).append(")");
      }

      codeSegment.genCommentLine(codeSegment.getCommentLabelForTransform(), transformName.toString(), ""); /*I18NOK:LSM**/

     
      // Description (could be blank)
      codeSegment.genCommentLine(codeSegment.getCommentLabelForDescription(), getDescription(), "");

      getSourceTargetComment( codeSegment );
      
      // put out a note if the step has been marked as not active
      codeSegment.genCommentValidateExplicitOn(divider + "*");

      // close comment
      codeSegment.addSourceCode(" *").addSourceCode(divider).addSourceCode("*/ \n\n");     

      return codeSegment;
   }
   
   /**
    * Create Header comment for the beginning of a step
    *
    * @param step - The transformationStep object
    * 
    * @return this CodegenRequest object
    */
   public final ICodeSegment getValidateUnsupportedComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      // header line
      String divider = codeSegment.repeat(ICodeSegment.STEP_DIVIDER, ICodeSegment.LINE_LENGTH - 4);
      
      codeSegment.addSourceCode("/*").addSourceCode(divider).addSourceCode("* \n");
      
      // step name and transformation type
      codeSegment.genCommentLine(codeSegment.getCommentLabelForStep(), getName(), getID());

      StringBuffer transformName = new StringBuffer();

      transformName.append( getDefaultName());
      //TODO fix version
      if (!DEFAULT_TRANSFORM_MODEL_VERSION.equals( getModelVersion() ))
      {
         transformName.append(" (").append(RB.getStringResource( "AbstractTransform.Version.txt")).append(" ").append(getModelVersion()).append(")");
      }

      codeSegment.genCommentLine(codeSegment.getCommentLabelForTransform(), transformName.toString(), ""); /*I18NOK:LSM**/

     
      // Description (could be blank)
      codeSegment.genCommentLine(codeSegment.getCommentLabelForDescription(), getDescription(), "");

      getSourceTargetComment( codeSegment );
      
      // put out a note if the step has been marked as not active
      if (!isValidateAvailable())
         codeSegment.genCommentValidateUnsupported(divider + "*");

      // close comment
      codeSegment.addSourceCode(" *").addSourceCode(divider).addSourceCode("*/ \n\n");     

      return codeSegment;
   }
   
   /**
    * Create Header comment for the beginning of a step
    *
    * @param step - The transformationStep object
    * 
    * @return this CodegenRequest object
    */
   public final ICodeSegment getStepComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      // header line
      String divider = codeSegment.repeat(ICodeSegment.STEP_DIVIDER, ICodeSegment.LINE_LENGTH - 4);
      
      codeSegment.addSourceCode("/*").addSourceCode(divider).addSourceCode("* \n");
      
      // step name and transformation type
      codeSegment.genCommentLine(codeSegment.getCommentLabelForStep(), getName(), getID());

      StringBuffer transformName = new StringBuffer();

      transformName.append( getDefaultName());

      //TODO fix version
      if (!DEFAULT_TRANSFORM_MODEL_VERSION.equals( getModelVersion() ))
      {
         transformName.append(" (").append(RB.getStringResource( "AbstractTransform.Version.txt")).append(" ").append(getModelVersion()).append(")");
      }

      codeSegment.genCommentLine(codeSegment.getCommentLabelForTransform(), transformName.toString(), ""); /*I18NOK:LSM**/

     
      // Description (could be blank)
      codeSegment.genCommentLine(codeSegment.getCommentLabelForDescription(), getDescription(), "");

      getSourceTargetComment( codeSegment );

      getAdditionalTransformationComment(codeSegment);
      
      // if this is user-written code, then display information about code
      if (isUsingUserWrittenCode())
      {
         codeSegment.genCommentUserWrittenLine(this,codeSegment.getCommentLabelForUserWritten());
      } // if - step is user defined


      // put out Notes if they exist
      codeSegment.genCommentNotesLine(this, divider + "*");

      // put out a note if the step has been marked as not active
      if (!isCodeGenerationEnabled())
         codeSegment.genCommentNotActive(divider + "*");

      if (hasWarnings())
      {
         codeSegment.genCommentLine("","","");
         codeSegment.genCommentLine( RB.getStringResource( "AbstractTransform.Warnings.txt" ),"","");
         List warnings = getWarnings();
         for (int i=0; i<warnings.size(); i++)
            codeSegment.genCommentLine( codeSegment.splitString( warnings.get( i ).toString(), ICodeSegment.LINE_LENGTH-5, false ) );
      }
      
      // close comment
      codeSegment.addSourceCode(" *").addSourceCode(divider).addSourceCode("*/ \n\n");     

      return codeSegment;

   } // method: getStepComment
   
   
   public ICodeSegment getSourceTargetComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
      return codeSegment;
   }
   
   public ICodeSegment getAdditionalTransformationComment(ICodeSegment codeSegment)
   throws CodegenException, BadLibraryDefinitionException
   {
   	return codeSegment;
   }
   
   public final ICodeSegment getGeneratedCodeHeaderMacrovars(ICodeSegment codeSegment)
   throws CodegenException
   {
      codeSegment.addSourceCode( "%let transformID = %quote(").addSourceCode( getID() ).addSourceCode(");\n");
      if (isResetTransformationReturnCodeWhenGeneratingCode())
         codeSegment.addSourceCode( "%let trans_rc = 0;\n");
      codeSegment.genDatetimeMacrovarAssignment(ICodeSegment.STEPSTARTTIME_MACROVAR_NAME, "datetime20." );
      
      return codeSegment;
   }
   

   public ICodeSegment getGeneratedCodeEnd(ICodeSegment codeSegment)
   throws CodegenException
   {
//    TODO need to do footer and other stuff
	   getClearLibraryCode(codeSegment);
       codeSegment.addSourceCode( "\n\n/** "  ).addSourceCode( RB.getStringResource( "AbstractTransform.StepEnd.txt" )).addSourceCode(" ").addSourceCode(this.getName()).addSourceCode(" **/\n\n");
      return codeSegment;
   }
   
   /**
    * Append the generated code for the transform either in validation mode or run mode
    * @param codeSegment code segment to append to
    * @param validateCode generate the code with no execution on to check for pushdown and valid syntax
    * @return codesegment
    * @throws CodegenException
    * @throws MdException
    * @throws RemoteException
    * @throws BadServerDefinitionException
    * @throws BadLibraryDefinitionException
    * @throws ServerException
    * @see com.sas.etl.models.job.transforms.sql.ISQLTransform#getGeneratedCode(com.sas.etl.models.job.ICodeSegment, boolean)
    */
   protected ICodeSegment getGeneratedCode( ICodeSegment codeSegment, boolean validateCode ) 
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      String systemOptions = getSystemOptions();
      if (systemOptions!=null && systemOptions.length()>0)
         codeSegment.genSystemOptions( systemOptions );
      return codeSegment;
   }

   
   /**
    * Generates code into the code string buffer.
    * 
    * @param environment the code generation environment
    * 
    * @return the string buffer that contains the code
    * @throws RemoteException 
    * @throws MdException 
    */
   protected ICodeSegment getGeneratedCode( ICodeSegment codeSegment )
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
      return getGeneratedCode( codeSegment, false);
   }
   
   /**
    * Sets options to be used for the system.  There is no validation done
    * on these options.
    * 
    * @param sOptions the options
    */
   public void setSystemOptions( String sOptions )
   {
      if (ObjectComparator.isEqual(m_sSystemOptions, sOptions ))
         return;
      
      if (sOptions!=null && sOptions.isEmpty())
      	sOptions = null;
      
//      if (sOptions == null)
//         throw new NullPointerException( "system options must not be null" );   // I18NOK:EMS

      if (isUndoSupported())
         undoableEditHappened( new SetSystemOptionsUndoable( m_sSystemOptions, sOptions ) );
      
      m_sSystemOptions = sOptions;
      fireModelChangedEvent( SYSTEM_OPTIONS_CHANGED, null );
   }
   
   /**
    * Sets whether the transform should use user written code.
    * 
    * @param bUseUserWrittenCode true = use user written code
    */
   public void setUseUserWrittenCode( boolean bUseUserWrittenCode )
   {
      m_codeUserWritten.setIsActive( bUseUserWrittenCode );
//      if (m_bUseUserWrittenCode == bUseUserWrittenCode)
//         return;
//      
//      if (isUndoSupported())
//         undoableEditHappened( new SetUseUserWrittenCodeUndoable( m_bUseUserWrittenCode, bUseUserWrittenCode ) );
//      m_bUseUserWrittenCode = bUseUserWrittenCode;
//      fireModelChangedEvent( USE_USER_WRITTEN_CODE_CHANGED, null );
   }
   
   /**
    * Gets the options to be used for the system.
    * 
    * @return the options
    * 
    * @see #setSystemOptions(String)
    */
   public String getSystemOptions()
   {
      return m_sSystemOptions;
   }
   
   /**
    * get the UI placement options saved, x and y position and node id
    * @param UI placement
    */
   public IUIPlacement getUIPlacement()
   {
	   if (m_UIPlacement == null)
		   m_UIPlacement = new UIPlacement();
	   return m_UIPlacement;
   }
   
   private void setUIPlacement(IUIPlacement placement)
   {
	   m_UIPlacement = placement;
   }
   
   /**
    * Is the transform using user written code instead of generated code?
    * 
    * @return true = the transform is using user written code
    */
   public boolean isUsingUserWrittenCode()
   {
      return m_codeUserWritten.isActive();
   }

   /**
    * Sets the user written code to be used for this transform.
    * 
    * @param code the user written code
    * @see com.sas.etl.models.job.ITransform#setUserWrittenCode(com.sas.etl.models.job.ICodeSource)
    */
   public void setUserWrittenCode( ICodeSource code )
   {
//      ICodeSource src = m_codeUserWritten.getUserWrittenSourceCode();
//      if (src == code)
//         return;
      
      // delete the old one and make sure the new one is not in the deleted objects list (for undo)
//      if (src != null)
//         addToDeletedObjects( src );
//      removeFromDeletedObjects( code );
      
      m_codeUserWritten.setUserWrittenSourceCode( code);
   }
   
   /**
    * Gets the user written code for the transform.
    * 
    * @return the code for the transform
    */
   public IUserWrittenCodeContainer getUserWrittenCode()
   {
      return m_codeUserWritten;
   }
   
   
   /**
    * Sets the host on which the transform's code will be executed.
    * 
    * @param server the server on which the transform's code will be executed
    */
   public void setExecutionServer( IServer server )
   {
      if (m_executionServer == server)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetExecutionServerUndoable( m_executionServer, server ) );
      m_executionServer = server;
      fireModelChangedEvent( EXECUTION_SERVER_CHANGED, m_executionServer );
   }
   
   /**
    * Gets the host on which the transform's code will be executed.
    * 
    * @return the host on which the transform's code will be executed
    */
   public IServer getExecutionServer()
   {
      return m_executionServer;
   }

   public boolean isResetTransformationReturnCodeWhenGeneratingCode()
   {
      return true;
   }
   
   public boolean isGenerateDISHeaderAndFooters()
   {
      return m_bGenerateDISHeaderAndFooters;
   }

   public void setGenerateDISHeaderAndFooters( boolean generateHeaderFooters )
   {
      if (m_bGenerateDISHeaderAndFooters==generateHeaderFooters)
         return;
      
      if (isUndoSupported())
         undoableEditHappened( new SetGenerateDISHeaderAndFooters(m_bGenerateDISHeaderAndFooters, generateHeaderFooters) );
      
      m_bGenerateDISHeaderAndFooters = generateHeaderFooters;
      
      setPreProcessEnabled(m_bGenerateDISHeaderAndFooters );
      setPostProcessEnabled(m_bGenerateDISHeaderAndFooters );

      fireModelChangedEvent( GENERATE_DIS_HEADER_AND_FOOTERS_CHANGED, null );
      
   }
   
   /**
    * Get the IServer for this step, takes default server passed in as consideration for the host selection.
    * 
    * @param defaultServer the default IServer, if this method is to take the default server into consideration, otherwise null
    * 
    * @return The IServer for this step
    * 
    */
   public IServer getServerForStep(IServer defaultServer) 
   throws BadServerDefinitionException, BadLibraryDefinitionException
   {
      IServer server = getExecutionServer();
      if (server==null)
         server = defaultServer;
      
      return server;
   } // method: getServerForStep
   

   /**
    * Is validate for this transform's code available
    * @return true = the object does support validatation of it's code
    *
    * @see com.sas.etl.models.IObject#isChanged()
    */
   public boolean isValidateAvailable()
   {
      return false;
   }
   
   //---------------------------------------------------------------------------
   // Persistence
   //---------------------------------------------------------------------------
   
   /**
    * Saves the transform as XML to the specified stream.
    * 
    * @param strm the output stream
    * 
    * @throws IOException
    * 
    * @see com.sas.etl.models.IStreamPersistable#saveXMLToStream(java.io.OutputStream)
    */
   public void saveXMLToStream( OutputStream strm ) throws IOException
   {
      // TODO Auto-generated method stub
   }

   /**
    * Loads the transform from XML from the specified stream.
    * 
    * @param strm the input stream
    * 
    * @throws IOException
    * 
    * @see com.sas.etl.models.IStreamPersistable#loadXMLFromStream(java.io.InputStream)
    */
   public void loadXMLFromStream( InputStream strm ) throws IOException
   {
      // TODO Auto-generated method stub
   }

   /**
    * Gets the type of OMR object used to represent the transform.
    * 
    * @return the OMR type.
    */
   public String getOMRType()
   {
      return MetadataObjects.TRANSFORMATIONSTEP;
   }
   
   /**
    * Gets the public type of the object.
    * 
    * @return Transformation
    * 
    * @see com.sas.workspace.models.SimpleObject#getPublicType()
    */
   public String getPublicType()
   {
      return PUBLIC_TYPE;
   }
   
   /**
    * Returns the metadata model version number for this object
    * 
    * @return UsageVersion attribute with the version of this object
 
    * @see com.sas.etl.models.impl.AbstractComplexPersistableObject#getArchitectureVersionNumber()
    */
   public UsageVersion getArchitectureVersionNumber()
   {
      //TODO: What to return here????  Do we want to use this?
      return (new UsageVersion(1, 0));
   }
   
   /**
    * Saves the transform to OMR.
    * 
    * @param omr the adapter used to access OMR
    * 
    * @throws MdException
    * @throws RemoteException
    */
   public void saveToOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      if (!isChanged())
         return;
      
      super.saveToOMR( omr );
      
      m_iInstanceVersion++;
      
      TransformationStep mdoStep = (TransformationStep) omr.acquireOMRObject( this ); 
      mdoStep.setTransformRole( getTransformRole() );
      // TODO persist instance version
      // TODO persist model version
      mdoStep.setIsActive(      isCodeGenerationEnabled() ? 1 : 0 );
      mdoStep.setIsUserDefined( isUsingUserWrittenCode()  ? 1 : 0 );

      AbstractTransformation mdoAnchor = getUserWrittenCodeAnchor( omr );
      if (mdoAnchor != null)
      {	  
         m_codeUserWritten.setContainerId( mdoAnchor.getFQID() );
         m_codeUserWritten.setContainerOMRType( mdoAnchor.getCMetadataType() );
         m_codeUserWritten.saveToOMR( omr );
      }
      //TODO change when you methods for options are added
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, SYSTEM_OPTIONS_VARIABLE_NAME, SYSTEM_OPTIONS_VARIABLE_NAME, "SYSTEM", getSystemOptions(), Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

      saveBooleanOptionToOMR( omr, DIAGNOSTICS_PROPERTY, isCollectingDiagnostics());
      
      saveBooleanOptionToOMR( omr, GENERATE_DIS_HEADER_AND_FOOTER_PROPERTY_NAME, m_bGenerateDISHeaderAndFooters );

      saveBooleanOptionToOMR( omr, OPTIONS_PROPERTYSET, OPTION_ENABLE_CHECKPOINTS, isCheckpointEnabled() );
      
      saveBooleanOptionToOMR( omr, OPTION_RUN_ALWAYS, isRunAlways() );
      
      savePropertyToOMR( omr, OPTIONS_PROPERTYSET, CLEAR_LIBRREFS, CLEAR_LIBRREFS, CLEAR_LIBRREFS, m_sClearLibrefs, Types.VARCHAR, USE_PROPERTYSET_PROPERTIES );

      saveStringOptionToOMR( omr, UI_PROPERTY, UIPlacementUtil.createXMLFromUIPlacement(getUIPlacement()));
      
      List computeLocations = mdoStep.getComputeLocations();
      // need to only remove other ServerContext's
      for (int i=computeLocations.size()-1; i>=0; i--)
      {
    	  Root s = (Root)computeLocations.get(i);
    	  if (MetadataObjects.SERVERCONTEXT.equals(s.getCMetadataType()))
    	  {
    		  computeLocations.remove(i);
    	  }
      }
      if (m_executionServer!=null)
         computeLocations.add( omr.acquireOMRObject( m_executionServer ) );
      
      saveTransformClassToOMR(omr);
//      saveControlFlowToOMR(   mdoStep, omr );
      getUIPlacement().setChanged(false);
      setChanged(false);
   }
   
   protected void saveTransformClassToOMR(OMRAdapter omr)
   throws MdException, RemoteException
   {
	   savePropertyToOMR( omr, null, "Class", "Class", "", getTransformClass(), Types.VARCHAR, USE_PROPERTIES_DIRECTLY );
	   Workspace.getDefaultLogger().debug("Saving:  The transformation class property.  Transform Name: " + getName() + " ID: " + getID());
   }
   
   protected AbstractTransformation getUserWrittenCodeAnchor( OMRAdapter omr ) throws MdException, RemoteException
   {
      return (AbstractTransformation) omr.acquireOMRObject( this );
   }
   
//   /**
//    * Saves control flow to OMR.
//    * 
//    * @param mdoStep the transformation step used to represent the transform
//    * @param omr     the OMR adapter
//    * 
//    * @throws MdException
//    * @throws RemoteException
//    */
//   protected void saveControlFlowToOMR( TransformationStep mdoStep, OMRAdapter omr ) throws MdException, RemoteException
//   {
//      // if there is a step precedence and it's not supposed to be used, delete it
//      if ((m_sStepPrecedenceID != null) && !m_bUseStepPrecedence)
//      {
//         omr.deleteOMRObject( m_sStepPrecedenceID, MetadataObjects.STEPPRECEDENCE );
//         m_sStepPrecedenceID = null;
//      }
//      
//      // if there are no successors, done
//      if (getControlSuccessorCount() == 0)
//         return;
//      
//      if (m_sStepPrecedenceID == null)
//         m_sStepPrecedenceID = createIDForNewObject();
//      StepPrecedence mdoSP = (StepPrecedence) omr.acquireOMRObject( m_sStepPrecedenceID, MetadataObjects.STEPPRECEDENCE );
//      mdoSP.setName( "untitled" );
//
//      List lSuccessors   = mdoSP.getSuccessors(   false );
//      List lPredecessors = mdoSP.getPredecessors( false );
//      
//      lSuccessors  .clear();
//      lPredecessors.clear();
//      lPredecessors.add( mdoStep );
//
//      ITransform[] aSuccessors = getControlSuccessors();
//      for ( int iSuccessor=0; iSuccessor<aSuccessors.length; iSuccessor++ )
//         lSuccessors.add( omr.acquireOMRObject( aSuccessors[iSuccessor] ) );
//   }
//   
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
      m_codeUserWritten.updateIds( mapIDs );
   }
   
   /**
    * Loads the transform from OMR.
    * 
    * @param omr the OMR adapter
    * 
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.IOMRPersistable#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) throws MdException, RemoteException
   {
      super.loadFromOMR( omr );
      TransformationStep mdoStep = (TransformationStep) omr.acquireOMRObject( this ); 

      setSystemOptions( loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, SYSTEM_OPTIONS_VARIABLE_NAME, "", USE_PROPERTYSET_PROPERTIES ));
      
      setCollectDiagnostics( loadBooleanOptionFromOMR( omr, DIAGNOSTICS_PROPERTY, false));
      
      setCodeGenerationEnabled( mdoStep.getIsActive() == 1 );

      // get user written code settings
      AbstractTransformation mdoAnchor = getUserWrittenCodeAnchor( omr );
      if (mdoAnchor != null)
      {	  
         m_codeUserWritten.setContainerId( mdoAnchor.getFQID() );
         m_codeUserWritten.setContainerOMRType( mdoAnchor.getCMetadataType() );
         m_codeUserWritten.loadFromOMR( omr );
      }
      List lHost = mdoStep.getComputeLocations();
      if (!lHost.isEmpty())
      {
         Root host = (Root)lHost.get( 0 );
         // only load up ServerContext's 
         if (MetadataObjects.SERVERCONTEXT.equals(host.getCMetadataType()))
         {
        	 IServer server = (IServer)omr.acquireObject( host );
        	 setExecutionServer( server );
         }
      }
      Workspace.getDefaultLogger().debug("Loading: The transformation class property.  Transform Name: " + getName() + " ID: " + getID());
      loadPropertyFromOMR( omr, null, "Class", getTransformClass(), USE_PROPERTIES_DIRECTLY );  // really only needed for save/deletion purposes
      
      setGenerateDISHeaderAndFooters( loadBooleanOptionFromOMR( omr, GENERATE_DIS_HEADER_AND_FOOTER_PROPERTY_NAME, true ) );

      setCheckpointEnabled( loadBooleanOptionFromOMR( omr, OPTIONS_PROPERTYSET, OPTION_ENABLE_CHECKPOINTS, false ) );
      
      setRunAlways( loadBooleanOptionFromOMR( omr, OPTION_RUN_ALWAYS, false ) );

      
      setUIPlacement(UIPlacementUtil.createUIPlacementFromXML(loadStringOptionFromOMR(omr, UI_PROPERTY, "")));
   
      String clearlibrefs_value = loadPropertyFromOMR( omr, OPTIONS_PROPERTYSET, CLEAR_LIBRREFS, CLEARLIBREFS_JOB, USE_PROPERTYSET_PROPERTIES);
      setClearLibrefGeneration(clearlibrefs_value);
       
      setChanged(false);
   }

//   /**
//    * Loads control flow from OMR.
//    * 
//    * @param mdoStep      the transformation step used to represent the transform
//    * @param omr          the OMR adapter
//    * 
//    * @throws MdException
//    * @throws RemoteException 
//    */
//   private void loadControlFlowFromOMR( TransformationStep mdoStep, OMRAdapter omr ) throws MdException, RemoteException
//   {
//      List lPredecessorDependencies = mdoStep.getPredecessorDependencies();
//      if (lPredecessorDependencies.isEmpty())
//         return;
//      
//      StepPrecedence mdoSP = (StepPrecedence) lPredecessorDependencies.get(0);
//      m_sStepPrecedenceID = mdoSP.getFQID();
//      m_bUseStepPrecedence = true;
//      
//      List lSuccessors = mdoSP.getSuccessors();
//      for ( int iSuccessor=0; iSuccessor<lSuccessors.size(); iSuccessor++ )
//      {
//         Root       mdoSuccessor       = (Root) lSuccessors.get( iSuccessor );
//         ITransform transformSuccessor = (ITransform) omr.acquireObject( mdoSuccessor );
//         addControlSuccessor( transformSuccessor );
//         transformSuccessor.addControlPredecessor( this );
//         transformSuccessor.setChanged( false );
//      }
//   }
//   
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
      
//      if (m_sStepPrecedenceID != null)
//         omr.deleteOMRObject( m_sStepPrecedenceID, MetadataObjects.STEPPRECEDENCE );
      if (m_codeUserWritten != null)
         m_codeUserWritten.deleteFromOMR( omr );
      
      super.deleteFromOMR( omr );
   }

   /**
    * Gets the template map used to populate an OMR adapter for the transform.
    * 
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssociations = (List) map.get( getOMRType() );
      lAssociations.add( TransformationStep.ASSOCIATION_PREDECESSORDEPENDENCIES_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_PROPERTIES_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_SOURCECODE_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_TRANSFORMATIONSOURCES_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_TRANSFORMATIONTARGETS_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_COMPUTELOCATIONS_NAME );
      lAssociations.add( TransformationStep.ASSOCIATION_CONDITIONACTIONSETS_NAME );
      map.put( MetadataObjects.TRANSFORMATIONSTEP, lAssociations );
      
      lAssociations = new ArrayList();
      lAssociations.add( Transformation.ASSOCIATION_SOURCECODE_NAME );
      map.put( MetadataObjects.TRANSFORMATION, lAssociations );

      lAssociations = new ArrayList();
      lAssociations.add( StepPrecedence.ASSOCIATION_SUCCESSORS_NAME );
      map.put( MetadataObjects.STEPPRECEDENCE, lAssociations );

      m_codeUserWritten.getOMRLoadTemplateMap( map );
      
      return map;
   }

   /**
    * Gets the template map used to copy the transform.
    * 
    * @return the copy template map
    */
   public Map getOMRCopyTemplateMap()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   /**
    * Gets the template map used to export the transform.
    * 
    * @return the export template map
    */
   public Map getOMRExportTemplateMap()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   /**
    * Gets the template map used to checkout the transform.
    * 
    * @return the checkout template map
    */
   public Map getOMRCheckOutTemplateMap()
   {
      // TODO Auto-generated method stub
      return null;
   }
 
   public List<IObject> pasteAction()
   throws MdException, RemoteException
   {
	   getUIPlacement().setNodeId( GUID.newGUID());
	   
	   return new ArrayList<IObject>();
   }
   
   public List<IPasteModify> getPasteChildObjects()
   {
   	return null;
   }
   
   /**
    * Dumps the transform to a print stream
    * 
    * @param strm the print stream.
    * 
    * @see com.sas.etl.models.IObject#dump(java.io.PrintStream)
    */
   public void dump( PrintStream strm )
   {
      super.dump( strm );
      strm.println( "CodeGenerationEnabled=" + m_bCodeGenerationEnabled );
      strm.println( "UseUserWrittenCode="    + m_codeUserWritten.isActive()    ); 
      
//      strm.println( "<Predecessors>" );        // I18NOK:EMS
//      for ( int iPredecessor=0; iPredecessor<m_lPredecessors.size(); iPredecessor++ )
//      {
//         IObject src = (IObject) m_lPredecessors.get( iPredecessor );
//         strm.println( src.getName() );
//      }
//      strm.println( "</Predecessors>" );        // I18NOK:EMS
//      
//      strm.println( "<Successors>" );        // I18NOK:EMS
//      for ( int iSuccessor=0; iSuccessor<m_lSuccessors.size(); iSuccessor++ )
//      {
//         IObject src = (IObject) m_lSuccessors.get( iSuccessor );
//         strm.println( src.getName() );
//      }
//      strm.println( "</Successors>" );        // I18NOK:EMS
   }
   public void dumpObjectToXML(PrintStream stream, int order)
   {
      stream.println( "<" + getPublicType() );
      
      stream.println("order=\"" + Integer.toString(order) + "\" ");
      stream.println("name=\"" + getName() + "\" ");
      stream.println("description=\"" + getDescription() + "\" ");
      
      stream.println("enabled=\"" + isCodeGenerationEnabled() + "\" ");
      stream.println("userWritten=\"" + isUsingUserWrittenCode() + "\" >");
      
      dumpXML(stream);
      
      stream.println( "</" + getPublicType() + ">");
   }
  

   public void dumpXML(PrintStream stream)
   {
 
   }
   
   /**
    * Adds the default setting for the transform 
    */
   public void addDefaultSettings() throws MdException, RemoteException
   {
   }
   
   
   //---------------------------------------------------------------------------
   // Helper Classes
   //---------------------------------------------------------------------------
   /**
    * SetJobHelper is the helper class for maintaining the transform's job.  It
    * is responsible for adding and/or removing the transform from the job when
    * the transform's job changes.
    */
   private class SetJobHelper extends AbstractSecondaryAttributeHelper
   {
      /**
       * Constructs the set job helper.
       */
      public SetJobHelper()
      {
         super( AbstractTransform.this );
      }

      /**
       * Adds the secondary object (this transform) to the primary object (the 
       * job being set).
       * 
       * @param primary the job
       * 
       * @see com.sas.etl.models.impl.AbstractSecondaryAttributeHelper#addTo()
       */
      protected void addTo( IObject primary )
      {
         ((IJob) primary).getTransformsList().add( AbstractTransform.this );
      }

      /**
       * Removes the secondary object (this transform) from the primary object 
       * (the job.
       * 
       * @param primary the job
       * 
       * @see com.sas.etl.models.impl.AbstractSecondaryAttributeHelper#removeFrom()
       */
      protected void removeFrom( IObject primary )
      {
         ((IJob) primary).getTransformsList().remove( AbstractTransform.this );
      }
   } // SetJobHelper
   
   //---------------------------------------------------------------------------
   // Undoables
   //---------------------------------------------------------------------------
   /**
    * SetCodeGenerationEnabledUndoable is the undoable for setting whether code
    * generation is enabled.
    */
   private class SetCodeGenerationEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_bOldEnabled;
      private boolean m_bNewEnabled;
      
      /**
       * Constructs the set code generation enabled undoable.
       * 
       * @param bOldEnabled the old code generation enabled attribute
       * @param bNewEnabled the new code generation enabled attribute
       */
      public SetCodeGenerationEnabledUndoable( boolean bOldEnabled, boolean bNewEnabled )
      {
         m_bOldEnabled = bOldEnabled;
         m_bNewEnabled = bNewEnabled;
      }
      
      /**
       * Undoes the setting of the code generation enabled attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setCodeGenerationEnabled( m_bOldEnabled );
      }
      
      /**
       * Redoes the setting of the code generation enabled attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setCodeGenerationEnabled( m_bNewEnabled );
      }
   } // SetCodeGenerationEnabledUndoable

   /**
    * SetCollectDiagnosticsUndoable is the undoable for setting whether diagnostics are used
    */
   private class SetCollectDiagnosticsUndoable extends AbstractUndoableEdit
   {
      private boolean m_bOldUseDiagnostics;
      private boolean m_bNewUseDiagnostics;
      
      /**
       * Constructs the set use diagnostics undoable.
       * 
       * @param bOldUseDiagnostics the old use diagnostics attribute
       * @param bNewUseDiagnostics the new use user written code attribute
       */
      public SetCollectDiagnosticsUndoable( boolean bOldUseDiagnostics, boolean bNewUseDiagnostics )
      {
         m_bOldUseDiagnostics = bOldUseDiagnostics;
         m_bNewUseDiagnostics = bNewUseDiagnostics;
      }
      
      /**
       * Undoes the setting of the use diagnostics attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setCollectDiagnostics( m_bOldUseDiagnostics );
      }
      
      /**
       * Redoes the setting of the use diagnostics attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setCollectDiagnostics( m_bNewUseDiagnostics );
      }
   } // SetCollectDiagnosticsUndoable
   
   /**
    * SetRunAlwaysUndoable is the undoable for SetRunAlways 
    */
   private class SetRunAlwaysUndoable extends AbstractUndoableEdit
   {
      private boolean m_bOldValue;
      private boolean m_bNewValue;
      
      /**
       * Constructs the set use SetRunAlways undoable.
       * 
       * @param bOldValue the old use diagnostics attribute
       * @param bNewValue the new use user written code attribute
       */
      public SetRunAlwaysUndoable( boolean bOldValue, boolean bNewValue )
      {
         m_bOldValue = bOldValue;
         m_bNewValue = bNewValue;
      }
      
      /**
       * Undoes the setting of the use SetRunAlways attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setRunAlways( m_bOldValue );
      }
      
      /**
       * Redoes the setting of the use SetRunAlways attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setRunAlways( m_bNewValue );
      }
   } // SetRunAlwaysUndoable
   
   /**
    * SetGenerateDISHeaderAndFooters is the undoable for setting whether the 
    * job is automatically generating the DIS header and footer code.
    */
   private class SetGenerateDISHeaderAndFooters extends AbstractUndoableEdit
   {
      private boolean m_bOldGenerateDISHeaderAndFooters;
      private boolean m_bNewGenerateDISHeaderAndFooters;
      
      /**
       * Constructs the set propagate automatically attribute undoable
       * 
       * @param bOldGenerateDISHeaderAndFooters the old GenerateDISHeaderAndFooters attribute
       * @param bNewGenerateDISHeaderAndFooters the new GenerateDISHeaderAndFooters attribute
       */
      public SetGenerateDISHeaderAndFooters( boolean bOldGenerateDISHeaderAndFooters, boolean bNewGenerateDISHeaderAndFooters )
      {
         m_bOldGenerateDISHeaderAndFooters = bOldGenerateDISHeaderAndFooters;
         m_bNewGenerateDISHeaderAndFooters = bNewGenerateDISHeaderAndFooters;
      }
      
      /**
       * Undoes the setting of the GenerateDISHeaderAndFooters attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setGenerateDISHeaderAndFooters( m_bOldGenerateDISHeaderAndFooters );
      }
      
      /**
       * Redoes the setting of the GenerateDISHeaderAndFooters attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setGenerateDISHeaderAndFooters( m_bNewGenerateDISHeaderAndFooters);
      }
   } // SetGenerateDISHeaderAndFootersUndoable

   /**
    * SetExecutionServerUndoable is the undoable for setting the execution
    * server attribute.
    */
   private class SetExecutionServerUndoable extends AbstractUndoableEdit
   {
      private IServer m_serverOld;
      private IServer m_serverNew;
      
      /**
       * Constructs the set server undoable.
       * 
       * @param serverOld the old server
       * @param serverNew the new server
       */
      public SetExecutionServerUndoable( IServer serverOld, IServer serverNew )
      {
         m_serverOld = serverOld;
         m_serverNew = serverNew;
      }
      
      /**
       * Undoes the setting of execution server.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setExecutionServer( m_serverOld );
      }
      
      /**
       * Redoes the setting of the execution server.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setExecutionServer( m_serverNew );
      }
   } // SetExecutionServerUndoable

//   /**
//    * AddPredecessorUndoable is the undoable edit for adding a predecessor.
//    */
//   private class AddPredecessorUndoable extends AbstractUndoableEdit
//   {
//      private int        m_iPredecessor;
//      private ITransform m_predecessor;
//      
//      /**
//       * Constructs the add predecessor undoable.
//       * 
//       * @param iPredecessor index of where predecessor was added
//       * @param predecessor  the predecessor that was added
//       */
//      public AddPredecessorUndoable( int iPredecessor, ITransform predecessor )
//      {
//         m_iPredecessor = iPredecessor;
//         m_predecessor  = predecessor;
//      }
//      
//      /**
//       * Undoes the addition of the predecessor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#undo()
//       */
//      public void undo()
//      {
//         super.undo();
//         removeControlPredecessor( m_predecessor );
//      }
//      
//      /**
//       * Redoes the addition of the predecessor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#redo()
//       */
//      public void redo()
//      {
//         super.redo();
//         addControlPredecessor( m_iPredecessor, m_predecessor );
//      }
//      
//      /**
//       * Kills the undoable edit allowing the edit to remove any references.
//       * 
//       * @see javax.swing.undo.UndoableEdit#die()
//       */
//      public void die()
//      {
//         m_predecessor = null;
//      }
//   } // AddPredecessorUndoable
//   
//   /**
//    * RemovePredecessorUndoable is the undoable edit for removing a predecessor.
//    */
//   private class RemovePredecessorUndoable extends AbstractUndoableEdit
//   {
//      private int        m_iPredecessor;
//      private ITransform m_predecessor;
//      
//      /**
//       * Constructs the remove predecessor undoable.
//       * 
//       * @param iPredecessor index of where predecessor was removed
//       * @param predecessor the predecessor that was removed
//       */
//      public RemovePredecessorUndoable( int iPredecessor, ITransform predecessor )
//      {
//         m_iPredecessor = iPredecessor;
//         m_predecessor  = predecessor;
//      }
//      
//      /**
//       * Undoes the removal of the predecessor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#undo()
//       */
//      public void undo()
//      {
//         super.undo();
//         addControlPredecessor( m_iPredecessor, m_predecessor );
//      }
//      
//      /**
//       * Redoes the removal of the predecessor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#redo()
//       */
//      public void redo()
//      {
//         super.redo();
//         removeControlPredecessor( m_predecessor );
//      }
//      
//      /**
//       * Kills the undoable edit allowing the edit to remove any references.
//       * 
//       * @see javax.swing.undo.UndoableEdit#die()
//       */
//      public void die()
//      {
//         m_predecessor = null;
//      }
//   } // RemovePredecessorUndoable
//
//   /**
//    * AddSuccessorUndoable is the undoable edit for adding a successor.
//    */
//   private class AddSuccessorUndoable extends AbstractUndoableEdit
//   {
//      private int        m_iSuccessor;
//      private ITransform m_successor;
//      
//      /**
//       * Constructs the add successor undoable.
//       * 
//       * @param iSuccessor index of where successor was added
//       * @param successor  the successor that was added
//       */
//      public AddSuccessorUndoable( int iSuccessor, ITransform successor )
//      {
//         m_iSuccessor = iSuccessor;
//         m_successor  = successor;
//      }
//      
//      /**
//       * Undoes the addition of the successor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#undo()
//       */
//      public void undo()
//      {
//         super.undo();
//         removeControlSuccessor( m_successor );
//      }
//      
//      /**
//       * Redoes the addition of the successor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#redo()
//       */
//      public void redo()
//      {
//         super.redo();
//         addControlSuccessor( m_iSuccessor, m_successor );
//      }
//      
//      /**
//       * Kills the undoable edit allowing the edit to remove any references.
//       * 
//       * @see javax.swing.undo.UndoableEdit#die()
//       */
//      public void die()
//      {
//         m_successor = null;
//      }
//   } // AddSuccessorUndoable
//   
//   /**
//    * RemoveSuccessorUndoable is the undoable edit for removing a successor.
//    */
//   private class RemoveSuccessorUndoable extends AbstractUndoableEdit
//   {
//      private int        m_iSuccessor;
//      private ITransform m_successor;
//      
//      /**
//       * Constructs the remove successor undoable.
//       * 
//       * @param iSuccessor index of where successor was removed
//       * @param successor the successor that was removed
//       */
//      public RemoveSuccessorUndoable( int iSuccessor, ITransform successor )
//      {
//         m_iSuccessor = iSuccessor;
//         m_successor  = successor;
//      }
//      
//      /**
//       * Undoes the removal of the successor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#undo()
//       */
//      public void undo()
//      {
//         super.undo();
//         addControlSuccessor( m_iSuccessor, m_successor );
//      }
//      
//      /**
//       * Redoes the removal of the successor.
//       * 
//       * @see javax.swing.undo.UndoableEdit#redo()
//       */
//      public void redo()
//      {
//         super.redo();
//         removeControlSuccessor( m_successor );
//      }
//      
//      /**
//       * Kills the undoable edit allowing the edit to remove any references.
//       * 
//       * @see javax.swing.undo.UndoableEdit#die()
//       */
//      public void die()
//      {
//         m_successor = null;
//      }
//   } // RemoveSuccessorUndoable
//   
   /**
    * SetSystemOptionsUndoable is the undoable for setting the sort 
    * transform's SYSTEM options attribute.
    */
   private class SetSystemOptionsUndoable extends AbstractUndoableEdit
   {
      private String m_oldSystemOptions;
      private String m_newSystemOptions;
      
      /**
       * Constructs the set SYSTEM options attribute undoable
       * 
       * @param oldSystemOptions the old SYSTEM options attribute
       * @param newSystemOptions the new SYSTEM options attribute
       */
      public SetSystemOptionsUndoable( String oldSystemOptions, String newSystemOptions )
      {
         m_oldSystemOptions = oldSystemOptions;
         m_newSystemOptions = newSystemOptions;
      }
      
      /**
       * Undoes the setting of the SYSTEM options attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setSystemOptions( m_oldSystemOptions );
      }
      
      /**
       * Redoes the setting of the SYSTEM options attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setSystemOptions( m_newSystemOptions );
      }
   } // SetSystemOptionsUndoable

   /**
    * SetCheckpointEnabled is the undoable for setting whether the 
    * job is automatically generating the DIS header and footer code.
    */
   private class SetCheckpointEnabledUndoable extends AbstractUndoableEdit
   {
      private boolean m_bOldCheckpointEnabled;
      private boolean m_bNewCheckpointEnabled;
      
      /**
       * Constructs the set propagate automatically attribute undoable
       * 
       * @param bOldCheckpointEnabled the old CheckpointEnabled attribute
       * @param bNewCheckpointEnabled the new CheckpointEnabled attribute
       */
      public SetCheckpointEnabledUndoable( boolean bOldCheckpointEnabled, boolean bNewCheckpointEnabled )
      {
         m_bOldCheckpointEnabled = bOldCheckpointEnabled;
         m_bNewCheckpointEnabled = bNewCheckpointEnabled;
      }
      
      /**
       * Undoes the setting of the CheckpointEnabled attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setCheckpointEnabled( m_bOldCheckpointEnabled );
      }
      
      /**
       * Redoes the setting of the CheckpointEnabled attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setCheckpointEnabled( m_bNewCheckpointEnabled);
      }
   } // SetCheckpointEnabledUndoable
   
   public final ICodeSegment[] getCodeSegments( ICodeGenerationEnvironment environment )
   throws MdException, RemoteException, BadLibraryDefinitionException, BadServerDefinitionException, ServerException, CodegenException
   {
      List segments = new ArrayList();
      
      IServer previousServer = environment.getDefaultServer();
      
      IServer stepServer = getServerForStep( previousServer );

      boolean isRemote = !ObjectComparator.isEqual(previousServer,stepServer);

      boolean bIsComplete = isCompleteWithUserWritten();

      //header 
      ICodeSegment header = environment.createNewCodeSegment(getPreProcessCode());
      
      getGeneratedCodeHeader( header, previousServer,  isRemote, false );

      segments.add(header);
      
      ICodeSegment body = environment.createNewCodeSegment(this);
      body.setIsBody(true);
      segments.add(body);
      if (isCodeGenerationEnabled())
      {
         if (bIsComplete)
         {

            if (!isUsingUserWrittenCode())
            {
               getGeneratedCode( body );
            }
            else
            {
            	body.genUserWrittenCode( this, false );
            }
            
         }
         else
         {
            // TODO fix this because this is redundant and should not have to
            // call in both cases
            List inc = getReasonsIncomplete();
            for ( int i = 0; i < inc.size(); i++ )
            	body.addCommentLine( inc.get( i ).toString() );
            body.addSourceCode( "\n" );
         }
      }
      ICodeSegment footer = environment.createNewCodeSegment( getPostProcessCode() );
      getGeneratedCodeFooter( footer,previousServer, isRemote, false );
      segments.add(footer);
      
      return (ICodeSegment[])segments.toArray(new ICodeSegment[segments.size()]);
   }

   /**
    * SetExplicitOnUndoable is the undoable for setting the 
    * transform's explicit on value
    */
   private class SetExplicitOnUndoable extends AbstractUndoableEdit
   {
      private boolean m_oldExplicitOn;
      private boolean m_newExplicitOn;
      
      /**
       * Constructs the set included in explicit on undoable
       * 
       * @param oldExplicitOn the old explicit on value
       * @param newExplicitOn the new explicit on valie
       */
      public SetExplicitOnUndoable( boolean oldExplicitOn, boolean newExplicitOn )
      {
         m_oldExplicitOn = oldExplicitOn;
         m_newExplicitOn = newExplicitOn;
      }
      
      /**
       * Undoes the setting of the dbms execution type.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setExplicitOn( m_oldExplicitOn );
      }
      
      /**
       * Redoes the setting of the dbms execution type.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setExplicitOn( m_newExplicitOn );
      }
   } // SetExplicitOnUndoable
   
   /**
    * 
    * Set clear libref undoable
    */
   private class SetClearLibrefsGenerationUndoable extends AbstractUndoableEdit
   {
      private String m_oldValue;
      private String m_newValue;
      
      /**
       * Constructs the use clear libref undoable
       * 
       * @param oldValue  the old value
       * @param newValue  the new value
       */
      public SetClearLibrefsGenerationUndoable (String oldValue, String newValue)
      {
         m_oldValue = oldValue;
         m_newValue = newValue;
      }
      
      /**
       * Undoes the setting of the use clear libref option.
       */
      public void undo()
      {
         super.undo();
         setClearLibrefGeneration(m_oldValue);
      }
      
      /**
       * Redoes the setting of the use clear libref option.
       */
      public void redo()
      {
         super.redo();
         setClearLibrefGeneration(m_newValue);
      }
   } // SetClearLibrefsGenerationUndoable
   
}