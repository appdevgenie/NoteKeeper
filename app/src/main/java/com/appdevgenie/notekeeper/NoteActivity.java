package com.appdevgenie.notekeeper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.google.android.material.snackbar.Snackbar;

import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_COURSE_ID;
import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TEXT;
import static com.appdevgenie.notekeeper.NoteActivityViewModel.ORIGINAL_NOTE_TITLE;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry.COURSE_TABLE_NAME;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_COURSE_ID;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_NOTE_TEXT;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.COLUMN_NOTE_TITLE;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.NOTE_TABLE_NAME;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Courses;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Notes;

public class NoteActivity extends AppCompatActivity implements
        android.app.LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
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
    private Cursor noteCursor;
    private int courseIdPos;
    private int noteTitlePos;
    private int noteTextPos;
    private SimpleCursorAdapter adapterCourses;
    private boolean coursesQueryFinished;
    private boolean notesQueryFinished;
    private Uri noteUri;

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

        //List<CourseInfo> courses = DataManager.getInstance().getCourses();
        adapterCourses = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_spinner_item,
                null,
                new String[]{CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[]{android.R.id.text1},
                0);
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapterCourses);

        getLoaderManager().initLoader(LOADER_COURSES, null, this);

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
            getLoaderManager().initLoader(LOADER_NOTES, null, this);
        }

        //Log.d(TAG, "onCreate");

    }

    private void loadCourseData() {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };
        Cursor cursor = db.query(COURSE_TABLE_NAME, courseColumns, null, null, null, null,
                CourseInfoEntry.COLUMN_COURSE_TITLE);
        adapterCourses.changeCursor(cursor);
    }

    private void loadNoteData() {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

//        String courseId = "android_intents";
//        String titleStart = "dynamic";

        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(noteId)};

        String[] noteColumns = {COLUMN_COURSE_ID, COLUMN_NOTE_TITLE, COLUMN_NOTE_TEXT};

        noteCursor = db.query(NOTE_TABLE_NAME, noteColumns, selection, selectionArgs, null, null, null);
        courseIdPos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        noteTitlePos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        noteTextPos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);

        noteCursor.moveToNext();
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
                deleteNoteFromDatabase();
            } else {
                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    private void deleteNoteFromDatabase() {
        final String selection = NoteInfoEntry._ID + " = ?";
        final String[] selectionArgs = {Integer.toString(noteId)};

        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
                db.delete(NOTE_TABLE_NAME, selection, selectionArgs);
                return null;
            }
        };
        task.execute();
    }

    private void storePreviousNoteValues() {
        CourseInfo courseInfo = DataManager.getInstance().getCourse(noteActivityViewModel.originalNoteCourseId);
        mNote.setCourse(courseInfo);
        mNote.setTitle(noteActivityViewModel.originalNoteTitle);
        mNote.setText(noteActivityViewModel.originalNoteText);
    }

    private void saveNote() {
        String courseId = selectCourseId();
        String noteTitle = textNoteTitle.getText().toString();
        String noteText = textNoteText.getText().toString();

        saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String selectCourseId() {
        int selectedPosition = spinnerCourses.getSelectedItemPosition();
        Cursor cursor = adapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);

        return cursor.getString(courseIdPos);
    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {(Integer.toString(noteId))};

        ContentValues values = new ContentValues();
        values.put(COLUMN_COURSE_ID, courseId);
        values.put(COLUMN_NOTE_TITLE, noteTitle);
        values.put(COLUMN_NOTE_TEXT, noteText);

        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        db.update(NOTE_TABLE_NAME, values, selection, selectionArgs);
    }

    private void displayNote() {
        String courseId = noteCursor.getString(courseIdPos);
        String noteTitle = noteCursor.getString(noteTitlePos);
        String noteText = noteCursor.getString(noteTextPos);

        int courseIndex = getIndexOfCourseId(courseId);
        spinnerCourses.setSelection(courseIndex);

        textNoteTitle.setText(noteTitle);
        textNoteText.setText(noteText);

        CourseEventBroadcastHelper.sendEventBroadcast(this, courseId, "Editing note");
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = adapterCourses.getCursor();
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;

        boolean more = cursor.moveToFirst();
        while (more) {
            String cursorCourseId = cursor.getString(courseIdPos);
            if (courseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            more = cursor.moveToNext();
        }

        return courseRowIndex;
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
        AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
            private ProgressBar progressBar;

            @Override
            protected void onPreExecute() {
                progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(1);
            }

            @Override
            protected Uri doInBackground(ContentValues... contentValues) {
                ContentValues insertValues = contentValues[0];
                Uri rowUri = getContentResolver().insert(Notes.CONTENT_URI, insertValues);

                simulateLongRunningWork(); // simulate slow database work
                publishProgress(2);

                simulateLongRunningWork(); // simulate slow work with data
                publishProgress(3);
                return rowUri;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int progressValue = values[0];
                progressBar.setProgress(progressValue);
            }

            @Override
            protected void onPostExecute(Uri uri) {
                Log.d(TAG, "onPostExecute: thread: " + Thread.currentThread().getId());
                noteUri = uri;
                displaySnackbar(noteUri.toString());
                progressBar.setVisibility(View.GONE);
            }
        };

        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        Log.d(TAG, "createNewNote: thread: " + Thread.currentThread().getId());
        task.execute(values);

    }

    private void displaySnackbar(String message) {
        View view = findViewById(R.id.spinner_courses);
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    private void simulateLongRunningWork() {
        try {
            Thread.sleep(2000);
        } catch(Exception ex) {}
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
        } else if (id == R.id.action_next) {
            moveNext();
        } else if (id == R.id.action_set_reminder) {
            showReminderNotification();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showReminderNotification() {
        NoteReminderNotification.notify(this,
                textNoteTitle.getText().toString(),
                textNoteText.getText().toString(),
                (int) ContentUris.parseId(noteUri));
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
        String text = "Check what I learned in the PluralSight course \"" +
                course.getTitle() + "\"\n" + textNoteText.getText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = null;
        if (i == LOADER_NOTES) {
            loader = createLoaderNotes();
        } else if (i == LOADER_COURSES) {
            loader = createLoaderCourses();
        }
        return loader;
    }

    private CursorLoader createLoaderCourses() {
        coursesQueryFinished = false;

        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID};

        return new CursorLoader(this, uri, courseColumns, null, null, Courses.COLUMN_COURSE_TITLE);
    }

    private CursorLoader createLoaderNotes() {
        notesQueryFinished = false;
        String[] noteColumns = {Notes.COLUMN_COURSE_ID, Notes.COLUMN_NOTE_TITLE, Notes.COLUMN_NOTE_TEXT};

        noteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, noteId);
        return new CursorLoader(this, noteUri, noteColumns, null, null, null);
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == LOADER_NOTES) {
            loadFinishedNotes(cursor);
        } else if (loader.getId() == LOADER_COURSES) {
            adapterCourses.changeCursor(cursor);
            coursesQueryFinished = true;
            displayNoteWhenQueriesFinished();
        }
    }

    private void loadFinishedNotes(Cursor cursor) {
        noteCursor = cursor;
        courseIdPos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        noteTitlePos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        noteTextPos = noteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);

        noteCursor.moveToNext();
        notesQueryFinished = true;
        displayNoteWhenQueriesFinished();
    }

    private void displayNoteWhenQueriesFinished() {
        if (notesQueryFinished && coursesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_NOTES) {
            if (noteCursor != null)
                noteCursor.close();
        } else if (loader.getId() == LOADER_COURSES) {
            adapterCourses.changeCursor(null);
        }

    }
}
