package org.silverpeas.tools.anonymization

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.silverpeas.tools.anonymization.ssv.CompInstSSVFile
import org.silverpeas.tools.anonymization.ssv.SpacesSSVFile
import org.silverpeas.tools.anonymization.ssv.UsersSSVFile
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
        Settings.apply(customSettingsPath)
    }

    val database = Settings.Database()
    Database.connect(database.url, database.driver, database.login, database.password)

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
        try {
            anonymizers.forEach { (type, processor) ->
                print("Anonymizing the ${type}...")
                processor.invoke()
                println(" DONE")
            }
        } finally {
            UsersSSVFile.close()
            SpacesSSVFile.close()
            CompInstSSVFile.close()
        }
    }
}