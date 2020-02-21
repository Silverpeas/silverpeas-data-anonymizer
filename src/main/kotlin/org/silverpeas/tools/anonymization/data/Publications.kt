package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.model.AnonymousPublication
import org.silverpeas.tools.anonymization.ssv.SSVLogger

/**
 * The publications.
 * @author mmoquillon
 */
object PublicationFather: Table("sb_publication_publifather") {
    val pubId = integer("pubid") references Publication.id
    val nodeId = integer("nodeid").nullable()

    override val primaryKey = PrimaryKey(pubId, nodeId, name = "PK_Publication_PubliFather")
}

object Publication : Table("sb_publication_publi"), Anonymizing {
    val id = integer("pubid")
    val instanceId = varchar("instanceid", 50)
    val name = varchar("pubname", 400)
    val description = varchar("pubdescription", 2000).nullable()
    val keywords = varchar("pubkeywords", 1000).nullable()
    val language = varchar("lang", 2).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_Publication_Publi")

    override fun anonymize() {
        leftJoin(PublicationFather).selectAll().distinct().forEach { pub ->
            val lang = pub[language] ?: "fr"
            val father = pub[PublicationFather.nodeId] ?: -1
            val anoPubli = AnonymousPublication(pub[id], pub[instanceId], father, lang)
            update({ id eq pub[id] }) {
                it[name] = anoPubli.name
                it[description] = anoPubli.description
                it[keywords] = anoPubli.keywords
            }
            SSVLogger.ofPublications().write(anoPubli)
        }
    }
}

object PublicationI18n : Table("sb_publication_publii18n"), Anonymizing {
    val id = integer("id")
    val pubid = integer("pubid") references Publication.id
    val name = varchar("name", 400)
    val description = varchar("description", 2000).nullable()
    val keywords = varchar("keywords", 1000).nullable()
    val language = varchar("lang", 2)

    override val primaryKey = PrimaryKey(id, name = "PK_Publication_PubliI18N")

    override fun anonymize() {
        innerJoin(Publication).selectAll().forUpdate().distinct().forEach { pub ->
            update({ id eq pub[id] }) {
                val anoPubli = AnonymousPublication(pub[pubid], "", language = pub[language])
                it[name] = anoPubli.name
                it[description] = anoPubli.description
                it[keywords] = anoPubli.keywords
            }
        }
    }
}