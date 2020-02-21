package org.silverpeas.tools.anonymization.ssv

import java.io.Closeable

/**
 * A logger of SSV files. It wraps all the SSV files used in the different anonymizing functions in order to set up
 * and to release of all them.
 * @author mmoquillon
 */
object SSVLogger : Closeable {

    private val ssvfiles = listOf(
        ofUsers(),
        ofSpaces(),
        ofComponentInstances(),
        ofNodes(),
        ofPublications()
    )

    init {
        ssvfiles.forEach { it.open() }
    }

    fun ofUsers() = UsersSSVFile

    fun ofSpaces() = SpacesSSVFile

    fun ofComponentInstances() = CompInstSSVFile

    fun ofNodes() = NodesSSVFile

    fun ofPublications() = PublicationsSSVFile

    override fun close() {
        ssvfiles.forEach { it.close() }
    }
}