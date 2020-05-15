package com.appdevgenie.notekeeper;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class NoteListActivity extends AppCompatActivity {
    private NoteRecyclerAdapter noteRecyclerAdapter;

    //private ArrayAdapter<NoteInfo> adapterNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NoteListActivity.this, NoteActivity.class));
            }
        });

        initialiseDisplayContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //adapterNotes.notifyDataSetChanged();
        noteRecyclerAdapter.notifyDataSetChanged();
    }

    private void initialiseDisplayContent() {
        /*final ListView listNotes = findViewById(R.id.list_notes);
        List<NoteInfo> notes = DataManager.getInstance().getNotes();
        adapterNotes = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notes);
        listNotes.setAdapter(adapterNotes);

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(NoteListActivity.this, NoteActivity.class);
//                NoteInfo note = (NoteInfo) listNotes.getItemAtPosition(position);
                intent.putExtra(NoteActivity.NOTE_POSITION, position);
                startActivity(intent);
            }
        });*/

        RecyclerView recyclerView = findViewById(R.id.list_notes);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        List<NoteInfo> notes = DataManager.getInstance().getNotes();
        noteRecyclerAdapter = new NoteRecyclerAdapter(this, notes);
        recyclerView.setAdapter(noteRecyclerAdapter);
    }

}
