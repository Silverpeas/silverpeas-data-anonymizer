package org.silverpeas.tools.anonymization.ssv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An abstract SSV file.
 * @author mmoquillon
 */
abstract class SSVFile(val filename: String) {

    private var printer: CSVPrinter? = null

    /**
     * Performs some additional task once the file is opened. For example, write the headers in the first line
     * of the SSV file.
     */
    abstract protected fun postOpening()

    /**
     * Opens the underlying physical file in the disk. If the file already exists, its content will be erased and
     * then replaced by any further recordings.
     */
    fun open() {
        val ssvPath = Paths.get(filename)
        printer = CSVPrinter(
            Files.newBufferedWriter(ssvPath),
            CSVFormat.DEFAULT.withDelimiter(';'))
        postOpening()
    }

    /**
     * Is this file opened, and then ready to accept records.
     */
    fun isOpened(): Boolean {
        return printer != null
    }

    /**
     * Is this file closed and then not ready to accept records.
     */
    fun isClosed(): Boolean {
        return !isOpened()
    }

    /**
     * Writes into this SSV file the specified record (one to several field values.
     * Does nothing if the file isn't opened.
     */
    fun write(vararg record: String) {
        printer?.printRecord(record.asIterable())
    }

    /**
     * Closes this file. No more records can then be written. Does nothing if the file isn't opened.
     */
    fun close() {
        printer?.close(true)
        printer = null
    }
}