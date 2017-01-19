package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/**
 * Created by milo on 03/06/2014.
 * Changed by wmstein on 18.02.2016
 */
public class EditTitleWidget extends LinearLayout
{
    private final TextView widget_title;
    private final EditText section_name;

    public EditTitleWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_title, this, true);
        widget_title = (TextView) findViewById(R.id.widgetTitle);
        section_name = (EditText) findViewById(R.id.sectionName);
    }

    public void setWidgetTitle(String title)
    {
        widget_title.setText(title);
    }

    public String getSectionName()
    {
        return section_name.getText().toString();
    }

    public void setSectionName(String name)
    {
        section_name.setText(name);
    }

    public void setHint(String hint)
    {
        section_name.setHint(hint);
    }

}
