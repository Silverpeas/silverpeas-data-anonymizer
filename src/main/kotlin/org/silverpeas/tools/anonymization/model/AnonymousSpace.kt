package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 *
 * @author mmoquillon
 */
class AnonymousSpace(id: Int, parent: Int? = null, val language: String = "fr") {
    val id = "WA${id}"
    val name = "${Settings.spaces.namePrefix[language]} $id"
    val description = Settings.spaces.description[language]
    val parentId = if (parent == null) "" else "WA${parent}"
}