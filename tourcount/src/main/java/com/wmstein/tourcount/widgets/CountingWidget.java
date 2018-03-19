package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.AutoFitText;
import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import java.lang.reflect.Field;

/************************************************************
 * Used by CountingActivity.java and widget_counting.xml
 * Created by milo on 25/05/2014.
 * Adopted for TourCount by wmstein on 2016-02-18,
 * last edited by wmstein on 2018-03-19
 */
public class CountingWidget extends RelativeLayout
{
    public static String TAG = "tourcountCountingWidget";
    public Count count;
    private final TextView countName;
    private final ImageView pSpecies;
    private final AutoFitText countCount;

    public CountingWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting, this, true);
        countCount = (AutoFitText) findViewById(R.id.countCount);
        countName = (TextView) findViewById(R.id.countName);
        pSpecies = (ImageView) findViewById(R.id.pSpecies);
    }

    public void setCount(Count newcount)
    {
        count = newcount;
        String rname = "p" + count.code; // species picture resource name

        int resId = getResId(rname);
        if (resId != 0)
        {
            pSpecies.setImageResource(resId);
        }

        countName.setText(count.name);
        countCount.setText(String.valueOf(count.count));
        ImageButton countUpButton = (ImageButton) findViewById(R.id.buttonUp);
        countUpButton.setTag(count.id);
        ImageButton countDownButton = (ImageButton) findViewById(R.id.buttonDown);
        countDownButton.setTag(count.id);
        ImageButton editButton = (ImageButton) findViewById(R.id.buttonEdit);
        editButton.setTag(count.id);
    }

    public void countUp()
    {
        countCount.setText(String.valueOf(count.increase()));
    }

    public void countDown()
    {
        countCount.setText(String.valueOf(count.safe_decrease()));
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
