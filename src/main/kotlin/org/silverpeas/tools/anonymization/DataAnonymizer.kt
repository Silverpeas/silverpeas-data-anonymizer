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
            anonymizers.forEach { (type, processor) ->
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
/*

Database.connect("jdbc:postgresql://localhost:5432/silverbench", "org.postgresql.Driver", "spuser", "spuser")

NodeI18n.leftJoin(Node).select {
    (Node.id neq 0) and not((Node.id eq 1) and (Node.instanceId notLike "gallery%")) and not(
        (Node.id eq 2) and ((Node.instanceId notLike "blog%") or (Node.instanceId notLike "gallery%"))
    )
}.forUpdate().distinct().forEach { node ->
    if (node[Node.instanceId].isNullOrBlank()) {
        deleteWhere { NodeI18n.id eq node[NodeI18n.id] }
    } else {
        val anoNode = AnonymousNode.of(node[Node.instanceId]?: "").make(node[NodeI18n.nodeId], language = node[NodeI18n.language])
        update({ NodeI18n.id eq node[NodeI18n.id] }) {
            it[NodeI18n.name] = anoNode.name
            it[NodeI18n.description] = anoNode.description
        }
    }
}

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notLike
import org.jetbrains.exposed.sql.transactions.transaction
import org.silverpeas.tools.anonymization.data.Node
import org.silverpeas.tools.anonymization.data.NodeI18n
import org.silverpeas.tools.anonymization.model.AnonymousNode

Database.connect("jdbc:postgresql://localhost:5432/silverbench", "org.postgresql.Driver", "spuser", "spuser")

transaction {
    val row = NodeI18n.leftJoin(Node).select {
        NodeI18n.nodeId eq 3474
    }.distinct().firstOrNull()
    if (row != null) {
        println("instance id is ${row[Node.instanceId]}")
        if (row[Node.instanceId].isNullOrBlank()) {
            NodeI18n.deleteWhere { NodeI18n.nodeId eq row[NodeI18n.nodeId] }
        } else {
            NodeI18n.update({ NodeI18n.nodeId eq row[NodeI18n.nodeId] }) {
                it[NodeI18n.name] = "Toto"
                it[NodeI18n.description] = "Chez les papoos"
            }
        }
    } else {
        println("no row found!")
    }
}

 */