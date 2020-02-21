package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings

/**
 * An anonymous application instance. It represents the result of the anonymizing of an application instance.
 * @author mmoquillon
 */
class AnonymousAppInst(val app: String, id: Int, space: Int? = null, language: String = "fr") {
    val id = "${app}${id}"
    val name = getParametrizedName(language, id)
    val description = Settings.appInsts.description[language]
    val spaceId = if (space == null) "" else "WA${space}"

    private fun getParametrizedName(language: String, id: Int): String {
        var name = Settings.appInsts.namePrefix[language]
        if (name == null || name.isEmpty()) {
            name = "$app $id"
        } else {
            name += " $id"
        }
        return name
    }
}