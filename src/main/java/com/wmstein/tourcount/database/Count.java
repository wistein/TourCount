package com.wmstein.tourcount.database;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

public class Count
{
    public int id;
    public int section_id;
    public int count;
    public String name;
    public String notes;

    public int increase()
    {
        count = count + 1;
        return count;
    }

    public int safe_decrease()
    {
        if (count > 0)
        {
            count = count - 1;
        }
        return count;
    }

    //setting C_ID
    // added by wmstein
    public void setC_ID (int C_id)
    {
        this.id = C_id;
    }
    //setting C_SECTION_ID
    // added by wmstein
    public void setC_SECTION_ID (int C_sectid)
    {
        this.section_id = C_sectid;
    }
    //setting C_COUNT
    // added by wmstein
    public void setC_COUNT (int C_count)
    {
        this.count = C_count;
    }
    //setting C_NAME
    // added by wmstein
    public void setC_NAME (String C_name)
    {
        this.name = C_name;
    }
    //setting C_NOTES
    // added by wmstein
    public void setC_NOTES (String C_notes)
    {
        this.notes = C_notes;
    }
    
}
