package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Individuals;

/**********************************
 * Created by wmstein on 2018-03-21
 * Last edited on 2019-01-27
 */
public class ListIndivRemWidget extends RelativeLayout
{
    private final TextView txtBemInd;

    public ListIndivRemWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.widget_list_indiv_rem, this, true);
        txtBemInd = findViewById(R.id.txtBemInd);
    }

    public void setRem(Individuals individuals)
    {
        txtBemInd.setText(individuals.notes);
    }

}
