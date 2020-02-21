package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.AnonymousNode

/**
 * SSV file to save the node data ready to be used in tests against a running Silverpeas platform.
 * @author mmoquillon
 */
object NodesSSVFile: SSVFile("nodes.ssv") {

    override fun postOpening() {
        write("Id", "Parent", "AppInstId")
    }

    fun write(node: AnonymousNode) {
        write(node.id.toString(), node.parent.toString(), node.instanceId)
    }

}