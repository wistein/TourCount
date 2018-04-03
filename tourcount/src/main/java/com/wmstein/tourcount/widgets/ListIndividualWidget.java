package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Individuals;

/**********************************
 * Created by wmstein on 2018-02-22
 * Last edited on 2018-03-31
 */
public class ListIndividualWidget extends RelativeLayout
{
    private final TextView txtIndLoc;
    private final TextView txtIndSex;
    private final TextView txtIndStad;
    private final TextView txtIndLa;
    private final TextView txtIndLo;
    private final TextView txtIndHt;
    private final TextView txtIndStat;
    private final TextView txtIndCnt;

    public ListIndividualWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_individual, this, true);
        txtIndLoc = findViewById(R.id.txtIndLoc);
        txtIndSex = findViewById(R.id.txtIndSex);
        txtIndStad = findViewById(R.id.txtIndStad);
        txtIndLa = findViewById(R.id.txtIndLa);
        txtIndLo = findViewById(R.id.txtIndLo);
        txtIndHt = findViewById(R.id.txtIndHt);
        txtIndStat = findViewById(R.id.txtIndStat);
        txtIndCnt = findViewById(R.id.txtIndCnt);
    }

    public void setIndividual(Individuals individual)
    {
        txtIndLoc.setText(individual.locality);
        txtIndSex.setText(individual.sex);
        if (individual.stadium.length() > 0)
            txtIndStad.setText(individual.stadium.substring(0, 1));
        else
            txtIndStad.setText("");

        int slen;
        if (individual.coord_x == 0)
        {
            txtIndLa.setText("");
        }
        else
        {
            slen = String.valueOf(individual.coord_x).length();
            if (slen > 8)
            {
                txtIndLa.setText(String.valueOf(individual.coord_x).substring(0, 8));
            }
            else
            {
                txtIndLa.setText(String.valueOf(individual.coord_x));
            }
        }
        if (individual.coord_y == 0)
        {
            txtIndLo.setText("");
        }
        else
        {
            slen = String.valueOf(individual.coord_y).length();
            if (slen > 8)
            {
                txtIndLo.setText(String.valueOf(individual.coord_y).substring(0, 8));
            }
            else
            {
                txtIndLo.setText(String.valueOf(individual.coord_y));
            }
        }
        if (individual.coord_z == 0)
        {
            txtIndHt.setText("");
        }
        else
        {
            txtIndHt.setText(String.format("%s m", String.valueOf(Math.round(individual.coord_z))));
        }
        txtIndStat.setText(String.valueOf(individual.state_1_6));
        txtIndCnt.setText(String.valueOf(individual.icount));
    }

    public String getIndNotes(Individuals individual)
    {
        return individual.notes;
    }
}
