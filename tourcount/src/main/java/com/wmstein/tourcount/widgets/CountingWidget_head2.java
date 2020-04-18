/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import java.util.Objects;

/****************************************************
 * Interface for widget_counting_head2.xml
 * used by Counting(L)Activity
 * Created by wmstein 2016-12-18
 * Last edited on 2020-04-17
 */
public class CountingWidget_head2 extends RelativeLayout
{
    private static final String TAG = "tourcountCntWidget_hd2";

    private TextView countHead2;

    public Count count;

    public CountingWidget_head2(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_head2, this, true);
        countHead2 = findViewById(R.id.countHead2);
    }


    public void setCountHead2(Count count)
    {
        // set TextView countHead2
        countHead2.setText(count.notes);
        // set ImageButton Edit
        ImageButton editButton = findViewById(R.id.buttonEdit);
        editButton.setTag(count.id);
    }

    public void setFont(Boolean large)
    {
        if (large)
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Setzt große Schrift.");
            countHead2.setTextSize(16);
        }
        else
        {
            if (MyDebug.LOG)
                Log.d(TAG, "Setzt kleine Schrift.");
            countHead2.setTextSize(14);
        }
    }

}
