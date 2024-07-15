/* $Id$ */
/**
 * Title:       INotesContainer.java
 * Description: A container of notes.
 * Copyright:   Copyright (c) 2007
 * Company:     SAS Institute
 * Author:      Russ Robison
 * Support:     Russ Robison
 */
package com.sas.etl.models;

import java.util.List;

import com.sas.etl.models.other.INote;

/**
 * INotesContainer is a container of public notes. It is also a container of a 
 * private note. Unlike public notes, the container owns its private note and 
 * is responsible for managing its deletion.
 */
public interface INotesContainer extends IObject
{
   /** text role for notes */
   static final String NOTE_ROLE = "Note";
   /** name for document containing public notes */
   static final String NOTES_DOCUMENT_NAME = "PublicNotes";
   /** name for Private Note textstore */
   static final String PRIVATE_NOTE_NAME = "PrivateNote";
   
   // event types
   /** event type for a note was added */
   static final String NOTE_ADDED   = "Object:NoteAdded";
   /** event type for a note was removed */
   static final String NOTE_REMOVED = "Object:NoteRemoved";
   /** event type for private note changed */
   static final String PRIVATE_NOTE_CHANGED = "NotesContainer:PrivateNoteChanged";
   
   /**
    * Gets the notes in the notes container.
    * 
    * @return the notes
    */
   INote[] getNotes();
   
   /**
    * Gets the list of notes in the notes container.  The list may be directly
    * modified to modify the notes in the container.  The methods in the list
    * that are not implemented will throw an exception.
    * 
    * @return the list of notes
    */
   List getNotesList();
   
   /**
    * Gets the private note in the notes container. The container can only have 
    * one private note.
    * 
    * @return The private note for this container
    */
   String getPrivateNote();
   
   /**
    * Sets the private note in the notes container. The container can only have 
    * one private note.
    * 
    * @param sPrivateNote the private note
    */
   void setPrivateNote( String sPrivateNote );
}

