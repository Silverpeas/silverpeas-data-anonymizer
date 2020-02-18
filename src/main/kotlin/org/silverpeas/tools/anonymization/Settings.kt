package org.silverpeas.tools.anonymization

import org.apache.commons.codec.digest.Crypt
import java.io.FileInputStream
import java.nio.file.Path
import java.util.*

/**
 * Settings for the data anonymization program. It provides for all of the Silverpeas data the settings to
 * anonymize these data.
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

    /**
     * Loads the settings defined in the specified file. The loaded properties override the predefine ones.
     */
    fun load(customSettingsPath: Path) {
        val settings = Properties()
        settings.load(FileInputStream(customSettingsPath.toFile()))
        settings.forEach { p, v ->
            props.setProperty(p.toString(), v.toString())
        }
    }

    /**
     * Is the Silverpeas home directory path set?
     */
    fun isSilverpeasHomeDefined() = silverpeasHome().isNotEmpty()

    /**
     * Gets the path of the Silverpeas home directory or an empty string if this path isn't defined in the
     * settings.
     */
    fun silverpeasHome() = props.getProperty("silverpeas.home") ?: ""

    /**
     * Gets the settings in the given language for the space identified by the specified identifier and with the
     * given space parent identifier.
     */
    class Space(language: String, id: Int, parent: Int?) {
        val id = "WA${id}"
        val name = "${props.getProperty("space.name.${language}")} $id"
        val description = null
        val parentId = if (parent == null) "" else "WA${parent}"
    }

    /**
     * Gets the settings in the given language for the component instance of the specified type and identified by the
     * given identifier and with as parent the specified space identifier.
     */
    class ComponentInst(language: String, val type: String, id: Int, space: Int?) {
        val id = "${type}${id}"
        val name = getParametrizedName(language, id)
        val description = null
        val spaceId = if (space == null) "" else "WA${space}"

        private fun getParametrizedName(language: String, id: Int): String {
            var name = props.getProperty("app.name.${language}")
            if (name == null || name.isEmpty()) {
                name = "$type $id"
            } else {
                name += " $id"
            }
            return name
        }
    }

    /**
     * Gets the settings for the domain identified with the specified identifier.
     */
    class Domain(id: Int) {
        val name = "${props.getProperty("domain.name")} $id"
        val description = null
        val serverUrl = null
        val driver = "org.silverpeas.core.admin.domain.driver.sqldriver.SQLDriver"
        private val technicalName = props.getProperty("domain.name") + id
        val authServerName = "autDomain${technicalName}"
        val descriptor = "org.silverpeas.domains.domain${technicalName}"
        private val tablePrefix = "domain${technicalName}_"
        val usersTableName = tablePrefix + "user"
        val groupsTableName = tablePrefix + "group"
        val groupUserRelsTableName = tablePrefix + "group_user_rel"
        val authDescriptorName = "$authServerName.properties"
        val descriptorName = "domain${technicalName}.properties"
    }

    /**
     * Gets the settings for the user in the given domain and identified with the specified identifier.
     */
    class User(id: Int, val domainId: Int?) {
        val firstName = "${props.getProperty("user.firstName")} $id"
        val lastName = "${props.getProperty("user.lastName")} $id"
        val email: String = props.getProperty("user.email")
        val login = "${props.getProperty("user.firstName")}${id}.${props.getProperty("user.lastName")}${id}"
        val plainPassword: String = props.getProperty("user.password")
        val cryptedPassword: String = Crypt.crypt(plainPassword, randomSalt())
        val company = "Silverpeas"

        private fun randomSalt(): String {
            val saltBuf = StringBuilder("$6$")
            while (saltBuf.length < 16) {
                val index = (random.nextFloat() * SALTCHARS.length).toInt()
                saltBuf.append(SALTCHARS.substring(index, index + 1))
            }
            return saltBuf.toString()
        }
    }

    /**
     * Gets the settings for the group identified by the specified identifier.
     */
    class Group(val id: Int) {
        val name = "${props.getProperty("group.name")} $id"
        val description = null
    }

    /**
     * Gets the settings of the database in which are stored the data to anonymize.
     */
    class Database {
        val url: String = props.getProperty("db.jdbc.url")
        val driver: String = props.getProperty("db.jdbc.driver")
        val login: String = props.getProperty("db.login")
        val password: String = props.getProperty("db.password")
    }
}