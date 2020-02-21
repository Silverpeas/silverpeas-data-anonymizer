package org.silverpeas.tools.anonymization

import org.silverpeas.tools.anonymization.data.*

/**
 * Definitions of all of the functions to use for anonymizing the data in the Silverpeas database. Each function is
 * on a given data type. Those functions require to be registered within the dictionary of anonymization processors that
 * is used by the anonymization program.
 * @author mmoquillon
 */

val anonymizers = mapOf(
    "spaces" to {
        Space.anonymize()
        SpaceI18n.anonymize()
    },
    "components" to {
        ComponentInst.anonymize()
        ComponentInstI18n.anonymize()
    },
    "domains" to Domain::anonymize,
    "users" to {
        SilverpeasUser.anonymize()
        SilverpeasDomainUser.anonymize()
    },
    "groups" to {
        SilverpeasGroup.anonymize()
        SilverpeasDomainGroup.anonymize()
    },
    "nodes" to {
        Node.anonymize()
        NodeI18n.anonymize()
    },
    "publication" to {
        Publication.anonymize()

        PublicationI18n.anonymize()
    }
)
