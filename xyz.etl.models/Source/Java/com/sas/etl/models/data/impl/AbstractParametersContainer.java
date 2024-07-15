/* $Id: AbstractParametersContainer.java,v 1.1.2.22.14.6.12.1.86.1 2021/01/29 00:31:28 sasclw Exp $ */
/**
 * Title:       BaseParameters.java
 * Description:
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Chris Watson
 * Support:     Chris Watson
 */
package com.sas.etl.models.data.impl;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import com.sas.etl.models.IModel;
import com.sas.etl.models.ServerException;
import com.sas.etl.models.data.BadLibraryDefinitionException;
import com.sas.etl.models.data.IParametersContainer;
import com.sas.etl.models.impl.AbstractComplexPersistableObject;
import com.sas.etl.models.impl.OMRAdapter;
import com.sas.etl.models.job.ICodeGenerationEnvironment;
import com.sas.etl.models.job.ICodeSegment;
import com.sas.etl.models.job.impl.CodeGenerationEnvironment;
import com.sas.etl.models.job.impl.CodeSegment;
import com.sas.etl.models.job.impl.CodegenException;
import com.sas.etl.models.other.BadServerDefinitionException;
import com.sas.etl.models.prompts.IPromptDefinitionValue;
import com.sas.etl.models.prompts.IPromptModel;
import com.sas.etl.models.prompts.impl.BasePromptModel;
import com.sas.etl.models.prompts.impl.PromptUtils;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.MetadataObjects;
import com.sas.metadata.remote.PromptGroup;
import com.sas.metadata.remote.Root;
import com.sas.metadata.remote.Transformation;
import com.sas.prompts.PromptUtil;
import com.sas.prompts.PromptValues;
import com.sas.prompts.definitions.PromptDefinitionInterface;
import com.sas.prompts.groups.PromptGroupInterface;
import com.sas.services.ServiceException;
import com.sas.services.information.util.SmartTypes;
import com.sas.storage.exception.ServerConnectionException;
import com.sas.workspace.Workspace;

/**
 * Abstract class container for parameterized objects.
 * 
 */
public abstract class AbstractParametersContainer extends AbstractComplexPersistableObject implements IParametersContainer
{
   private PromptGroupInterface m_promptGroup;
   private IPromptModel m_promptModel;
   
   
   /**
    * Constructor
    * @param sID container id
    * @param model model object
    */
   public AbstractParametersContainer(String sID, IModel model)
   {
      super(sID, model);
      m_promptGroup = PromptUtils.createPromptGroup();
      updatePromptModel();
   }
      
   /**
    * Delete the object from omr
    * @param omr adapter
    * @throws RemoteException
    * @throws MdException
    * @see com.sas.etl.models.impl.AbstractComplexPersistableObject#deleteFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void deleteFromOMR(OMRAdapter omr)
   throws RemoteException, MdException
   {
      if (isNew())
         return;

      super.deleteFromOMR(omr);

      PromptGroup pGroup = (PromptGroup)omr.acquireOMRObject( this );

      if (pGroup!=null && !pGroup.isNewObject())
      {
         Transformation trans = pGroup.getValueSource();
         if (trans!=null && !trans.isNewObject())
            omr.deleteOMRObject( trans.getFQID(), trans.getCMetadataType() );
      }
   }   
   
   /**
    * dispose of the model
    * 
    * @see com.sas.etl.models.impl.BaseObject#dispose()
    */
   public void dispose()
   {
      if (m_promptModel != null)
         m_promptModel.dispose();
      
      super.dispose();
   }
   
   /**
    * Get the omr type
    * @return PROMPTGROUP
    * @see com.sas.etl.models.IOMRPersistable#getOMRType()
    */
   public String getOMRType()
   {
      return MetadataObjects.PROMPTGROUP;
   }

   /**
    * Get the public type
    * @return EMBEDDED:PROMPTGROUP
    * @see com.sas.workspace.models.SimpleObject#getPublicType()
    */
   public String getPublicType()
   {
      return SmartTypes.PREFIX_EMBEDDED + SmartTypes.TYPE_PROMPTGROUP;
   }
   
   /**
    * Set the prompt group for the object
    * @param promptGroup prompt group
    * @see com.sas.etl.models.data.IParametersContainer#setPromptGroup(com.sas.prompts.groups.PromptGroupInterface)
    */
   public void setPromptGroup(PromptGroupInterface promptGroup)
   {
      if (m_promptGroup==promptGroup)
         return;

      if (isUndoSupported())
         undoableEditHappened( new SetPromptGroupUndoable( m_promptGroup, promptGroup ) );
      
      m_promptGroup = promptGroup;
      
      updatePromptModel();
      
      fireModelChangedEvent( PROMPTGROUP_CHANGED, m_promptGroup );
   }
   
