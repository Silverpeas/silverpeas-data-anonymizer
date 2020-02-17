package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.Settings

/**
 * SSV file to save component instances data ready to be used in tests against a running Silverpeas platform.
 * @author mmoquillon
 */
object CompInstSSVFile: SSVFile("components.ssv") {

    override fun postOpening() {
        write("Id", "Type", "Space")
    }

    fun write(compInst: Settings.ComponentInst) {
        write(compInst.id, compInst.type, compInst.spaceId)
    }

}