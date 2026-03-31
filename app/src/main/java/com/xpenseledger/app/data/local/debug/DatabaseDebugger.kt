package com.xpenseledger.app.data.local.debug

/**
 * DEPRECATED: DatabaseDebugger has been replaced with direct database inspection.
 *
 * This utility was used for 8-step root cause analysis of transaction visibility issues,
 * but all debug methods have been removed from DAOs to reduce code bloat.
 *
 * If you need to debug database issues:
 * 1. Use Room InspectionCompanion or Database Inspector in Android Studio
 * 2. Check the database directly with ADB shell
 * 3. Examine migration logs during app startup
 * 4. Use logcat to search for build errors
 *
 * This file is kept for reference but should be deleted in the next cleanup.
 * The debugging approach was helpful but created technical debt with unused DAO methods.
 */
@Deprecated("Use Android Studio Database Inspector or ADB shell instead")
class DatabaseDebugger
