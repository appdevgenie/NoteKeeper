package com.appdevgenie.notekeeper;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class CourseRecyclerAdapter extends RecyclerView.Adapter<CourseRecyclerAdapter.ViewHolder> {

    private Context context;
    private final LayoutInflater layoutInflater;
    private final List<CourseInfo> courses;

    public CourseRecyclerAdapter(Context context, List<CourseInfo> courses) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.courses = courses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.item_course_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseInfo note = courses.get(position);
        holder.textCourse.setText(note.getTitle());
        holder.currentPosition = position;
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        public final TextView textCourse;
        public int currentPosition;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCourse = itemView.findViewById(R.id.text_course);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, courses.get(currentPosition).getTitle(), Snackbar.LENGTH_LONG).show();
                   /* Intent intent = new Intent(context, NoteActivity.class);
                    intent.putExtra(NoteActivity.NOTE_POSITION, currentPosition);
                    context.startActivity(intent);*/
                }
            });
        }
    }
}
