# XpenseLedger - Project Documentation

## Overview

XpenseLedger is a secure, feature-rich Android expense management application built with modern Android development practices. It provides users with a comprehensive tool for tracking, categorizing, and managing their financial transactions with enterprise-grade security measures.

## Quick Links

- [Technology Stack](TECH_STACK.md) - Complete list of dependencies and versions
- [Architecture & Design](ARCHITECTURE.md) - System design, patterns, and structure
- [Framework Details](FRAMEWORK.md) - Android framework specifics and components
- [Development Workflow](WORKFLOW.md) - Development, build, and deployment processes

## Project Specifications

| Property | Value |
|----------|-------|
| **Target SDK** | 36 |
| **Min SDK** | 24 |
| **Build Tool** | Gradle 8.5.2 |
| **Language** | Kotlin 2.0.21 |
| **JDK Version** | 17 |
| **Package Name** | com.xpenseledger.app |
| **Version** | 1.0 |

## Key Features

### Core Functionality
- **Expense Tracking** - Add, edit, delete, and categorize expenses
- **Category Management** - Create and customize expense categories
- **Data Export/Import** - Export expenses to external formats and import from files
- **Statistics & Reports** - Visual representations of spending patterns

### Security Features
- **PIN Protection** - Secure PIN-based app access
- **Biometric Authentication** - Fingerprint/Face recognition support
- **Database Encryption** - SQLCipher encrypted local database
- **Secure Storage** - Android KeyStore integration
- **Session Management** - Automatic session timeout and management

### User Experience
- **Jetpack Compose UI** - Modern, declarative UI framework
- **Material Design 3** - Latest Material Design standards
- **Navigation Compose** - Type-safe navigation
- **User Profiles** - Per-device user profiles and settings

## Module Structure

```
XpenseLedger/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── java/com/xpenseledger/app/
│   │   │   ├── data/             # Data layer (repositories, DAOs, entities)
│   │   │   ├── domain/           # Domain layer (models, use cases)
│   │   │   ├── ui/               # UI layer (screens, navigation, viewmodels)
│   │   │   ├── security/         # Security components (auth, encryption)
│   │   │   ├── di/               # Dependency injection modules
│   │   │   └── utils/            # Utility functions
│   │   ├── res/                  # Resources (layouts, strings, drawables)
│   │   └── AndroidManifest.xml   # App manifest
│   ├── build.gradle.kts          # App-level build configuration
│   └── proguard-rules.pro        # ProGuard rules for release builds
├── gradle/libs.versions.toml     # Centralized dependency versions
├── build.gradle.kts              # Root build configuration
└── gradle.properties             # Gradle configuration
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK 36 (API level 36)
- Gradle 8.5.2 or compatible (wrapper included)

### Build Instructions
```bash
# Clone the repository
git clone <repository>

# Build debug APK
./gradlew build

# Build release APK
./gradlew buildRelease

# Run tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### Run Instructions
```bash
# Run debug build
./gradlew installDebug

# Launch app on connected device
adb shell am start -n com.xpenseledger.app/.ui.activity.MainActivity
```

## Development Quick References

### Adding a Feature
1. Define domain models in `domain/model/`
2. Create repository interface in `domain/repository/`
3. Implement repository in `data/repository/`
4. Create use cases in `domain/usecase/`
5. Build ViewModel in `ui/viewmodel/`
6. Create UI composables in `ui/screen/`
7. Add navigation routes in `ui/navigation/NavRoutes.kt`
8. Wire dependencies in `di/` modules

### Database Operations
- Use Room DAOs for database access
- Leverage Flow/StateFlow for reactive updates
- Apply migrations in `DatabaseModule.kt`

### UI Development
- Use Jetpack Compose for all new UI
- Follow Material Design 3 guidelines
- Implement state management with ViewModels
- Use Navigation Compose for screen transitions

### Security Considerations
- Use `KeyStoreManager` for sensitive key storage
- Leverage `EncryptionManager` for data encryption
- Implement PIN verification through `PinManager`
- Use `BiometricAuthManager` for biometric authentication
- Manage user sessions with `SessionManager`

## Build Configuration

All dependencies and plugins are managed through centralized version catalog in `gradle/libs.versions.toml`. When adding new dependencies:

1. Add version in `[versions]`
2. Add library reference in `[libraries]`
3. Add plugin reference if needed in `[plugins]`
4. Reference in build.gradle.kts using `libs.xxx`

## ProGuard/R8 Configuration

Place ProGuard rules in `app/proguard-rules.pro`. Key rules are applied:
- Resource shrinking enabled in release builds
- Code minification enabled for security
- Custom keep rules for serialization and reflection

## Testing

### Unit Tests
Located in `src/test/` - Test business logic and utilities

### Instrumentation Tests
Located in `src/androidTest/` - Test UI and Android integrations

## Code Style & Standards

- **Language**: Kotlin
- **Pattern**: MVVM + Clean Architecture
- **State Management**: Flow/StateFlow with ViewModels
- **UI Framework**: Jetpack Compose
- **DI Framework**: Hilt

## Version Control

- Branch strategy: Feature branches from main
- Commit messages: Clear, descriptive messages
- Pull requests: Required before merging to main
- Code review: All changes reviewed before merge

## Troubleshooting

### Common Issues

**Build Fails**
- Clear build cache: `./gradlew clean`
- Invalidate Android Studio cache: File → Invalidate Caches
- Check JDK version: Ensure Java 17 is configured

**App Crashes on Startup**
- Check database migrations in `DatabaseModule`
- Verify all Hilt modules are properly installed
- Review logcat for detailed error messages

**Compose Preview Not Working**
- Rebuild project with `./gradlew build`
- Check Compose version compatibility
- Ensure kotlinCompilerExtensionVersion is set

## Additional Resources

- [Android Developer Docs](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Material Design 3](https://m3.material.io/)

## Support & Contact

For issues, questions, or contributions, please refer to the project repository and documentation.

---

**Last Updated**: March 2026  
**Documentation Version**: 1.0
