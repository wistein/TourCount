package com.wmstein.tourcount;

/*
  This code from:
  http://pastebin.com/raw.php?i=e6WyrwSN
  As mentioned in this thread:
  https://stackoverflow.com/questions/16017165/auto-fit-textview-for-android
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;
//import android.widget.RelativeLayout.LayoutParams;

/**
 * This class builds a new android Widget named AutoFitText which can be used instead of a TextView
 * to have the text font size in it automatically fit to match the screen width. Credits go largely
 * to Dunni, gjpc, gregm and speedplane from Stackoverflow, method has been (style-) optimized and
 * rewritten to match android coding standards and our MBC. This version upgrades the original
 * "AutoFitTextView" to now also be adaptable to height and to accept the different TextView types
 * (Button, TextClock etc.)
 *
 * @author pheuschk
 * createDate: 18.04.2013
 * 
 * Modified by wmstein on 18.03.2016
 * Bug fixed (height of single character) and context comments changed 
 */
@SuppressWarnings("unused")
public class AutoFitText extends TextView
{
    private static String TAG = "tourcountAutoFitText";
    /**
     * Global min and max for text size. Remember: values are in pixels!
     */
    final int MIN_TEXT_SIZE = 8;   // was 10
    final int MAX_TEXT_SIZE = 100; // was 400, best: 80?

    /**
     * A dummy {@link TextView} to test the text size without actually showing anything to the user
     */
    private TextView mTestView;

    /**
     * A dummy {@link Paint} to test the text size without actually showing anything to the user
     */
    Paint mTestPaint;

    /**
     * Scaling factor for fonts. It's a method of calculating independently (!) from the actual
     * density of the screen that is used so users have the same experience on different devices. We
     * will use DisplayMetrics in the Constructor to get the value of the factor and then calculate
     * SP from pixel values
     */
    private final float mScaledDensityFactor;

    /**
     * Defines how close we want to be to the factual size of the Text-field. Lower values mean
     * higher precision but also exponentially higher computing cost (more loop runs)
     */
    final float mThreshold = 0.5f; // original was 0.5f

    /**
     * Constructor for call without attributes --> invoke constructor with AttributeSet null
     */
    public AutoFitText(Context context)
    {
        this(context, null);
    }

    @SuppressLint("NewApi")
    public AutoFitText(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mScaledDensityFactor = context.getResources().getDisplayMetrics().scaledDensity;
        mTestView = new TextView(context);

        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());

        this.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                // make an initial call to onSizeChanged to make sure that refitText is triggered
                onSizeChanged(AutoFitText.this.getWidth(), AutoFitText.this.getHeight(), 0, 0);
                
                // Remove the LayoutListener immediately so we don't run into an infinite loop
                AutoFitText.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * Main method of this widget. Resizes the font so the specified text fits in the text box
     * assuming the text box has the specified width. This is done via a dummy text view that is
     * refit until it matches the real target width and height up to a certain threshold factor
     *
     * @param targetFieldWidth  The width that the TextView currently has and wants filled
     * @param targetFieldHeight The height that the TextView currently has and wants filled
     */
    private void refitText(String text, int targetFieldWidth, int targetFieldHeight)
    {
        // Variables need to be visible outside the loops for later use. Remember size is in pixels
        float lowerTextSize = MIN_TEXT_SIZE;
        float upperTextSize = MAX_TEXT_SIZE;

        // Force the text to wrap. 
        this.setMaxWidth(targetFieldWidth);
        // added by wmstein to shrink if single character would be too high
        this.setMaxHeight(targetFieldHeight);

        // Padding should not be an issue since we never define it programmatically in this app
        // but just to be sure we cut it off here
        targetFieldWidth = targetFieldWidth - this.getPaddingLeft() - this.getPaddingRight();
        targetFieldHeight = targetFieldHeight - this.getPaddingTop() - this.getPaddingBottom();

        // Initialize the dummy with some params (that are largely ignored anyway, but this is
        // mandatory to not get a NullPointerException)
        mTestView.setLayoutParams(new LayoutParams(targetFieldWidth, targetFieldHeight));

        // maxWidth is crucial! Otherwise the text would never line wrap but blow up the width
        mTestView.setMaxWidth(targetFieldWidth);
        // added by wmstein to control height
        mTestView.setMaxHeight(targetFieldHeight);

        /*************************** Converging algorithm 1 ***********************************/
        // only a single line
        for (float testSize; (upperTextSize - lowerTextSize) > mThreshold; )
        {

            // Go to the mean value...
            testSize = (upperTextSize + lowerTextSize) / 2;

            mTestView.setTextSize(TypedValue.COMPLEX_UNIT_SP, testSize / mScaledDensityFactor);
            mTestView.setText(text);
            mTestView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            // added if (... || height criterium)
            if (mTestView.getMeasuredWidth() >= targetFieldWidth || mTestView.getMeasuredHeight() >= targetFieldHeight)
            {
                upperTextSize = testSize; // Font is too big, decrease upperSize
            }
            else
            {
                lowerTextSize = testSize; // Font is too small, increase lowerSize
            }
        }

        /*************
         * skipped a lot of unused code here
         */
        
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, lowerTextSize / mScaledDensityFactor);
        //next unnecessary line of void method skipped by wmstein
        //return;
    }

    /**
     * This method receives a call upon a change in text content of the TextView. Unfortunately it
     * is also called - among others - upon text size change which means that we MUST NEVER CALL
     * {@link "#refitText"(String)} from this method! Doing so would result in an endless loop that
     * would ultimately result in a stack overflow and termination of the application
     * <p/>
     * So for the time being this method does absolutely nothing. If you want to notify the view of
     * a changed text call {@link #setText(CharSequence)}
     */
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
    {
        // Super implementation is also intentionally empty so for now we do absolutely nothing here
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        if (width != oldWidth && height != oldHeight)
        {
            refitText(this.getText().toString(), width, height);
        }
    }

    /**
     * This method is guaranteed to be called by {@link TextView#setText(CharSequence)} immediately.
     * Therefore we can safely add our modifications here and then have the parent class resume its
     * work. So if text has changed you should always call {@link TextView#setText(CharSequence)} or
     * {@link TextView#setText(CharSequence, BufferType)} if you know whether the {@link BufferType}
     * is normal, editable or spannable. Note: the method will default to {@link BufferType#NORMAL}
     * if you don't pass an argument.
     */
    @Override
    public void setText(CharSequence text, BufferType type)
    {
        int targetFieldWidth = this.getWidth();
        int targetFieldHeight = this.getHeight();

        if (targetFieldWidth <= 0 || targetFieldHeight <= 0 || text.equals(""))
        {
            // Log.v("tag", "Some values are empty, AutoFitText was not able to construct properly");
        }
        else
        {
            refitText(text.toString(), targetFieldWidth, targetFieldHeight);
        }
        super.setText(text, type);
    }

}
