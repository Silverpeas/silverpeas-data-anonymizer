package org.silverpeas.tools.anonymization

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.silverpeas.tools.anonymization.ssv.SSVLogger
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * The Silverpeas data anonymizer program.
 * @author mmoquillon
 */

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val customSettingsPath = Paths.get(args[0])
        if (!Files.exists(customSettingsPath)) {
            System.err.println("The custom settings file isn't found at '${customSettingsPath.toAbsolutePath()}'")
            exitProcess(1)
        }
        Settings.load(customSettingsPath)
    }

    Database.connect(
        Settings.database.url,
        Settings.database.driver,
        Settings.database.login,
        Settings.database.password
    )

    if (Settings.isSilverpeasHomeDefined()) {
        val silverpeasHomePath = Paths.get(Settings.silverpeasHome())
        if (!Files.exists(silverpeasHomePath)) {
            System.err.println("The silverpeas home directory isn't found at '${silverpeasHomePath.toAbsolutePath()}'")
            exitProcess(2)
        }
        println(
            """Silverpeas home directory set at ${Settings.silverpeasHome()}: 
both the database and the data files will be anonymized"""
        )
    } else {
        println("Silverpeas home directory not set: only the database will be anonymized")
    }

    transaction {
        SSVLogger.use {
            val enabledAnonymizers = Settings.anonymizers()
            anonymizers.filterKeys { anonymizer -> enabledAnonymizers.contains(anonymizer) }
                .forEach { (type, processor) ->
                try {
                    print("Anonymizing the ${type}...")
                    processor.invoke()
                    println(" DONE")
                } catch (e: Exception) {
                    println(" ERROR: " + e.message)
                    this.rollback()
                    e.printStackTrace(System.err)
                    exitProcess(1)
                }
            }
        }
    }
}