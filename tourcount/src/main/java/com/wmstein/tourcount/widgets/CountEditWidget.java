package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.wmstein.tourcount.R;

import java.io.Serializable;

/**
 * Created by milo on 04/06/2014.
 * Changed by wmstein on 18.02.2016
 * used by EditSectionActivity
 */
public class CountEditWidget extends LinearLayout implements Serializable
{
    public int countId;
    private final transient EditText countName;
    private final transient EditText countCode;
    private final ImageButton deleteButton;

    public CountEditWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_count, this, true);
        countName = (EditText) findViewById(R.id.countName);
        countCode = (EditText) findViewById(R.id.countCode);
        deleteButton = (ImageButton) findViewById(R.id.deleteCount);
        deleteButton.setTag(0);
    }

    public String getCountName()
    {
        return countName.getText().toString();
    }

    public void setCountName(String name)
    {
        countName.setText(name);
    }

    public String getCountCode()
    {
        return countCode.getText().toString();
    }

    public void setCountCode(String name)
    {
        countCode.setText(name);
    }

    public void setCountId(int id)
    {
        countId = id;
        deleteButton.setTag(id);
    }

}
