/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.AutoFitText;
import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

/**
 * Created by milo on 25/05/2014.
 * Changed by wmstein on 18.02.2016
 */
public class CountingWidgetLH extends RelativeLayout
{
    public static String TAG = "tourcountCountingWidgetLH";

    private TextView countName;
    private AutoFitText countCount;

    public Count count;

    public CountingWidgetLH(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_lh, this, true);
        countCount = (AutoFitText) findViewById(R.id.countCountLH);
        countName = (TextView) findViewById(R.id.countNameLH);
    }

    public void setCount(Count newcount)
    {
        count = newcount;
        countCount.setText(String.valueOf(count.count));
        countName.setText(count.name);
        ImageButton countUpButton = (ImageButton) findViewById(R.id.buttonUpLH);
        countUpButton.setTag(count.id);
        ImageButton countDownButton = (ImageButton) findViewById(R.id.buttonDownLH);
        countDownButton.setTag(count.id);
        ImageButton editButton = (ImageButton) findViewById(R.id.buttonEditLH);
        editButton.setTag(count.id);
    }

    public void countUpLH()
    {
        count.increase();
        countCount.setText(String.valueOf(count.count));
    }

    public void countDownLH()
    {
        count.safe_decrease();
        countCount.setText(String.valueOf(count.count));
    }

}
