package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import java.lang.reflect.Field;


/**
 * Created by wmstein on 15.03.2016
 */
public class ListSpeciesWidget extends RelativeLayout
{
    private final TextView txtSpecName;
    private final ImageView picSpecies;
    private final TextView specCount;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_species, this, true);
        txtSpecName = (TextView) findViewById(R.id.txtSpecName);
        specCount = (TextView) findViewById(R.id.specCount);
        picSpecies = (ImageView) findViewById(R.id.picSpecies);
    }

    public void setCount(Count spec)
    {
        String rname = "p" + spec.code; // species picture resource name

        int resId = getResId(rname);
        if (resId != 0)
        {
            picSpecies.setImageResource(resId);
        }

        txtSpecName.setText(spec.name);
        specCount.setText(String.valueOf(spec.count));
    }

    //Parameter spec_count for use in ListSpeciesActivity
    public int getSpec_count(Count newcount)
    {
        return newcount.count;
    }

    //Parameter spec_name for use in ListSpeciesActivity
    public String getSpec_name(Count newcount)
    {
        return newcount.name;
    }

    //Parameter spec_notes for use in ListSpeciesActivity
    public String getSpec_notes(Count newcount)
    {
        return newcount.notes;
    }

    // Get resource ID from resource name
    private int getResId(String rName)
    {
        try
        {
            Class res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e)
        {
            return 0;
        }
    }

}
