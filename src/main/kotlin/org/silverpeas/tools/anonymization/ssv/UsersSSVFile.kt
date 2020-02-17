package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.Settings

/**
 * A SSV file in which all the users scanned in the database will be saved.
 * @author mmoquillon
 */
object UsersSSVFile: SSVFile("user.ssv") {

    override fun postOpening() {
        write("Firstname",
            "Lastname",
            "Login",
            "Password")
    }

    fun write(user: Settings.User) {
        write(user.firstName, user.lastName, user.login, user.plainPassword)
    }

}