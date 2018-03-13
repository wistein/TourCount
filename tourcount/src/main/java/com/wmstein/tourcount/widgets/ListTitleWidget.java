/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/**
 * Created by wmstein on 06.06.2016
 */
public class ListTitleWidget extends LinearLayout
{
    private final TextView list_title;
    private final TextView list_name;

    public ListTitleWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_title, this, true);
        list_title = (TextView) findViewById(R.id.listTitle);
        list_name = (TextView) findViewById(R.id.listName);
    }

    public void setListTitle(String title)
    {
        list_title.setText(title);
    }

    public void setListName(String name)
    {
        list_name.setText(name);
    }

}
