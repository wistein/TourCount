package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/***************************************************************
 * EditIndividualWidget.java used by EditIndividualActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-05-15.
 * Last edited on 2018-03-31
 */
public class EditIndividualWidget extends LinearLayout
{
    private final TextView widget_locality1; // locality
    private final EditText widget_locality2;
    private final TextView widget_zcoord1;  //height
    private final TextView widget_zcoord2;
    private final TextView widget_stadium1; // stadium
    private final TextView widget_stadium2;
    private final TextView widget_state1; // state_1-6
    private final EditText widget_state2;
    private final TextView widget_count1; // number of individuals
    private final EditText widget_count2;
    private final TextView widget_indivnote1; // note
    private final EditText widget_indivnote2;
    private final TextView widget_xcoord1; // x-coord
    private final TextView widget_xcoord2;
    private final TextView widget_ycoord1; // y-coord
    private final TextView widget_ycoord2;

    public EditIndividualWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_individual, this, true);
        widget_locality1 = findViewById(R.id.widgetLocality1); // Locality
        widget_locality2 = findViewById(R.id.widgetLocality2);
        widget_zcoord1 = findViewById(R.id.widgetZCoord1); // Height
        widget_zcoord2 = findViewById(R.id.widgetZCoord2);
        widget_stadium1 = findViewById(R.id.widgetStadium1); // Stadium
        widget_stadium2 = findViewById(R.id.widgetStadium2);
        widget_state1 = findViewById(R.id.widgetState1); // State_1-6
        widget_state2 = findViewById(R.id.widgetState2);
        widget_count1 = findViewById(R.id.widgetCount1); // number of individuals
        widget_count2 = findViewById(R.id.widgetCount2);
        widget_indivnote1 = findViewById(R.id.widgetIndivNote1); // Note
        widget_indivnote2 = findViewById(R.id.widgetIndivNote2);
        widget_xcoord1 = findViewById(R.id.widgetXCoord1); // X-Coord
        widget_xcoord2 = findViewById(R.id.widgetXCoord2);
        widget_ycoord1 = findViewById(R.id.widgetYCoord1); // Y-Coord
        widget_ycoord2 = findViewById(R.id.widgetYCoord2);
    }

    // Following the SETS
    // locality
    public void setWidgetLocality1(String title)
    {
        widget_locality1.setText(title);
    }

    // stadium
    public void setWidgetStadium1(String title)
    {
        widget_stadium1.setText(title);
    }

    // state
    public void setWidgetState1(String title)
    {
        widget_state1.setText(title);
    }

    // number of individuals
    public void setWidgetCount1(String title)
    {
        widget_count1.setText(title);
    }

    // note
    public void setWidgetIndivNote1(String title)
    {
        widget_indivnote1.setText(title);
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

    // z-coord
    public void setWidgetZCoord1(String title)
    {
        widget_zcoord1.setText(title);
    }

    public void setWidgetZCoord2(String name)
    {
        widget_zcoord2.setText(name);
    }

    // following the GETS
    // get locality
    public String getWidgetLocality2()
    {
        return widget_locality2.getText().toString();
    }

    public void setWidgetLocality2(String name)
    {
        widget_locality2.setText(String.valueOf(name));
    }

    // get stadium with plausi
    public String getWidgetStadium2()
    {
        return widget_stadium2.getText().toString();
    }

    public void setWidgetStadium2(String name)
    {
        widget_stadium2.setText(name);
    }

    // get state with plausi
    public int getWidgetState2()
    {
        String text = widget_state2.getText().toString();
        String regEx = "^[0-9]*$";
        if (text.equals(""))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException nfe)
            {
                return 0;
            }
        }
    }

    public void setWidgetState2(int name)
    {
        widget_state2.setText(String.valueOf(name));
    }

    // get number of individuals
    public int getWidgetCount2()
    {
        String text = widget_count2.getText().toString();
            try
            {
                // value >= 0
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException nfe)
            {
                return -1; // count < 0 or text has no digit
            }
    }

    public void setWidgetCount2(int name)
    {
        widget_count2.setText(String.valueOf(name));
    }

    // get PLZ with plausi
    public String getWidgetIndivNote2()
    {
        return widget_indivnote2.getText().toString();
    }

    public void setWidgetIndivNote2(String name)
    {
        widget_indivnote2.setText(name);
    }

}
