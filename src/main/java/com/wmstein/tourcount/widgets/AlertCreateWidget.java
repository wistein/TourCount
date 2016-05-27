package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.wmstein.tourcount.R;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Created by milo on 02/06/2014.
 * This is the widget for creating an alert in the CountOptionsActivity.
 */
public class AlertCreateWidget extends LinearLayout implements Serializable
{
    EditText alert_name;
    EditText alert_value;
    int alert_id;
    ImageButton deleteButton;

    public AlertCreateWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_alert_create, this, true);
        alert_name = (EditText) findViewById(R.id.alert_name);
        alert_value = (EditText) findViewById(R.id.alert_value);
        alert_id = 0;
        deleteButton = (ImageButton) findViewById(R.id.delete_button);
        deleteButton.setTag(Integer.valueOf(0));
    }

    public String getAlertName()
    {
        return alert_name.getText().toString();
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that tourcount doesn't crash
    public int getAlertValue()
    {
        String text = alert_value.getText().toString();
        if (StringUtils.isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }

    public int getAlertId()
    {
        return alert_id;
    }

    public void setAlertName(String name)
    {
        alert_name.setText(name);
    }

    public void setAlertValue(int value)
    {
        alert_value.setText(String.valueOf(value));
    }

    public void setAlertId(int id)
    {
        alert_id = id;
        deleteButton.setTag(Integer.valueOf(id));
    }

}
