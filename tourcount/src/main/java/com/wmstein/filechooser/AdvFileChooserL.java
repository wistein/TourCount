package com.wmstein.filechooser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;

import com.wmstein.tourcount.R;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AdvFileChooserL lets you select files from sdcard directory.
 * It will be called within WelcomeActivity and uses FileArrayAdapter and Option.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18, last change on 2022-05-21
 */
public class AdvFileChooserL extends Activity
{
    private File currentDir;
    private FileArrayAdapter adapter;
    private FileFilter fileFilter;
    private ArrayList<String> extensions;
    private String filterFileName;

    @SuppressLint("SourceLockedOrientationActivity")
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
                fileFilter = pathname -> (
                    (pathname.getName().contains(".") &&
                        pathname.getName().contains(filterFileName) &&
                        extensions.contains(pathname.getName().substring(pathname.getName().lastIndexOf(".")))
                    )
                );
            }
        }

        //currentDir = /storage/emulated/0/Android/data/com.wmstein.tourcount/files
        currentDir = new File(Objects.requireNonNull(getApplicationContext().getExternalFilesDir(null)).getAbsolutePath());
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
            assert dirs != null;
            for (File ff : dirs)
            {
                if (!ff.isHidden())
                {
                    fls.add(new Option(ff.getName(), getString(R.string.fileSize) + ": "
                        + ff.length() + " B,  " + getString(R.string.date) + ": "
                        + dform.format(ff.lastModified()), ff.getAbsolutePath(), false));
                }
            }
        } catch (Exception e)
        {
            // do nothing
        }

        Collections.sort(fls);
        ListView listView = findViewById(R.id.lvFiles);

        adapter = new FileArrayAdapter(listView.getContext(), R.layout.file_view, fls);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((l, v, position, id) -> {
            Option o = adapter.getItem(position);
                assert o != null;
                if (!o.isBack())
                    doSelect(o);
                else
                {
                    currentDir = new File(o.getPath());
                    fill(currentDir);
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
