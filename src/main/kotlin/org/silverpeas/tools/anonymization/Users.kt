package org.silverpeas.tools.anonymization

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.silverpeas.tools.anonymization.ssv.UsersSSVFile
import java.nio.file.Files
import java.nio.file.Paths
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

    private const val DOMAIN_DRIVER = "org.silverpeas.core.admin.domain.driver.sqldriver.SQLDriver"

    /**
     * Anonymizes all the additional user domains in Silverpeas. It anonymizes also the users and the groups that are
     * part of the anonymized domain.
     */
    override fun anonymize() {
        select { id greater 0 }.forUpdate().distinct().forEach { domain ->
            val newDomain = Settings.Domain(domain[id])
            if (domain[driver].endsWith("SQLDriver")) {
                renameSQLDomainUserAndGroupTables(domain, newDomain)
            } else if (domain[driver].endsWith("LDAPDriver")) {
                convertLDAPToSQLDomain(domain, newDomain)
            }
            update({ id eq domain[id] }) {
                it[name] = newDomain.name
                it[description] = newDomain.description
                it[serverUrl] = newDomain.serverUrl
                it[driver] = newDomain.driver
                it[authServer] = newDomain.authServerName
                it[descriptor] = newDomain.descriptor
            }
            val domainDesc = domain[descriptor].removePrefix("org.silverpeas.domains.") + ".properties"
            val authDesc = domain[authServer] + ".properties"
            renameDescriptorsFile(Pair(domainDesc, authDesc), newDomain.technicalName)
        }
    }

    private fun encodeToInt(value: String): Int {
        return value.hashCode().absoluteValue
    }

    private fun DomainUserTable.setCommonUserFields(row: InsertStatement<Number>, userId: Int) {
        val user = Settings.User(userId)
        row[firstName] = user.firstName
        row[lastName] = user.lastName
        row[email] = user.email
        row[login] = user.login
        row[password] = user.cryptedPassword
        row[company] = user.company
        UsersSSVFile.write(user)
    }

    private fun renameDescriptorsFile(descriptors: Pair<String, String>, newName: String) {
        if (Settings.isSilverpeasHomeDefined()) {
            val propertiesPath = Paths.get(Settings.silverpeasHome(), "properties", "org", "silverpeas")

            val domainDefPath = propertiesPath.resolve(Paths.get("domains", descriptors.first))
            val newDomainDefPath = domainDefPath.resolveSibling("domain${newName}.properties")

            val authDefPath = propertiesPath.resolve(Paths.get("authentication", descriptors.second))
            val newAuthDefPath = authDefPath.resolveSibling("auth${newName}.properties")

            if (!Files.exists(domainDefPath) || !Files.exists(authDefPath)) {
                println("The domain descriptors for ${descriptors.first} doesn't exist in the file system: do nothing")
            } else {
                Files.move(domainDefPath, newDomainDefPath)
                Files.move(authDefPath, newAuthDefPath)
            }
        }
    }

    private fun convertLDAPToSQLDomain(domain: ResultRow, newDomain: Settings.Domain) {
        val tableNamePrefix = "domain${newDomain.technicalName}_"
        val newUserTable = DomainUserTable("${tableNamePrefix}user")
        val newGroupTable = DomainGroupTable("${tableNamePrefix}group")
        val newGroupUserRelTable = object : Table("${tableNamePrefix}group_user_rel") {
            val userId = integer("userid") references newUserTable.id
            val groupId = integer("groupid") references newGroupTable.id
        }
        SchemaUtils.create(newUserTable, newGroupTable, newGroupUserRelTable)

        SilverpeasUser.select { (SilverpeasUser.domainId eq domain[id]) and (SilverpeasUser.state neq "DELETED") }
            .forUpdate().distinct().forEach { user ->
                val specificId = user[SilverpeasUser.specificId].toIntOrDefault(Domain::encodeToInt)
                newUserTable.insert { row ->
                    row[id] = specificId
                    setCommonUserFields(row, user[SilverpeasUser.id])
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

            val theGroup =  Settings.Group(group[SilverpeasGroup.id])
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

        val groupUserRelTable = object : Table("st_group_user_rel") {
            val userId = integer("userid") references SilverpeasUser.id
            val groupId = integer("groupid") references SilverpeasGroup.id
        }
        groupUserRelTable.innerJoin(SilverpeasUser).innerJoin(SilverpeasGroup).select {
            (SilverpeasUser.domainId eq domain[id]) and (SilverpeasGroup.domainId eq domain[id])
        }.forEach { rel ->
            newGroupUserRelTable.insert { row ->
                row[userId] = rel[SilverpeasUser.specificId].toIntOrDefault(Domain::encodeToInt)
                row[groupId] = rel[SilverpeasGroup.specificId].toIntOrDefault(Domain::encodeToInt)
            }
        }
    }

    private fun renameSQLDomainUserAndGroupTables(domain: ResultRow, newDomain: Settings.Domain) {
        val newTableNamePrefix = "domain${newDomain.technicalName}_"
        val newUserTable = DomainUserTable("${newTableNamePrefix}user")
        val newGroupTable = DomainGroupTable("${newTableNamePrefix}group")
        val newGroupUserRelTable = object : Table("${newTableNamePrefix}group_user_rel") {
            val userId = integer("userid") references newUserTable.id
            val groupId = integer("groupid") references newGroupTable.id
        }
        SchemaUtils.create(newUserTable, newGroupTable, newGroupUserRelTable)

        val tableNamePrefix = domain[descriptor].removePrefix("org.silverpeas.domains.").toLowerCase()
        val userTable = DomainUserTable("${tableNamePrefix}_user")
        val groupTable = DomainGroupTable("${tableNamePrefix}_group")
        val groupUserRelTable = object : Table("${tableNamePrefix}_group_user_rel") {
            val userId = integer("userid") references userTable.id
            val groupId = integer("groupid") references groupTable.id
        }
        userTable.selectAll().distinct().forEach { user ->
            newUserTable.insert { row ->
                row[id] = user[userTable.id]
                setCommonUserFields(row, user[userTable.id])
                row[title] = user[userTable.title]
                row[position] = user[userTable.position]
            }
        }
        groupTable.selectAll().distinct().forEach { group ->
            val theGroup = Settings.Group(group[groupTable.id])
            newGroupTable.insert { row ->
                row[id] = theGroup.id
                row[parentId] = group[groupTable.parentId]
                row[name] = theGroup.name
                row[description] = theGroup.description
            }
        }
        groupUserRelTable.selectAll().distinct().forEach { rel ->
            newGroupUserRelTable.insert { row ->
                row[userId] = rel[groupUserRelTable.userId]
                row[groupId] = rel[groupUserRelTable.groupId]
            }
        }

        SchemaUtils.drop(groupUserRelTable, userTable, groupTable)
    }
}

/**
 * The different types of users.
 */
open class UserTable(name: String = "") : Table(name), Anonymizing {
    val id = integer("id")
    val firstName = varchar("firstname", 100).nullable()
    val lastName = varchar("lastname", 100)
    val email = varchar("email", 100).nullable()
    val login = varchar("login", 50)

    override val primaryKey = PrimaryKey(id)

    protected open fun update(user: Settings.User, stmt: UpdateStatement) {
        stmt[firstName] = user.firstName
        stmt[lastName] = user.lastName
        stmt[email] = user.email
        stmt[login] = user.login
    }

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { user ->
            update({ id eq user[id] }) {
                val theUser = Settings.User(user[id])
                update(theUser, it)
                UsersSSVFile.write(theUser)
            }
        }
    }
}

