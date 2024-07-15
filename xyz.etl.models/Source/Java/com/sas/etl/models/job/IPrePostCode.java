/* $Id: IPrePostCode.java,v 1.1.2.6 2008/05/12 20:50:53 sasclw Exp $ */
/**
 * Title:       IPrePostCode.java
 * Description: Interface for users of pre- and post-code.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Daniel Wong
 * Support:     Daniel Wong
 */
package com.sas.etl.models.job;

import com.sas.etl.models.IPersistableObject;

/**
 * IPrePostCode defines the interface for an object that can have pre- and
 * post-code attached to it.
 */
public interface IPrePostCode extends IPersistableObject
{
   /** event type for use pre-process code changed */
   static final String USE_PRE_PROCESS_CODE_CHANGED     = "IPrePostCode:UsePreProcessCodeChanged";
   /** event type for pre-process code changed */
   static final String PRE_PROCESS_CODE_CHANGED         = "IPrePostCode:PreProcessCodeChanged";
   /** event type for use post-process code changed */
   static final String USE_POST_PROCESS_CODE_CHANGED    = "IPrePostCode:UsePostProcessCodeChanged";
   /** event type for post-process code changed */
   static final String POST_PROCESS_CODE_CHANGED        = "IPrePostCode:PostProcessCodeChanged";

   /** role for pre-process container **/
   static final String PREPROCESS_ROLE = "PreProcess";
   /** role for post-process container **/
   static final String POSTPROCESS_ROLE = "PostProcess";
   
   /**
    * event if the pre process availability has been enabled/disabled
    */
   static final String PREPROCESS_ENABLED_CHANGED = "IPrePostCode:PreProcessEnabledChanged";
   
   /**
    * event if the post process availability has been enabled/disabled
    */
   static final String POSTPROCESS_ENABLED_CHANGED = "IPrePostCode:PostProcessEnabledChanged";
   
   /**
    * Sets whether the transform should use the pre-process code.
    * 
    * @param bUsePreProcessCode true = use the pre-process code
    */
   void setUsePreProcessCode( boolean bUsePreProcessCode );
   
   /**
    * Is the transform using pre-process code instead of generated code?
    * 
    * @return true = the transform is using pre-process code
    */
   boolean isUsingPreProcessCode();
   
   /**
    * Sets the pre-process code for the transform.
    * 
    * @param code the pre-process code
    */
   void setPreProcessCode( ICodeSource code );
   
   /**
    * Gets the pre-process code for the transform.
    * 
    * @return the pre-process code
    */
   IUserWrittenCodeContainer getPreProcessCode();
   
   /**
    * Sets whether the transform should use the post-process code.
    * 
    * @param bUsePostProcessCode true = use the post-process code
    */
   void setUsePostProcessCode( boolean bUsePostProcessCode );
   
   /**
    * Is the transform using post-process code instead of generated code?
    * 
    * @return true = the transform is using post-process code
    */
   boolean isUsingPostProcessCode();
   
   /**
    * Sets the post-process code for the transform.
    * 
    * @param code the post-process code
    */
   void setPostProcessCode( ICodeSource code );
   
   /**
    * Gets the post-process code for the transform.
    * 
    * @return the post-process code
    */
   IUserWrittenCodeContainer getPostProcessCode();

   /**
    * Is preprocess availability is enabled
    * @return true if the pre process is available
    */
   boolean isPreProcessEnabled();
   
   /**
    * Is postprocess availability is enabled
    * 
    * @return true if the post process is available
    */
   boolean isPostProcessEnabled();
}

