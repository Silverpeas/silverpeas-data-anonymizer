package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 * An anonymous publication in Silverpeas. A publication is a complex non-structured contribution in Silverpeas acting
 * as a link between different contents (form or WYSIWYG text, attachments, ...)
 * @author mmoquillon
 */
class AnonymousPublication(val id: Int, val instanceId: String, val nodeId: Int = -1, language: String = "fr") {
    val name = "${Settings.publications.namePrefix[language]} $id"
    val description = Settings.publications.description[language]
    val keywords = Settings.publications.keywords[language]
}