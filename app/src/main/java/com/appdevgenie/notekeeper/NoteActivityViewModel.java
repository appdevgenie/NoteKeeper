package com.appdevgenie.notekeeper;

import android.os.Bundle;

import androidx.lifecycle.ViewModel;

public class NoteActivityViewModel extends ViewModel {
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_TEXT";

    public String originalNoteCourseId;
    public String originalNoteTitle;
    public String originalNoteText;
    public boolean isNewlyCreated = true;

    public void saveState(Bundle outState) {
        outState.putString(ORIGINAL_NOTE_COURSE_ID, originalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, originalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, originalNoteText);
    }

    public void restoreState(Bundle inState){
        originalNoteCourseId = inState.getString(ORIGINAL_NOTE_COURSE_ID);
        originalNoteTitle = inState.getString(ORIGINAL_NOTE_TITLE);
        originalNoteText = inState.getString(ORIGINAL_NOTE_TEXT);
    }
}
