package com.xpenseledger.app.common

/**
 * Centralized constants for the entire application.
 * Reduces magic strings and makes values configurable.
 */

// ─────────────────────────────────────────────────────────────────────────────
//  Authentication Constants
// ─────────────────────────────────────────────────────────────────────────────

object AuthConstants {
    const val PIN_LENGTH = 6
    const val MAX_PIN_LENGTH = PIN_LENGTH
    const val MIN_PIN_LENGTH = PIN_LENGTH
    const val LEGACY_MAX_PIN_LENGTH = 5
    const val MAX_FAILED_ATTEMPTS = 5
    
    // Lockout durations in seconds for progressive delays
    val LOCKOUT_DURATIONS = intArrayOf(30, 60, 120, 300)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Session & Timeout Constants
// ─────────────────────────────────────────────────────────────────────────────

object SessionConstants {
    // Inactivity timeout before auto-locking (2 minutes)
    const val DEFAULT_TIMEOUT_MS = 2 * 60 * 1000L
    
    // How often to check for timeout (5 seconds)
    const val TIMEOUT_CHECK_INTERVAL_MS = 5_000L
    
    // How long a subscription stays alive before disposing (5 seconds)
    const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5_000L
}

// ─────────────────────────────────────────────────────────────────────────────
//  Date & Time Format Constants
// ─────────────────────────────────────────────────────────────────────────────

object DateFormats {
    // Display format used in UI (e.g., "Mar 26")
    const val DISPLAY_FORMAT = "MMM yy"
    
    // Key format used for grouping by month (e.g., "2026-03")
    const val KEY_FORMAT = "yyyy-MM"
    
    // Timestamp format for logging and debug output
    const val ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
    
    // Day format for individual transactions
    const val DAY_FORMAT = "dd MMM yyyy"
}

// ─────────────────────────────────────────────────────────────────────────────
//  Database Constants
// ─────────────────────────────────────────────────────────────────────────────

object DatabaseConstants {
    const val DB_NAME = "xpense_ledger.db"
    const val DB_VERSION = 6
    
    // Table names
    const val TABLE_EXPENSES = "expenses"
    const val TABLE_CATEGORIES = "categories"
    
    // Category type identifiers
    const val CATEGORY_TYPE_MAIN = "MAIN"
    const val CATEGORY_TYPE_SUB = "SUB"
    
    // Default fallback category for orphaned expenses
    const val DEFAULT_CATEGORY_ID = 82L
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category Constants
// ─────────────────────────────────────────────────────────────────────────────

object CategoryConstants {
    // Main category IDs
    const val FOOD_ID = 1L
    const val TRANSPORT_ID = 2L
    const val BILLS_ID = 3L
    const val SHOPPING_ID = 4L
    const val HEALTH_ID = 5L
    const val ENTERTAINMENT_ID = 6L
    const val FINANCE_ID = 7L
    const val OTHER_ID = 8L
    const val HOUSEHOLD_ID = 9L
    const val TRAVEL_ID = 200L
    
    // Food subcategories
    const val DINING_OUT_ID = 10L
    const val COFFEE_SNACKS_ID = 11L
    const val FOOD_DELIVERY_ID = 12L
    const val GROCERY_ID = 13L
}

// ─────────────────────────────────────────────────────────────────────────────
//  UI & Animation Constants
// ─────────────────────────────────────────────────────────────────────────────

object AnimationConstants {
    const val COUNTER_ANIMATION_DURATION_MS = 900
    const val PULSE_ANIMATION_DURATION_MS = 2800
    const val RING_SCALE_ANIMATION_DURATION_MS = 3000
    const val TRANSITION_ANIMATION_DURATION_MS = 700
}

object UiConstants {
    const val DEFAULT_PADDING_DP = 16
    const val DEFAULT_RADIUS_DP = 14
    const val DEFAULT_ELEVATION_DP = 8
    const val MIN_BUTTON_HEIGHT_DP = 50
}

// ─────────────────────────────────────────────────────────────────────────────
//  Encryption & Security Constants
// ─────────────────────────────────────────────────────────────────────────────

object SecurityConstants {
    const val SECURE_PREFS_NAME = "secure_prefs"
    const val PIN_SECURE_PREFS_NAME = "pin_secure"
    const val DB_KEY_STORAGE_KEY = "db_key"
}
