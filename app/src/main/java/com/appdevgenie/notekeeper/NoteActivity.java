package com.appdevgenie.notekeeper;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

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
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.NOTE_TABLE_NAME;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Courses;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Notes;

public class NoteActivity extends AppCompatActivity implements
        android.app.LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    private final String TAG = getClass().getSimpleName();
    //public static final String NOTE_URI = "com.appdevgenie.notekeeper.NOTE_URI";
    public static final String NOTE_ID = "com.appdevgenie.notekeeper.NOTE_ID";
//    public static final String ORIGINAL_NOTE_COURSE_ID = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_COURSE_ID";
//    public static final String ORIGINAL_NOTE_TITLE = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_TITLE";
//    public static final String ORIGINAL_NOTE_TEXT = "com.appdevgenie.notekeeper.ORIGINAL_NOTE_TEXT";
    public static final int ID_NOT_SET = -1;

    private NoteInfo mNote = new NoteInfo(DataManager.getInstance().getCourses().get(0), "", "");

    private boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNoteId;
    private boolean mIsCancelling;
//    private String mOriginalNoteCourseId;
//    private String mOriginalNoteTitle;
//    private String mOriginalNoteText;
    private NoteKeeperOpenHelper dbOpenHelper;
    private NoteActivityViewModel noteActivityViewModel;
    private Cursor mNoteCursor;
    private int mCourseIdPos;
    private int mNoteTitlePos;
    private int mNoteTextPos;
    private SimpleCursorAdapter mAdapterCourses;
    private boolean mCoursesQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri noteUri;
    private ModuleStatusView moduleStatusView;

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
            noteUri = noteActivityViewModel.restoreNoteUri();
        }

        mSpinnerCourses = findViewById(R.id.spinner_courses);
        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                new String[] {CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[] {android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);
        getLoaderManager().initLoader(LOADER_COURSES, null, this);

        readDisplayStateValues();
        if(savedInstanceState == null) {
            saveOriginalNoteValues();
        } else {
            //restoreOriginalNoteValues(savedInstanceState);
            //String stringNoteUri = savedInstanceState.getString(NOTE_URI);
            //noteUri = Uri.parse(stringNoteUri);
        }

        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteText = findViewById(R.id.text_note_text);

        if(!mIsNewNote)
            getLoaderManager().initLoader(LOADER_NOTES, null, this);

        moduleStatusView = findViewById(R.id.module_status_view);
        loadModuleStatusValues();
    }

    private void loadModuleStatusValues() {
        int totalNumberOfModules = 11;
        int completedNumberOfModules = 7;
        boolean[] moduleStatus = new boolean[totalNumberOfModules];
        for (int moduleIndex = 0; moduleIndex < completedNumberOfModules; moduleIndex++) {
            moduleStatus[moduleIndex] = true;
        }
        moduleStatusView.setModuleStatus(moduleStatus);
    }

    @Override
    protected void onDestroy() {
        dbOpenHelper.close();
        super.onDestroy();
    }

    private void loadCourseData() {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        String[] courseColumns = {
                CourseInfoEntry.COLUMN_COURSE_TITLE,
                CourseInfoEntry.COLUMN_COURSE_ID,
                CourseInfoEntry._ID
        };
        Cursor cursor = db.query(COURSE_TABLE_NAME, courseColumns,
                null, null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);
        mAdapterCourses.changeCursor(cursor);
    }

    private void loadNoteData() {
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = {Integer.toString(mNoteId)};

        String[] noteColumns = {
                NoteInfoEntry.COLUMN_COURSE_ID,
                NoteInfoEntry.COLUMN_NOTE_TITLE,
                NoteInfoEntry.COLUMN_NOTE_TEXT
        };
        mNoteCursor = db.query(NOTE_TABLE_NAME, noteColumns,
                selection, selectionArgs, null, null, null);
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToNext();
        displayNote();
    }


    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
