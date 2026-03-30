# Technology Stack Documentation

## Overview

XpenseLedger uses a modern, production-grade technology stack built on the Android platform with Kotlin as the primary language.

## Core Technologies

### Operating System & Platform
| Component | Version | Purpose |
|-----------|---------|---------|
| **Android Framework** | API 24-36 | Core Android APIs and runtime |
| **Minimum SDK** | 24 | Support for Android 7.0+ |
| **Target SDK** | 36 | Target Android 15 (latest features) |
| **Compile SDK** | 36 | Build against latest APIs |

### Build Tools & Languages
| Component | Version | Purpose |
|-----------|---------|---------|
| **Kotlin** | 2.0.21 | Primary programming language |
| **JDK** | 17 | Java runtime for development |
| **Gradle** | 8.5.2 | Build automation and dependency management |
| **Android Gradle Plugin** | 8.5.2 | Android-specific Gradle extensions |

## Core Dependencies

### Jetpack Libraries

#### Compose & UI Framework
```
androidx.compose.ui:ui (2024.09.00)
androidx.compose.ui:ui-graphics (2024.09.00)
androidx.compose.ui:ui-tooling (2024.09.00)
androidx.compose.ui:ui-tooling-preview (2024.09.00)
androidx.compose.material3:material3 (Latest)
androidx.compose.material:material-icons-extended (Latest)
androidx.activity:activity-compose (1.8.0)
```

**Purpose**: Modern declarative UI framework for composing Android interfaces

**Key Features**:
- Function-based UI composition
- Reusable composable functions
- Hot reload support in development
- Material Design 3 components
- State management integrated

#### Navigation
```
androidx.navigation:navigation-compose (2.8.9)
androidx.hilt:hilt-navigation-compose (1.2.0)
```

**Purpose**: Type-safe, composable-based navigation

**Features**:
- Navigation graph support
- Deep linking capabilities
- State preservation during navigation
- Hilt integration for ViewModel scoping

#### Lifecycle Management
```
androidx.lifecycle:lifecycle-runtime-ktx (2.6.1)
androidx.core:core-ktx (1.10.1)
```

**Purpose**: Lifecycle-aware components and Kotlin extensions

**Features**:
- Lifecycle-aware coroutines
- ViewModels for screen state
- Extension functions for common tasks

### Dependency Injection

#### Hilt
```
com.google.dagger:hilt-android (2.52)
com.google.dagger:hilt-android-compiler (2.52)
androidx.hilt:hilt-navigation-compose (1.2.0)
```

**Purpose**: Dependency injection framework for Android

**Components Used**:
- `@HiltAndroidApp` for app initialization
- `@Module` and `@Provides` for dependency declaration
- `@Inject` for dependency injection
- Scoped components (Singleton, ActivityScope)

**Modules**:
- `DatabaseModule` - Database and persistence layer
- `RepositoryModule` - Repository implementations
- `SecurityModule` - Security-related dependencies

### Data Persistence

#### Room Database
```
androidx.room:room-runtime (2.6.1)
androidx.room:room-ktx (2.6.1)
androidx.room:room-compiler (2.6.1) [KSP]
```

**Purpose**: Type-safe database abstraction layer

**Features**:
- SQL database with type safety
- Automatic schema generation
- Built-in migration support
- Coroutine integration with Flow

**Database Name**: `xpenseledger_encrypted.db`

**Migrations Implemented**:
- Migration 1→2
- Migration 2→3
- Migration 3→4
- Migration 4→5

#### SQLCipher (Database Encryption)
```
net.zetetic:android-database-sqlcipher (Latest)
```

**Purpose**: Transparent database encryption

**Implementation**:
- Encrypts database file at rest
- Uses Android KeyStore for passphrase management
- Integrated with Room through `SupportFactory`

### Code Generation & Processing

#### Kotlin Symbol Processing (KSP)
```
com.google.devtools.ksp (2.0.21-1.0.28)
```

**Purpose**: Fast and lightweight code generation

**Used For**:
- Room database compilation
- Hilt dependency injection code generation
- Custom annotation processing

#### Kotlin Serialization
```
org.jetbrains.kotlin.plugin.serialization (2.0.21)
```

**Purpose**: Compile-time safe serialization

**Use Cases**:
- JSON serialization/deserialization
- Data import/export functionality
- API communication

### Testing Libraries

