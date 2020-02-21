package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.model.AnonymousNode
import org.silverpeas.tools.anonymization.ssv.SSVLogger

/**
 * Definitions of the nodes that are used as a generic way to represent a labelled container of user contributions to
 * categorize those contributions: a folder, a media album, and so on.
 * @author mmoquillon
 */

sealed class NodeTable(name: String) : Table(name), Anonymizing {
    val name = varchar("nodename", 1000)
    val description = varchar("nodedescription", 2000)
}


object Node : NodeTable("sb_node_node") {
    val id = integer("nodeid")
    val instanceId = varchar("instanceid", 50)
    val parent = integer("nodefatherid")

    override val primaryKey = PrimaryKey(id, instanceId, name = "PK_Node_Node")

    override fun anonymize() {
        select { id greater 2 }.forUpdate().distinct().forEach { node ->
            val anoNode = AnonymousNode.of(node[instanceId]).make(node[id], node[parent])
            update({ id eq node[id] }) {
                it[name] = anoNode.name
                it[description] = anoNode.description
            }
            SSVLogger.ofNodes().write(anoNode)
        }
    }
}

object NodeI18n : NodeTable("sb_node_nodei18n") {
    val id = integer("id")
    val nodeId = integer("nodeid") references Node.id
    val language = varchar("lang", 2)

    override val primaryKey = PrimaryKey(id, name = "PK_Node_NodeI18N")

    override fun anonymize() {
        innerJoin(Node).select { nodeId greater 2 }.forUpdate().distinct().forEach { node ->
            val anoNode = AnonymousNode.of(node[Node.instanceId]).make(node[nodeId], language = node[language])
            update({ id eq node[id] }) {
                it[NodeI18n.name] = anoNode.name
                it[NodeI18n.description] = anoNode.description
            }
        }
    }
}