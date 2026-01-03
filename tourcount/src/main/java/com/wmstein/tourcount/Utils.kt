package com.wmstein.tourcount

import android.text.Html
import android.text.Spanned

/***********************************************
 * object Utils cares for
 * - formatted Html in ShowTextDialog and Toasts
 *
 * Copyright 2016-2026 wmstein
 * Created by wmstein on 2017-09-25,
 * last modified in Java on 2018-06-13,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-01-02.
 */
internal object Utils {
    @JvmStatic
    fun fromHtml(source: String?): Spanned {
        return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    }
}

