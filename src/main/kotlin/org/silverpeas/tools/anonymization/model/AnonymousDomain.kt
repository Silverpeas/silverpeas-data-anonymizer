package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 * An anonymous Silverpeas domain of users.
 * @author mmoquillon
 */
class AnonymousDomain(id: Int) {

    val name: String
    val description = null
    val serverUrl = Settings.domains.serverUrl
    val driver = "org.silverpeas.core.admin.domain.driver.sqldriver.SQLDriver"
    val type: String
    val usersTableName: String
    val groupsTableName: String
    val groupUserRelsTableName: String
    val authServerName: String
    val authDescriptorName: String
    val descriptorName: String

    init {
        val prefix = Settings.domains.namePrefix
        val technicalName = prefix + id
        val tablePrefix = "domain${technicalName}_"
        name = "$prefix $id"
        type = "org.silverpeas.domains.domain${technicalName}"
        usersTableName = tablePrefix + "user"
        groupsTableName = tablePrefix + "group"
        groupUserRelsTableName = tablePrefix + "group_user_rel"
        authServerName = "autDomain${technicalName}"
        authDescriptorName = "$authServerName.properties"
        descriptorName = "domain${technicalName}.properties"
    }
}