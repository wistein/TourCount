package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;

/**
 * Created by milo on 26/05/2014.
 * Changed by wmstein on 18.02.2016
 */
public class NotesWidget extends LinearLayout
{
    private static final String TAG = "TourCount Notes Widget";
    private final TextView textView;

    public NotesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_notes, this, true);
        textView = (TextView) findViewById(R.id.notes_text);
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
            textView.setTextSize(22);
        }
        else
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Setzt kleine Schrift.");
            textView.setTextSize(14);
        }
    }

}

