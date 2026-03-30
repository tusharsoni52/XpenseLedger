# Documentation Index

## 📚 Complete Documentation Library for XpenseLedger

Welcome to the XpenseLedger development documentation. This index helps you navigate all available resources.

## Quick Navigation

### For New Developers
Start here to understand the project:
1. **[README.md](README.md)** - Project overview and getting started (10 min read)
2. **[TECH_STACK.md](TECH_STACK.md)** - Technologies used (15 min read)
3. **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design and patterns (20 min read)
4. **[FRAMEWORK.md](FRAMEWORK.md)** - Android framework details (15 min read)
5. **[WORKFLOW.md](WORKFLOW.md)** - Development process (25 min read)

### For Feature Development
When building new features:
1. Read: **[ARCHITECTURE.md](ARCHITECTURE.md)** - Understand the design patterns
2. Reference: **[FRAMEWORK.md](FRAMEWORK.md)** - Android components & Compose
3. Follow: **[WORKFLOW.md](WORKFLOW.md)** - Feature development steps

### For Debugging
When troubleshooting:
1. Check: **[WORKFLOW.md](WORKFLOW.md#debugging-workflow)** - Debugging techniques
2. Review: **[TECH_STACK.md](TECH_STACK.md)** - Dependency versions
3. Consult: **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design

### For Building & Testing
When preparing builds:
1. Follow: **[WORKFLOW.md](WORKFLOW.md#build-workflow)** - Build commands
2. Reference: **[WORKFLOW.md](WORKFLOW.md#testing-workflow)** - Testing procedures
3. Check: **[WORKFLOW.md](WORKFLOW.md#release-workflow)** - Release process

## Document Overview

### README.md
**Purpose**: Project overview and quick reference

**Contents**:
- Project specifications (SDK versions, languages)
- Key features (tracking, security, UI)
- Module structure
- Getting started instructions
- Development quick references
- Build configuration
- ProGuard settings
- Testing guides
- Code style & standards
- Troubleshooting

**Best For**: Understanding what the project does, how to build it, and basic troubleshooting

**Estimated Read Time**: 10 minutes

---

### TECH_STACK.md
**Purpose**: Detailed technology and dependency documentation

**Contents**:
- Core technologies (Android, Kotlin, JDK)
- Build tools and versions
- Jetpack libraries (Compose, Navigation, Lifecycle)
- Dependency Injection (Hilt)
- Data persistence (Room, SQLCipher)
- Code generation (KSP, Serialization)
- Testing libraries (JUnit, Espresso, Compose Test)
- Encryption & Security libraries
- Performance optimizations
- Compatibility matrix
- Version update strategy
- Environmental setup

**Best For**: Understanding what libraries are used, their versions, and why

**Estimated Read Time**: 15 minutes

---

### ARCHITECTURE.md
**Purpose**: System design, patterns, and structure

**Contents**:
- High-level architecture overview
- Three-layer architecture (Presentation, Domain, Data)
- Presentation layer (Screens, ViewModels, Navigation)
- Domain layer (Models, Repository interfaces, Use Cases)
- Data layer (DAOs, Entities, Mappers, Encryption)
- Dependency Injection architecture (Modules)
- Security architecture (Encryption, Auth, Biometrics)
- Data flow diagrams
- Navigation architecture
- Testing architecture
- State management strategy
- Concurrency model (Coroutines, Flow)
- Performance considerations
- Scalability guidelines

**Best For**: Understanding system design, adding new features, and grasping the overall structure

**Estimated Read Time**: 20 minutes

---

### FRAMEWORK.md
**Purpose**: Android framework specifics and component usage

**Contents**:
- Application entry points (XpenseLedgerApp, MainActivity)
- Jetpack Compose concepts
- Material Design 3 components
- Navigation system (NavGraph, routes)
- ViewModel & State management
- Activity lifecycle
- Resource management (strings, colors, dimensions)
- Android Manifest structure
- Theme system (Material Design 3)
- Asset management (icon generation)
- Build features configuration
- Testing framework integration
- System integration points
- Hilt integration with components
- Configuration changes handling
- Accessibility features

**Best For**: Understanding Android-specific implementations and Compose usage

**Estimated Read Time**: 15 minutes

---

### WORKFLOW.md
**Purpose**: Development, building, testing, and deployment processes

**Contents**:
- Environment setup and prerequisites
- IDE configuration
- Gradle build system
- Build commands (debug, release, tests)
- Build variants (debug vs release)
- Build troubleshooting
- Unit testing workflow
- Instrumentation testing workflow
- Test coverage
- Feature development step-by-step
- Code review process
- Debugging workflow
- Release preparation
- Distribution methods
- CI/CD integration
- Performance profiling
- Documentation maintenance

**Best For**: Building the project, running tests, developing features, and releasing

**Estimated Read Time**: 25 minutes

---

## Key Technologies at a Glance

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Kotlin | 2.0.21 |
| **UI Framework** | Jetpack Compose | 2024.09.00 |
| **Database** | Room + SQLCipher | 2.6.1 |
| **DI Container** | Hilt | 2.52 |
| **Navigation** | Navigation Compose | 2.8.9 |
| **Build Tool** | Gradle | 8.5.2 |
| **JDK** | Java | 17 |
| **Min/Target SDK** | Android | 24/36 |

## Common Development Tasks

### I want to...

#### Add a new feature
→ Follow steps in **[WORKFLOW.md](WORKFLOW.md#creating-a-new-feature)**
→ Review pattern in **[ARCHITECTURE.md](ARCHITECTURE.md#scalability)**

#### Fix a bug
→ Debug using guide in **[WORKFLOW.md](WORKFLOW.md#debugging-workflow)**
→ Check architecture layer in **[ARCHITECTURE.md](ARCHITECTURE.md)**

#### Run tests
→ See **[WORKFLOW.md](WORKFLOW.md#testing-workflow)**
→ Learn patterns in **[ARCHITECTURE.md](ARCHITECTURE.md#testing-architecture)**

#### Build for release
→ Follow **[WORKFLOW.md](WORKFLOW.md#release-workflow)**
→ Check minification in **[README.md](README.md#preguardr8-configuration)**

#### Add a new dependency
→ Guide in **[TECH_STACK.md](TECH_STACK.md#dependency-management)**
→ Check compatibility in **[TECH_STACK.md](TECH_STACK.md#compatibility-matrix)**

#### Understand the database
→ See schema in **[ARCHITECTURE.md](ARCHITECTURE.md#database-design)**
→ Check encryption in **[ARCHITECTURE.md](ARCHITECTURE.md#security-architecture)**

#### Set up environment
→ Follow **[WORKFLOW.md](WORKFLOW.md#development-environment-setup)**
→ Check requirements in **[README.md](README.md#prerequisites)**

#### Optimize performance
→ Read **[WORKFLOW.md](WORKFLOW.md#performance-optimization-workflow)**
→ Check patterns in **[ARCHITECTURE.md](ARCHITECTURE.md#performance-considerations)**

## Architecture Quick Reference

### Three Layers
```
UI (Compose, ViewModels, Navigation)
↓
Domain (Models, Use Cases, Repository Interfaces)
↓
Data (Room DB, DAOs, Entities, Repositories)
```

### Data Flow
```
User Action → ViewModel → Use Case → Repository → DAO → Database
Database → DAO → Repository → ViewModel (Flow) → Compose UI Recomposition
```

### Key Directories
```
data/          Data layer (DB, repositories, mappers)
domain/        Domain layer (models, use cases)
ui/            Presentation layer (screens, viewmodels)
security/      Security components (crypto, auth)
di/            Dependency injection modules
```

## Best Practices Summary

### Code Organization
- One feature per package/folder
- Clear separation of concerns
- DI modules for each layer

### State Management
- StateFlow in ViewModels
- Never mutate state directly
- Use coroutines safely with viewModelScope

### Testing
- Unit test business logic
- Instrument test UI
- Mock dependencies

### Security
- Use KeyStore for sensitive data
- Encrypt database (SQLCipher)
- Validate user input

### Performance
- Lazy load collections
- Remember expensive computations
- Minimize recomposition

### Accessibility
- Semantic modifiers on Compose
- Content descriptions for images
- Sufficient touch targets

## Getting Help

### For Errors
1. Check **[WORKFLOW.md](WORKFLOW.md#build-troubleshooting)** - Common issues
2. Review corresponding documentation section
3. Check logcat output
4. Review code comments

### For Understanding
1. Read relevant documentation section
2. Review example code in the module
3. Check test files for patterns
4. Run and debug locally

### For Features
1. Check **[WORKFLOW.md](WORKFLOW.md#feature-development-workflow)**
2. Review **[ARCHITECTURE.md](ARCHITECTURE.md#scalability)**
3. Follow established patterns

## Documentation Metadata

| Metadata | Value |
|----------|-------|
| **Created** | March 2026 |
| **Last Updated** | March 2026 |
| **Documentation Version** | 1.0 |
| **Project Version** | 1.0 |
| **Target Audience** | Developers (all levels) |
| **Total Read Time** | ~95 minutes (all docs) |

## Related Resources

### External Documentation
- [Android Developer Docs](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)

### Tools & IDE
- [Android Studio IDE](https://developer.android.com/studio)
- [Gradle Build Tool](https://gradle.org/)
- [Git Version Control](https://git-scm.com/)

## File Locations

All documentation files are located in:
```
app/src/main/resources/docs/
├── README.md           (Project overview)
├── TECH_STACK.md       (Technology details)
├── ARCHITECTURE.md     (System design)
├── FRAMEWORK.md        (Android framework)
├── WORKFLOW.md         (Development process)
└── INDEX.md            (This file)
```

## Changelog

### Version 1.0 (March 2026)
- Initial documentation created
- All core documents written
- Index created

---

**Navigation Tip**: Use Ctrl+Click (or Cmd+Click on Mac) on markdown links to jump between documents in your IDE.

**Bookmark This**: This index is your navigation hub. Return here when jumping between topics.

**Last Updated**: March 2026
