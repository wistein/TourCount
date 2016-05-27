package com.wmstein.tourcount;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import java.io.Serializable;

/**
 * Created by milo on 12/08/2014.
 * Changed by wmstein on 18.02.2016
 */
public class NewCount extends EditText implements Serializable
{
    public NewCount(Context context)
    {
        super(context);
    }

    public NewCount(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public NewCount(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

}
