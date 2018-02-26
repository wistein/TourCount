package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/**
 * Created by wmstein on 18.02.2016
 * used by CountOptionsActivity
 */
public class EditNotesWidget extends LinearLayout
{
    private final TextView widget_title;
    private final EditText notes_name;

    public EditNotesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_title, this, true);
        widget_title = (TextView) findViewById(R.id.widgetTitle);
        notes_name = (EditText) findViewById(R.id.sectionName);
        notes_name.requestFocus();
        
        //Force to show keyboard, needs hideSoftInputFromWindow in CountOptionsActivity
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void setWidgetTitle(String title)
    {
        widget_title.setText(title);
    }

    public void setNotesName(String name)
    {
        notes_name.setText(name);
    }

    public void setHint(String hint)
    {
        notes_name.setHint(hint);
    }

    public String getNotesName()
    {
        return notes_name.getText().toString();
    }

}
