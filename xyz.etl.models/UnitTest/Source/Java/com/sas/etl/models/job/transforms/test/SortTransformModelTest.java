/* $Id$ */
/**
 * Title: AbstractDataTransformTest.java Description: Copyright: Copyright (c)
 * 2006 Company: SAS Institute Author: Russ Robison Support: Russ Robison
 */

package com.sas.etl.models.job.transforms.test;

import java.awt.List;
import java.rmi.RemoteException;
import java.util.Arrays;


import com.sas.etl.models.IObject;
import com.sas.etl.models.data.IColumn;
import com.sas.etl.models.data.IDataObject;
import com.sas.etl.models.data.ITable;
import com.sas.etl.models.data.IWorkTable;
import com.sas.etl.models.impl.ModelEvent;
import com.sas.etl.models.job.IDataTransform;
import com.sas.etl.models.job.impl.test.AbstractDataTransformTest;
import com.sas.etl.models.job.transforms.SortTransformModel;
import com.sas.etl.models.job.transforms.common.ISortColumn;
import com.sas.etl.models.job.transforms.common.ISorting;
import com.sas.metadata.remote.MdException;

/**
 * The class <code>AbstractDataTransformTest</code> contains tests for the
 * class {@link <code>AbstractDataTransform</code>}
 * 
 * @pattern JUnit Test Case
 * 
 * @generatedBy CodePro at 10/30/06 2:31 PM
 * 
 * @author sasrlr
 * 
 * @version $Revision$
 */
public class SortTransformModelTest extends AbstractDataTransformTest
{
   private SortTransformModel m_sort;
   private ITable             m_tblSource;
   private ITable             m_tblTarget;

   /**
    * Construct new test instance
    * 
    * @param name the test name
    */
   public SortTransformModelTest( String name )
   {
      super( name );
   }
   
   protected void setTestObject( IObject object )
   {
      super.setTestObject( object );
      m_sort = (SortTransformModel) object;
   }
   
   protected IObject createNewTestObject()
   {
      return getModel().getObjectFactory().createNewTransform( SortTransformModel.getTransformTypeID(), getFullRepositoryID() );
   }
   
   protected IObject createTestObject( String sID )
   {
      return getModel().getObjectFactory().createTransform( SortTransformModel.getTransformTypeID(), sID );
   }
   
   protected void setUpOMR() throws MdException, RemoteException
   {
      super.setUpOMR();
      m_tblSource = createTable( "source", "" );
      m_tblTarget = createTable( "target", "" );
   }
   
   protected void tearDownOMR() throws MdException, RemoteException
   {
      super.tearDownOMR();
      if (m_tblSource != null)
         deleteObject( m_tblSource );
      if (m_tblTarget != null)
         deleteObject( m_tblTarget );
      m_tblSource = null;
      m_tblTarget = null;
   }
   
