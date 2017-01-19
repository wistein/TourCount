package com.wmstein.filechooser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.wmstein.tourcount.R;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdvFileChooser lets you select files from sdcard directory.
 * It will be called within WelcomeActivity and uses FileArrayAdapter and Option.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Modifications by wmstein on 18.06.2016
 */

public class AdvFileChooser extends Activity
{
    private File currentDir;
    private FileArrayAdapter adapter;
    private FileFilter fileFilter;
    private ArrayList<String> extensions;
    private String filterFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            if (extras.getStringArrayList("filterFileExtension") != null)
            {
                extensions = extras.getStringArrayList("filterFileExtension");
                filterFileName = extras.getString("filterFileName");
                fileFilter = new FileFilter()
                {
                    @Override
                    public boolean accept(File pathname)
                    {
                        return
                            (
                                (pathname.getName().contains(".") &&
                                    pathname.getName().contains(filterFileName) &&
                                    extensions.contains(pathname.getName().substring(pathname.getName().lastIndexOf(".")))
                                )
                            );
                    }
                };
            }
        }

        // currentDir = new File ("/sdcard/")
        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
        fill(currentDir);
    }

    // Back-key return from filechooser
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    // List only files in /sdcard
    private void fill(File f)
    {
        File[] dirs;

        if (fileFilter != null)
            dirs = f.listFiles(fileFilter);
        else
            dirs = f.listFiles();

        this.setTitle(getString(R.string.currentDir) + ": " + f.getName());
        List<Option> fls = new ArrayList<>();
        @SuppressLint("SimpleDateFormat") 
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try
        {
            for (File ff : dirs)
            {
                if (!ff.isHidden())
                {
                    fls.add(new Option(ff.getName(), getString(R.string.fileSize) + ": "
                        + ff.length() + " B,  " + getString(R.string.date) + ": "
                        + dform.format(ff.lastModified()), ff.getAbsolutePath(), false, false, false));
                }
            }
        } catch (Exception e)
        {
            // do nothing
        }

        Collections.sort(fls);
        ListView listView = (ListView) findViewById(R.id.lvFiles);

        adapter = new FileArrayAdapter(listView.getContext(), R.layout.file_view, fls);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> l, View v, int position,
                                    long id)
            {
                // TODO Auto-generated method stub
                Option o = adapter.getItem(position);
                assert o != null;
                if (!o.isBack())
                    doSelect(o);
                else
                {
                    currentDir = new File(o.getPath());
                    fill(currentDir);
                }
            }

        });
    }

    private void doSelect(final Option o)
    {
        //onFileClick(o);
        File fileSelected = new File(o.getPath());
        Intent intent = new Intent();
        intent.putExtra("fileSelected", fileSelected.getAbsolutePath());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
