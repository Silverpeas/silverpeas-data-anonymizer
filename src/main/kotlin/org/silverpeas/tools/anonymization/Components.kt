package org.silverpeas.tools.anonymization

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.silverpeas.tools.anonymization.ssv.SSVLogger

/**
 * The components instantiated in a given Silverpeas platform. A component in Silverpeas is a multi-instantiable
 * low-coupled compartmentalized resource that provides a given application-centric functionalities. For example, a
 * Kmelia component instance provides the functionalities of an EDM.
 * @author mmoquillon
 */
sealed class ComponentInstTable(name: String) : Table(name), Anonymizing {
    val id = integer("id")
    val name = varchar("name", 100)
    val description = varchar("description", 400).nullable()
}

object ComponentInst : ComponentInstTable("st_componentinstance") {
    val component = varchar("componentname", 100)
    val space = integer("spaceid") references Space.id

    override val primaryKey = PrimaryKey(id, name = "PK_ComponentInstance")

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { component ->
            update({ id eq component[id] }) {
                val compInst = Settings.ComponentInst(
                    "fr",
                    component[ComponentInst.component],
                    component[ComponentInst.id],
                    component[space]
                )
                it[ComponentInstI18n.name] = compInst.name
                it[ComponentInstI18n.description] = compInst.description
                SSVLogger.ofComponentInstances().write(compInst)
            }
        }
    }
}

object ComponentInstI18n : ComponentInstTable("st_componentinstancei18n") {
    private val componentId = integer("componentid") references(ComponentInst.id)
    private val language = varchar("lang", 2)

    override fun anonymize() {
        innerJoin(ComponentInst).selectAll().forUpdate().distinct().forEach { component ->
            update({ ComponentInstI18n.id eq component[ComponentInstI18n.id] }) {
                val compInst = Settings.ComponentInst(
                    component[language],
                    component[ComponentInst.component],
                    component[ComponentInst.id],
                    component[ComponentInst.space]
                )
                it[name] = compInst.name
                it[description] = compInst.description
            }
        }
    }
}
