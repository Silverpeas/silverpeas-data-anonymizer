package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.Settings
import org.silverpeas.tools.anonymization.model.AnonymousDomain
import org.silverpeas.tools.anonymization.model.AnonymousGroup
import org.silverpeas.tools.anonymization.model.AnonymousUser
import org.silverpeas.tools.anonymization.ssv.SSVLogger
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.absoluteValue

/**
 * Definitions of the users, the user groups and of the user domains to which users and groups are related:
 * a user domain is an organizational container of users and user groups; each user and group are in a given domain.
 * Silverpeas defines a default domain named "Silverpeas" or "domainSilverpeas".
 * @author mmoquillon
 */

/**
 * Add a method in String to convert it to an int or, if the number format isn't correct, apply a default conversion
 * function.
 */
fun String.toIntOrDefault(default: (String) -> Int): Int {
    var asInt = this.toIntOrNull()
    if (asInt == null) {
        asInt = default.invoke(this)
    }
    return asInt
}

/**
 * The user domains. Each domain is related to its own users and groups. So, the anonymizing function operates both
 * on the anonymization of the domain and on the users and groups of that domain.
 */
object Domain : Table("st_domain"), Anonymizing {
    private val id = integer("id")
    private val name = varchar("name", 100)
    private val description = varchar("description", 400).nullable()
    private val descriptor = varchar("propfilename", 100)
    private val authServer = varchar("authenticationserver", 100)
    private val driver = varchar("classname", 100)
    private val serverUrl = varchar("silverpeasserverurl", 400).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_ST_Domain")

    private const val AUTH_SQL_TEMPLATE = "autDomainSQL.properties"
    private const val DOMAIN_SQL_TEMPLATE = "domainSQL.properties"

