package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Section;

import java.util.Objects;

/*************************************************************
 * ListMetaWidget.java used by ListSpeciesActivity.java 
 * Created by wmstein for com.wmstein.tourcount on 2016-04-19,
 * last edited on 2021-01-26
 */
public class ListMetaWidget extends LinearLayout
{
    private final TextView widget_lmeta1; // temperature
    private final TextView widget_litem1;
    private final TextView widget_lmeta2; // wind
    private final TextView widget_litem2;
    private final TextView widget_lmeta3; // clouds
    private final TextView widget_litem3;
    private final TextView widget_lplz1; // plz
    private final TextView widget_lplz2;
    private final TextView widget_lcity; // city
    private final TextView widget_litem4;
    private final TextView widget_lplace; // place
    private final TextView widget_litem5;
    private final TextView widget_ldate1; // date
    private final TextView widget_ldate2;
    private final TextView widget_lstartTm1; // start_tm
    private final TextView widget_lstartTm2;
    private final TextView widget_lendTm1; // end_tm
    private final TextView widget_lendTm2;
    private final TextView widget_dlo1; // average longitude
    private final TextView widget_dlo2;
    private final TextView widget_dla1; // average latitude
    private final TextView widget_dla2;
    private final TextView widget_muncert1; // mean uncertainty
    private final TextView widget_muncert2;

    public ListMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true);
        widget_lmeta1 = findViewById(R.id.widgetLMeta1);
        widget_litem1 = findViewById(R.id.widgetLItem1);
        widget_lmeta2 = findViewById(R.id.widgetLMeta2);
        widget_litem2 = findViewById(R.id.widgetLItem2);
        widget_lmeta3 = findViewById(R.id.widgetLMeta3);
        widget_litem3 = findViewById(R.id.widgetLItem3);
        widget_lplz1 = findViewById(R.id.widgetLPlz1);
        widget_lplz2 = findViewById(R.id.widgetLPlz2);
        widget_lcity = findViewById(R.id.widgetLCity);
        widget_litem4 = findViewById(R.id.widgetLItem4);
        widget_lplace = findViewById(R.id.widgetLPlace);
        widget_litem5 = findViewById(R.id.widgetLItem5);
        widget_ldate1 = findViewById(R.id.widgetLDate1);
        widget_ldate2 = findViewById(R.id.widgetLDate2);
        widget_lstartTm1 = findViewById(R.id.widgetLStartTm1);
        widget_lstartTm2 = findViewById(R.id.widgetLStartTm2);
        widget_lendTm1 = findViewById(R.id.widgetLEndTm1);
        widget_lendTm2 = findViewById(R.id.widgetLEndTm2);
        widget_dlo1 = findViewById(R.id.widgetdlo1);
        widget_dlo2 = findViewById(R.id.widgetdlo2);
        widget_dla1 = findViewById(R.id.widgetdla1);
        widget_dla2 = findViewById(R.id.widgetdla2);
        widget_muncert1 = findViewById(R.id.widgetmuncert1);
        widget_muncert2 = findViewById(R.id.widgetmuncert2);
    }

    // Following the SETS
    public void setMetaWidget(Section section)
    {
        widget_lmeta1.setText(R.string.temperature);
        widget_litem1.setText(String.valueOf(section.temp));
        widget_lmeta2.setText(R.string.wind);
        widget_litem2.setText(String.valueOf(section.wind));
        widget_lmeta3.setText(R.string.clouds);
        widget_litem3.setText(String.valueOf(section.clouds));
        widget_lplz1.setText(R.string.plz);
        widget_lplz2.setText(section.plz);
        widget_lcity.setText(R.string.city);
        widget_litem4.setText(section.city);
        widget_lplace.setText(R.string.place);
        widget_litem5.setText(section.place);
        widget_ldate1.setText(R.string.date);
        widget_ldate2.setText(section.date);
        widget_lstartTm1.setText(R.string.starttm);
        widget_lstartTm2.setText(section.start_tm);
        widget_lendTm1.setText(R.string.endtm);
        widget_lendTm2.setText(section.end_tm);
        widget_dlo1.setText(R.string.dLo);
        widget_dla1.setText(R.string.dLa);
        widget_muncert1.setText(R.string.mUncert);
    }
    
    public void setWidget_dla2(double name)
    {
        int slen = String.valueOf(name).length();
        if(slen > 8)
        {
            widget_dla2.setText(String.valueOf(name).substring(0, 8));
        }
        else 
        {
            widget_dla2.setText(String.valueOf(name));
        }
    }
    public void setWidget_dlo2(double name)
    {
        int slen = String.valueOf(name).length();
        if(slen > 8)
        {
            widget_dlo2.setText(String.valueOf(name).substring(0, 8));
        }
        else
        {
            widget_dlo2.setText(String.valueOf(name));
        }
    }
    public void setWidget_muncert2(double name)
    {
        widget_muncert2.setText(String.format("%s m", Math.round(name)));
    }
    
}
