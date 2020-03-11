package org.silverpeas.tools.anonymization

/**
 * Interface qualifying an object as able to anonymize itself. Each object participating to the anonymization
 * of the data in Silverpeas must implement this interface.
 * @author mmoquillon
 */
interface Anonymizing {
    /**
     * Anonymize the data of the objects.
     */
    fun anonymize()
}



