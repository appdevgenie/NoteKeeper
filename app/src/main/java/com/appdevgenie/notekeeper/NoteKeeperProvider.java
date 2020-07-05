package com.appdevgenie.notekeeper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry.COURSE_TABLE_NAME;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import static com.appdevgenie.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry.NOTE_TABLE_NAME;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.AUTHORITY;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Courses;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.CoursesIdColumns;
import static com.appdevgenie.notekeeper.NoteKeeperProviderContract.Notes;

public class NoteKeeperProvider extends ContentProvider {

    private static final String MIME_VENDOR_TYPE = "vnd." + NoteKeeperProviderContract.AUTHORITY + ".";

    private NoteKeeperOpenHelper dbOpenHelper;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int COURSES = 0;
    public static final int NOTES = 1;
    public static final int NOTES_EXPANDED = 2;
    public static final int NOTES_ROW = 3;
    private static final int COURSES_ROW = 4;
    private static final int NOTES_EXPANDED_ROW = 5;

    static {
        uriMatcher.addURI(AUTHORITY, Courses.PATH, COURSES);
        uriMatcher.addURI(AUTHORITY, Notes.PATH, NOTES);
        uriMatcher.addURI(AUTHORITY, Notes.PATH_EXPANDED, NOTES_EXPANDED);
        uriMatcher.addURI(AUTHORITY, Notes.PATH + "/#", NOTES_ROW);
        uriMatcher.addURI(AUTHORITY, Notes.PATH + "/#", NOTES_ROW);
        uriMatcher.addURI(AUTHORITY, Notes.PATH_EXPANDED + "/#", NOTES_EXPANDED_ROW);
    }

    public NoteKeeperProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        long rowId = -1;
        String rowSelection = null;
        String[] rowSelectionArgs = null;
        int nRows = -1;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        int uriMatch = uriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                nRows = db.delete(COURSE_TABLE_NAME, selection, selectionArgs);
                break;
            case NOTES:
                nRows = db.delete(NOTE_TABLE_NAME, selection, selectionArgs);
                break;
            case NOTES_EXPANDED:
                // throw exception saying that this is a read-only table
            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.delete(COURSE_TABLE_NAME, rowSelection, rowSelectionArgs);
                break;
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.delete(NOTE_TABLE_NAME, rowSelection, rowSelectionArgs);
                break;
            case NOTES_EXPANDED_ROW:
                // throw exception saying that this is a read-only table
                break;
        }

        return nRows;
    }

    @Override
    public String getType(Uri uri) {
        String mimeType = null;
        int uriMatch = uriMatcher.match(uri);
        switch(uriMatch){
            case COURSES:
                // vnd.android.cursor.dir/vnd.com.appdevgenie.notekeeper.provider.courses
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
                break;
            case NOTES:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
                break;
            case NOTES_EXPANDED:
                mimeType = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
                break;
            case COURSES_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
                break;
            case NOTES_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
                break;
            case NOTES_EXPANDED_ROW:
                mimeType = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
                break;
        }
        return mimeType;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        long rowId = -1;
        Uri rowUri = null;

        int uriMatch = uriMatcher.match(uri);
        switch (uriMatch) {
            case NOTES:
                rowId = db.insert(NOTE_TABLE_NAME, null, values);
                rowUri = ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
                break;

            case COURSES:
                rowId = db.insert(COURSE_TABLE_NAME, null, values);
                rowUri = ContentUris.withAppendedId(Courses.CONTENT_URI, rowId);
                break;

            case NOTES_EXPANDED:
                //read only
                break;
        }

        return rowUri;
    }

    @Override
    public boolean onCreate() {
        dbOpenHelper = new NoteKeeperOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        int uriMatch = uriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                cursor = db.query(COURSE_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case NOTES:
                cursor = db.query(NOTE_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case NOTES_EXPANDED:
                cursor = notesExpandedQuery(db, projection, selection, selectionArgs, sortOrder);
                break;

            case NOTES_ROW:
                long rowId = ContentUris.parseId(uri);
                String rowSelection = NoteInfoEntry._ID + " = ?";
                String[] rowSelectionArgs = new String[]{Long.toString(rowId)};
                cursor = db.query(NOTE_TABLE_NAME, projection, rowSelection, rowSelectionArgs, null, null, null);
                break;
        }

        return cursor;
    }

    private Cursor notesExpandedQuery(SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String[] columns = new String[projection.length];
        for (int idx = 0; idx < projection.length; idx++) {
            columns[idx] = projection[idx].equals(BaseColumns._ID) ||
                    projection[idx].equals(CoursesIdColumns.COLUMN_COURSE_ID) ?
                    NoteInfoEntry.getQName(projection[idx]) : projection[idx];
        }

        String tablesWithJoin = NOTE_TABLE_NAME + " JOIN " +
                COURSE_TABLE_NAME + " ON " +
                NoteInfoEntry.getQName(NoteInfoEntry.COLUMN_COURSE_ID) + " = " +
                CourseInfoEntry.getQName(CourseInfoEntry.COLUMN_COURSE_ID);

        return db.query(tablesWithJoin, columns, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        long rowId = -1;
        String rowSelection = null;
        String[] rowSelectionArgs = null;
        int nRows = -1;
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();

        int uriMatch = uriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                nRows = db.update(COURSE_TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTES:
                nRows = db.update(NOTE_TABLE_NAME, values, selection, selectionArgs);
                break;
            case NOTES_EXPANDED:
                // throw exception saying that this is a read-only table
            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.update(COURSE_TABLE_NAME, values, rowSelection, rowSelectionArgs);
                break;
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                nRows = db.update(NOTE_TABLE_NAME, values, rowSelection, rowSelectionArgs);
                break;
            case NOTES_EXPANDED_ROW:
                // throw exception saying that this is a read-only table
                break;
        }

        return nRows;
    }
}
