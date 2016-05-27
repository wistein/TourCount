package com.wmstein.tourcount.widgets;

/*
 * ListMetaWidget.java used by ListSpeciesActivity.java
 * Created by wmstein for com.wmstein.tourcount on 19.04.2016
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

public class ListMetaWidget extends LinearLayout
{
    TextView widget_lmeta1; // temperature
    TextView widget_litem1;
    TextView widget_lmeta2; // wind
    TextView widget_litem2;
    TextView widget_lmeta3; // clouds
    TextView widget_litem3;
    TextView widget_lplz1; // plz
    TextView widget_lplz2;
    TextView widget_lcity; // city
    TextView widget_litem4;
    TextView widget_lplace; // place
    TextView widget_litem5;
    TextView widget_ldate1; // date
    TextView widget_ldate2;
    TextView widget_lstartTm1; // start_tm
    TextView widget_lstartTm2;
    TextView widget_lendTm1; // end_tm
    TextView widget_lendTm2;

    public ListMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_meta, this, true);
        widget_lmeta1 = (TextView) findViewById(R.id.widgetLMeta1);
        widget_litem1 = (TextView) findViewById(R.id.widgetLItem1);
        widget_lmeta2 = (TextView) findViewById(R.id.widgetLMeta2);
        widget_litem2 = (TextView) findViewById(R.id.widgetLItem2);
        widget_lmeta3 = (TextView) findViewById(R.id.widgetLMeta3);
        widget_litem3 = (TextView) findViewById(R.id.widgetLItem3);
        widget_lplz1 = (TextView) findViewById(R.id.widgetLPlz1);
        widget_lplz2 = (TextView) findViewById(R.id.widgetLPlz2);
        widget_lcity = (TextView) findViewById(R.id.widgetLCity);
        widget_litem4 = (TextView) findViewById(R.id.widgetLItem4);
        widget_lplace = (TextView) findViewById(R.id.widgetLPlace);
        widget_litem5 = (TextView) findViewById(R.id.widgetLItem5);
        widget_ldate1 = (TextView) findViewById(R.id.widgetLDate1);
        widget_ldate2 = (TextView) findViewById(R.id.widgetLDate2);
        widget_lstartTm1 = (TextView) findViewById(R.id.widgetLStartTm1);
        widget_lstartTm2 = (TextView) findViewById(R.id.widgetLStartTm2);
        widget_lendTm1 = (TextView) findViewById(R.id.widgetLEndTm1);
        widget_lendTm2 = (TextView) findViewById(R.id.widgetLEndTm2);
    }

    // Following the SETS
    // temperature
    public void setWidgetLMeta1(String title)
    {
        widget_lmeta1.setText(title);
    }

    public void setWidgetLItem1(int name)
    {
        widget_litem1.setText(String.valueOf(name));
    }

    // wind
    public void setWidgetLMeta2(String title)
    {
        widget_lmeta2.setText(title);
    }

    public void setWidgetLItem2(int name)
    {
        widget_litem2.setText(String.valueOf(name));
    }

    // clouds
    public void setWidgetLMeta3(String title)
    {
        widget_lmeta3.setText(title);
    }

    public void setWidgetLItem3(int name)
    {
        widget_litem3.setText(String.valueOf(name));
    }

    // PLZ
    public void setWidgetLPlz1(String title)
    {
        widget_lplz1.setText(title);
    }

    public void setWidgetLPlz2(String name)
    {
        widget_lplz2.setText(name);
    }

    // city
    public void setWidgetLCity(String title)
    {
        widget_lcity.setText(title);
    }

    public void setWidgetLItem4(String name)
    {
        widget_litem4.setText(name);
    }

    // place
    public void setWidgetLPlace(String title)
    {
        widget_lplace.setText(title);
    }

    public void setWidgetLItem5(String name)
    {
        widget_litem5.setText(name);
    }

    // date
    public void setWidgetLDate1(String title)
    {
        widget_ldate1.setText(title);
    }

    public void setWidgetLDate2(String name)
    {
        widget_ldate2.setText(name);
    }

    // start_tm
    public void setWidgetLStartTm1(String title)
    {
        widget_lstartTm1.setText(title);
    }

    public void setWidgetLStartTm2(String name)
    {
        widget_lstartTm2.setText(name);
    }

    // end_tm
    public void setWidgetLEndTm1(String title)
    {
        widget_lendTm1.setText(title);
    }

    public void setWidgetLEndTm2(String name)
    {
        widget_lendTm2.setText(name);
    }

}
