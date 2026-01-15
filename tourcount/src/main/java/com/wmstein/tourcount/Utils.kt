package com.wmstein.tourcount

import androidx.core.text.HtmlCompat
import android.text.Spanned

/***************************************************
 * fromHtml cares for Android versions compatibility
 *   in Toasts and text dialogs with Html formatting
 *
 * Created by wistein on 2017-09-25,
 * last modified in Java on 2018-06-13,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-01-15.
 */
internal object Utils {
    @JvmStatic
    fun fromHtml(source: String?): Spanned {
        return HtmlCompat.fromHtml(source!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

}
