package com.wmstein.tourcount

import java.io.Closeable
import java.io.Flushable
import java.io.IOException
import java.io.PrintWriter
import java.io.Writer

/*******************************************************************************
 * Based on "A very simple CSV writer" by
 * @author Glen Smith
 * Copyright 2015 Bytecode Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * [...](https://www.apache.org/licenses/LICENSE-2.0)
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Code is extracted from the OpenCSV library and reduced to needed functions
 * and modifications for TourCount by wmstein on 2016-05-27.
 *
 * Input fields should provide leading and trailing "\"" to mark them as String.
 *
 * Last edited in Java on 2023-06-17,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2026-02-23
 */
internal class CSVWriter private constructor(
    private val rawWriter: Writer,
    // separator is the delimiter to use for separating entries
    private val separator: Char = ',',
) : Closeable, Flushable {
    private val pw: PrintWriter = PrintWriter(rawWriter)
    private val newLine: String = "\n"
    private val carriageReturn: String = "\r"

    constructor(writer: Writer) : this(writer, ',')

    /**
     * Writes the next line to the file.
     * @param nextLine - string array with each comma-separated element as a separate entry.
     */
    fun writeNext(nextLine: Array<String?>?) {
        if (nextLine == null) {
            return
        }
        val sb =
            StringBuilder(nextLine.size * 2) // The worse case where all elements have to be escaped.
        for (i in nextLine.indices) {
            if (i != 0) {
                sb.append(separator)
            }
            val nextElement = nextLine[i] ?: continue
            val stringContainsSpecialCharacters = stringContainsSpecialCharacters(nextElement)

            // Append nextElement
            if (stringContainsSpecialCharacters) {
                sb.append(processLine(nextElement))
            } else {
                sb.append(nextElement)
            }
        }
        sb.append(newLine)
        pw.write(sb.toString())
    }

    /**
     * Checks to see if the line contains special characters.
     * @param line - returns true if the line contains the quote, escape, separator, newline or return.
     */
    private fun stringContainsSpecialCharacters(line: String): Boolean {
        return line.indexOf(separator) != -1      // ,
                || line.contains(newLine)        // \n
                || line.contains(carriageReturn) // \r
    }

    /**
     * Processes all the characters in a line.
     * @param nextElement - element to process.
     * Returns a StringBuilder with the elements' data.
     */
    private fun processLine(nextElement: String): StringBuilder {
        // This is for the worse case where all elements have to be escaped.
        val sb = StringBuilder(nextElement.length * 2)
        for (element in nextElement) {
            processCharacter(sb, element)
        }
        return sb
    }

    /**
     * Appends the character to the StringBuilder.
     * @param sb       - StringBuffer holding the processed character.
     * @param nextChar - character to process
     */
    // Modified version processCharacter
    private fun processCharacter(sb: StringBuilder, nextChar: Char) {
        sb.append(nextChar)
    }

    /**
     * Flush underlying stream to writer.
     */
    override fun flush() {
        pw.flush()
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     * @throws IOException if bad things happen
     */
    @Throws(IOException::class)
    override fun close() {
        flush()
        pw.close()
        rawWriter.close()
    }

}
