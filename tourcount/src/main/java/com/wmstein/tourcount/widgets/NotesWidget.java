package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;

import java.util.Objects;

/**********************************
 * NotesWidget used by CountingActivity
 * shows scalable notes line
 * Created by milo on 26/05/2014.
 * Adopted for TourCount by wmstein on 2016-02-18,
 * last edited on 2022-04-25
 */
public class NotesWidget extends LinearLayout
{
    private static final String TAG = "TourCountNotesWidget";
    private final TextView textView;

    public NotesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_notes, this, true);
        textView = findViewById(R.id.notes_text);
    }

    public void setNotes(String notes)
    {
        textView.setText(notes);
    }

    public void setFont(Boolean large)
    {
        if (large)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Setzt große Schrift.");
            textView.setTextSize(16);
        }
        else
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Setzt kleine Schrift.");
            textView.setTextSize(14);
        }
    }

}

