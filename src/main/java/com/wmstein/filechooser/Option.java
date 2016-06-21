package com.wmstein.filechooser;

/**
 * Option is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Modifications by wmstein on 18.06.2016
 */

public class Option implements Comparable<Option>
{
    private String name;
    private String data;
    private String path;
    private boolean folder;
    private boolean parent;
    private boolean back;

    public Option(String n, String d, String p, boolean folder, boolean parent, boolean back)
    {
        name = n;
        data = d;
        path = p;
        this.folder = folder;
        this.parent = parent;
        this.back = back;
    }

    public String getName()
    {
        return name;
    }

    public String getData()
    {
        return data;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public int compareTo(Option o)
    {
        if (this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }

    public boolean isFolder()
    {
        return folder;
    }

    public boolean isParent()
    {
        return parent;
    }

    public boolean isBack()
    {
        return back;
    }

}
