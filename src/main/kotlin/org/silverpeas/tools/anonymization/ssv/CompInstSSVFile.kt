package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.Settings

/**
 *
 * @author mmoquillon
 */
object CompInstSSVFile: SSVFile("components.ssv") {

    init {
        write("Id", "Type")
    }

    fun write(compInst: Settings.ComponentInst) {
        write(compInst.id, compInst.type)
    }

}