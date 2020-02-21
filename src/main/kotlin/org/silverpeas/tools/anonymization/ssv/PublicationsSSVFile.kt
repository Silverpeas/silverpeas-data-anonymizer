package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.AnonymousPublication

/**
 * SSV file to save the publications data ready to be used in tests against a running Silverpeas platform.
 * @author mmoquillon
 */
object PublicationsSSVFile: SSVFile("publications.ssv") {

    override fun postOpening() {
        write("Id", "AppInstId", "NodeId")
    }

    fun write(pub: AnonymousPublication) {
        write(pub.id.toString(), pub.instanceId, pub.nodeId.toString())
    }

}