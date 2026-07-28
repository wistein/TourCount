package com.wmstein.tourcount

import android.text.Spanned

import androidx.core.text.HtmlCompat

import java.util.Locale

/***************************************************
 * Utils has string functions
 *
 * - fromHtml() cares for Android versions compatibility
 *   in Toasts and text dialogs with HTML formatting
 *
 * - nameSpecG sets the system language name titel for the data language
 *
 * Created by wmstein on 2017-09-25,
 * last modified in Java on 2018-06-13,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-07-21.
 */
internal object Utils {
    @JvmStatic
    fun fromHtml(source: String?): Spanned {
        return HtmlCompat.fromHtml(source!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    @JvmStatic
    fun nameSpecG(dataLanguage: String): String {
        var nameSpec = "Name"
        val sysLanguage = Locale.getDefault().toString().substring(0, 2)

        when (sysLanguage) {
            "de" if dataLanguage == "de" -> nameSpec = "Deutscher Name"
            "en" if dataLanguage == "de" -> nameSpec = "German name"
            "fr" if dataLanguage == "de" -> nameSpec = "Nom allemand"
            "it" if dataLanguage == "de" -> nameSpec = "Nome tedesco"
            "es" if dataLanguage == "de" -> nameSpec = "Nombre alemán"

            "de" if dataLanguage == "en" -> nameSpec = "Englischer Name"
            "en" if dataLanguage == "en" -> nameSpec = "English name"
            "fr" if dataLanguage == "en" -> nameSpec = "Nom anglais"
            "it" if dataLanguage == "en" -> nameSpec = "Nome inglese"
            "es" if dataLanguage == "en" -> nameSpec = "Nombre inglés"

            "de" if dataLanguage == "fr" -> nameSpec = "Französischer Name"
            "en" if dataLanguage == "fr" -> nameSpec = "French name"
            "fr" if dataLanguage == "fr" -> nameSpec = "Nom français"
            "it" if dataLanguage == "fr" -> nameSpec = "Nome francese"
            "es" if dataLanguage == "fr" -> nameSpec = "Nombre francés"

            "de" if dataLanguage == "it" -> nameSpec = "Italienischer Name"
            "en" if dataLanguage == "it" -> nameSpec = "Italian name"
            "fr" if dataLanguage == "it" -> nameSpec = "Nom italien"
            "it" if dataLanguage == "it" -> nameSpec = "Nome italiano"
            "es" if dataLanguage == "it" -> nameSpec = "Nombre italiano"

            "de" if dataLanguage == "es" -> nameSpec = "Spanischer Name"
            "en" if dataLanguage == "es" -> nameSpec = "Spanish name"
            "fr" if dataLanguage == "es" -> nameSpec = "Nom espagnol"
            "it" if dataLanguage == "es" -> nameSpec = "Nome spagnolo"
            "es" if dataLanguage == "es" -> nameSpec = "Nombre español"
        }
        return nameSpec
    }

}
