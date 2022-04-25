/*
 * Copyright (c) 2018. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import androidx.annotation.NonNull;

/****************************************************
 * Interface for widget_counting_head1.xml
 * used by CountingActivity
 * Created by wmstein 2016-12-18
 * Last edited on 2022-04-25
 */
public class CountingWidget_head1 extends ArrayAdapter<String>
{
    private final String[] idArray;
    private final String[] contentArray1;
    private final String[] contentArray2;
    private final Integer[] imageArray;
    private final String[] contentArray3;

    public Count count;
    private final LayoutInflater inflater;

    public CountingWidget_head1(Context context, String[] idArray, String[] nameArray, String[] codeArray, Integer[] imageArray, String[] nameArrayG)
    {
        super(context, R.layout.widget_counting_head1, R.id.countName, nameArray);
        this.idArray = idArray;
        this.contentArray1 = nameArray;
        this.contentArray2 = codeArray;
        this.imageArray = imageArray;
        this.contentArray3 = nameArrayG;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
    {
        return getCustomView(position, parent);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        return getCustomView(position, parent);
    }

    private View getCustomView(int position, ViewGroup parent)
    {
        View head1 = inflater.inflate(R.layout.widget_counting_head1, parent, false);

        TextView countId = head1.findViewById(R.id.countId);
        countId.setText(idArray[position]);

        TextView countName = head1.findViewById(R.id.countName);
        countName.setText(contentArray1[position]);

        TextView countNameg = head1.findViewById(R.id.countNameg);
        countNameg.setText(contentArray3[position]);

        TextView countCode = head1.findViewById(R.id.countCode);
        countCode.setText(contentArray2[position]);

        ImageView pSpecies = head1.findViewById(R.id.pSpecies);
        pSpecies.setImageResource(imageArray[position]);

        return head1;
    }

}