#### Unit Testing
```
junit:junit (4.13.2)
androidx.test.ext:junit (1.1.5)
```

**Purpose**: Unit test framework and Android test extensions

#### Instrumentation Testing
```
androidx.test.espresso:espresso-core (3.5.1)
androidx.compose.ui:ui-test-junit4
androidx.compose.ui:ui-test-manifest
```

**Purpose**: UI and integration testing

**Capabilities**:
- Espresso for UI automation
- Compose test utilities
- Device testing tools

### Build Enhancements

#### Compose Compiler Plugin
```
org.jetbrains.kotlin.plugin.compose (2.0.21)
```

**Purpose**: Kotlin compiler plugin for Compose optimization

## Dependency Management

### Centralized Version Catalog
Location: `gradle/libs.versions.toml`

All versions are defined centrally to ensure consistency:

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
composeBom = "2024.09.00"

[libraries]
# Listed with version references

[plugins]
# Gradle plugins with versions
```

## Security Stack

### Encryption & Cryptography
- **Database**: SQLCipher with Android KeyStore
- **Key Management**: Android KeyStore Manager
- **Algorithm**: AES (handled by Android framework)

### Authentication
- **PIN Protection**: Custom PIN manager
- **Biometric Auth**: BiometricPrompt API
- **Session Management**: Custom session timeout

### Code Obfuscation
- **R8**: Code minification and obfuscation
- **ProGuard Rules**: Custom rules in `proguard-rules.pro`
- **Resource Shrinking**: Enabled for release builds

## Performance Optimizations

### Compilation
- **Code Generation**: KSP for faster compilation than KAPT
- **Incremental Build**: Supported through Gradle
- **Parallel Execution**: Enabled for faster builds

### Runtime
- **Lazy Initialization**: Hilt lazy dependencies
- **Flow-based Reactivity**: Efficient state updates
- **Compose Recomposition**: Optimized with smart recomposition

## Compatibility Matrix

| Feature | Min SDK | Target SDK | Notes |
|---------|---------|----------|-------|
| Jetpack Compose | 21 | 36 | Full support |
| Material Design 3 | 21 | 36 | Full support |
| Biometric API | 28 | 36 | Graceful degradation |
| SQLCipher | 21 | 36 | Full support |
| Hilt | 14 | 36 | Full support |

## Build Configuration

### Debug Build
```
minifyEnabled = false
shrinkResources = false
debuggable = true
```

### Release Build
```
minifyEnabled = true
shrinkResources = true
debuggable = false
proguardFiles = [default, custom rules]
```

## Gradle Wrapper

- **Gradle Version**: 8.5.2 (pinned in wrapper)
- **Gradle Distribution**: Official distribution
- **Location**: `gradle/wrapper/`

## Known Third-Party Libraries

All third-party libraries are declared through version catalog. Common transitive dependencies include:

- **Kotlin Standard Library**
- **Kotlin Coroutines** (via lifecycle)
- **Material Components**
- **AndroidX Foundation Libraries**

## Version Update Strategy

### Patch Updates (x.x.Z)
- Security fixes and bug patches
- Update automatically when available
- No breaking changes expected

### Minor Updates (x.Y.0)
- New features and improvements
- Update quarterly or as needed
- Minor API additions, backward compatible

### Major Updates (X.0.0)
- Significant changes and breaking changes
- Plan and test thoroughly
- Update SDK targets and APIs as needed

## Notable Implementation Details

### Kotlin Extensions
- `androidx.core.ktx` provides shorthand functions
- `androidx.lifecycle.ktx` provides coroutine integration
- `androidx.room.ktx` provides Flow/Live data extensions

### Compose BOM (Bill of Materials)
- Ensures Compose libraries are compatible
- All Compose UI libraries inherit version from BOM
- Simplifies dependency management

### KSP vs KAPT
- Uses KSP for faster compilation
- Room compiler supports KSP directly
- Hilt compiler supports KSP directly
- Significantly faster build times compared to KAPT

## Environmental Setup

### Required Software
```
Android Studio: Arctic Fox (2021.1.1) or later
JDK: 17 (bundled with Android Studio)
SDK Platforms: API 24, 36 (downloaded via SDK Manager)
Gradle: 8.5.2 (via wrapper, no installation needed)
```

---

**Last Updated**: March 2026  
**Stack Version**: 1.0
