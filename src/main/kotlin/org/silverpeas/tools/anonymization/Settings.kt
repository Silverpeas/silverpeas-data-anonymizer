package org.silverpeas.tools.anonymization

import java.io.FileInputStream
import java.nio.file.Path
import java.util.*

/**
 * Settings for the data anonymization program. It provides for all of the Silverpeas data the settings to
 * anonymize these data.
 * @author mmoquillon
 */
object Settings {

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
     * The settings about the anonymization of the domains
     */
    object domains {
        val namePrefix = props.getProperty("domain.name")
        val serverUrl = props.getProperty("domain.serverUrl") ?: ""
    }

    /**
     * The settings about the anonymization of the users
     */
    object users {
        val firstNamePrefix = props.getProperty("user.firstName")
        val lastNamePrefix = props.getProperty("user.lastName")
        val email = props.getProperty("user.email")
        val password = props.getProperty("user.password")
        val company = "Silverpeas"
    }

    /**
     * The settings about the anonymization of the groups
     */
    object groups {
        val namePrefix = props.getProperty("group.name")
        val description = null
    }

    /**
     * The settings about the anonymization of the collaborative spaces
     */
    object spaces {
        val namePrefix = mapOf(
            "fr" to props.getProperty("space.name.fr"),
            "en" to props.getProperty("space.name.en")
        )

        val description = mapOf(
            "fr" to null,
            "en" to null
        )
    }

    /**
     * The settings about the anonymization of the component instances
     */
    object appInsts {
        val namePrefix = mapOf(
            "fr" to props.getProperty("app.name.fr"),
            "en" to props.getProperty("app.name.fr")
        )

        val description = mapOf(
            "fr" to null,
            "en" to null
        )
    }

    object folders {
        val namePrefix = mapOf(
            "fr" to props.getProperty("node.folder.name.fr"),
            "en" to props.getProperty("node.folder.name.en")
        )

        val description = mapOf(
            "fr" to props.getProperty("node.folder.name.fr"),
            "en" to props.getProperty("node.folder.name.en")
        )
    }

    object albums {
        val namePrefix = mapOf(
            "fr" to props.getProperty("node.album.name.fr"),
            "en" to props.getProperty("node.album.name.en")
        )

        val description = mapOf(
            "fr" to props.getProperty("node.album.name.fr"),
            "en" to props.getProperty("node.album.name.en")
        )

    }

    object categories {
        val namePrefix = mapOf(
            "fr" to props.getProperty("node.category.name.fr"),
            "en" to props.getProperty("node.category.name.en")
        )

        val description = mapOf(
            "fr" to props.getProperty("node.category.name.fr"),
            "en" to props.getProperty("node.category.name.en")
        )
    }

    object publications {
        val namePrefix = mapOf(
            "fr" to props.getProperty("publication.name.fr"),
            "en" to props.getProperty("publication.name.en")
        )

        val description = mapOf(
            "fr" to props.getProperty("publication.name.fr"),
            "en" to props.getProperty("publication.name.en")
        )

        val keywords = mapOf(
            "fr" to null,
            "en" to null
        )
    }

    /**
     * The settings of the database in which are stored the data to anonymize.
     */
    object database {
        val url: String = props.getProperty("db.jdbc.url")
        val driver: String = props.getProperty("db.jdbc.driver")
        val login: String = props.getProperty("db.login")
        val password: String = props.getProperty("db.password")
    }
}