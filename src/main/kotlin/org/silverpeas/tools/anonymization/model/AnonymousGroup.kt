package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 * An anonymous group of users in Silverpeas.
 * @author mmoquillon
 */
class AnonymousGroup(val id: Int) {

    val name = "${Settings.groups.namePrefix} $id"
    val description = Settings.groups.description
}