//        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
//        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
//        mOriginalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
        noteActivityViewModel.originalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        noteActivityViewModel.originalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        noteActivityViewModel.originalNoteText = savedInstanceState.getString(ORIGINAL_NOTE_TEXT);
    }

    private void saveOriginalNoteValues() {
        if(mIsNewNote)
            return;
//        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
//        mOriginalNoteTitle = mNote.getTitle();
//        mOriginalNoteText = mNote.getText();
        noteActivityViewModel.originalNoteCourseId = mNote.getCourse().getCourseId();
        noteActivityViewModel.originalNoteTitle = mNote.getTitle();
        noteActivityViewModel.originalNoteText = mNote.getText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mIsCancelling) {
//            Log.i(TAG, "Cancelling note at position: " + mNoteId);
            if(mIsNewNote) {
                deleteNoteFromDatabase();
            } else {
                storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "************** onPause **************");
    }

    private void deleteNoteFromDatabase() {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                getContentResolver().delete(noteUri, null, null);
                return null;
            }
        };
        task.execute();
    }

    private void storePreviousNoteValues() {
//        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
//        mNote.setCourse(course);
//        mNote.setTitle(mOriginalNoteTitle);
//        mNote.setText(mOriginalNoteText);
        CourseInfo courseInfo = DataManager.getInstance().getCourse(noteActivityViewModel.originalNoteCourseId);
        mNote.setCourse(courseInfo);
        mNote.setTitle(noteActivityViewModel.originalNoteTitle);
        mNote.setText(noteActivityViewModel.originalNoteText);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
//        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
//        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);
        noteActivityViewModel.saveState(outState);

        //outState.putString(NOTE_URI, noteUri.toString());
        noteActivityViewModel.saveNoteUri(noteUri);
    }

    private void saveNote() {
        String courseId = selectedCourseId();
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        saveNoteToDatabase(courseId, noteTitle, noteText);
    }

    private String selectedCourseId() {
        int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
        Cursor cursor = mAdapterCourses.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        String courseId = cursor.getString(courseIdPos);
        return courseId;
    }

    private void saveNoteToDatabase(String courseId, String noteTitle, String noteText) {
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, courseId);
        values.put(Notes.COLUMN_NOTE_TITLE, noteTitle);
        values.put(Notes.COLUMN_NOTE_TEXT, noteText);

        getContentResolver().update(noteUri, values, null, null);
    }

    private void displayNote() {
        String courseId = mNoteCursor.getString(mCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);

        int courseIndex = getIndexOfCourseId(courseId);

        mSpinnerCourses.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteText.setText(noteText);

        CourseEventBroadcastHelper.sendEventBroadcast(this, courseId, "Editing Note");
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mAdapterCourses.getCursor();
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;

        boolean more = cursor.moveToFirst();
        while(more) {
            String cursorCourseId = cursor.getString(courseIdPos);
            if(courseId.equals(cursorCourseId))
                break;

            courseRowIndex++;
            more = cursor.moveToNext();
        }
        return courseRowIndex;
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
        mIsNewNote = mNoteId == ID_NOT_SET;
        if(mIsNewNote) {
            createNewNote();
        }

//        Log.i(TAG, "mNoteId: " + mNoteId);
//        mNote = DataManager.getInstance().getNotes().get(mNoteId);

    }

    private void createNewNote() {
        AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
            private ProgressBar mProgressBar;

            @Override
            protected void onPreExecute() {
                mProgressBar = findViewById(R.id.progress_bar);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(1);
            }

            @Override
            protected Uri doInBackground(ContentValues... params) {
                Log.d(TAG, "doInBackground - thread: " + Thread.currentThread().getId());
                ContentValues insertValues = params[0];
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
                mProgressBar.setProgress(progressValue);
            }

            @Override
            protected void onPostExecute(Uri uri) {
                Log.d(TAG, "onPostExecute - thread: " + Thread.currentThread().getId());
                noteUri = uri;
                displaySnackbar(noteUri.toString());
                mProgressBar.setVisibility(View.GONE);
            }
        };

        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        Log.d(TAG, "Call to execute - thread: " + Thread.currentThread().getId());
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        } else if(id == R.id.action_next) {
            moveNext();
        } else if(id == R.id.action_set_reminder) {
            showReminderNotification();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showReminderNotification() {
        String noteTitle = mTextNoteTitle.getText().toString();
        String noteText = mTextNoteText.getText().toString();
        int noteId = (int)ContentUris.parseId(noteUri);

        Intent intent = new Intent(this, NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, noteText);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        long currentTimeInMilliseconds = SystemClock.elapsedRealtime();
        long ONE_HOUR = 60 * 60 * 1000;
        long TEN_SECONDS = 10 * 1000;
        long alarmTime = currentTimeInMilliseconds + TEN_SECONDS;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, alarmTime, pendingIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
        item.setEnabled(mNoteId < lastNoteIndex);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();

        ++mNoteId;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);

        saveOriginalNoteValues();
        displayNote();
        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what I learned in the Pluralsight course \"" +
                course.getTitle() +"\"\n" + mTextNoteText.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if(id == LOADER_NOTES)
            loader = createLoaderNotes();
        else if (id == LOADER_COURSES)
            loader = createLoaderCourses();
        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;
        Uri uri = Courses.CONTENT_URI;
        String[] courseColumns = {
                Courses.COLUMN_COURSE_TITLE,
                Courses.COLUMN_COURSE_ID,
                Courses._ID
        };
        return new CursorLoader(this, uri, courseColumns, null, null, Courses.COLUMN_COURSE_TITLE);
    }

    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
        String[] noteColumns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT
        };
        noteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        return new CursorLoader(this, noteUri, noteColumns, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
        else if(loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(data);
            mCoursesQueryFinished = true;
            displayNoteWhenQueriesFinished();
        }
    }

    private void loadFinishedNotes(Cursor data) {
        mNoteCursor = data;
        mCourseIdPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);

        mNoteCursor.moveToFirst();

        mNotesQueryFinished = true;
        displayNoteWhenQueriesFinished();

    }

    private void displayNoteWhenQueriesFinished() {
        if(mNotesQueryFinished && mCoursesQueryFinished)
            displayNote();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(loader.getId() == LOADER_NOTES) {
            if(mNoteCursor != null)
                mNoteCursor.close();
        } else if(loader.getId() == LOADER_COURSES) {
            mAdapterCourses.changeCursor(null);
        }
    }
}
