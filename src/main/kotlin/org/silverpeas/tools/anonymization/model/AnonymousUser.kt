package org.silverpeas.tools.anonymization.model

import org.apache.commons.codec.digest.Crypt
import org.silverpeas.tools.anonymization.Settings
import java.util.*

/**
 *
 * @author mmoquillon
 */
class AnonymousUser(val id: Int, var domainId: Int? = null) {

    companion object {
        private val saltChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890/."
        private val random: Random = Random()

        private fun randomSalt(): String {
            val saltBuf = StringBuilder("$6$")
            while (saltBuf.length < 16) {
                val index = (random.nextFloat() * saltChars.length).toInt()
                saltBuf.append(saltChars.substring(index, index + 1))
            }
            return saltBuf.toString()
        }
    }

    val firstName = "${Settings.users.firstNamePrefix}$id"
    val lastName = "${Settings.users.lastNamePrefix}$id"
    val email = Settings.users.email
    val login = "${Settings.users.firstNamePrefix}${id}.${Settings.users.lastNamePrefix}${id}"
    val plainPassword: String = Settings.users.password
    val cryptedPassword: String = Crypt.crypt(plainPassword, randomSalt())
    val company = Settings.users.company
}