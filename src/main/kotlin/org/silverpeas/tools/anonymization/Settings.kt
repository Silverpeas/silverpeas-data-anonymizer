package org.silverpeas.tools.anonymization

import org.apache.commons.codec.digest.Crypt
import java.io.FileInputStream
import java.nio.file.Path
import java.util.*

/**
 * Settings for the data anonymization program
 * @author mmoquillon
 */
object Settings {

    private const val SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890/."
    private val random: Random = Random()

    private val props = Properties()

    init {
        val inputStream = this.javaClass.getResourceAsStream("/settings.properties")
        props.load(inputStream)
    }

    fun apply(customSettingsPath: Path) {
        val settings = Properties()
        settings.load(FileInputStream(customSettingsPath.toFile()))
        settings.forEach { p, v ->
            props.setProperty(p.toString(), v.toString())
        }
    }

    fun isSilverpeasHomeDefined() = !silverpeasHome().isEmpty()

    fun silverpeasHome() = props.getProperty("silverpeas.home") ?: ""

    class Space(language: String, id: Int, parent: Int?) {
        val id = "WA${id}"
        val name = "${props.getProperty("space.name.${language}")} $id"
        val description = null
        val parentId = if (parent == null) "" else "WA${parent}"
    }

    class ComponentInst(val type: String, id: Int, space: Int?) {
        val id = "${type}${id}"
        val name = "$type $id"
        val description = null
        val spaceId = if (space == null) "" else "WA${space}"
    }

    class Domain(val id: Int) {
        val name = "${props.getProperty("domain.name")} $id"
        val description = null
        val serverUrl = null
        val driver = ""
        val technicalName = props.getProperty("domain.name") + id
        val authServerName = "authDomain${technicalName}"
        val descriptor = "org.silverpeas.domains.domain${technicalName}"
    }

    class User(val id: Int) {
        val firstName = "${props.getProperty("user.firstName")} $id"
        val lastName = "${props.getProperty("user.lastName")} $id"
        val email = props.getProperty("user.email")
        val login = "${props.getProperty("user.firstName")}${id}.${props.getProperty("user.lastName")}${id}"
        val plainPassword = props.getProperty("user.password")
        val cryptedPassword = Crypt.crypt(plainPassword, randomSalt())
        val company = "Silverpeas"
    }

    class Group(val id: Int) {
        val name = "${props.getProperty("group.name")} $id"
        val description = null
    }

    class Database {
        val url = props.getProperty("db.jdbc.url")
        val driver = props.getProperty("db.jdbc.driver")
        val login = props.getProperty("db.login")
        val password = props.getProperty("db.password")
    }

    private fun randomSalt(): String {
        val saltBuf = StringBuilder("$6$")
        while (saltBuf.length < 16) {
            val index = (random.nextFloat() * SALTCHARS.length).toInt()
            saltBuf.append(SALTCHARS.substring(index, index + 1))
        }
        return saltBuf.toString()
    }
}