/*
 * Copyright (c) 2018. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.tourcount.AutoFitText;
import com.wmstein.tourcount.R;
import com.wmstein.tourcount.database.Count;

import java.util.Objects;

/**********************************
 * Interface for widget_counting_lhi.xml
 * used by CountingActivity
 * Created by wmstein 2016-12-18
 * modified for TourCount on 2018-03-31
 * last edited on 2021-01-26
 */
public class CountingWidgetLH extends RelativeLayout
{
    private final TextView namef1i;
    private final TextView namef2i;
    private final TextView namef3i;
    private final TextView namepi;
    private final TextView nameli;
    private final TextView nameei;
    private final AutoFitText countCountf1i; // section internal counters
    private final AutoFitText countCountf2i;
    private final AutoFitText countCountf3i;
    private final AutoFitText countCountpi;
    private final AutoFitText countCountli;
    private final AutoFitText countCountei;

    public Count count;

    public CountingWidgetLH(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_lhi, this, true);
        namef1i = findViewById(R.id.f1iNameLH);
        namef2i = findViewById(R.id.f2iNameLH);
        namef3i = findViewById(R.id.f3iNameLH);
        namepi = findViewById(R.id.piNameLH);
        nameli = findViewById(R.id.liNameLH);
        nameei = findViewById(R.id.eiNameLH);
        countCountf1i = findViewById(R.id.countCountLHf1i);
        countCountf2i = findViewById(R.id.countCountLHf2i);
        countCountf3i = findViewById(R.id.countCountLHf3i);
        countCountpi = findViewById(R.id.countCountLHpi);
        countCountli = findViewById(R.id.countCountLHli);
        countCountei = findViewById(R.id.countCountLHei);
    }

    public void setCount(Count newcount)
    {
        count = newcount;

        namef1i.setText(getContext().getString(R.string.countImagomfHint));
        namef2i.setText(getContext().getString(R.string.countImagomHint));
        namef3i.setText(getContext().getString(R.string.countImagofHint));
        namepi.setText(getContext().getString(R.string.countPupaHint));
        nameli.setText(getContext().getString(R.string.countLarvaHint));
        nameei.setText(getContext().getString(R.string.countOvoHint));
        countCountf1i.setText(String.valueOf(count.count_f1i));
        countCountf2i.setText(String.valueOf(count.count_f2i));
        countCountf3i.setText(String.valueOf(count.count_f3i));
        countCountpi.setText(String.valueOf(count.count_pi));
        countCountli.setText(String.valueOf(count.count_li));
        countCountei.setText(String.valueOf(count.count_ei));
        ImageButton countUpf1iButton = findViewById(R.id.buttonUpLHf1i);
        countUpf1iButton.setTag(count.id);
        ImageButton countUpf2iButton = findViewById(R.id.buttonUpLHf2i);
        countUpf2iButton.setTag(count.id);
        ImageButton countUpf3iButton = findViewById(R.id.buttonUpLHf3i);
        countUpf3iButton.setTag(count.id);
        ImageButton countUppiButton = findViewById(R.id.buttonUpLHpi);
        countUppiButton.setTag(count.id);
        ImageButton countUpliButton = findViewById(R.id.buttonUpLHli);
        countUpliButton.setTag(count.id);
        ImageButton countUpeiButton = findViewById(R.id.buttonUpLHei);
        countUpeiButton.setTag(count.id);
        ImageButton countDownf1iButton = findViewById(R.id.buttonDownLHf1i);
        ImageButton countDownf2iButton = findViewById(R.id.buttonDownLHf2i);
        ImageButton countDownf3iButton = findViewById(R.id.buttonDownLHf3i);
        ImageButton countDownpiButton = findViewById(R.id.buttonDownLHpi);
        ImageButton countDownliButton = findViewById(R.id.buttonDownLHli);
        ImageButton countDowneiButton = findViewById(R.id.buttonDownLHei);
        countDownf1iButton.setTag(count.id);
        countDownf2iButton.setTag(count.id);
        countDownf3iButton.setTag(count.id);
        countDownpiButton.setTag(count.id);
        countDownliButton.setTag(count.id);
        countDowneiButton.setTag(count.id);
    }

    // Count up/down and set value on lefthanded screen
    public void countUpLHf1i()
    {
        // increase count_f1i
        countCountf1i.setText(String.valueOf(count.increase_f1i()));
    }

    public void countDownLHf1i()
    {
        countCountf1i.setText(String.valueOf(count.safe_decrease_f1i()));
    }

    public void countUpLHf2i()
    {
        countCountf2i.setText(String.valueOf(count.increase_f2i()));
    }

    public void countDownLHf2i()
    {
        countCountf2i.setText(String.valueOf(count.safe_decrease_f2i()));
    }

    public void countUpLHf3i()
    {
        countCountf3i.setText(String.valueOf(count.increase_f3i()));
    }

    public void countDownLHf3i()
    {
        countCountf3i.setText(String.valueOf(count.safe_decrease_f3i()));
    }

    public void countUpLHpi()
    {
        countCountpi.setText(String.valueOf(count.increase_pi()));
    }

    public void countDownLHpi()
    {
        countCountpi.setText(String.valueOf(count.safe_decrease_pi()));
    }

    public void countUpLHli()
    {
        countCountli.setText(String.valueOf(count.increase_li()));
    }

    public void countDownLHli()
    {
        countCountli.setText(String.valueOf(count.safe_decrease_li()));
    }

    public void countUpLHei()
    {
        countCountei.setText(String.valueOf(count.increase_ei()));
    }

    public void countDownLHei()
    {
        countCountei.setText(String.valueOf(count.safe_decrease_ei()));
    }

}
