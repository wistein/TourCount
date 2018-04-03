package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.wmstein.tourcount.R;

import java.io.Serializable;

/************************************************
 * Used by EditSectionActivity
 * 
 * Created by milo on 04/06/2014.
 * Adopted for TourCount by wmstein on 2016-02-18
 * last edited by wmstein on 2018-03-31
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
        countName = findViewById(R.id.countName);
        countCode = findViewById(R.id.countCode);
        deleteButton = findViewById(R.id.deleteCount);
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
