/* $Id: ISortingTransform.java,v 1.1.2.5 2008/05/02 20:02:31 sasclw Exp $ */
package com.sas.etl.models.job;

import com.sas.etl.models.IObject;
import com.sas.etl.models.data.IDataObject;
import com.sas.etl.models.job.transforms.common.ISorting;

/**
 * ISortingTransform
 */
public interface ISortingTransform extends IObject
{
   /**
    * sort order changed id
    */
   static final String SORT_ORDER_CHANGED = "ISortingTransform:SortOrderChanged";
   
   /**
    * Gets the data sources for the transform.
    * 
    * @return the data sources
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataSources()
    */
   IDataObject[] getDataSources();

   /**
    * Gets the data targets for the transform.
    * 
    * @return the data targets
    * 
    * @see com.sas.etl.models.job.IDataTransform#getDataTargets()
    */
   IDataObject[] getDataTargets();
   
   
   /**
    * Get the sorting object
    * @return sorting object
    */
   public ISorting getSortOrder();
   
   
}
