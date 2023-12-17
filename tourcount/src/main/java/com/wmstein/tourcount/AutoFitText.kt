package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView

/**************************************************************************************************
 * This class builds a new android Widget named AutoFitText which can be used instead of a TextView
 * to have the text font size in it automatically fit to match the screen width. Credits go largely
 * to Dunni, gjpc, gregm and speedplane from Stackoverflow, method has been (style-) optimized and
 * rewritten to match android coding standards and our MBC.
 *
 * This version upgrades the original "AutoFitTextView" to now also be adaptable to height and to
 * accept the different TextView types (Button, TextClock etc.)
 *
 * @author pheuschk
 * createDate: 18.04.2013
 *
 * Modified for TourCount by wmstein since 2016-03-18
 * Bug fixed (height of single character), cleaned of unused code and context comments changed,
 * last edited in Java on 2023-05-24,
 * converted to Kotlin on 2023-07-06
 * last edited on 2023-12-15,
 */
class AutoFitText @SuppressLint("NewApi") constructor(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {
    /**
     * Scaling factor for fonts. It's a method of calculating independently (!) from the actual
     * density of the screen that is used so users have the same experience on different devices. We
     * will use DisplayMetrics in the Constructor to get the value of the factor and then calculate
     * SP from pixel values
     */
    private val mScaledDensityFactor: Float

    /**
     * A dummy [Paint] to test the text size without actually showing anything to the user
     */
    private val mTestPaint: Paint

    /**
     * A dummy [TextView] to test the text size without actually showing anything to the user
     */
    private val mTestView: TextView

    /**
     * Constructor for call without attributes --> invoke constructor with AttributeSet null
     */
    constructor(context: Context) : this(context, null)

    init {
        mScaledDensityFactor = context.resources.displayMetrics.density
        mTestView = TextView(context)
        mTestPaint = Paint()
        mTestPaint.set(this.paint)
        this.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // make an initial call to onSizeChanged to make sure that refitText is triggered
                onSizeChanged(this@AutoFitText.width, this@AutoFitText.height, 0, 0)

                // Remove the LayoutListener immediately so we don't run into an infinite loop
                this@AutoFitText.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * Main method of this widget. Resizes the font so the specified text fits in the text box
     * assuming the text box has the specified width. This is done via a dummy text view that is
     * refit until it matches the real target width and height up to a certain threshold factor
     *
     * @param targetFieldW  The width that the TextView currently has and wants filled
     * @param targetFieldH The height that the TextView currently has and wants filled
     */
    private fun refitText(text: String, targetFieldW: Int, targetFieldH: Int) {
        // Variables need to be visible outside the loops for later use. Remember size is in pixels
        var targetFieldWidth = targetFieldW
        var targetFieldHeight = targetFieldH
        var lowerTextSize = MIN_TEXT_SIZE.toFloat()
        var upperTextSize = MAX_TEXT_SIZE.toFloat()

        // Force the text to wrap. 
        this.maxWidth = targetFieldWidth
        // added by wmstein to shrink if single character would be too high
        this.maxHeight = targetFieldHeight

        // Padding should not be an issue since we never define it programmatically in this app
        // but just to be sure we cut it off here
        targetFieldWidth = targetFieldWidth - this.paddingLeft - this.paddingRight
        targetFieldHeight = targetFieldHeight - this.paddingTop - this.paddingBottom

        // Initialize the dummy with some params (that are largely ignored anyway, but this is
        // mandatory to not get a NullPointerException)
        mTestView.layoutParams = ViewGroup.LayoutParams(targetFieldWidth, targetFieldHeight)

        // maxWidth is crucial! Otherwise the text would never line wrap but blow up the width
        mTestView.maxWidth = targetFieldWidth
        // added by wmstein to control height
        mTestView.maxHeight = targetFieldHeight

        // Converging only a single line
        var testSize: Float
        while (upperTextSize - lowerTextSize > mThreshold) {

            // Go to the mean value...
            testSize = (upperTextSize + lowerTextSize) / 2
            mTestView.setTextSize(TypedValue.COMPLEX_UNIT_SP, testSize / mScaledDensityFactor)
            mTestView.text = text
            mTestView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

            // added if (... || height criterium)
            if (mTestView.measuredWidth >= targetFieldWidth || mTestView.measuredHeight >= targetFieldHeight) {
                upperTextSize = testSize // Font is too big, decrease upperSize
            } else {
                lowerTextSize = testSize // Font is too small, increase lowerSize
            }
        }
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, lowerTextSize / mScaledDensityFactor)
        //next unnecessary line of void method skipped by wmstein
        //return;
    }

    /**
     * This method receives a call upon a change in text content of the TextView. Unfortunately it
     * is also called - among others - upon text size change which means that we MUST NEVER CALL
     * []"">&quot;#refitText&quot;(String) from this method! Doing so would result in an endless loop that
     * would ultimately result in a stack overflow and termination of the application
     *
     * So for the time being this method does absolutely nothing. If you want to notify the view of
     * a changed text call [.setText]
     */
    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        // Super implementation is also intentionally empty so for now we do absolutely nothing here
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        if (width != oldWidth && height != oldHeight) {
            refitText(this.text.toString(), width, height)
        }
    }

    /**
     * This method is guaranteed to be called by [TextView.setText] immediately.
     * Therefore we can safely add our modifications here and then have the parent class resume its
     * work. So if text has changed you should always call [TextView.setText].
     */
    override fun setText(text: CharSequence, type: BufferType) {
        val targetFieldWidth = this.width
        val targetFieldHeight = this.height
        if (targetFieldWidth <= 0 || targetFieldHeight <= 0 || text == "") {
            if (MyDebug.LOG) Log.d(
                "AutoFitText",
                "164, Some values are empty, AutoFitText was not able to construct properly"
            )
        } else {
            refitText(text.toString(), targetFieldWidth, targetFieldHeight)
        }
        super.setText(text, type)
    }

    companion object {
        // Global min and max for text size. Remember: values are in pixels!
        const val MIN_TEXT_SIZE = 8 // was 10
        const val MAX_TEXT_SIZE = 100 // was 400, best: 80?

        /**
         * Defines how close we want to be to the factual size of the Text-field. Lower values mean
         * higher precision but also exponentially higher computing cost (more loop runs)
         */
        const val mThreshold = 0.5f // original was 0.5f
    }

}
