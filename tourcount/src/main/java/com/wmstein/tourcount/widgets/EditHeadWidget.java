package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/***************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 31.03.2016.
 */
public class EditHeadWidget extends LinearLayout
{
    private final TextView widget_co1; // used for country title
    private final EditText widget_co2; // used for country
    private final TextView widget_name1; // used for observer title
    private final EditText widget_name2; // used for observer

    public EditHeadWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_head, this, true);
        widget_co1 = (TextView) findViewById(R.id.widgetCo1);
        widget_co2 = (EditText) findViewById(R.id.widgetCo2);
        widget_name1 = (TextView) findViewById(R.id.widgetName1);
        widget_name2 = (EditText) findViewById(R.id.widgetName2);
    }

    // Following the SETS
    // country
    public void setWidgetCo1(String title)
    {
        widget_co1.setText(title);
    }

    public void setWidgetCo2(String name)
    {
        widget_co2.setText(name);
    }

    // name
    public void setWidgetName1(String title)
    {
        widget_name1.setText(title);
    }

    public void setWidgetName2(String name)
    {
        widget_name2.setText(name);
    }

    // Following the GETS
    // country
    public String setWidgetCo2()
    {
        return widget_co2.getText().toString();
    }

    // name
    public String setWidgetName2()
    {
        return widget_name2.getText().toString();
    }

}
