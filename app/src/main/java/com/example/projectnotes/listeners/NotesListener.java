package com.example.projectnotes.listeners;

import com.example.projectnotes.entities.Note;

public interface NotesListener {

    void onNoteClicked(Note note, int position);
}
