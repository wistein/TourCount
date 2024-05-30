package com.wmstein.tourcount

import android.content.Context
import android.content.res.Resources
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.SparseIntArray
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText
import java.util.Objects
import kotlin.math.roundToInt

/******************************************
 * AutoFitEditText is used in CountOptionsActivity to fit in larger count numbers
 *
 * Created by varsovski on 29-Oct-15, published on
 * https://github.com/viksaaskool/autofitedittext,
 * licensed under MIT License by Viktor Arsovski.
 *
 * Modified for TransektCount by wm.stein on 2023-09-18,
 * last edited in Java on 2023-09-18
 * converted to Kotlin on 2023-09-19
 * last edited on 2024-05-14.
 */
class AutoFitEditText @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatEditText(
    context!!, attrs, defStyle
) {
    private val availableSpaceRect = RectF()
    private val textCachedSizes = SparseIntArray()
    private val _sizeTester: SizeTester
    private var maxTextSize: Float
    private var spacingMult = 1.0f
    private var spacingAdd = 0.0f
    private val minTextSize: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        12f, resources.displayMetrics
    )
    private var widthLimit = 0
    private val _maxLines: Int
    private var initiallized = false
    private var paint: TextPaint? = null

    private interface SizeTester {
        /**
         * AutoFitEditText
         *
         * @param suggestedSize  Size of text to be tested
         * @param availableSpace available space in which text must fit
         * @return an integer < 0 if after applying `suggestedSize` to
         * text, it takes less space than `availableSpace`, > 0
         * otherwise
         */
        fun onTestSize(suggestedSize: Int, availableSpace: RectF): Int
    }

    init {
        // using the minimal recommended font size
        maxTextSize = textSize
        _maxLines = 1
        // prepare size tester:
        _sizeTester = object : SizeTester {
            val textRect = RectF()
            override fun onTestSize(
                suggestedSize: Int,
                availableSpace: RectF
            ): Int {
                paint!!.textSize = suggestedSize.toFloat()
                val text = Objects.requireNonNull(text).toString()
                val singleline = maxLines == 1
                if (singleline) {
                    textRect.bottom = paint!!.fontSpacing
                    textRect.right = paint!!.measureText(text)
                } else {
                    val sb = StaticLayout.Builder.obtain(text, 0, text.length, paint!!, widthLimit)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(spacingAdd, spacingMult)
                        .setIncludePad(true)
                    val layout = sb.build()
                    if (maxLines != 1
                        && layout.lineCount > maxLines
                    ) return 1
                    textRect.bottom = layout.height.toFloat()
                    var maxWidth = -1
                    for (i in 0 until layout.lineCount) if (maxWidth < layout.getLineWidth(i)) maxWidth =
                        layout.getLineWidth(i).toInt()
                    textRect.right = maxWidth.toFloat()
                }
                textRect.offsetTo(0f, 0f)
                return if (availableSpace.contains(textRect)) -1 else 1
                // else, too big
            }
        }
        initiallized = true
    }

    override fun setTypeface(tf: Typeface?) {
        if (paint == null) paint = TextPaint(getPaint())
        paint!!.typeface = tf
        super.setTypeface(tf)
    }

    override fun setTextSize(size: Float) {
        maxTextSize = size
        textCachedSizes.clear()
        adjustTextSize()
    }

    override fun getMaxLines(): Int {
        return _maxLines
    }

    override fun setTextSize(unit: Int, size: Float) {
        val c = context
        val r: Resources = if (c == null) Resources.getSystem() else c.resources
        maxTextSize = TypedValue.applyDimension(
            unit, size,
            r.displayMetrics
        )
        textCachedSizes.clear()
        adjustTextSize()
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        super.setLineSpacing(add, mult)
        spacingMult = mult
        spacingAdd = add
    }

    private fun reAdjust() {
        adjustTextSize()
    }

    private fun adjustTextSize() {
        if (!initiallized) return

        val startSize = minTextSize.roundToInt()
        val heightLimit = (measuredHeight - compoundPaddingBottom - compoundPaddingTop)
        widthLimit = (measuredWidth - compoundPaddingLeft - compoundPaddingRight)
        if (widthLimit <= 0) return

        availableSpaceRect.right = widthLimit.toFloat()
        availableSpaceRect.bottom = heightLimit.toFloat()
        super.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            efficientTextSizeSearch(
                startSize, this.maxTextSize.toInt(),
                _sizeTester, availableSpaceRect
            ).toFloat()
        )
    }

    private fun efficientTextSizeSearch(
        start: Int, end: Int,
        sizeTester: SizeTester, availableSpace: RectF
    ): Int {
        return binarySearch(start, end, sizeTester, availableSpace)
    }

    private fun binarySearch(
        start: Int, end: Int,
        sizeTester: SizeTester, availableSpace: RectF
    ): Int {
        var lastBest = start
        var lo = start
        var hi = end - 1
        var mid: Int
        while (lo <= hi) {
            mid = lo + hi ushr 1
            val midValCmp = sizeTester.onTestSize(mid, availableSpace)
            if (midValCmp < 0) {
                lastBest = lo
                lo = mid + 1
            } else if (midValCmp > 0) {
                hi = mid - 1
                lastBest = hi
            } else return mid
        }
        return lastBest
    }

    override fun onTextChanged(
        text: CharSequence, start: Int,
        before: Int, after: Int
    ) {
        super.onTextChanged(text, start, before, after)
        reAdjust()
    }

    override fun onSizeChanged(
        width: Int, height: Int,
        oldwidth: Int, oldheight: Int
    ) {
        textCachedSizes.clear()
        super.onSizeChanged(width, height, oldwidth, oldheight)
        if (width != oldwidth || height != oldheight) reAdjust()
    }
}