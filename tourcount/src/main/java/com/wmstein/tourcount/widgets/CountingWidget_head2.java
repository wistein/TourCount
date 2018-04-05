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

/****************************************************
 * Interface for widget_counting_head1.xml
 * Created by wmstein 18.12.2016
 * Last edited on 2018-03-31
 */
public class CountingWidget_head2 extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidget_head1";

    private TextView countHead2;

    public Count count;

    public CountingWidget_head2(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_head2, this, true);
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