   /**
    * Get the prompt group containing the parameters
    * @return prompt group
    * @see com.sas.etl.models.data.IParametersContainer#getPromptGroup()
    */
   public PromptGroupInterface getPromptGroup()
   {
      return m_promptGroup;
   }
   
   protected void updatePromptModel()
   {
      if (m_promptModel!=null)
         m_promptModel.dispose();
      
      m_promptModel = getNewPromptModel();     
   }
   
   protected IPromptModel getNewPromptModel()
   {
      PromptGroupInterface group = getPromptGroup();
      
      return new BasePromptModel(getModel(),this,group);
   }
   
   public IPromptModel getPromptModel()
   {
      return m_promptModel;
   }
   
   public IPromptDefinitionValue[] getParameters()
   {
	  IPromptModel model = getPromptModel();
	  
      return model!=null ? model.getPromptDefinitionValues() : new IPromptDefinitionValue[0];
   }

   /**
    * Gets the template map used to populate an OMR adapter for a parameters
    * container.
    * 
    * @return the load template map
    */
   public Map getOMRLoadTemplateMap()
   {
      Map map = super.getOMRLoadTemplateMap();
      
      List lAssociations = (List) map.get( getOMRType() );
      lAssociations.add( Root.ASSOCIATION_PROMPTS_NAME );
      
      return map;
   }

   public ICodeSegment getGeneratedDefaultParameterCode(ICodeSegment codeSegment)
   throws CodegenException, MdException, RemoteException, BadServerDefinitionException, BadLibraryDefinitionException, ServerException
   {
     
      IPromptModel model = getPromptModel();
      if (model!=null)
      {
    	 List<IPromptDefinitionValue> prompts = Arrays.asList(getParameters()); 
    	 ICodeGenerationEnvironment environment = codeSegment.getCodeGenerationEnvironment();
    	 try
         {    	 
	    	 prompts = environment.getDefinitionsNotGenerated(prompts, codeSegment);
	    	 IPromptDefinitionValue[] promptsArr = prompts.toArray(new IPromptDefinitionValue[prompts.size()]); 
	    	 
             codeSegment.addSourceCode( getDefaultParameterCode( codeSegment, promptsArr ) );
        	 codeSegment.getCodeGenerationEnvironment().addGeneratedParameters(this, codeSegment);
         }
         catch(ServiceException e)
         {
            throw new CodegenException(e, this);
         }
         catch(ServerConnectionException e)
         {
            throw new CodegenException(e, this);
         }
      }

      return codeSegment;
   }
   
   public void removeGeneratedDefaultParameters(ICodeSegment codeSegment)
   {
	   codeSegment.getCodeGenerationEnvironment().removeGeneratedParameters(this);
   }

   public StringBuffer getDefaultParameterCode()
   throws BadLibraryDefinitionException, BadServerDefinitionException, MdException, RemoteException, ServerException,
   CodegenException
   {
      ICodeSegment codeSegment = new CodeSegment(new CodeGenerationEnvironment(null), this);
      return getDefaultParameterCode(codeSegment);
   }
   
   public StringBuffer getDefaultParameterCode(ICodeSegment codeSegment)
   throws BadLibraryDefinitionException, BadServerDefinitionException, MdException, RemoteException, ServerException,
   CodegenException
   {
      return getDefaultParameterCode(codeSegment, getParameters());
   }

   private StringBuffer getDefaultParameterCode(ICodeSegment codeSegment, IPromptDefinitionValue[] prompts)
   throws BadLibraryDefinitionException, BadServerDefinitionException, MdException, RemoteException, ServerException,
   CodegenException
   {
      StringBuffer code = new StringBuffer();
      IPromptModel model = getPromptModel();
      try
      {
         if (model!=null)
         {
            StringBuffer macros = model.getOptionsString( codeSegment, null, true, true, false, false, prompts );
            if (macros!=null && macros.length()>0)
            {
               code.append( codeSegment.makeComment( MessageFormat.format(RB.getStringResource( "AbstractParametersContainer.ParameterMacroCode.ParameterDefaults.txt" ),new Object[]{getName()}) ));
               code.append(macros)
               .append( "\n" );
            }
         }
      }
      catch(ServiceException e)
      {
         throw new CodegenException(e, this);
      }
      catch(ServerConnectionException e)
      {
         throw new CodegenException(e, this);
      }
      return code;
   }
   
   public Map<String,String> getMacroNamesAndValues(ICodeSegment codeSegment)
   throws MdException, RemoteException, CodegenException, BadLibraryDefinitionException, BadServerDefinitionException, ServiceException, ServerConnectionException, ServerException
   {
	  IPromptModel model = getPromptModel();
	  Map<String,String> macros = new HashMap<String,String>();
      try
      {
         if (model!=null)
         {
            macros.putAll(model.getMacroNamesAndValues(codeSegment, null, true, true, false, false ));
         }
      }
      catch(ServiceException e)
      {
         throw new CodegenException(e, this);
      }
      catch(ServerConnectionException e)
      {
         throw new CodegenException(e, this);
      }
      return macros;
   }

   
   /**
    * If the object has a prompt group and the group has parameters (definitions)
    * @return true if a prompt group exists and it has definitions
    * @see com.sas.etl.models.data.IParametersContainer#hasParameters()
    */
   public boolean hasParameters()
   {
      return (m_promptGroup!=null && !m_promptGroup.getPromptDefinitions( true ).isEmpty());
   }
   
