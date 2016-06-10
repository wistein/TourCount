/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

/*
 * EditIndividualWidget.java used by EditIndividualActivity.java
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/*
 * Created by wmstein for com.wmstein.tourcount on 15.05.2016.
 */
public class EditIndividualWidget extends LinearLayout
{
    TextView widget_locality1; // locality
    EditText widget_locality2;
    TextView widget_sex1; // sex
    EditText widget_sex2;
    TextView widget_stadium1; // stadium
    EditText widget_stadium2;
    TextView widget_state1; // state_1-6
    EditText widget_state2;
    TextView widget_indivnote1; // note
    EditText widget_indivnote2;
    TextView widget_xcoord1; // x-coord
    TextView widget_xcoord2;
    TextView widget_ycoord1; // y-coord
    TextView widget_ycoord2;

    String regEx = "^[0-9]*$"; // plausi for numeric input

    public EditIndividualWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_individual, this, true);
        widget_locality1 = (TextView) findViewById(R.id.widgetLocality1); // Locality
        widget_locality2 = (EditText) findViewById(R.id.widgetLocality2);
        widget_sex1 = (TextView) findViewById(R.id.widgetSex1); // Sex
        widget_sex2 = (EditText) findViewById(R.id.widgetSex2);
        widget_stadium1 = (TextView) findViewById(R.id.widgetStadium1); // Stadium
        widget_stadium2 = (EditText) findViewById(R.id.widgetStadium2);
        widget_state1 = (TextView) findViewById(R.id.widgetState1); // State_1-6
        widget_state2 = (EditText) findViewById(R.id.widgetState2);
        widget_indivnote1 = (TextView) findViewById(R.id.widgetIndivNote1); // Note
        widget_indivnote2 = (EditText) findViewById(R.id.widgetIndivNote2);
        widget_xcoord1 = (TextView) findViewById(R.id.widgetXCoord1); // X-Coord
        widget_xcoord2 = (TextView) findViewById(R.id.widgetXCoord2);
        widget_ycoord1 = (TextView) findViewById(R.id.widgetYCoord1); // Y-Coord
        widget_ycoord2 = (TextView) findViewById(R.id.widgetYCoord2);
    }

    // Following the SETS
    // locality
    public void setWidgetLocality1(String title)
    {
        widget_locality1.setText(title);
    }

    public void setWidgetLocality2(String name)
    {
        widget_locality2.setText(String.valueOf(name));
    }

    // sex
    public void setWidgetSex1(String title)
    {
        widget_sex1.setText(title);
    }

    public void setWidgetSex2(String name)
    {
        widget_sex2.setText(name);
    }

    // stadium
    public void setWidgetStadium1(String title)
    {
        widget_stadium1.setText(title);
    }

    public void setWidgetStadium2(String name)
    {
        widget_stadium2.setText(name);
    }

    // state
    public void setWidgetState1(String title)
    {
        widget_state1.setText(title);
    }

    public void setWidgetState2(int name)
    {
        widget_state2.setText(String.valueOf(name));
    }

    // note
    public void setWidgetIndivNote1(String title)
    {
        widget_indivnote1.setText(title);
    }

    public void setWidgetIndivNote2(String name)
    {
        widget_indivnote2.setText(name);
    }

    // x-coord
    public void setWidgetXCoord1(String title)
    {
        widget_xcoord1.setText(title);
    }

    public void setWidgetXCoord2(String name)
    {
        widget_xcoord2.setText(name);
    }

    // y-coord
    public void setWidgetYCoord1(String title)
    {
        widget_ycoord1.setText(title);
    }

    public void setWidgetYCoord2(String name)
    {
        widget_ycoord2.setText(name);
    }


    // following the GETS
    // get locality
    public String getWidgetLocality2()
    {
        return widget_locality2.getText().toString();
    }

    // get sex with plausi
    public String getWidgetSex2()
    {
        return widget_sex2.getText().toString();
    }

    // get stadium with plausi
    public String getWidgetStadium2()
    {
        return widget_stadium2.getText().toString();
    }

    // get state with plausi
    public int getWidgetState2()
    {
        String text = widget_state2.getText().toString();
        if (text.equals(""))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
            return Integer.parseInt(text);
    }

    // get PLZ with plausi
    public String getWidgetIndivNote2()
    {
        return widget_indivnote2.getText().toString();
    }

}
