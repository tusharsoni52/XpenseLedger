# SQLCipher 16 KB Alignment Issue - Solution Guide

## Issue Summary

**Warning**: `libsqlcipher.so` is not aligned at 16 KB boundaries  
**Impact**: App will be rejected on Google Play for Android 15+ targeting  
**Deadline**: November 1st, 2025  
**Current Version**: SQLCipher 4.5.4  
**App Target SDK**: 36 (Android 15)

## Root Cause

SQLCipher 4.5.4's pre-compiled native libraries (`libsqlcipher.so`) in the AAR don't have LOAD segments aligned at 16 KB boundaries. This is required by Android 15+ and enforced by Google Play.

## Solutions (Ranked by Feasibility)

### Solution 1: Update SQLCipher (RECOMMENDED) ⭐

Check if SQLCipher has released a newer version with 16 KB alignment support.

**Current**: `net.zetetic:android-database-sqlcipher:4.5.4`

**Action**: Update to the latest stable version:

```gradle
// In app/build.gradle.kts
implementation("net.zetetic:android-database-sqlcipher:4.6.0")  // Or newer
```

**Steps**:
1. Check [SQLCipher releases](https://www.zetetic.net/sqlcipher/android/) for latest version
2. Update the dependency version
3. Test thoroughly with database encryption
4. Rebuild APK and check for warning

**Testing**:
```bash
# Build release APK
./gradlew assembleRelease

# Check for 16 KB alignment warnings
# If gone, you're set!
```

---

### Solution 2: Workaround - Rebuild SQLCipher Locally

If no newer version available, rebuild SQLCipher with proper alignment.

**Complexity**: HIGH  
**Time Estimate**: 4-8 hours

**Requirements**:
- Android NDK
- Build tools
- SQLCipher source code

**Steps**:
1. Download SQLCipher source: https://www.zetetic.net/sqlcipher/open/
2. Configure build for 16 KB alignment
3. Build native libraries
4. Create custom AAR
5. Reference local AAR in gradle

**Not Recommended** for most projects due to complexity.

---

### Solution 3: Alternative - Use Room with Built-in Encryption (FUTURE)

Android 15+ provides built-in database encryption alternatives.

**Notes**:
- Requires API 35+ (your minSdk is 24, so not immediately viable)
- Conditional usage pattern needed
- Would need significant refactoring

---

### Solution 4: Defer SDK 36 Targeting (NOT RECOMMENDED)

Keep targeting SDK 35 temporarily, but:
- ❌ Miss out on latest Android features
- ❌ Google Play will eventually require SDK 36
- ❌ Not a real solution, just delays problem

---

## Immediate Action Plan

### SHORT TERM (Next Release)

1. **Check SQLCipher Updates**
   ```bash
   # Check Maven Central for newer versions
   # https://mvnrepository.com/artifact/net.zetetic/android-database-sqlcipher
   ```

2. **Try Latest Version**
   ```gradle
   // In app/build.gradle.kts, line 87
   implementation("net.zetetic:android-database-sqlcipher:4.6.0")  // Or latest
   ```

3. **Build and Verify**
   ```bash
   ./gradlew clean assembleRelease
   ```

4. **Check Warning Gone**
   - If warning disappears → Deploy!
   - If warning remains → Contact SQLCipher support

---

### MEDIUM TERM (If No Official Fix)

Contact SQLCipher support:
- https://www.zetetic.net/sqlcipher/support/
- Report the 16 KB alignment issue
- Request updated AAR or guidance

---

### LONG TERM (Backup Plan)

If SQLCipher doesn't fix:
1. Evaluate alternative encryption libraries
2. Plan migration strategy
3. Test with actual data backup/restore

## Current Configuration

**File**: `app/build.gradle.kts` (Line 87)

```kotlin
// SQLCipher for encrypted Room database (latest available on Maven Central)
implementation("net.zetetic:android-database-sqlcipher:4.5.4")  // ← Update this
```

## What NOT to Do

❌ Ignore the warning - Google Play will reject your app  
❌ Lower targetSdk to avoid requirement - Only delays problem  
❌ Remove encryption - Violates security best practices  
❌ Use ProGuard/R8 tricks - Won't solve native library alignment  

## Testing the Fix

### Test 1: Build Release APK
```bash
./gradlew clean assembleRelease
```

### Test 2: Check for Warnings
```bash
# Look for output containing:
# "APK app-release.apk is not compatible with 16 KB devices"
# If NOT present → Issue fixed ✅
```

### Test 3: Verify Database Encryption Still Works
```kotlin
// In unit/instrumentation tests:
// 1. Create database with sensitive data
// 2. Verify data encrypts/decrypts correctly
// 3. Check database file is not readable as plaintext
```

### Test 4: Upload to Play Console (Test Release)
1. Internal testing track
2. Check pre-launch report
3. Verify no 16 KB alignment errors

---

## Version Strategy

| Version | 16 KB Support | Status | Action |
|---------|---------------|--------|--------|
| 4.5.4 | ❌ No | Current | Update ASAP |
| 4.6.0+ | ✅ Yes (TBD) | TBD | Check & upgrade |
| 5.0.0+ | ✅ Yes (TBD) | TBD | Evaluate for future |

---

## Timeline

| Date | Milestone | Action |
|------|-----------|--------|
| **NOW** | Issue discovered | Update SQLCipher version |
| **Q2 2026** | Pre-production testing | Verify on Play Console test track |
| **Q3 2026** | Final testing | Stress test encryption |
| **Before 11/1/2025** | Release | Deploy to production |

---

## FAQ

**Q: Will my app be rejected?**  
A: Yes, starting November 1st, 2025 for new apps or updates on Google Play.

**Q: Does this affect existing users?**  
A: No, only new installs on Google Play. Existing users won't be affected.

**Q: Can I just update SQLCipher version?**  
A: Try it first - it's the simplest solution. Most dependency updates are drop-in replacements.

**Q: Do I need to migrate existing database?**  
A: No, SQLCipher backward compatible. Old databases will work with new version.

**Q: What if newer SQLCipher has breaking changes?**  
A: Run full test suite, especially:
- Database creation
- Encryption/decryption
- Data migration
- Backup/restore functionality

**Q: What about keyboard/input libraries with native code?**  
A: If you add other native libraries in future, check their 16 KB alignment too.

---

## References

- **Android 15 Requirements**: https://developer.android.com/about/versions/15
- **16 KB Page Size**: https://developer.android.com/about/versions/15/behavior-changes-15#16kb
- **SQLCipher Android**: https://www.zetetic.net/sqlcipher/android/
- **Play Console Help**: https://support.google.com/googleplay/android-developer/answer/14139265

---

## Action Checklist

### Immediate (This Week)
- [ ] Check SQLCipher latest version availability
- [ ] Read SQLCipher release notes for breaking changes
- [ ] Update version in `app/build.gradle.kts`
- [ ] Run `./gradlew clean build`
- [ ] Check for compilation errors
- [ ] Run unit and instrumentation tests

### Near-Term (This Month)
- [ ] Test on physical device/emulator
- [ ] Verify database encryption works
- [ ] Test data import/export
- [ ] Check APK for warning message

### Pre-Release (Before 11/1/2025)
- [ ] Build final APK
- [ ] Verify no 16 KB alignment warnings
- [ ] Upload to Play Console test track
- [ ] Check pre-launch report
- [ ] Get final approval before production release

---

**Status**: ⚠️ **ACTION REQUIRED - Priority: HIGH**

**Deadline**: November 1st, 2025 (if publishing to Google Play)  
**Time to Fix**: 15 minutes (if update available) - 4+ hours (if rebuild needed)  
**Recommendation**: Start with version update, test thoroughly
