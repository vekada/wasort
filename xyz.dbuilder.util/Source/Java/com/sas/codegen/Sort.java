/* $Id: Sort.java,v 1.1.2.4 2007/01/17 14:57:59 sasclw Exp $ */
/*
 * Title:       Sort
 * Description: Generate SAS code for the Sort transformation
 * Copyright:   Copyright (c) 2003
 * Company:     SAS 
 * Author:      Martha Hall
 * Support:     Martha Hall
 */
package com.sas.codegen;

import com.sas.metadata.remote.ClassifierMap;
import com.sas.metadata.remote.DataTable;
import com.sas.metadata.remote.MdException;
import com.sas.metadata.remote.Select;
import com.sas.workspace.OptionsPropertyHandler;
import com.sas.workspace.WAdminResource;

/**
 * Generate SAS code for the Sort transformation
 */
public class Sort
{
   private static WAdminResource bundle = WAdminResource.getBundle(Sort.class);

   // constants
   private static final String SORT_OPTIONS   = "SORT";
   
   /** 
	 * constructor
	 */
   private Sort()
   {
   } // constructor

   /**
    * Entry point for Transformation codegen
    *
    * @param cgReq CodegenRequest
    * @param classifierMap ClassifierMap
    *
    * @throws MdException
    * @throws java.rmi.RemoteException
    */
   public static void executeUtility(CodegenRequest cgReq, ClassifierMap classifierMap) throws MdException, java.rmi.RemoteException
   {
      // make sure that there is a source and a target table
      cgReq.checkSourceAndTarget(classifierMap);

      // make sure that the target is not an external table
      cgReq.checkTargetsForExternalTable(classifierMap);
      
      // make sure that the sources are not plain external tables (not permanent views)
      cgReq.checkSourcesForExternalTable(classifierMap);
      
      // Get the source table in order to get the table options
      DataTableCG sourceTableCG = cgReq.getSourceTableCG(classifierMap);

      // source data set options
      String sourceOptions = sourceTableCG.getSourceTableOptions();

      // there should only be 1 output, so just have to get the first classifier source
      DataTable targetTable = (DataTable)classifierMap.getClassifierTargets().get(0);
      
      DataTableCG targetTableCG = (DataTableCG)CodeGenUtil.getCodeGenClass(targetTable);

      // get the libname.dsname of the output table
      String targetName = targetTableCG.getDataLocation(cgReq);

      // source data set options
      String targetOptions = targetTableCG.getTargetTableOptions(true);
      
      // get the options for proc sort
      OptionsPropertyHandler op = new OptionsPropertyHandler(classifierMap);
      // get the options for PROC SORT
      String sortOptions = op.getOptionsString(SORT_OPTIONS);

      // get the options for the OPTIONS statement
      cgReq.genSystemOptions(classifierMap);

      // delete the target table
      cgReq.genTableDelete(targetTable);

      // generate the Extract to do the renames
      if (AutoExtract.columnMapping(classifierMap, cgReq, sourceOptions))
         sourceOptions = "";

      // proc sort statement
      cgReq.addSourceCode("proc sort data = &SYSLAST \n");  
      
      // data= option
      if (sourceOptions.trim().length() > 0)
         cgReq.addSourceCode("                    (" + sourceOptions.trim() + ") \n");

      // out= option
      cgReq.addSourceCode("          out = " + targetName);
      
      if (targetOptions.length() > 0)
        cgReq.addSourceCode("\n")
             .addSourceCode("                    " + targetOptions.trim());
      
      // proc sort options
      if (sortOptions.trim().length() > 0) 
         cgReq.addSourceCode("\n")
              .addSourceCode("          " + sortOptions.trim()); 

      cgReq.addSourceCode("; \n")
           .indent();

      // by statement
      String byStatement = cgReq.makeByStatement((Select) classifierMap);

      // error if no columns in OrderBy
      if (byStatement == null)
         throw new CodegenException(bundle.getString("Sort.OrderByNoColumns.msg.txt"));

      cgReq.addSourceCode(byStatement + "; \n")
           .unIndent()
           .addSourceCode("run; \n\n")
           .genRCSetCall("&syserr");   /*I18nOK:LINE*/
           
   } // method: executeUtility

} // class: Sort