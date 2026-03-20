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
 * last edited on 2026-03-13.
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

        if (sysLanguage == "de" && dataLanguage == "de") nameSpec = "Deutscher Name"
        else if (sysLanguage == "en" && dataLanguage == "de") nameSpec = "German name"
        else if (sysLanguage == "fr" && dataLanguage == "de") nameSpec = "Nom allemand"
        else if (sysLanguage == "it" && dataLanguage == "de") nameSpec = "Nome tedesco"
        else if (sysLanguage == "es" && dataLanguage == "de") nameSpec = "Nombre alemán"

        else if (sysLanguage == "de" && dataLanguage == "en") nameSpec = "Englischer Name"
        else if (sysLanguage == "en" && dataLanguage == "en") nameSpec = "English name"
        else if (sysLanguage == "fr" && dataLanguage == "en") nameSpec = "Nom anglais"
        else if (sysLanguage == "it" && dataLanguage == "en") nameSpec = "Nome inglese"
        else if (sysLanguage == "es" && dataLanguage == "en") nameSpec = "Nombre inglés"

        else if (sysLanguage == "de" && dataLanguage == "fr") nameSpec = "Französischer Name"
        else if (sysLanguage == "en" && dataLanguage == "fr") nameSpec = "French name"
        else if (sysLanguage == "fr" && dataLanguage == "fr") nameSpec = "Nom français"
        else if (sysLanguage == "it" && dataLanguage == "fr") nameSpec = "Nome francese"
        else if (sysLanguage == "es" && dataLanguage == "fr") nameSpec = "Nombre francés"

        else if (sysLanguage == "de" && dataLanguage == "it") nameSpec = "Italienischer Name"
        else if (sysLanguage == "en" && dataLanguage == "it") nameSpec = "Italian name"
        else if (sysLanguage == "fr" && dataLanguage == "it") nameSpec = "Nom italien"
        else if (sysLanguage == "it" && dataLanguage == "it") nameSpec = "Nome italiano"
        else if (sysLanguage == "es" && dataLanguage == "it") nameSpec = "Nombre italiano"

        else if (sysLanguage == "de" && dataLanguage == "es") nameSpec = "Spanischer Name"
        else if (sysLanguage == "en" && dataLanguage == "es") nameSpec = "Spanish name"
        else if (sysLanguage == "fr" && dataLanguage == "es") nameSpec = "Nom espagnol"
        else if (sysLanguage == "it" && dataLanguage == "es") nameSpec = "Nome spagnolo"
        else if (sysLanguage == "es" && dataLanguage == "es") nameSpec = "Nombre español"

        return nameSpec
    }

}