    /**
     * Anonymizes all the additional user domains in Silverpeas. It anonymizes also the users and the groups that are
     * part of the anonymized domain.
     */
    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { domain ->
            val anoDomain = AnonymousDomain(domain[id])
            if (domain[id] > 0) {
                anonymizeDomainSpecificData(domain, anoDomain)
                update({ id eq domain[id] }) {
                    it[name] = anoDomain.name
                    it[description] = anoDomain.description
                    it[serverUrl] = anoDomain.serverUrl
                    it[driver] = anoDomain.driver
                    it[authServer] = anoDomain.authServerName
                    it[descriptor] = anoDomain.type
                }
                val domainDescName = domain[descriptor].removePrefix("org.silverpeas.domains.") + ".properties"
                val authDescName = domain[authServer] + ".properties"
                convertDescriptorsFile(Pair(domainDescName, authDescName), anoDomain)
            } else {
                update {
                    it[serverUrl] = anoDomain.serverUrl
                }
            }
        }
    }

    private fun encodeToInt(value: String): Int {
        return value.hashCode().absoluteValue
    }

    private fun DomainUserTable.setCommonUserFields(user: AnonymousUser, row: InsertStatement<Number>) {
        row[firstName] = user.firstName
        row[lastName] = user.lastName
        row[email] = user.email
        row[login] = user.login
        row[password] = user.cryptedPassword
        row[company] = user.company
    }

    private fun convertDescriptorsFile(descriptors: Pair<String, String>, anoDomain: AnonymousDomain) {
        if (Settings.isSilverpeasHomeDefined()) {
            val propertiesPath = Paths.get(Settings.silverpeasHome(), "properties", "org", "silverpeas")

            val domainDefPath = propertiesPath.resolve(Paths.get("domains", descriptors.first))
            val domainTemplatePath = domainDefPath.resolveSibling(DOMAIN_SQL_TEMPLATE)
            val newDomainDefPath = domainDefPath.resolveSibling(anoDomain.descriptorName)

            val authDefPath = propertiesPath.resolve(Paths.get("authentication", descriptors.second))
            val authTemplatePath = authDefPath.resolveSibling(AUTH_SQL_TEMPLATE)
            val newAuthDefPath = authDefPath.resolveSibling(anoDomain.authDescriptorName)

            if (!Files.exists(domainDefPath) || !Files.exists(authDefPath)) {
                println("The domain descriptors for ${descriptors.first} doesn't exist in the file system: do nothing")
            } else {
                val props = Properties()

                props.load(FileInputStream(domainTemplatePath.toFile()))
                props.setProperty("database.SQLUserTableName", anoDomain.usersTableName)
                props.setProperty("database.SQLGroupTableName", anoDomain.groupsTableName)
                props.setProperty("database.SQLUserGroupTableName", anoDomain.groupUserRelsTableName)
                props.store(newDomainDefPath.toFile().writer(Charsets.ISO_8859_1), null)

                props.load(FileInputStream(authTemplatePath.toFile()))
                props.setProperty("autServer0.SQLUserTableName", anoDomain.usersTableName)
                props.store(newAuthDefPath.toFile().writer(Charsets.ISO_8859_1), null)

                Files.delete(domainDefPath)
                Files.delete(authDefPath)
            }
        }
    }

    private fun anonymizeDomainSpecificData(domain: ResultRow, newDomain: AnonymousDomain) {
        val newUserTable = DomainUserTable(newDomain.usersTableName)
        val newGroupTable = DomainGroupTable(newDomain.groupsTableName)
        val newGroupUserRelTable = object : Table(newDomain.groupUserRelsTableName) {
            val userId = integer("userid") references newUserTable.id
            val groupId = integer("groupid") references newGroupTable.id
        }
        SchemaUtils.create(newUserTable, newGroupTable, newGroupUserRelTable)

        SilverpeasUser.select { (SilverpeasUser.domainId eq domain[id]) and (SilverpeasUser.state neq "DELETED") }
            .distinct().forEach { user ->
                val specificId = user[SilverpeasUser.specificId].toIntOrDefault(Domain::encodeToInt)
                newUserTable.insert { row ->
                    val anoUser = AnonymousUser(user[SilverpeasUser.id])
                    row[id] = specificId
                    setCommonUserFields(anoUser, row)
                }
                SilverpeasUser.update({ SilverpeasUser.id eq user[SilverpeasUser.id] }) { row ->
                    row[SilverpeasUser.specificId] = specificId.toString()
                }
            }

        SilverpeasGroup.select { SilverpeasGroup.domainId eq domain[id] }.distinct().forEach { group ->
            val specificId = group[SilverpeasGroup.specificId].toIntOrDefault(Domain::encodeToInt)
            var parent: Int? = null
            if (group[SilverpeasGroup.parentId] != null) {
                val parentGroup =
                    SilverpeasGroup.select { (SilverpeasGroup.domainId eq domain[id]) and (SilverpeasGroup.specificId eq group[id].toString()) }
                        .firstOrNull()
                val value = parentGroup?.getOrNull(SilverpeasGroup.specificId)
                parent = value?.toIntOrDefault(Domain::encodeToInt)
            }

            val theGroup = AnonymousGroup(group[SilverpeasGroup.id])
            newGroupTable.insert { row ->
                row[id] = specificId
                row[parentId] = parent
                row[name] = theGroup.name
                row[description] = theGroup.description
            }

            SilverpeasGroup.update({ SilverpeasGroup.id eq group[SilverpeasGroup.id] }) { row ->
                row[SilverpeasGroup.specificId] = specificId.toString()
            }
        }

        SilverpeasGroupUserRelTable.innerJoin(SilverpeasUser).innerJoin(
            SilverpeasGroup
        ).select {
            (SilverpeasUser.domainId eq domain[id]) and (SilverpeasGroup.domainId eq domain[id])
        }.forEach { rel ->
            newGroupUserRelTable.insert { row ->
                row[newGroupUserRelTable.userId] = rel[SilverpeasUser.specificId].toIntOrDefault(
                    Domain::encodeToInt
                )
                row[newGroupUserRelTable.groupId] = rel[SilverpeasGroup.specificId].toIntOrDefault(
                    Domain::encodeToInt
                )
            }
        }

        if (domain[driver].endsWith("SQLDriver")) {
            val tableNamePrefix = domain[descriptor].removePrefix("org.silverpeas.domains.").toLowerCase()
            val userTable = DomainUserTable("${tableNamePrefix}_user")
            val groupTable = DomainGroupTable("${tableNamePrefix}_group")
            val groupUserRelTable = object : Table("${tableNamePrefix}_group_user_rel") {
                val userId = integer("userid") references userTable.id
                val groupId = integer("groupid") references groupTable.id
            }
            SchemaUtils.drop(groupUserRelTable, userTable, groupTable)
        }
    }
}

/**
 * The different types of users.
 */
