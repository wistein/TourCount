package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/***************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 03.04.2016
 */
public class ListHeadWidget extends LinearLayout
{
    public static String TAG = "tourcountListHeadWidget";

    private final TextView widget_lco; // used for country title
    private final TextView widget_lco1; // used for country
    private final TextView widget_lname; // used for observer title
    private final TextView widget_lname1; // used for observer

    public ListHeadWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_head, this, true);
        widget_lco = (TextView) findViewById(R.id.widgetLCo);
        widget_lco1 = (TextView) findViewById(R.id.widgetLCo1);
        widget_lname = (TextView) findViewById(R.id.widgetLName);
        widget_lname1 = (TextView) findViewById(R.id.widgetLName1);
    }

    public void setWidgetLCo(String title)
    {
        widget_lco.setText(title);
    }

    public void setWidgetLCo1(String name)
    {
        widget_lco1.setText(name);
    }

    public void setWidgetLName(String title)
    {
        widget_lname.setText(title);
    }

    public void setWidgetLName1(String name)
    {
        widget_lname1.setText(name);
    }

}
