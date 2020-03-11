package org.silverpeas.tools.anonymization.data

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.model.ACE
import org.silverpeas.tools.anonymization.model.AnonymousAppInst
import org.silverpeas.tools.anonymization.model.AnonymousUser
import org.silverpeas.tools.anonymization.ssv.SSVLogger
import kotlin.system.measureTimeMillis

/**
 * The authorizations of the users to access a component instance.
 * @author mmoquillon
 */

/**
 * The roles in Silverpeas
 */
object Roles : Table("st_userrole") {
    val id = integer("id")
    val instanceId = integer("instanceid") references ComponentInst.id
    val nodeId = integer("objectid").nullable()
    val roleName = varchar("rolename", 100)

    override val primaryKey = PrimaryKey(id)
}

/**
 * The roles playing by the users in Silverpeas
 */
object UserInRoles : Table("st_userrole_user_rel") {
    val roleId = integer("userroleid") references Roles.id
    val userId = integer("userid") references SilverpeasUser.id
}

/**
 * The roles playing by the group of users in Silverpeas
 */
object GroupInRoles : Table("st_userrole_group_rel") {
    val roleId = integer("userroleid") references Roles.id
    val groupId = integer("groupid") references SilverpeasGroupUserRelTable.groupId
}

/**
 * The authorizations of both the users and the groups of users to access the application instances in Silverpeas.
 * The anonymization here just store the ACL into a SSV file. No real anonymization is performed on the ACL.
 */
object Authorizations : Anonymizing {

    private fun writeACE(row: ResultRow) {
        SSVLogger.ofACL().write(ACE(row[Roles.instanceId], row[SilverpeasUser.id], row[Roles.roleName]))
    }

    override fun anonymize() = runBlocking {
        (SilverpeasUser innerJoin SilverpeasGroupUserRelTable innerJoin GroupInRoles innerJoin Roles)
            .slice(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .select { Roles.nodeId.isNull() }.withDistinct()
            .groupBy(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .orderBy(Pair(Roles.instanceId, SortOrder.ASC), Pair(SilverpeasUser.id, SortOrder.ASC))
            .forEach { row ->
                writeACE(row)
            }
        (SilverpeasUser innerJoin UserInRoles innerJoin Roles)
            .slice(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .select { Roles.nodeId.isNull() }.withDistinct()
            .groupBy(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .orderBy(Pair(Roles.instanceId, SortOrder.ASC), Pair(SilverpeasUser.id, SortOrder.ASC))
            .forEach { row ->
                writeACE(row)
            }
    }
}