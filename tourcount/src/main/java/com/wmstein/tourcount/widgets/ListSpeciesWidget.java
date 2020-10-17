package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import java.lang.reflect.Field;
import java.util.Objects;

/*******************************************************
 * ListSpeciesWidget shows count info area for a species
 * ListSpeciesActivity shows the result page
 * Created for TourCount by wmstein on 15.03.2016
 * Last edited on 2020-10-17
 */
public class ListSpeciesWidget extends RelativeLayout
{
    private final TextView txtSpecName;
    private final TextView txtSpecNameG;
    private final ImageView picSpecies;
    private final TextView specCount;
    private final TextView specCountf1i;
    private final TextView specCountf2i;
    private final TextView specCountf3i;
    private final TextView specCountpi;
    private final TextView specCountli;
    private final TextView specCountei;
    private final TextView txtSpecRem;
    private final TextView txtSpecRemT;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_species, this, true);
        txtSpecName = findViewById(R.id.txtSpecName);
        txtSpecNameG = findViewById(R.id.txtSpecNameG);
        txtSpecRemT = findViewById(R.id.txtSpecRemT);
        txtSpecRem = findViewById(R.id.txtSpecRem);
        specCount = findViewById(R.id.specCount);
        specCountf1i = findViewById(R.id.specCountf1i);
        specCountf2i = findViewById(R.id.specCountf2i);
        specCountf3i = findViewById(R.id.specCountf3i);
        specCountpi = findViewById(R.id.specCountpi);
        specCountli = findViewById(R.id.specCountli);
        specCountei = findViewById(R.id.specCountei);
        picSpecies = findViewById(R.id.picSpecies);
    }

    public void setCount(Count spec)
    {
        String rname = "p" + spec.code; // species picture resource name

        int resId = getResId(rname);
        if (resId != 0)
        {
            picSpecies.setImageResource(resId);
        }

        int spCount = spec.count_f1i + spec.count_f2i + spec.count_f3i + spec.count_pi 
            + spec.count_li + spec.count_ei;
        txtSpecName.setText(spec.name);
        if (spec.name_g != null)
        {
            if (!spec.name_g.isEmpty())
            {
                txtSpecNameG.setText(spec.name_g);
            }
            else
            {
                txtSpecNameG.setText("");
            }
        }

        specCount.setText(String.valueOf(spCount));
        specCountf1i.setText(String.valueOf(spec.count_f1i));
        specCountf2i.setText(String.valueOf(spec.count_f2i));
        specCountf3i.setText(String.valueOf(spec.count_f3i));
        specCountpi.setText(String.valueOf(spec.count_pi));
        specCountli.setText(String.valueOf(spec.count_li));
        specCountei.setText(String.valueOf(spec.count_ei));
        if (spec.notes != null)
        {
            if (!spec.notes.isEmpty())
            {
                txtSpecRem.setText(spec.notes);
                txtSpecRem.setVisibility(View.VISIBLE);
                txtSpecRemT.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            txtSpecRem.setVisibility(View.GONE);
            txtSpecRemT.setVisibility(View.GONE);
        } 
    }

    //Parameter spec_count* for use in ListSpeciesActivity
    public int getSpec_countf1i(Count spec)
    {
        return spec.count_f1i;
    }

    public int getSpec_countf2i(Count spec)
    {
        return spec.count_f2i;
    }

    public int getSpec_countf3i(Count spec)
    {
        return spec.count_f3i;
    }

    public int getSpec_countpi(Count spec)
    {
        return spec.count_pi;
    }

    public int getSpec_countli(Count spec)
    {
        return spec.count_li;
    }

    public int getSpec_countei(Count spec)
    {
        return spec.count_ei;
    }

    //Parameter spec_name for use in ListSpeciesActivity
    public String getSpec_name(Count newcount)
    {
        return newcount.name;
    }

    // Get resource ID from resource name
    private int getResId(String rName)
    {
        try
        {
            Class<R.drawable> res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e)
        {
            return 0;
        }
    }

}