sealed class UserTable(name: String = "") : Table(name),
    Anonymizing {
    val id = integer("id")
    val firstName = varchar("firstname", 100).nullable()
    val lastName = varchar("lastname", 100)
    val email = varchar("email", 200).nullable()
    val login = varchar("login", 50)

    override val primaryKey = PrimaryKey(id)

    open protected fun anonymousUser(row: ResultRow): AnonymousUser {
        return AnonymousUser(row[id])
    }

    protected abstract fun update(row: ResultRow, stmt: UpdateStatement)

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { user ->
            update({ id eq user[id] }) {
                update(user, it)
            }
        }
    }
}

/**
 * The users in a given user domain in Silverpeas.
 */
open class DomainUserTable(name: String = "") : UserTable(name) {
    val password = varchar("password", 123).nullable()
    val isPasswordValid = char("passwordvalid").default('Y')
    val title = varchar("title", 100).nullable()
    val company = varchar("company", 100).nullable()
    val position = varchar("position", 100).nullable()
    val boss = varchar("boss", 100).nullable()
    val phone = varchar("phone", 20).nullable()
    val homePhone = varchar("homephone", 20).nullable()
    val fax = varchar("fax", 20).nullable()
    val cellPhone = varchar("cellphone", 20).nullable()
    val address = varchar("address", 500).nullable()

    override fun update(row: ResultRow, stmt: UpdateStatement) {
        val anoUser = anonymousUser(row)
        stmt[firstName] = anoUser.firstName
        stmt[lastName] = anoUser.lastName
        stmt[email] = anoUser.email
        stmt[login] = anoUser.login
        stmt[password] = anoUser.cryptedPassword
        stmt[company] = anoUser.company
    }
}

/**
 * All the users in Silverpeas, whatever their domain.
 */
object SilverpeasUser : UserTable("st_user") {
    val domainId = integer("domainid")
    val specificId = varchar("specificid", 500)
    val state = varchar("state", 30)

    override fun anonymousUser(row: ResultRow): AnonymousUser {
        return AnonymousUser(row[id], row[domainId])
    }

    override fun update(row: ResultRow, stmt: UpdateStatement) {
        val anoUser = anonymousUser(row)
        if (!anoUser.isPlatFormAdmin) {
            stmt[firstName] = anoUser.firstName
            stmt[lastName] = anoUser.lastName
            stmt[email] = anoUser.email
            if ("DELETED" != row[state]) {
                stmt[login] = anoUser.login
                if ("REMOVED" != row[state])
                    SSVLogger.ofUsers().write(anoUser)
            }
        }
    }
}

/**
 * The users in the default Silverpeas domain.
 */
object SilverpeasDomainUser : DomainUserTable("domainsp_user") {

    override fun anonymousUser(row: ResultRow): AnonymousUser {
        return AnonymousUser(row[SilverpeasUser.id])
    }

    override fun anonymize() {
        join(
            SilverpeasUser,
            JoinType.INNER,
            SilverpeasUser.specificId,
            SilverpeasDomainUser.id.castTo<Column<String>>(VarCharColumnType())
        ).select { SilverpeasDomainUser.id neq 0 }.forUpdate().distinct().forEach { user ->
            update({ id eq user[id] }) {
                update(user, it)
            }
        }
    }
}

/**
 * The different types of groups.
 */
sealed class GroupTable(name: String = "") : Table(name),
    Anonymizing {
    val id = integer("id")
    val parentId = integer("supergroupid").references(id).nullable()
    val name = varchar("name", 100)
    val description = varchar("description", 400).nullable()

    override val primaryKey = PrimaryKey(id)

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { group ->
            val anoGroup = AnonymousGroup(group[id])
            update({ id eq group[id] }) { row ->
                row[name] = anoGroup.name
                row[description] = anoGroup.description
            }
        }
    }
}

/**
 * The groups in a given user domain in Silverpeas.
 */
open class DomainGroupTable(name: String = "") : GroupTable(name)

/**
 * All the groups in Silverpeas, whatever their domain.
 */
object SilverpeasGroup : GroupTable("st_group") {
    val domainId = integer("domainid")
    val specificId = varchar("specificid", 500)
}

/**
 * The groups in the default Silverpeas domain.
 */
object SilverpeasDomainGroup: DomainGroupTable("domainsp_group")

/**
 * The user memberships in the Silverpeas groups
 */
object SilverpeasGroupUserRelTable : Table("st_group_user_rel") {
    val userId = integer("userid") references SilverpeasUser.id
    val groupId = integer("groupid") references SilverpeasGroup.id
}