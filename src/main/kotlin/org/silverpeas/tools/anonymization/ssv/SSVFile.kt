package org.silverpeas.tools.anonymization.ssv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An abstract SSV file
 * @author mmoquillon
 */
abstract class SSVFile(val filename: String) {

    private val printer: CSVPrinter

    init {
        val ssvPath = Paths.get(filename)
        printer = CSVPrinter(
            Files.newBufferedWriter(ssvPath),
            CSVFormat.DEFAULT.withDelimiter(';'))
    }

    fun write(vararg record: String) {
        printer.printRecord(record.asIterable())
    }

    fun close() {
        printer.close(true)
    }
}