/**
 * The users in a given user domain in Silverpeas.
 */
open class DomainUserTable(name: String = "") : UserTable(name) {
    val password = varchar("password", 123).nullable()
    val company = varchar("company", 50).nullable()
    val phone = varchar("phone", 20).nullable()
    val homePhone = varchar("homephone", 20).nullable()
    val cellPhone = varchar("cellphone", 20).nullable()
    val fax = varchar("fax", 20).nullable()
    val address = varchar("address", 500).nullable()
    val title = varchar("title", 100).nullable()
    val position = varchar("position", 100).nullable()
    val boss = varchar("boss", 100).nullable()
    val isPasswordValid = char("passwordvalid").default('Y')

    override fun update(user: Settings.User, stmt: UpdateStatement) {
        super.update(user, stmt)
        stmt[password] = user.cryptedPassword
        stmt[company] = user.company
    }
}

/**
 * All the users in Silverpeas, whatever their domain.
 */
object SilverpeasUser : UserTable("st_user") {
    val domainId = integer("domainid")
    val specificId = varchar("specificid", 500)
    val state = varchar("state", 30)
}

/**
 * The different types of groups.
 */
open class GroupTable(name: String = "") : Table(name), Anonymizing {
    val id = integer("id")
    val parentId = integer("supergroupid").references(id).nullable()
    val name = varchar("name", 100)
    val description = varchar("description", 400).nullable()

    override val primaryKey = PrimaryKey(id)

    override fun anonymize() {
        selectAll().forUpdate().distinct().forEach { group ->
            val theGroup = Settings.Group(group[id])
            update({ id eq group[id] }) { row ->
                row[name] = theGroup.name
                row[description] = theGroup.description
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
