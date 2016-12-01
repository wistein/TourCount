package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.Section;

import java.lang.reflect.Field;


/**
 * Created by wmstein on 15.03.2016
 */
public class ListSpeciesWidget extends RelativeLayout
{
    public static String TAG = "tourcountListSpeciesWidget";

    private TextView txtSpecName;
    private ImageView picSpecies;
    private TextView specCount;
    private TextView txtSpecRem;

    public int spec_count;
    public Count spec;
    public Section section;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_species, this, true);
        txtSpecName = (TextView) findViewById(R.id.txtSpecName);
        specCount = (TextView) findViewById(R.id.specCount);
        txtSpecRem = (TextView) findViewById(R.id.txtSpecRem);
        picSpecies = (ImageView) findViewById(R.id.picSpecies);
    }

    public void setCount(Count spec, Section section)
    {
        String rname = "p" + spec.code; // species picture resource name

        int resId = getResId(rname);
        if (resId != 0)
        {
            picSpecies.setImageResource(resId);
        }

        txtSpecName.setText(spec.name);
        specCount.setText(String.valueOf(spec.count));
        txtSpecRem.setText(spec.notes);
    }

    //Parameter spec_count for use in ListSpeciesActivity
    public int getSpec_count(Count newcount)
    {
        spec = newcount;
        spec_count = spec.count;
        return spec_count;
    }

    // Get resource ID from resource name
    public int getResId(String rName)
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
