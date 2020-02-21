package org.silverpeas.tools.anonymization.ssv

import org.silverpeas.tools.anonymization.model.AnonymousSpace

/**
 * SSV file to save collaborative spaces data ready to be used in tests against a running Silverpeas platform.
 * @author mmoquillon
 */
object SpacesSSVFile: SSVFile("spaces.ssv") {

    override fun postOpening() {
        write("Id", "Parent")
    }

    fun write(space: AnonymousSpace) {
        write(space.id, space.parentId)
    }

}