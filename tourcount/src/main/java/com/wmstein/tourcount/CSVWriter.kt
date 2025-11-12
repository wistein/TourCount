package com.wmstein.tourcount

import java.io.Closeable
import java.io.Flushable
import java.io.IOException
import java.io.PrintWriter
import java.io.Writer

/*************************************************************************
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
 * Based on "A very simple CSV writer" by
 * @author Glen Smith
 *
 * Reduced to needed functions with modifications for TourCount by wmstein.
 * Last edited in Java on 2023-06-17,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-11-01
 */
internal class CSVWriter private constructor(
    // rawWriter is the writer to an underlying CSV source
    private val rawWriter: Writer,
    // ,
    private val separator: Char = DEFAULT_SEPARATOR,
    // "
    private val quoteChar: Char = DEFAULT_QUOTE_CHARACTER,
    // "
    private val escapeChar: Char = DEFAULT_ESCAPE_CHARACTER,
    // \n
    private val lineEnd: String = DEFAULT_LINE_END,
) : Closeable, Flushable {
    private val pw: PrintWriter = PrintWriter(rawWriter)

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param separator  the delimiter to use for separating entries
     * @param quoteChar  the character to use for quoted elements
     * @param escapeChar the character to use for escaping quotechars or escapechars
     */
    constructor(writer: Writer) : this(writer, DEFAULT_SEPARATOR)

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

            if (stringContainsSpecialCharacters && quoteChar != NO_QUOTE_CHARACTER) {
                sb.append(quoteChar)
            }
            if (stringContainsSpecialCharacters) {
                sb.append(processLine(nextElement))
            } else {
                sb.append(nextElement)
            }
            if (stringContainsSpecialCharacters && quoteChar != NO_QUOTE_CHARACTER) {
                sb.append(quoteChar)
            }
        }
        sb.append(lineEnd)
        pw.write(sb.toString())
    }

    /**
     * Checks to see if the line contains special characters.
     * @param line - returns true if the line contains the quote, escape, separator, newline or return.
     */
    private fun stringContainsSpecialCharacters(line: String): Boolean {
        return line.indexOf(quoteChar) != -1                // "
                || line.indexOf(escapeChar) != -1           // "
                || line.indexOf(separator) != -1     // ,
                || line.contains(DEFAULT_LINE_END)  // \n
                || line.contains("\r")              // \r
    }

    /**
     * Processes all the characters in a line.
     * @param nextElement - element to process.
     * Returns a StringBuilder with the elements data.
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
     * Appends the character to the StringBuilder adding the escape character if needed.
     * @param sb       - StringBuffer holding the processed character.
     * @param nextChar - character to process
     */
    private fun processCharacter(sb: StringBuilder, nextChar: Char) {
        if (escapeChar != NO_ESCAPE_CHARACTER && checkCharactersToEscape(nextChar)) {
            sb.append(escapeChar).append(nextChar)
        } else {
            sb.append(nextChar)
        }
    }

    private fun checkCharactersToEscape(nextChar: Char): Boolean {
        return if (quoteChar == NO_QUOTE_CHARACTER)
            nextChar == quoteChar || nextChar == escapeChar || nextChar == separator
        else nextChar == quoteChar || nextChar == escapeChar
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

    companion object {
        //The default quote character to use if none is supplied to the constructor.
        private const val DEFAULT_QUOTE_CHARACTER = '"'

        //The character used for escaping quotes.
        private const val DEFAULT_ESCAPE_CHARACTER = '"'

        //The default separator to use if none is supplied to the constructor.
        private const val DEFAULT_SEPARATOR = ','

        //The quote constant to use when you wish to suppress all quoting.
        private const val NO_QUOTE_CHARACTER = '\u0000'

        //The escape constant to use when you wish to suppress all escaping.
        private const val NO_ESCAPE_CHARACTER = '\u0000'

        //Default line terminator.
        private const val DEFAULT_LINE_END = "\n"
    }
}
