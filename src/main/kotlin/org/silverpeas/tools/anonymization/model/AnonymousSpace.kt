package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 *
 * @author mmoquillon
 */
class AnonymousSpace(val localId: Int, parent: Int? = null, val language: String = "fr") {
    val id = "WA${localId}"
    val name = "${Settings.spaces.namePrefix[language]} $localId"
    val description = Settings.spaces.description[language]
    val parentId = if (parent == null) "" else "WA${parent}"
}