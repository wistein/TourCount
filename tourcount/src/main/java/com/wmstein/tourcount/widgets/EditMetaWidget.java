package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

import java.util.Objects;

/*************************************************************
 * EditMetaWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited on 2019-02-12
 */
public class EditMetaWidget extends LinearLayout
{
    private final TextView widget_temp1; // temperature
    private final EditText widget_temp2;
    private final TextView widget_wind1; // wind
    private final EditText widget_wind2;
    private final TextView widget_clouds1; // clouds
    private final EditText widget_clouds2;
    private final TextView widget_plz1; // plz
    private final TextView widget_plz2;
    private final TextView widget_city1; // city
    private final TextView widget_city2;
    private final TextView widget_place1; // place
    private final TextView widget_place2;
    private final TextView widget_date1; // date
    private final TextView widget_date2;
    private final TextView widget_startTm1; // start_tm
    private final TextView widget_startTm2;
    private final TextView widget_endTm1; // end_tm
    private final TextView widget_endTm2;

    private final String regEx = "^[0-9]*$"; // plausi for numeric input

    public EditMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta, this, true);
        widget_temp1 = findViewById(R.id.widgetTemp1); // temperature
        widget_temp2 = findViewById(R.id.widgetTemp2);
        widget_wind1 = findViewById(R.id.widgetWind1); // wind
        widget_wind2 = findViewById(R.id.widgetWind2);
        widget_clouds1 = findViewById(R.id.widgetClouds1); // clouds
        widget_clouds2 = findViewById(R.id.widgetClouds2);
        widget_plz1 = findViewById(R.id.widgetPlz1); // plz
        widget_plz2 = findViewById(R.id.widgetPlz2);
        widget_city1 = findViewById(R.id.widgetCity); // city
        widget_city2 = findViewById(R.id.widgetItem4);
        widget_place1 = findViewById(R.id.widgetPlace); // place
        widget_place2 = findViewById(R.id.widgetItem5);
        widget_date1 = findViewById(R.id.widgetDate1);
        widget_date2 = findViewById(R.id.widgetDate2);
        widget_startTm1 = findViewById(R.id.widgetStartTm1);
        widget_startTm2 = findViewById(R.id.widgetStartTm2);
        widget_endTm1 = findViewById(R.id.widgetEndTm1);
        widget_endTm2 = findViewById(R.id.widgetEndTm2);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    private static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

    // Following the SETS
    // temperature
    public void setWidgetTemp1(String title)
    {
        widget_temp1.setText(title);
    }

    // wind
    public void setWidgetWind1(String title)
    {
        widget_wind1.setText(title);
    }

    // clouds
    public void setWidgetClouds1(String title)
    {
        widget_clouds1.setText(title);
    }

    // PLZ
    public void setWidgetPlz1(String title)
    {
        widget_plz1.setText(title);
    }

    // city
    public void setWidgetCity1(String title)
    {
        widget_city1.setText(title);
    }

    // place
    public void setWidgetPlace1(String title)
    {
        widget_place1.setText(title);
    }

    // date
    public void setWidgetDate1(String title)
    {
        widget_date1.setText(title);
    }

    // start_tm
    public void setWidgetStartTm1(String title)
    {
        widget_startTm1.setText(title);
    }

    // end_tm
    public void setWidgetEndTm1(String title)
    {
        widget_endTm1.setText(title);
    }

    // following the GETS
    // get temperature with plausi
    public int getWidgetTemp2()
    {
        String text = widget_temp2.getText().toString();
        if (isEmpty(text))
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
                return 100;
            }
        }
    }

    public void setWidgetTemp2(int name)
    {
        widget_temp2.setText(String.valueOf(name));
    }

    // get wind with plausi
    public int getWidgetWind2()
    {
        String text = widget_wind2.getText().toString();
        if (isEmpty(text))
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
                return 100;
            }
        }
    }

    public void setWidgetWind2(int name)
    {
        widget_wind2.setText(String.valueOf(name));
    }

    // get clouds with plausi
    public int getWidgetClouds2()
    {
        String text = widget_clouds2.getText().toString();
        if (isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 200;
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException nfe)
            {
                return 100;
            }
        }
    }

    public void setWidgetClouds2(int name)
    {
        widget_clouds2.setText(String.valueOf(name));
    }

    // get PLZ with plausi
    public String getWidgetPlz2()
    {
        return widget_plz2.getText().toString();
    }

    public void setWidgetPlz2(String name)
    {
        widget_plz2.setText(name);
    }

    // get city with plausi
    public String getWidgetCity2()
    {
        return widget_city2.getText().toString();
    }

    public void setWidgetCity2(String name)
    {
        widget_city2.setText(name);
    }

    // get place with plausi
    public String getWidgetPlace2()
    {
        return widget_place2.getText().toString();
    }

    public void setWidgetPlace2(String name)
    {
        widget_place2.setText(name);
    }

    public String getWidgetDate2()
    {
        return widget_date2.getText().toString();
    }

    public void setWidgetDate2(String name)
    {
        widget_date2.setText(name);
    }

    public String getWidgetStartTm2()
    {
        return widget_startTm2.getText().toString();
    }

    public void setWidgetStartTm2(String name)
    {
        widget_startTm2.setText(name);
    }

    public String getWidgetEndTm2()
    {
        return widget_endTm2.getText().toString();
    }

    public void setWidgetEndTm2(String name)
    {
        widget_endTm2.setText(name);
    }

}
