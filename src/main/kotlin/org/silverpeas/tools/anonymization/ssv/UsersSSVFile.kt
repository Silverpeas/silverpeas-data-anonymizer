package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.AnonymousUser

/**
 * A SSV file in which all the users scanned in the database will be saved.
 * @author mmoquillon
 */
object UsersSSVFile: SSVFile("users.ssv") {

    override fun postOpening() {
        write(
            "Id",
            "Firstname",
            "Lastname",
            "Login",
            "Password",
            "Domain"
        )
    }

    fun write(user: AnonymousUser) {
        write(
            user.id.toString(),
            user.firstName,
            user.lastName,
            user.login,
            user.plainPassword,
            user.domainId?.toString() ?: ""
        )
    }

}