   public void testStable()
   {
      // test
      assertEquals( "default", m_sort.isStable(), true );

      enableUndo();

      // test no changes
      m_sort.setStable( true );
      assertUnchanged();
      assertNoEvents();
      
      // change to not stable
      m_sort.setStable( false );
      assertChangedAndReset();
      assertEquals( m_sort.isStable(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );
      getUndoManager().undo();
      assertChangedAndReset();
      assertEquals( m_sort.isStable(), true );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );
      getUndoManager().redo();
      assertChanged();
      assertEquals( m_sort.isStable(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isStable(), false );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setStable( false );
      assertUnchanged();
      assertNoEvents();

      // change to stable
      m_sort.setStable( true );
      assertEquals( m_sort.isStable(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.isStable(), false );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.isStable(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.STABLE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isStable(), true );
   }
   
   public void testOverwrite()
   {
      // test
      assertEquals( "default", m_sort.isOverwrite(), false );

      enableUndo();

      // test no changes
      m_sort.setOverwrite( false );
      assertUnchanged();
      assertNoEvents();
      
      // change to not stable
      m_sort.setOverwrite( true );
      assertChangedAndReset();
      assertEquals( m_sort.isOverwrite(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );
      getUndoManager().undo();
      assertChangedAndReset();
      assertEquals( m_sort.isOverwrite(), false );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );
      getUndoManager().redo();
      assertChanged();
      assertEquals( m_sort.isOverwrite(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isOverwrite(), true );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setOverwrite( true );
      assertUnchanged();
      assertNoEvents();

      // change to stable
      m_sort.setOverwrite( false );
      assertEquals( m_sort.isOverwrite(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.isOverwrite(), true );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.isOverwrite(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.OVERWRITE_OPTIONS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isOverwrite(), false );
   }
   
   public void testSortTags()
   {
      // test
      assertEquals( "default", m_sort.isSortTags(), false );

      enableUndo();

      // test no changes
      m_sort.setSortTags( false );
      assertUnchanged();
      assertNoEvents();
      
      // change to sort tags
      m_sort.setSortTags( true );
      assertEquals( m_sort.isSortTags(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.isSortTags(), false );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.isSortTags(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isSortTags(), true );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setSortTags( true );
      assertUnchanged();
      assertNoEvents();

      // change to not sort tags
      m_sort.setSortTags( false );
      assertChangedAndReset();
      assertEquals( m_sort.isSortTags(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );
      getUndoManager().undo();
      assertChangedAndReset();
      assertEquals( m_sort.isSortTags(), true );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );
      getUndoManager().redo();
      assertChanged();
      assertEquals( m_sort.isSortTags(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.SORT_TAGS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isSortTags(), false );

   }
   
   public void testReplaceDataSet()
   {
      // test
      assertEquals( "default", m_sort.isReplaceDataSet(), false );

      enableUndo();

      // test no changes
      m_sort.setReplaceDataSet( false );
      assertUnchanged();
      assertNoEvents();
      
      // change to replace dataset
      m_sort.setReplaceDataSet( true );
      assertEquals( m_sort.isReplaceDataSet(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.isReplaceDataSet(), false );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.isReplaceDataSet(), true );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isReplaceDataSet(), true );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setReplaceDataSet( true );
      assertUnchanged();
      assertNoEvents();

      // change to not replace dataset
      m_sort.setReplaceDataSet( false );
      assertChangedAndReset();
      assertEquals( m_sort.isReplaceDataSet(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );
      getUndoManager().undo();
      assertChangedAndReset();
      assertEquals( m_sort.isReplaceDataSet(), true );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );
      getUndoManager().redo();
      assertChanged();
      assertEquals( m_sort.isReplaceDataSet(), false );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.REPLACE_DATASET_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.isReplaceDataSet(), false );
   }
   
   public void testDuplicateRecordHandling()
   {
      assertEquals( "default", m_sort.getDuplicateRecordHandling(), SortTransformModel.ALLOW_DUPLICATES );
      
      enableUndo();
      
      // test no changes
      m_sort.setDuplicateRecordHandling( SortTransformModel.ALLOW_DUPLICATES );
      assertUnchanged();
      assertNoEvents();
      
      // change to no duplicate keys
      m_sort.setDuplicateRecordHandling( SortTransformModel.NO_DUPLICATE_KEYS );
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_KEYS );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.ALLOW_DUPLICATES );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_KEYS );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_KEYS );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setDuplicateRecordHandling( SortTransformModel.NO_DUPLICATE_KEYS );
      assertUnchanged();
      assertNoEvents();
      
      // change to no duplicate records
      m_sort.setDuplicateRecordHandling( SortTransformModel.NO_DUPLICATE_RECORDS );
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_RECORDS );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_KEYS );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_RECORDS );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_RECORDS );

      getUndoManager().discardAllEdits();

      // test no changes
      m_sort.setDuplicateRecordHandling( SortTransformModel.NO_DUPLICATE_RECORDS );
      assertUnchanged();
      assertNoEvents();
      
      // change to duplicate records
      m_sort.setDuplicateRecordHandling( SortTransformModel.ALLOW_DUPLICATES );
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.ALLOW_DUPLICATES );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().undo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.NO_DUPLICATE_RECORDS );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );
      getUndoManager().redo();
      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.ALLOW_DUPLICATES );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.DUPLICATE_RECORD_HANDLING_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( m_sort.getDuplicateRecordHandling(), SortTransformModel.ALLOW_DUPLICATES );
   }

   public void testDuplicateRecordHandlingInvalid()
   {
      try
      {
         m_sort.setDuplicateRecordHandling( -1 );
         fail( "no exception thrown for invalid duplicate record handling option" );
      }
      catch (IllegalArgumentException expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
      
      try
      {
         m_sort.setDuplicateRecordHandling( 3 );
         fail( "no exception thrown for invalid duplicate record handling option" );
      }
      catch (IllegalArgumentException expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
   }
   
   public void testCollatingSequence()
   {
      // test
      assertEquals( "default", "", m_sort.getCollatingSequence() );

      enableUndo();

      // test no changes
      m_sort.setCollatingSequence( "" );
      assertUnchanged();
      assertNoEvents();
      
      // change to danish
      m_sort.setCollatingSequence( "Danish" );
      assertEquals( "Danish", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "", m_sort.getCollatingSequence() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "Danish", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "Danish", m_sort.getCollatingSequence() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setCollatingSequence( "Danish" );
      assertUnchanged();
      assertNoEvents();
      
      // change to japanese
      m_sort.setCollatingSequence( "Japanese" );
      assertEquals( "Japanese", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "Danish", m_sort.getCollatingSequence() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "Japanese", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "Japanese", m_sort.getCollatingSequence() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setCollatingSequence( "Japanese" );
      assertUnchanged();
      assertNoEvents();
      
      // change to default
      m_sort.setCollatingSequence( "" );
      assertEquals( "", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "Japanese", m_sort.getCollatingSequence() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "", m_sort.getCollatingSequence() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.COLLATING_SEQUENCE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "", m_sort.getCollatingSequence() );
   }

   public void testCollatingSequenceNull()
   {
      try
      {
         m_sort.setCollatingSequence( null );
         fail( "no exception thrown for invalid collating sequence option" );
      }
      catch (NullPointerException expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
   }
   
   public void testMemorySize()
   {
      // test
      assertEquals( "default", "", m_sort.getMemorySize() );

      enableUndo();

      // test no changes
      m_sort.setMemorySize( "" );
      assertUnchanged();
      assertNoEvents();
      
      // change to max
      m_sort.setMemorySize( "MAX" );
      assertEquals( "MAX", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "", m_sort.getMemorySize() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "MAX", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "MAX", m_sort.getMemorySize() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setMemorySize( "MAX" );
      assertUnchanged();
      assertNoEvents();
      
      // change to 128.5K
      m_sort.setMemorySize( "128.5K" );
      assertEquals( "128.5K", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "MAX", m_sort.getMemorySize() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "128.5K", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "128.5K", m_sort.getMemorySize() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setMemorySize( "128.5K" );
      assertUnchanged();
      assertNoEvents();
      
      // change to fudge
      m_sort.setMemorySize( "fudge" );
      assertEquals( "fudge", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "128.5K", m_sort.getMemorySize() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "fudge", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "fudge", m_sort.getMemorySize() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setMemorySize( "fudge" );
      assertUnchanged();
      assertNoEvents();
      
      // change to no memory size specified
      m_sort.setMemorySize( "" );
      assertEquals( "", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "fudge", m_sort.getMemorySize() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "", m_sort.getMemorySize() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.MEMORY_SIZE_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "", m_sort.getMemorySize() );
   }

   public void testMemorySizeNull()
   {
      try
      {
         m_sort.setMemorySize( null );
         fail( "no exception thrown for invalid memory size option" );
      }
      catch (NullPointerException expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
   }
   
   public void testProcSortOptions()
   {
      // test
      assertEquals( "default", "", m_sort.getProcSortOptions() );

      enableUndo();

      // test no changes
      m_sort.setProcSortOptions( "" );
      assertUnchanged();
      assertNoEvents();
      
      // change to an obscure option
      m_sort.setProcSortOptions( "some obscure sort option" );
      assertEquals( "some obscure sort option", m_sort.getProcSortOptions() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "", m_sort.getProcSortOptions() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "some obscure sort option", m_sort.getProcSortOptions() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "some obscure sort option", m_sort.getProcSortOptions() );

      getUndoManager().discardAllEdits();
      
      // test no changes
      m_sort.setProcSortOptions( "some obscure sort option" );
      assertUnchanged();
      assertNoEvents();
      
      // change to no options
      m_sort.setProcSortOptions( "" );
      assertEquals( "", m_sort.getProcSortOptions() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );
      getUndoManager().undo();
      assertEquals( "some obscure sort option", m_sort.getProcSortOptions() );
      assertFalse( getUndoManager().canUndo() );
      assertTrue(  getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );
      getUndoManager().redo();
      assertEquals( "", m_sort.getProcSortOptions() );
      assertTrue(  getUndoManager().canUndo() );
      assertFalse( getUndoManager().canRedo() );
      assertEvent( m_sort, SortTransformModel.PROC_SORT_OPTIONS_CHANGED, null );

      // persist
      saveTestObject();
      loadNewTestObjectInstance();

      assertEquals( "", m_sort.getProcSortOptions() );
   }

   public void testProcSortOptionsNull()
   {
      try
      {
         m_sort.setProcSortOptions( null );
         fail( "no exception thrown for invalid proc sort option" );
      }
      catch (NullPointerException expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
   }
   
   public void testDefaultWorkTableIsNotAView()
   {
      IWorkTable tbl = m_sort.addNewWorkTable();
      assertFalse( tbl.isView() );
   }
   
   private ITable createTable( String sName, String sColumnNamePreFix )
   {
      ITable tbl = getModel().getObjectFactory().createNewPhysicalTable( getFullRepositoryID() );
      tbl.setName( sName );
      tbl.setDescription( "generated by " + getClass() );
      
      IColumn col1 = getModel().getObjectFactory().createNewColumn( tbl.getID() );
      IColumn col2 = getModel().getObjectFactory().createNewColumn( tbl.getID() );
      IColumn col3 = getModel().getObjectFactory().createNewColumn( tbl.getID() );
      IColumn col4 = getModel().getObjectFactory().createNewColumn( tbl.getID() );
      IColumn col5 = getModel().getObjectFactory().createNewColumn( tbl.getID() );
      col1.setName( sColumnNamePreFix + "one"   );
      col2.setName( sColumnNamePreFix + "two"   );
      col3.setName( sColumnNamePreFix + "three" );
      col4.setName( sColumnNamePreFix + "four"  );
      col5.setName( sColumnNamePreFix + "five"  );
      col1.setDescription( "generated by " + getClass() );
      col2.setDescription( "generated by " + getClass() );
      col3.setDescription( "generated by " + getClass() );
      col4.setDescription( "generated by " + getClass() );
      col5.setDescription( "generated by " + getClass() );
      tbl.addColumn( col1 );
      tbl.addColumn( col2 );
      tbl.addColumn( col3 );
      tbl.addColumn( col4 );
      tbl.addColumn( col5 );
      
      saveObject( tbl );
      clearEvents();
      return tbl;
   }

   private class SortTransformEventsFilter implements IEventFilter
   {
      public boolean pass( ModelEvent ev )
      {
         return ev.getModelObject() == m_sort;
      }
   }
   
   public void testAddSortColumn()
   {
      ITable  tblTarget = getTestTargetTable();
      IColumn colTarget = tblTarget.getColumns()[0];

      m_sort.addDataTarget( tblTarget );
      clearEvents();
      enableUndo();
      
      ISortColumn sortColumn = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), colTarget );
      ISorting sort = m_sort.getSortOrder();
      
      sort.addSortColumn( sortColumn );
      assertChanged();
      assertEvent( sort, ISorting.SORTING_COLUMN_ADDED, sortColumn );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertEquals( sortColumn, sort.getSortColumns()[0] );
      assertEquals( ISorting.ASCENDING, sortColumn.getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      getUndoManager().undo();
      assertEvent( sort, ISorting.SORTING_COLUMN_REMOVED, sortColumn );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertEquals( 0, sort.size() );
      assertTrue(   getUndoManager().canRedo() );
      assertFalse(  getUndoManager().canUndo() );

      getUndoManager().redo();
      assertEvent( sort, ISorting.SORTING_COLUMN_ADDED, sortColumn );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertEquals( sortColumn, sort.getSortColumns()[0] );
      assertEquals( ISorting.ASCENDING, sortColumn.getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      sort = m_sort.getSortOrder();
      
      assertEquals( 1, sort.size() );
      assertEquals( colTarget.getID(), sort.getSortColumns()[0].getColumn().getID() );
      assertEquals( ISorting.ASCENDING, sort.getSortColumns()[0].getDirection() );
   }
   
   public void testAddSortColumn4Times()
   {
      ITable  tblTarget = getTestTargetTable();
      IColumn[] aColumns = tblTarget.getColumns();

      m_sort.addDataTarget( tblTarget );
      ISorting sort = m_sort.getSortOrder();
      
      ISortColumn[] aSortColumns = new ISortColumn[4];
      aSortColumns[0] = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[0] );
      aSortColumns[1] = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[1] );
      aSortColumns[1].setDirection( ISorting.DESCENDING );
      aSortColumns[2] = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[2] );
      aSortColumns[2].setDirection( ISorting.DESCENDING );
      aSortColumns[3] = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[3] );
      
      sort.addSortColumn( aSortColumns[0] );
      sort.addSortColumn( aSortColumns[1] );
      sort.addSortColumn( aSortColumns[2] );
      sort.addSortColumn( aSortColumns[3] );
      
      ISortColumn[] aSortCols = sort.getSortColumns();
      assertEquals( 4, sort.size() );
      assertSame(   aSortColumns[0], aSortCols[0] );  
      assertSame(   aSortColumns[1], aSortCols[1] );  
      assertSame(   aSortColumns[2], aSortCols[2] );  
      assertSame(   aSortColumns[3], aSortCols[3] );
      assertEquals( ISorting.ASCENDING,  aSortCols[0].getDirection() );  
      assertEquals( ISorting.DESCENDING, aSortCols[1].getDirection() );  
      assertEquals( ISorting.DESCENDING, aSortCols[2].getDirection() );  
      assertEquals( ISorting.ASCENDING,  aSortCols[3].getDirection() );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      sort = m_sort.getSortOrder();
      
      aSortCols = sort.getSortColumns();
      
      assertEquals( 4, sort.size() );
      assertEquals( aColumns[0].getID(), aSortCols[0].getColumn().getID() );  
      assertEquals( aColumns[1].getID(), aSortCols[1].getColumn().getID() );  
      assertEquals( aColumns[2].getID(), aSortCols[2].getColumn().getID() );  
      assertEquals( aColumns[3].getID(), aSortCols[3].getColumn().getID() );
      assertEquals( ISorting.ASCENDING,  aSortCols[0].getDirection() );  
      assertEquals( ISorting.DESCENDING, aSortCols[1].getDirection() );  
      assertEquals( ISorting.DESCENDING, aSortCols[2].getDirection() );  
      assertEquals( ISorting.ASCENDING,  aSortCols[3].getDirection() );
   }

   private void addFourSortColumns()
   {
      ITable  tblTarget = getTestTargetTable();
      IColumn[] aColumns = tblTarget.getColumns();

      m_sort.addDataTarget( tblTarget );
      ISorting sort = m_sort.getSortOrder();
      ISortColumn col1 = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[0] );
      ISortColumn col2 = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[1] );
      col2.setDirection( ISorting.DESCENDING );
      ISortColumn col3 = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[2] );
      col3.setDirection( ISorting.DESCENDING );
      ISortColumn col4 = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[3] );

      sort.addSortColumn( col1 );
      sort.addSortColumn( col2 );
      sort.addSortColumn( col3 );
      sort.addSortColumn( col4 );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      clearEvents();
   }
   
   public void testAddSortColumnAt2()
   {
      addFourSortColumns();
      
      
      ISorting sort = m_sort.getSortOrder();

      ITable   tblTarget = (ITable) m_sort.getDataTargets()[0];
      IColumn  colTarget = tblTarget.getColumns()[4];
      IColumn[] aCols = tblTarget.getColumns();
      ISortColumn[] aOldColumns = sort.getSortColumns();
      ISortColumn[] aNewColumns = new ISortColumn[5];
      aNewColumns[0] = aOldColumns[0];
      aNewColumns[1] = aOldColumns[1];
      aNewColumns[2] = m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), colTarget );
      aNewColumns[2].setDirection( ISorting.DESCENDING );
      aNewColumns[3] = aOldColumns[2];
      aNewColumns[4] = aOldColumns[3];
      
      clearEvents();
      enableUndo();
      
      sort.addSortColumn( 2, aNewColumns[2] );
      assertChanged();
      assertEvent( sort, ISorting.SORTING_COLUMN_ADDED, aNewColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aNewColumns, sort.getSortColumns() ) );
      assertEquals( ISorting.DESCENDING, aNewColumns[2].getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );      
      
      getUndoManager().undo();
      assertEvent( sort, ISorting.SORTING_COLUMN_REMOVED, aNewColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aOldColumns, sort.getSortColumns() ) );
      assertTrue(   getUndoManager().canRedo() );
      assertFalse(  getUndoManager().canUndo() );

      getUndoManager().redo();
      assertEvent( sort, ISorting.SORTING_COLUMN_ADDED, aNewColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aNewColumns, sort.getSortColumns() ) );
      assertEquals( ISorting.DESCENDING, aNewColumns[2].getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      sort = m_sort.getSortOrder();
      assertEquals( 5, sort.size() );
      IColumn[] aSrtCols = sort.getColumnObjects();
      assertEquals( aCols[0].getID(), aSrtCols[0].getID() );
      assertEquals( aCols[1].getID(), aSrtCols[1].getID() );
      assertEquals( aCols[4].getID(), aSrtCols[2].getID() );
      assertEquals( aCols[2].getID(), aSrtCols[3].getID() );
      assertEquals( aCols[3].getID(), aSrtCols[4].getID() );
      assertEquals( ISorting.DESCENDING, sort.getSortColumns()[2].getDirection() );
   }
   
   public void testRemoveSortColumn()
   {
      addFourSortColumns();
      enableUndo();

      ITable   tblTarget = (ITable) m_sort.getDataTargets()[0];
      IColumn[] aCols = tblTarget.getColumns();
      
      ISorting sort = m_sort.getSortOrder();
      
      ISortColumn[] aOldColumns = sort.getSortColumns();
      ISortColumn[] aNewColumns = new ISortColumn[3];
      aNewColumns[0] = aOldColumns[0];
      aNewColumns[1] = aOldColumns[1];
      aNewColumns[2] = aOldColumns[3];
      
      sort.removeSortColumn( aOldColumns[2] );
      assertChanged();
      assertEvent( sort, ISorting.SORTING_COLUMN_REMOVED, aOldColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aNewColumns, sort.getSortColumns() ) );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      getUndoManager().undo();
      assertEvent( sort, ISorting.SORTING_COLUMN_ADDED, aOldColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aOldColumns, sort.getSortColumns() ) );
      assertTrue(   getUndoManager().canRedo() );
      assertFalse(  getUndoManager().canUndo() );

      getUndoManager().redo();
      assertEvent( sort, ISorting.SORTING_COLUMN_REMOVED, aOldColumns[2] );
      assertEvent( m_sort, SortTransformModel.SORT_COLUMNS_CHANGED, null );
      assertTrue( Arrays.equals( aNewColumns, sort.getSortColumns() ) );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      sort = m_sort.getSortOrder();
      assertEquals( 3, sort.size() );
      assertEquals( aCols[0].getID(), sort.getSortColumns()[0].getColumn().getID() );
      assertEquals( aCols[1].getID(), sort.getSortColumns()[1].getColumn().getID() );
      assertEquals( aCols[3].getID(), sort.getSortColumns()[2].getColumn().getID() );
   }
   
   public void testSetSortDirection()
   {
      addFourSortColumns();
      enableUndo();
      
      ISorting sort = m_sort.getSortOrder();
      ISortColumn col = sort.getSortColumns()[2];
      
      // test no change
      col.setDirection( ISorting.DESCENDING );
      assertUnchanged();
      assertNoEvents();
      
      // test changing direction
      col.setDirection( ISorting.ASCENDING );
      assertChanged();  // reset does not work
      assertEvent( col, ISortColumn.SORTING_DIRECTION_CHANGED, null );
      assertEquals( ISorting.ASCENDING, col.getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      getUndoManager().undo();
      assertChanged();  // reset does not work
      assertEvent( col, ISortColumn.SORTING_DIRECTION_CHANGED, null );
      assertEquals( ISorting.DESCENDING, col.getDirection() );
      assertTrue(   getUndoManager().canRedo() );
      assertFalse(  getUndoManager().canUndo() );

      getUndoManager().redo();
      assertChanged();  // reset does not work
      assertEvent( col, ISortColumn.SORTING_DIRECTION_CHANGED, null );
      assertEquals( ISorting.ASCENDING, col.getDirection() );
      assertTrue(   getUndoManager().canUndo() );
      assertFalse(  getUndoManager().canRedo() );
      
      saveTestObject();
      loadNewTestObjectInstance();
      
      sort = m_sort.getSortOrder();
      col = sort.getSortColumns()[2];
      assertEquals( ISorting.ASCENDING, col.getDirection() );
   }
   
   public void testSetSortDirectionInvalid()
   {
      addFourSortColumns();
      ITable    tblTarget = (ITable) m_sort.getDataTargets()[0];
      IColumn[] aColumns  = tblTarget.getColumns();
      ISortColumn col = m_sort.getSortOrder().getSortColumns()[2];

      try
      {
         col.setDirection( "Asc" );
         fail( "no exception setting sort direction on a column that is not a sort column" );
      }
      catch (Exception expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
      

      
      try
      {
         col.setDirection( null );
         fail( "no exception setting sort direction with a null direction" );
      }
      catch (Exception expected)
      {
      }
      assertNoEvents();
      assertUnchanged();      
   }
   
   public void testGetSortDirectionInvalid()
   {
      addFourSortColumns();
      ITable    tblTarget = (ITable) m_sort.getDataTargets()[0];
      IColumn[] aColumns  = tblTarget.getColumns();
           
      ISortColumn sortColumn =  m_sort.getModel().getObjectFactory().createNewSortColumn( m_sort.getID(), aColumns[0] );
      
      try
      {
         sortColumn.setDirection( IColumn.SORT_NONE );
         fail( "no exception getting sort direction on a column that is not a sort column" );
      }
      catch (Exception expected)
      {
      }
      assertNoEvents();
      assertUnchanged();
   }
   
   
//   public void testOldOptions()
//   {
//      // change the test object and load it
//      SortTransformModel old = m_sort;
//      try
//      {
//      
//         setTestObject( getModel().getObjectFactory().createTransform( ObjectFactory.SORT_TRANSFORM, "A5CZRN0J.AL000813" ) );
//         loadNewTestObjectInstance();
//         
//         assertTrue(   "stable",          m_sort.isStable()         );
//         assertFalse(  "replace dataset", m_sort.isReplaceDataSet() );
//         assertFalse(  "sort tags",       m_sort.isSortTags()       );
//         
//         assertEquals( "duplicates",      SortTransformModel.ALLOW_DUPLICATES, m_sort.getDuplicateRecordHandling() );
//
//         assertEquals( "sequence",           "", m_sort.getCollatingSequence() );
//         assertEquals( "size",               "", m_sort.getMemorySize() );
//         assertEquals( "proc sort options",  "", m_sort.getProcSortOptions() );
//   
//         // change the test object and load it
//         setTestObject( getModel().getObjectFactory().createTransform( ObjectFactory.SORT_TRANSFORM, "A5CZRN0J.AL000812" ) );
//         loadNewTestObjectInstance();
//         
//         assertTrue(  "stable",          m_sort.isStable()         );
//         assertFalse( "replace dataset", m_sort.isReplaceDataSet() );
//         assertFalse( "sort tags",       m_sort.isSortTags()       );
//   
//         assertEquals( "duplicates",      SortTransformModel.ALLOW_DUPLICATES, m_sort.getDuplicateRecordHandling() );
//
//         assertEquals( "sequence",           "", m_sort.getCollatingSequence() );
//         assertEquals( "size",               "", m_sort.getMemorySize() );
//         assertEquals( "proc sort options",  "", m_sort.getProcSortOptions() );
//
//         // change the test object and load it
//         setTestObject( getModel().getObjectFactory().createTransform( ObjectFactory.SORT_TRANSFORM, "A5CZRN0J.AL0006JF" ) );
//         loadNewTestObjectInstance();
//         
//         assertFalse( "stable",          m_sort.isStable()         );
//         assertTrue(  "replace dataset", m_sort.isReplaceDataSet() );
//         assertTrue(  "sort tags",       m_sort.isSortTags()       );
//         
//         assertEquals( "duplicates",      SortTransformModel.NO_DUPLICATE_KEYS, m_sort.getDuplicateRecordHandling() );
//
//         assertEquals( "sequence",           "fudge", m_sort.getCollatingSequence() );
//         assertEquals( "size",               "128M",  m_sort.getMemorySize() );
//         assertEquals( "proc sort options",  "abc",   m_sort.getProcSortOptions() );
//      }
//      finally
//      {
//         setTestObject( old );
//      }
//   }
}

/*
 * $CPS$ This comment was generated by CodePro. Do not edit it. patternId =
 * com.instantiations.assist.eclipse.pattern.testCasePattern strategyId =
 * com.instantiations.assist.eclipse.pattern.testCasePattern.junitTestCase
 * additionalTestNames = assertTrue = false callTestMethod = true createMain =
 * false createSetUp = false createTearDown = false createTestFixture = false
 * createTestStubs = false methods = package = com.sas.metadata.models.job.test
 * package.sourceFolder = JobModelTest superclassType =
 * com.sas.metadata.models.job.test.AbstractTransformTest testCase =
 * AbstractDataTransformTest testClassType =
 * com.sas.metadata.models.job.AbstractDataTransform
 */