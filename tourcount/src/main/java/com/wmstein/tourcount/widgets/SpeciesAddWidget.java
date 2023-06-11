package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.TourCountApplication;

import java.io.Serializable;
import java.util.Objects;

/************************************************
 * Used by AddSpeciesActivity
 * shows list of selectable species with name, code, picture and add button
 * Created for TourCount by wmstein on 2019-04-03
 * last edited by wmstein on 2020-10-18
 */
public class SpeciesAddWidget extends LinearLayout implements Serializable
{
    private final transient TextView specName;
    private final transient TextView specNameG;
    private final transient TextView specCode;
    private final transient TextView specId;
    private final transient ImageView specPic;
    private final ImageButton addButton;

    final LayoutInflater inflater;

    public SpeciesAddWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_spec, this, true);

        specName = findViewById(R.id.specName);
        specNameG = findViewById(R.id.specNameG);
        specCode = findViewById(R.id.specCode);
        specId = findViewById(R.id.specId);
        specPic = findViewById(R.id.specPic);
        addButton = findViewById(R.id.addCount);
        addButton.setTag(0);
    }

    public String getSpecName()
    {
        return specName.getText().toString();
    }

    public void setSpecName(String name)
    {
        specName.setText(name);
    }

    public String getSpecNameG()
    {
        return specNameG.getText().toString();
    }

    public void setSpecNameG(String nameg)
    {
        specNameG.setText(nameg);
    }

    public String getSpecCode()
    {
        return specCode.getText().toString();
    }

    public void setSpecCode(String code)
    {
        specCode.setText(code);
    }

    public void setSpecId(String id)
    {
        specId.setText(id);
        addButton.setTag(Integer.parseInt(id) - 1);
    }

    public void setPSpec(String ucode)
    {
        String rname = "p" + ucode; // species picture resource name

        // make instance of class TransektCountApplication to reference non-static method 
        TourCountApplication tourCountApp = new TourCountApplication();

        int resId = tourCountApp.getResId(rname);
        if (resId != 0)
        {
            specPic.setImageResource(resId);
        }
    }

}
