package com.wmstein.tourcount;

/**
 * Copyright 2015 Bytecode Pty Ltd.
 * Reduced to needed functions for TourCount by wmstein
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/******************************************************************
 * Based on "A very simple CSV writer" by
 *
 * @author Glen Smith
 *         <p>
 *         The following code is an extraction of the library of Glen Smith
 *         and provides only the functions needed here.
 *         Modified by wmstein
 */
class CSVWriter implements Closeable, Flushable
{
    //The character used for escaping quotes.
    private static final char DEFAULT_ESCAPE_CHARACTER = '"';
    //The default separator to use if none is supplied to the constructor.
    private static final char DEFAULT_SEPARATOR = ',';
    //The default quote character to use if none is supplied to the constructor.
    private static final char DEFAULT_QUOTE_CHARACTER = '"';
    //The quote constant to use when you wish to suppress all quoting.
    private static final char NO_QUOTE_CHARACTER = '\u0000';
    //The escape constant to use when you wish to suppress all escaping.
    private static final char NO_ESCAPE_CHARACTER = '\u0000';
    //Default line terminator.
    private static final String DEFAULT_LINE_END = "\n";

    private final Writer rawWriter;
    private final PrintWriter pw;
    private final char separator;
    private final char quotechar;
    private final char escapechar;
    private final String lineEnd;

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer)
    {
        this(writer, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    private CSVWriter(Writer writer, char separator)
    {
        this(writer, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    private CSVWriter(Writer writer, char separator, char quotechar)
    {
        this(writer, separator, quotechar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer     the writer to an underlying CSV source.
     * @param separator  the delimiter to use for separating entries
     * @param quotechar  the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or escapechars
     */
    private CSVWriter(Writer writer, char separator, char quotechar, char escapechar)
    {
        this(writer, separator, quotechar, escapechar, DEFAULT_LINE_END);
    }

    /**
     * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
     *
     * @param writer     the writer to an underlying CSV source.
     * @param separator  the delimiter to use for separating entries
     * @param quotechar  the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or escapechars
     * @param lineEnd    the line feed terminator to use
     */
    private CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd)
    {
        this.rawWriter = writer;
        this.pw = new PrintWriter(writer);
        this.separator = separator;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine         a string array with each comma-separated element as a separate
     *                         entry.
     * @param applyQuotesToAll true if all values are to be quoted.  false applies quotes only
     *                         to values which contain the separator, escape, quote or new line characters.
     */
    private void writeNext(String[] nextLine, boolean applyQuotesToAll)
    {

        if (nextLine == null)
        {
            return;
        }

        StringBuilder sb = new StringBuilder(nextLine.length * 2); // This is for the worse case where all elements have to be escaped.
        for (int i = 0; i < nextLine.length; i++)
        {
            if (i != 0)
            {
                sb.append(separator);
            }

            String nextElement = nextLine[i];

            if (nextElement == null)
            {
                continue;
            }

            Boolean stringContainsSpecialCharacters = stringContainsSpecialCharacters(nextElement);

            if ((applyQuotesToAll || stringContainsSpecialCharacters) && quotechar != NO_QUOTE_CHARACTER)
            {
                sb.append(quotechar);
            }

            if (stringContainsSpecialCharacters)
            {
                sb.append(processLine(nextElement));
            }
            else
            {
                sb.append(nextElement);
            }

            if ((applyQuotesToAll || stringContainsSpecialCharacters) && quotechar != NO_QUOTE_CHARACTER)
            {
                sb.append(quotechar);
            }
        }
        sb.append(lineEnd);
        pw.write(sb.toString());
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a separate
     *                 entry.
     */
    public void writeNext(String[] nextLine)
    {
        writeNext(nextLine, true);
    }

    /**
     * checks to see if the line contains special characters.
     *
     * @param line - element of data to check for special characters.
     * @return true if the line contains the quote, escape, separator, newline or return.
     */
    private boolean stringContainsSpecialCharacters(String line)
    {
        return line.indexOf(quotechar) != -1 || line.indexOf(escapechar) != -1 || line.indexOf(separator) != -1 || line.contains(DEFAULT_LINE_END) || line.contains("\r");
    }

    /**
     * Processes all the characters in a line.
     *
     * @param nextElement - element to process.
     * @return a StringBuilder with the elements data.
     */
    private StringBuilder processLine(String nextElement)
    {
        StringBuilder sb = new StringBuilder(nextElement.length() * 2); // this is for the worse case where all elements have to be escaped.
        for (int j = 0; j < nextElement.length(); j++)
        {
            char nextChar = nextElement.charAt(j);
            processCharacter(sb, nextChar);
        }
        return sb;
    }

    /**
     * Appends the character to the StringBuilder adding the escape character if needed.
     *
     * @param sb       - StringBuffer holding the processed character.
     * @param nextChar - character to process
     */
    private void processCharacter(StringBuilder sb, char nextChar)
    {
        if (escapechar != NO_ESCAPE_CHARACTER && checkCharactersToEscape(nextChar))
        {
            sb.append(escapechar).append(nextChar);
        }
        else
        {
            sb.append(nextChar);
        }
    }

    private boolean checkCharactersToEscape(char nextChar)
    {
        return quotechar == NO_QUOTE_CHARACTER ?
            (nextChar == quotechar || nextChar == escapechar || nextChar == separator)
            : (nextChar == quotechar || nextChar == escapechar);
    }

    /**
     * Flush underlying stream to writer.
     *
     * @throws IOException if bad things happen
     */
    public void flush() throws IOException
    {
        pw.flush();
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     */
    public void close() throws IOException
    {
        flush();
        pw.close();
        rawWriter.close();
    }
}
