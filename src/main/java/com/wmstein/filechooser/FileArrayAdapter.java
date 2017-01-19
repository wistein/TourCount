package com.wmstein.filechooser;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wmstein.tourcount.R;

import java.util.List;

/**
 * FileArrayAdapter is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Modifications by wmstein on 18.06.2016
 */

class FileArrayAdapter extends ArrayAdapter<Option>
{

    private final Context c;
    private final int id;
    private final List<Option> items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<Option> objects)
    {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }

    public Option getItem(int i)
    {
        return items.get(i);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            LayoutInflater vi = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }
        final Option o = items.get(position);
        if (o != null)
        {
            ImageView im = (ImageView) v.findViewById(R.id.img1);
            TextView t1 = (TextView) v.findViewById(R.id.TextView01);
            TextView t2 = (TextView) v.findViewById(R.id.TextView02);

            String name = o.getName().toLowerCase();
            if (name.endsWith(".db"))
                im.setImageResource(R.drawable.db);
            else
                im.setImageResource(R.drawable.whitepage);

            if (t1 != null)
                t1.setText(o.getName());
            if (t2 != null)
                t2.setText(o.getData());
        }
        return v;
    }

}
