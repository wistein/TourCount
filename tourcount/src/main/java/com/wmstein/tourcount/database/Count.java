package com.wmstein.tourcount.database;

/*************************************************
 * Definitions and basic functions for table Count
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-02-18, 
 * last change on 2022-03-23
 */
public class Count
{
    public int id;
    public int count_f1i;
    public int count_f2i;
    public int count_f3i;
    public int count_pi;
    public int count_li;
    public int count_ei;
    public String name;
    public String code;
    public String notes;
    public String name_g;

    public int increase_f1i()
    {
        count_f1i = count_f1i + 1;
        return count_f1i;
    }

    public int increase_f2i()
    {
        count_f2i = count_f2i + 1;
        return count_f2i;
    }

    public int increase_f3i()
    {
        count_f3i = count_f3i + 1;
        return count_f3i;
    }

    public int increase_pi()
    {
        count_pi = count_pi + 1;
        return count_pi;
    }

    public int increase_li()
    {
        count_li = count_li + 1;
        return count_li;
    }

    public int increase_ei()
    {
        count_ei = count_ei + 1;
        return count_ei;
    }

    // decreases
    public int safe_decrease_f1i()
    {
        if (count_f1i > 0)
        {
            count_f1i = count_f1i - 1;
        }
        return count_f1i;
    }

    public int safe_decrease_f2i()
    {
        if (count_f2i > 0)
        {
            count_f2i = count_f2i - 1;
        }
        return count_f2i;
    }

    public int safe_decrease_f3i()
    {
        if (count_f3i > 0)
        {
            count_f3i = count_f3i - 1;
        }
        return count_f3i;
    }

    public int safe_decrease_pi()
    {
        if (count_pi > 0)
        {
            count_pi = count_pi - 1;
        }
        return count_pi;
    }

    public int safe_decrease_li()
    {
        if (count_li > 0)
        {
            count_li = count_li - 1;
        }
        return count_li;
    }

    public int safe_decrease_ei()
    {
        if (count_ei > 0)
        {
            count_ei = count_ei - 1;
        }
        return count_ei;
    }

}
