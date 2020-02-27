package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.AnonymousAppInst

/**
 * SSV file to save component instances data ready to be used in tests against a running Silverpeas platform.
 * @author mmoquillon
 */
object CompInstSSVFile: SSVFile("components.ssv") {

    override fun postOpening() {
        write("TechId", "Id", "Application", "Space")
    }

    fun write(compInst: AnonymousAppInst) {
        write(compInst.localId.toString(), compInst.id, compInst.app, compInst.spaceId)
    }

}