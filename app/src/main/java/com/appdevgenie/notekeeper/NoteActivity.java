package com.appdevgenie.notekeeper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_COURSE_ID;
import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TEXT;
import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TITLE;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_COURSE_ID;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_NOTE_TEXT;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_NOTE_TITLE;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.NOTE_TABLE_NAME;

public class NoteActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    public static final String NOTE_ID = "com.appdevgenie.notekeeper.NOTE_POSITION";
    public static final int ID_NOT_SET = -1;

    private NoteInfo mNote = new NoteInfo(DataManager.getInstance().getCourses().get(0), "", "");
    private boolean mIsNewNote;
    private Spinner spinnerCourses;
    private EditText textNoteTitle;
    private EditText textNoteText;
    private int noteId;
    private boolean isCancelling;
    private NoteActivityViewModel noteActivityViewModel;
    private NoteKeeperOpenHelper dbOpenHelper;
    private Cursor cursor;
    private int courseIdPos;
    private int noteTitlePos;
    private int noteTextPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbOpenHelper = new NoteKeeperOpenHelper(this);

        ViewModelProvider viewModelProvider = new ViewModelProvider(getViewModelStore(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()));
        noteActivityViewModel = viewModelProvider.get(NoteActivityViewModel.class);

        if (noteActivityViewModel.isNewlyCreated && savedInstanceState != null) {
            noteActivityViewModel.restoreState(savedInstanceState);
        }

        noteActivityViewModel.isNewlyCreated = false;

        spinnerCourses = findViewById(R.id.spinner_courses);

        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        ArrayAdapter<CourseInfo> adapterCourses = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapterCourses);

        readDisplayStateValues();
        saveOriginalNoteValues();
//        if(savedInstanceState == null) {
//            saveOriginalNoteValues();
//        } else {
//            restoreOriginalNoteValues(savedInstanceState);
//        }

        textNoteTitle = findViewById(R.id.text_note_title);
        textNoteText = findViewById(R.id.text_note_text);
        if (!mIsNewNote) {
            loadNoteData();
        }

        Log.d(TAG, "onCreate");

    }

    private void loadNoteData() {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        String courseId = "android_intents";
        String titleStart = "dynamic";

        String selection = NoteKeeperDatabaseContract.NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(noteId)};

        String[] noteColumns = {COLUMN_COURSE_ID, COLUMN_NOTE_TITLE, COLUMN_NOTE_TEXT};

        cursor = db.query(NOTE_TABLE_NAME, noteColumns, selection, selectionArgs, null, null, null);
        courseIdPos = cursor.getColumnIndex(COLUMN_COURSE_ID);
        noteTitlePos = cursor.getColumnIndex(COLUMN_NOTE_TITLE);
        noteTextPos = cursor.getColumnIndex(COLUMN_NOTE_TEXT);

        cursor.moveToNext();
        displayNote();
    }

    @Override
    protected void onDestroy() {
        dbOpenHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        noteActivityViewModel.saveState(outState);
    }

    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        noteActivityViewModel.originalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        noteActivityViewModel.originalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        noteActivityViewModel.originalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalNoteValues() {
        if (mIsNewNote)
            return;

        noteActivityViewModel.originalNoteCourseId = mNote.getCourse().getCourseId();
        noteActivityViewModel.originalNoteTitle = mNote.getTitle();
        noteActivityViewModel.originalNoteText = mNote.getText();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Cancelling note at position: " + noteId);
        if (isCancelling) {
            if (mIsNewNote) {
                DataManager.getInstance().removeNote(noteId);
            } else {
                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    private void storePreviousNoteValues() {
        CourseInfo courseInfo = DataManager.getInstance().getCourse(noteActivityViewModel.originalNoteCourseId);
        mNote.setCourse(courseInfo);
        mNote.setTitle(noteActivityViewModel.originalNoteTitle);
        mNote.setText(noteActivityViewModel.originalNoteText);
    }

    private void saveNote() {
        mNote.setCourse((CourseInfo) spinnerCourses.getSelectedItem());
        mNote.setTitle(textNoteTitle.getText().toString());
        mNote.setText(textNoteText.getText().toString());
    }

    private void displayNote() {
        String courseId = cursor.getString(courseIdPos);
        String noteTitle = cursor.getString(noteTitlePos);
        String noteText = cursor.getString(noteTextPos);
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        CourseInfo course = DataManager.getInstance().getCourse(courseId);
        int courseIndex = courses.indexOf(course);
        spinnerCourses.setSelection(courseIndex);
        textNoteTitle.setText(noteTitle);
        textNoteText.setText(noteText);
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        noteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = noteId == ID_NOT_SET;
        if (mIsNewNote) {
            createNewNote();
        }
        Log.i(TAG, "mNotePosition: " + noteId);
        //mNote = DataManager.getInstance().getNotes().get(noteId);

    }

    private void createNewNote() {
        DataManager dataManager = DataManager.getInstance();
        noteId = dataManager.createNewNote();
       //mNote = dataManager.getNotes().get(notePosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {
            isCancelling = true;
            finish();
        } else if(id == R.id.action_next){
            moveNext();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(noteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        //saveNote();

        ++noteId;
        mNote = DataManager.getInstance().getNotes().get(noteId);

        saveOriginalNoteValues();
        displayNote();

        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) spinnerCourses.getSelectedItem();
        String subject = textNoteTitle.getText().toString();
        String text = "Check what I learned in the Pluralsight course \"" +
                course.getTitle() + "\"\n" + textNoteText.getText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }
}
