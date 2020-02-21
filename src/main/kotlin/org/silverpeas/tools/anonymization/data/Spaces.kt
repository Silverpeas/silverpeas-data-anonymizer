package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.model.AnonymousSpace
import org.silverpeas.tools.anonymization.ssv.SSVLogger

/**
 * The collaborative spaces.
 * @author mmoquillon
 */
sealed class SpaceTable(name: String) : Table(name), Anonymizing {
    val id = integer("id")
    protected val name = varchar("name", 100)
    protected val description = varchar("description", 400).nullable()
}

object Space : SpaceTable("st_space") {
    val parent = integer("domainfatherid").references(id).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_Space")

    const val PERSONNAL_SPACE = "Personal space%"

    override fun anonymize() {
        select { name notLike PERSONNAL_SPACE }.forUpdate().distinct().forEach { space ->
            update({ id eq space[id] }) {
                val anoSpace = AnonymousSpace(space[id], parent = space[parent])
                it[name] = anoSpace.name
                it[description] = anoSpace.description
                SSVLogger.ofSpaces().write(anoSpace)
            }
        }
    }
}

object SpaceI18n : SpaceTable("st_spacei18n") {
    private val language = varchar("lang", 2)

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { space ->
            val anoSpace = AnonymousSpace(space[id], language =  space[language])
            update({ id eq space[id] }) {
                it[name] = anoSpace.name
                it[description] = anoSpace.description
            }
        }
    }
}