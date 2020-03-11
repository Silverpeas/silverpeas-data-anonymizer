package org.silverpeas.tools.anonymization.model

/**
 * An Access Control Entry in an ACL: the entry defines the access right of a user for a given application instance and
 * with which privilege (with which role)
 * @author mmoquillon
 */
class ACE(val appInstLocalId: Int, val userId: Int, val role: String)