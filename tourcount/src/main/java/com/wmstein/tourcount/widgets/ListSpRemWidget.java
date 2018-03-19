package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

/**********************************
 * Created by wmstein on 24.02.2018
 */
public class ListSpRemWidget extends RelativeLayout
{
    private final TextView txtSpecRem;
    private String txtRem ="";

    public ListSpRemWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_sp_rem, this, true);
        txtSpecRem = (TextView) findViewById(R.id.txtSpecRem);
    }

    public void setRem(Count spec)
    {
        txtSpecRem.setText(spec.notes);
    }

}