   /**
    * Load from omr
    * @param omr omradapter
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.impl.AbstractComplexPersistableObject#loadFromOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void loadFromOMR( OMRAdapter omr ) 
   throws MdException, RemoteException
   {
      super.loadFromOMR( omr );
      
      Root mdObject = omr.acquireOMRObject( this );
      
      PromptGroupInterface pGroup = PromptUtils.getPromptGroupFromMetadataObject(Workspace.getWorkspace()!=null ? Workspace.getWorkspace().getUserContext() : null, mdObject );
      if (pGroup!=null)
         setPromptGroup( pGroup );
      else
         setPromptGroup( PromptUtils.createPromptGroup());
      
      setChanged( false );
   }
   
   protected Map getConflictingMacroNames()
   {
      PromptGroupInterface group = getPromptGroup();
      
      PromptValues promptValues = new PromptValues( group );
      Map conflicts = new HashMap();
      List visibleDefinitions = PromptUtil.getVisualOrderOfDefinitions( promptValues, group, false );
      int size = visibleDefinitions.size();
      for (int i=0; i<size; i++)
      {
         PromptDefinitionInterface prompt = (PromptDefinitionInterface)visibleDefinitions.get( i );
         conflicts.putAll( PromptUtil.getConflictingPromptsBasedOnMacroVariableName( group, prompt ));
      }
      return conflicts;
   }
   
   /**
    * if the parameters are complete
    * @return treu if complete
    * @see com.sas.etl.models.impl.BaseObject#isComplete()
    */
   public boolean isComplete()
   {
      Map conflicts = getConflictingMacroNames();

      if (conflicts.size()>0)
         return false;
      
      return super.isComplete();
   }
   
   /**
    * Reasons the parameters are incomplete
    * @return list of reasons incomplete, or empty list if no reasons
    * @see com.sas.etl.models.impl.BaseObject#getReasonsIncomplete()
    */
   public List getReasonsIncomplete()
   {
      List reasons = new ArrayList();
      reasons.addAll( super.getReasonsIncomplete());
      
      Map conflicts = getConflictingMacroNames();
      int size = conflicts.size();
      for (int i=0; i<size; i++)
      {
         reasons.add( "Macro name conflict for prompt " + conflicts.keySet().toArray()[i] );
      }
      

      return reasons;
   }

   /**
    * Save to omr
    * @param omr omradapter
    * @throws MdException
    * @throws RemoteException
    * @see com.sas.etl.models.impl.AbstractComplexPersistableObject#saveToOMR(com.sas.etl.models.impl.OMRAdapter)
    */
   public void saveToOMR( OMRAdapter omr ) 
   throws MdException, RemoteException
   {
      if (!isChanged())
         return;
               
      super.saveToOMR( omr );

      Root mdObject = omr.acquireOMRObject( this );

      mdObject.getPrompts(false).clear();
      if (hasParameters())
      {
         PromptUtils.savePromptGroupToMetadataObject(Workspace.getWorkspace().getUserContext(), getModel().createIDForNewObject( getID() ), m_promptGroup, mdObject );
      }
      
      setChanged( false );
   }
   
   /**
    * SetPromptGroupUndoable is the undoable for setting the prompt group 
    * for parameters
    */
   private class SetPromptGroupUndoable extends AbstractUndoableEdit
   {
      private PromptGroupInterface m_oldPromptGroup;
      private PromptGroupInterface m_newPromptGroup;
      
      /**
       * Constructs the set prompt group attribute undoable
       * 
       * @param oldPromptGroup the old prompt group attribute
       * @param newPromptGroup the new prompt group attribute
       */
      public SetPromptGroupUndoable( PromptGroupInterface oldPromptGroup, PromptGroupInterface newPromptGroup )
      {
         m_oldPromptGroup = oldPromptGroup;
         m_newPromptGroup = newPromptGroup;
      }
      
      /**
       * Undoes the setting of the prompt group attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#undo()
       */
      public void undo()
      {
         super.undo();
         setPromptGroup( m_oldPromptGroup );
      }
      
      /**
       * Redoes the setting of the prompt group attribute.
       * 
       * @see javax.swing.undo.UndoableEdit#redo()
       */
      public void redo()
      {
         super.redo();
         setPromptGroup( m_newPromptGroup );
      }
   } // SetPromptGroupUndoable


   
}


