package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.model.AnonymousAppInst
import org.silverpeas.tools.anonymization.ssv.SSVLogger

/**
 * The components instantiated in a given Silverpeas platform. A component in Silverpeas is a multi-instantiable
 * low-coupled compartmentalized resource that provides a given application-centric functionalities. For example, a
 * Kmelia component instance provides the functionalities of an EDM.
 * @author mmoquillon
 */
sealed class ComponentInstTable(name: String) : Table(name),
    Anonymizing {
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
                val anoAppInst = AnonymousAppInst(
                    component[ComponentInst.component],
                    component[id],
                    space = component[space]
                )
                it[ComponentInstI18n.name] = anoAppInst.name
                it[ComponentInstI18n.description] = anoAppInst.description
                SSVLogger.ofComponentInstances().write(anoAppInst)
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
                val anoAppInst = AnonymousAppInst(
                    component[ComponentInst.component],
                    component[ComponentInst.id],
                    space = component[ComponentInst.space],
                    language = component[language]
                )
                it[name] = anoAppInst.name
                it[description] = anoAppInst.description
            }
        }
    }
}
