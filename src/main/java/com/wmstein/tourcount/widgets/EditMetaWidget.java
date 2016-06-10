package com.wmstein.tourcount.widgets;

/*
 * EditMetaWidget.java used by EditMetaActivity.java
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

import org.apache.commons.lang3.StringUtils;

/*
 * Created by wmstein for com.wmstein.tourcount on 02.04.2016.
 */
public class EditMetaWidget extends LinearLayout
{
    TextView widget_temp1; // temperature
    EditText widget_temp2;
    TextView widget_wind1; // wind
    EditText widget_wind2;
    TextView widget_clouds1; // clouds
    EditText widget_clouds2;
    TextView widget_plz1; // plz
    TextView widget_plz2;
    TextView widget_city1; // city
    TextView widget_city2;
    TextView widget_place1; // place
    TextView widget_place2;
    TextView widget_date1; // date
    TextView widget_date2;
    TextView widget_startTm1; // start_tm
    TextView widget_startTm2;
    TextView widget_endTm1; // end_tm
    TextView widget_endTm2;

    String regEx = "^[0-9]*$"; // plausi for numeric input

    public EditMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_meta, this, true);
        widget_temp1 = (TextView) findViewById(R.id.widgetTemp1); // temperature
        widget_temp2 = (EditText) findViewById(R.id.widgetTemp2);
        widget_wind1 = (TextView) findViewById(R.id.widgetWind1); // wind
        widget_wind2 = (EditText) findViewById(R.id.widgetWind2);
        widget_clouds1 = (TextView) findViewById(R.id.widgetClouds1); // clouds
        widget_clouds2 = (EditText) findViewById(R.id.widgetClouds2);
        widget_plz1 = (TextView) findViewById(R.id.widgetPlz1); // plz
        widget_plz2 = (TextView) findViewById(R.id.widgetPlz2);
        widget_city1 = (TextView) findViewById(R.id.widgetCity); // city
        widget_city2 = (TextView) findViewById(R.id.widgetItem4);
        widget_place1 = (TextView) findViewById(R.id.widgetPlace); // place
        widget_place2 = (TextView) findViewById(R.id.widgetItem5);
        widget_date1 = (TextView) findViewById(R.id.widgetDate1);
        widget_date2 = (TextView) findViewById(R.id.widgetDate2);
        widget_startTm1 = (TextView) findViewById(R.id.widgetStartTm1);
        widget_startTm2 = (TextView) findViewById(R.id.widgetStartTm2);
        widget_endTm1 = (TextView) findViewById(R.id.widgetEndTm1);
        widget_endTm2 = (TextView) findViewById(R.id.widgetEndTm2);
    }

    // Following the SETS
    // temperature
    public void setWidgetTemp1(String title)
    {
        widget_temp1.setText(title);
    }

    public void setWidgetTemp2(int name)
    {
        widget_temp2.setText(String.valueOf(name));
    }

    // wind
    public void setWidgetWind1(String title)
    {
        widget_wind1.setText(title);
    }

    public void setWidgetWind2(int name)
    {
        widget_wind2.setText(String.valueOf(name));
    }

    // clouds
    public void setWidgetClouds1(String title)
    {
        widget_clouds1.setText(title);
    }

    public void setWidgetClouds2(int name)
    {
        widget_clouds2.setText(String.valueOf(name));
    }

    // PLZ
    public void setWidgetPlz1(String title)
    {
        widget_plz1.setText(title);
    }

    public void setWidgetPlz2(String name)
    {
        widget_plz2.setText(name);
    }

    // city
    public void setWidgetCity1(String title)
    {
        widget_city1.setText(title);
    }

    public void setWidgetCity2(String name)
    {
        widget_city2.setText(name);
    }

    // place
    public void setWidgetPlace1(String title)
    {
        widget_place1.setText(title);
    }

    public void setWidgetPlace2(String name)
    {
        widget_place2.setText(name);
    }

    // date
    public void setWidgetDate1(String title)
    {
        widget_date1.setText(title);
    }

    public void setWidgetDate2(String name)
    {
        widget_date2.setText(name);
    }

    // start_tm
    public void setWidgetStartTm1(String title)
    {
        widget_startTm1.setText(title);
    }

    public void setWidgetStartTm2(String name)
    {
        widget_startTm2.setText(name);
    }

    // end_tm
    public void setWidgetEndTm1(String title)
    {
        widget_endTm1.setText(title);
    }

    public void setWidgetEndTm2(String name)
    {
        widget_endTm2.setText(name);
    }


    // following the GETS
    // get temperature with plausi
    public int getWidgetTemp2()
    {
        String text = widget_temp2.getText().toString();
        if (StringUtils.isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
            return Integer.parseInt(text);
    }

    // get wind with plausi
    public int getWidgetWind2()
    {
        String text = widget_wind2.getText().toString();
        if (StringUtils.isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
            return Integer.parseInt(text);
    }

    // get clouds with plausi
    public int getWidgetClouds2()
    {
        String text = widget_clouds2.getText().toString();
        if (StringUtils.isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 200;
        else
            return Integer.parseInt(text);
    }

    // get PLZ with plausi
    public String getWidgetPlz2()
    {
        return widget_plz2.getText().toString();
    }

    // get city with plausi
    public String getWidgetCity2()
    {
        return widget_city2.getText().toString();
    }

    // get place with plausi
    public String getWidgetPlace2()
    {
        return widget_place2.getText().toString();
    }

    public String getWidgetDate2()
    {
        return widget_date2.getText().toString();
    }

    public String getWidgetStartTm2()
    {
        return widget_startTm2.getText().toString();
    }

    public String getWidgetEndTm2()
    {
        return widget_endTm2.getText().toString();
    }

}
