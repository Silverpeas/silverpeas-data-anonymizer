package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.Settings

/**
 * A SSV file in which all the users scanned in the database will be saved.
 * @author mmoquillon
 */
object SpacesSSVFile: SSVFile("spaces.ssv") {

    init {
        write("Id")
    }

    fun write(space: Settings.Space) {
        write(space.id)
    }

}