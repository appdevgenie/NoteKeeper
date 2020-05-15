package com.appdevgenie.notekeeper;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteRecyclerAdapter extends RecyclerView.Adapter<NoteRecyclerAdapter.ViewHolder> {

    private Context context;
    private final LayoutInflater layoutInflater;
    private final List<NoteInfo> noteInfoList;

    public NoteRecyclerAdapter(Context context, List<NoteInfo> noteInfoList) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.noteInfoList = noteInfoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.item_note_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteInfo note = noteInfoList.get(position);
        holder.textCourse.setText(note.getCourse().getTitle());
        holder.textTitle.setText(note.getTitle());
        holder.currentPosition = position;
    }

    @Override
    public int getItemCount() {
        return noteInfoList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        public final TextView textCourse;
        public final TextView textTitle;
        public int currentPosition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textCourse = itemView.findViewById(R.id.text_course);
            textTitle = itemView.findViewById(R.id.text_title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, NoteActivity.class);
                    intent.putExtra(NoteActivity.NOTE_POSITION, currentPosition);
                    context.startActivity(intent);
                }
            });
        }
    }
}
