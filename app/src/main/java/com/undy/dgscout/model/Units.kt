package com.undy.dgscout.model

enum class DistanceUnit { FEET, METERS }

enum class UnitPreference {
    AUTOMATIC, FEET, METERS;

    fun resolve(localeDefault: DistanceUnit): DistanceUnit = when (this) {
        AUTOMATIC -> localeDefault
        FEET -> DistanceUnit.FEET
        METERS -> DistanceUnit.METERS
    }
}

/** The US is the only country where disc golfers commonly measure distance in feet. */
fun isUnitedStates(countryCode: String?): Boolean = countryCode?.equals("US", ignoreCase = true) == true
