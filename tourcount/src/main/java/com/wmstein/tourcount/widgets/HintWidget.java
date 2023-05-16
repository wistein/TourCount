package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

import java.util.Objects;

/**********************************
 * HintWidget used by CountingActivity
 * shows single Hint line
 * Created for TourCount by wmstein on 2023-05-16,
 * last edited on 2023-05-16
 */
public class HintWidget extends LinearLayout
{
    private static final String TAG = "TourCountNotesWidget";
    private final TextView textView;

    public HintWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_hint, this, true);
        textView = findViewById(R.id.hint_text);
    }

    public void setHint1(String notes)
    {
        textView.setText(notes);
    }

}

