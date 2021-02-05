package com.example.projectnotes.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.projectnotes.R;
import com.example.projectnotes.activities.MainActivity;
import com.example.projectnotes.entities.Note;
import com.example.projectnotes.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private NotesListener notesListener;

    private Timer timer;
    private List<Note> notesSource;

    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        notesSource = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position));

        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notesListener.onNoteClicked(notes.get(position), position);

            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView textTitle, textSubtitle, textDateTime,textDisplayURL;
        LinearLayout layoutNote;
        RoundedImageView imageNote;

        // for  holding the View to avoid multiple findViewById
        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.textDate);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNote = itemView.findViewById(R.id.imageNote);
            textDisplayURL=itemView.findViewById(R.id.textDisplayURL);

        }


        // method to set data to each row acoording to given data;
        void setNote(Note note) {
            textTitle.setText(note.getTitle());

            if (note.getSubtitle().trim().isEmpty()) {
                textSubtitle.setVisibility(View.GONE);
            } else {
                textSubtitle.setText(note.getSubtitle());
            }

            textDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));

                if (note.getColor().equals("#FDBE3B")) {
                    textTitle.setTextColor(Color.BLACK);
                    textSubtitle.setTextColor(Color.BLACK);
                    textDateTime.setTextColor(Color.BLACK);

                    textDisplayURL.setLinkTextColor(Color.BLUE);
                } // For yellow Background
                else if (note.getColor().equals("#FF4842")) {
                    textTitle.setTextColor(Color.WHITE);
                    textSubtitle.setTextColor(Color.WHITE);
                    textDateTime.setTextColor(Color.WHITE);
                    textDisplayURL.setTextColor(Color.BLACK);
                    textDisplayURL.setLinkTextColor(Color.YELLOW);

                } // For Red Background
                else if (note.getColor().equals("#000000")) {
                    textTitle.setTextColor(Color.rgb(211, 211, 211));
                    textSubtitle.setTextColor(Color.rgb(211, 211, 211));
                    textDateTime.setTextColor(Color.rgb(211, 211, 211));
                    textDisplayURL.setLinkTextColor(Color.rgb(1,255,199));
                } // For Black Background
                else {
                    textTitle.setTextColor(Color.WHITE);
                    textSubtitle.setTextColor(Color.WHITE);
                    textDateTime.setTextColor(Color.WHITE);
                    textDisplayURL.setLinkTextColor(Color.YELLOW);
                    textDisplayURL.setLinkTextColor(Color.rgb(1,255,199));
                } // for gray background

            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            // for image
            if (!note.getImagePath().equals("null")) {
                //imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                Glide.with(itemView).
                        load(note.getImagePath()).
                        into(imageNote);
                Log.d("MYLOG", "Loaded..");

                imageNote.setVisibility(View.VISIBLE);

            } else {
                imageNote.setVisibility(View.GONE);
            }

            if (note.getWebLink()!=null)
            {
                textDisplayURL.setText(note.getWebLink());
                textDisplayURL.setVisibility(View.VISIBLE);
            }
            else{
                textDisplayURL.setVisibility(View.GONE);
            }

        }

    }


    public void searchNotes(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    notes = notesSource;
                } else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : notesSource)
                    {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });

            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }


}
