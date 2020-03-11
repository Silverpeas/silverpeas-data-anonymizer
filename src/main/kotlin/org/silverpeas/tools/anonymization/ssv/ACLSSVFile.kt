package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.ACE

/**
 * SSV file in which is stored the ACL of each application instance and for each user.
 * @author mmoquillon
 */
object ACLSSVFile: SSVFile("acl.ssv") {
    override fun postOpening() {
        write("ApplInstTechId", "UserId", "Role")
    }

    fun write(ace: ACE) {
        write(ace.appInstLocalId.toString(), ace.userId.toString(), ace.role)
    }
}