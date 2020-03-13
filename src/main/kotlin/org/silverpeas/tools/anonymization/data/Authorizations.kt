package org.silverpeas.tools.anonymization.data

import org.jetbrains.exposed.sql.*
import org.silverpeas.tools.anonymization.Anonymizing
import org.silverpeas.tools.anonymization.Settings
import org.silverpeas.tools.anonymization.model.ACE
import org.silverpeas.tools.anonymization.ssv.SSVLogger

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
 * The anonymization here is just for storing the ACL into a SSV file during the anonymization process. No real
 * data anonymization is performed on the ACL. The ACL to store into a SSV file can be filtered per application
 * instances.
 */
object Authorizations : Anonymizing {

    private fun writeACE(row: ResultRow) {
        SSVLogger.ofACL().write(ACE(row[Roles.instanceId], row[SilverpeasUser.id], row[Roles.roleName]))
    }
    /*
    select count(distinct u.id) from st_user u where u.id not in (select ur.userid from st_userrole_user_rel ur inner join st_userrole r on ur.userroleid = r.id inner join st_componentinstance i on i.id = r.instanceid where i.componentname in ('kmelia', 'toolbox', 'kmax')) or u.id not in (select ug.userid from st_group_user_rel ug inner join st_userrole_group_rel gr on gr.groupid = ug.userid inner join st_userrole r on gr.userroleid = r.id inner join st_componentinstance i on i.id = r.instanceid where i.componentname in ('kmelia', 'toolbox', 'kmax'));
     */
    override fun anonymize() {
        val query1 = (SilverpeasUser innerJoin SilverpeasGroupUserRelTable innerJoin GroupInRoles innerJoin Roles)
            .slice(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .select { Roles.nodeId.isNull() and (SilverpeasUser.state neq "DELETED")  }.withDistinct()
            .groupBy(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .orderBy(Pair(Roles.instanceId, SortOrder.ASC), Pair(SilverpeasUser.id, SortOrder.ASC))

        val query2 = (SilverpeasUser innerJoin UserInRoles innerJoin Roles)
            .slice(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .select { Roles.nodeId.isNull() and (SilverpeasUser.state neq "DELETED")  }.withDistinct()
            .groupBy(Roles.instanceId, SilverpeasUser.id, Roles.roleName)
            .orderBy(Pair(Roles.instanceId, SortOrder.ASC), Pair(SilverpeasUser.id, SortOrder.ASC))

        if (Settings.acl.appInst.isNotEmpty()) {
            query1.andWhere { Roles.instanceId inList Settings.acl.appInst }
            query2.andWhere { Roles.instanceId inList Settings.acl.appInst }
        }

        query1.forEach {
            writeACE(it)
        }

        query2.forEach {
            writeACE(it)
        }
    }
}