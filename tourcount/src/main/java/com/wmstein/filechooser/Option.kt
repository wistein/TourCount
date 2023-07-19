package com.wmstein.filechooser

import java.util.Locale

/**
 * Option is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Modifications by wmstein on 2020-04-17,
 * converted to Kotlin on 2023-07-05
 */
class Option(val name: String?, val data: String, val path: String, val isBack: Boolean) :
    Comparable<Option> {

    override fun compareTo(other: Option): Int {
        return name?.lowercase(Locale.getDefault())?.compareTo(other.name!!.lowercase(Locale.getDefault()))
            ?: throw IllegalArgumentException()
    }
}