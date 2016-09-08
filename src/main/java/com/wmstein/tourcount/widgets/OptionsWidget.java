package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;

/**
 * Created by milo on 27/05/2014.
 * Changed by wmstein on 18.02.2016
 * Edit options for species
 * used by CountOptionsActivity in conjunction with widget_options.xml
 */
public class OptionsWidget extends LinearLayout
{
    private TextView instructions;
    private TextView number;

    public OptionsWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_options, this, true);
        instructions = (TextView) findViewById(R.id.help_text);
        number = (EditText) findViewById(R.id.count_parameter_edit);
    }

    public void setInstructions(String i)
    {
        instructions.setText(i);
    }

    public void setParameterValue(int i)
    {
        number.setText(String.valueOf(i));
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that tourcount doesn't crash
    public int getParameterValue()
    {
        String text = number.getText().toString();
        if (isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }

    /**
     * Checks if a CharSequence is empty ("") or null
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
    public static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

}
