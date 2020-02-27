package org.silverpeas.tools.anonymization.model

import org.silverpeas.tools.anonymization.Settings
import kotlin.reflect.KClass

/**
 * An anonymous node in an hierarchical tree of information categorization. A node is a generic representation of a
 * labelled container of data aiming to categorize, to classify the user contributions. It can be a folder in an EDM or
 * an album in a media library, or a category in a blog.
 * @author mmoquillon
 */
sealed class AnonymousNode(val id: Int, val instanceId: String, val parent: Int = NO_PARENT) {

    class Factory(val clazz: KClass<out AnonymousNode>, val instanceId: String) {

        fun make(id: Int, parent: Int = NO_PARENT, language: String = "fr"): AnonymousNode {
            return clazz.constructors.first().call(id, instanceId, parent, language)
        }
    }

    companion object {
        val NO_PARENT = -1

        fun of(instanceId: String) = when (instanceId.replace(Regex("\\d+"), "")) {
            "kmelia", "kmax", "toolbox" -> Factory(AnonymousFolder::class, instanceId)
            "gallery" -> Factory(AnonymousAlbum::class, instanceId)
            else -> Factory(AnonymousCategory::class, instanceId)
        }
    }

    abstract val name: String
    abstract val description: String
}

class AnonymousFolder(id: Int, instanceId: String, parent: Int = NO_PARENT, language: String) :
    AnonymousNode(id, instanceId, parent) {

    override val name = "${Settings.folders.namePrefix[language]} $id"
    override val description = "${Settings.folders.description[language]}"
}

class AnonymousAlbum(id: Int, instanceId: String, parent: Int = NO_PARENT, language: String) :
    AnonymousNode(id, instanceId, parent) {

    override val name = "${Settings.albums.namePrefix[language]} $id"
    override val description = "${Settings.albums.description[language]}"
}

class AnonymousCategory(id: Int, instanceId: String, parent: Int = NO_PARENT, language: String) :
    AnonymousNode(id, instanceId, parent) {

    override val name = "${Settings.categories.namePrefix[language]} $id"
    override val description = "${Settings.categories.description[language]}"

}