package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

import java.util.Objects;

/**********************************
 * Created by wmstein on 2016-06-06,
 * last edited on 2019-02-12
 */
public class ListTitleWidget extends LinearLayout
{
    private final TextView list_title;
    private final TextView list_name;

    public ListTitleWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_title, this, true);
        list_title = findViewById(R.id.listTitle);
        list_name = findViewById(R.id.listName);
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
