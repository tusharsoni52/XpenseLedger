# Development Workflow Documentation

## Overview

This document describes the development, build, test, and deployment workflows for XpenseLedger.

## Development Environment Setup

### Prerequisites

1. **Java Development Kit (JDK)**
   - Required Version: JDK 17
   - Set JAVA_HOME environment variable
   - Verify installation: `java -version`

2. **Android Studio**
   - Minimum: Arctic Fox (2021.1.1) or later
   - Download from: https://developer.android.com/studio
   - Recommended: Latest stable version

3. **Android SDK**
   - Target SDK: 36 (Android 15)
   - Min SDK: 24 (Android 7.0)
   - CMake: Not required (no native code)
   - NDK: Not required (no native code)

4. **Git**
   - Required for version control
   - Verify: `git --version`

### Initial Setup Steps

1. Clone repository:
   ```bash
   git clone <repository-url>
   cd XpenseLedger
   ```

2. Open in Android Studio:
   - File → Open → Select project root
   - Studio auto-detects Gradle and sets up

3. Gradle Sync:
   - Android Studio automatically syncs Gradle
   - Wait for indexing to complete
   - Verify no errors in "Build" tab

4. Configure SDK:
   - File → Project Structure → SDK Location
   - Ensure SDK path points to Android SDK
   - Install missing SDK platforms/tools if prompted

5. Create Virtual Device (optional):
   - Android Virtual Device Manager
   - Create device with API 36 for testing
   - Or use physical device with USB debugging enabled

### IDE Configuration

#### Kotlin Compiler Settings
- Settings → Languages & Frameworks → Kotlin Compiler
- Ensure using Kotlin 2.0.21
- Compose compiler extension version set

#### Code Inspection
- Settings → Editor → Inspections
- Enable all recommended inspections
- Configure severity levels as needed

#### Code Style
- Settings → Editor → Code Style
- Import style scheme if available
- Follow Kotlin style guide

## Build Workflow

### Gradle Build System

#### Understanding Gradle
- **Gradle Wrapper**: Ensures consistent version across environments
- **Version**: 8.5.2 (pinned in `gradle/wrapper/gradle-wrapper.properties`)
- **No installation needed**: Use `gradlew` (Windows: `gradlew.bat`) script

#### Build Commands

##### Clean Build
```bash
# Remove build artifacts
./gradlew clean

# Full rebuild from scratch
./gradlew clean build
```

##### Build Debug APK
```bash
# Build debug variant
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

##### Build Release APK
```bash
# Build release variant (with ProGuard minification)
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk

# Note: Requires signing configuration for production builds
```

##### Build Bundle
```bash
# Build Android App Bundle for Play Store
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

##### Install on Device
```bash
# Build and install debug build
./gradlew installDebug

# Install release build (if signed)
./gradlew installRelease
```

##### Run Tests
```bash
# Run all unit tests
./gradlew test

# Run instrumentation tests on device/emulator
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.xpenseledger.app.ExpenseViewModelTest
```

#### Build Variants

##### Debug
```gradle
buildTypes {
    debug {
        isMinifyEnabled = false
        isShrinkResources = false
        debuggable = true
    }
}
```

Benefits:
- Faster build times
- Full debugging support
- Readable code (not minified)
- Larger APK size

##### Release
```gradle
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

Benefits:
- Smaller APK size (30-50% smaller)
- Obfuscated code (harder to reverse engineer)
- Optimized for performance
- Ready for distribution

### Build Troubleshooting

#### Common Build Issues

**Issue: "Could not find android.jar"**
```
Solution: 
- File → Project Structure → SDK Location
- Ensure correct SDK path
- Download missing SDK platforms
```

**Issue: "Gradle sync failed"**
```
Solution:
- ./gradlew clean
- File → Invalidate Caches / Restart
- Check internet connection
- Verify JDK 17 is set
```

**Issue: "KSP compilation error"**
```
Solution:
- Clean Gradle cache: ./gradlew clean
- Rebuild project: ./gradlew build
- Check Kotlin version matches KSP version (2.0.21)
```

**Issue: "Room compilation failed"**
```
Solution:
- Verify @Entity annotations on database models
- Check @Dao interfaces
- Clear project: ./gradlew clean
- Rebuild: ./gradlew build
```

## Testing Workflow

### Unit Testing

#### Running Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run with verbose output
./gradlew test --info

# Run specific test file
./gradlew test --tests ExampleUnitTest
```

#### Test Structure
```
src/test/kotlin/com/xpenseledger/app/
├── domain/
│   └── usecase/
│       ├── ExportExpensesUseCaseTest.kt
│       └── ImportExpensesUseCaseTest.kt
├── ui/
│   └── viewmodel/
│       ├── ExpenseViewModelTest.kt
│       └── AuthViewModelTest.kt
└── utils/
    └── UtilsTest.kt
```

#### Example Unit Test
```kotlin
class ExpenseViewModelTest {
    private lateinit var viewModel: ExpenseViewModel
    private val mockRepository = mockk<ExpenseRepository>()
    
    @Before
    fun setUp() {
        viewModel = ExpenseViewModel(mockRepository)
    }
    
    @Test
    fun testLoadExpensesSuccess() = runTest {
        // Given
        val mockExpenses = listOf(mockk<Expense>())
        every { mockRepository.getAllExpenses() } returns flowOf(mockExpenses)
        
        // When
        viewModel.loadExpenses()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assert(state is UiState.Success)
        assert((state as UiState.Success).expenses == mockExpenses)
    }
}
```

### Instrumentation Testing

#### Running Instrumentation Tests
```bash
# Run all instrumentation tests
./gradlew connectedAndroidTest

# Run on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunner="..."

# Run with logging
./gradlew connectedAndroidTest --info
```

#### Test Structure
```
src/androidTest/kotlin/com/xpenseledger/app/
├── ui/
│   ├── ExpenseScreenTest.kt
│   └── LoginScreenTest.kt
└── database/
    └── ExpenseDaoTest.kt
```

#### Example Instrumentation Test
```kotlin
@HiltAndroidTest
class ExpenseScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testExpenseListDisplayed() {
        val mockViewModel = mockk<ExpenseViewModel>()
        every { mockViewModel.state } returns flowOf(
            UiState.Success(listOf(mockExpense()))
        ).stateIn(Dispatchers.Main)
        
        composeTestRule.setContent {
            ExpenseScreen(mockViewModel)
        }
        
        composeTestRule.onNodeWithText("Test Expense").assertIsDisplayed()
    }
}
```

### Test Coverage

#### Generate Coverage Report
```bash
# Run tests with coverage
./gradlew test jacocoTestReport

# Generate instrumentation test coverage
./gradlew connectedAndroidTest

# View report
# app/build/reports/jacoco/test/html/index.html
```

## Feature Development Workflow

### Creating a New Feature

#### Step 1: Design Domain Layer
```kotlin
// 1. Create domain model
data class NewFeature(
    val id: Long,
    val name: String,
    val description: String
)

// 2. Create repository interface
interface NewFeatureRepository {
    fun getAllFeatures(): Flow<List<NewFeature>>
    suspend fun createFeature(feature: NewFeature): Long
    suspend fun updateFeature(feature: NewFeature)
    suspend fun deleteFeature(id: Long)
}

// 3. Create use case (if complex logic)
class GetAllFeaturesUseCase @Inject constructor(
    private val repository: NewFeatureRepository
)
```

#### Step 2: Implement Data Layer
```kotlin
// 1. Create entity
@Entity(tableName = "new_features")
data class NewFeatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String
)

// 2. Create DAO
@Dao
interface NewFeatureDao {
    @Insert
    suspend fun insert(feature: NewFeatureEntity): Long
    
    @Query("SELECT * FROM new_features")
    fun getAll(): Flow<List<NewFeatureEntity>>
}

// 3. Create mapper
object NewFeatureMapper {
    fun toDomain(entity: NewFeatureEntity) = NewFeature(...)
    fun toEntity(domain: NewFeature) = NewFeatureEntity(...)
}

// 4. Implement repository
class NewFeatureRepositoryImpl @Inject constructor(
    private val dao: NewFeatureDao
) : NewFeatureRepository {
    override fun getAllFeatures() = dao.getAll().map { entities ->
        entities.map { NewFeatureMapper.toDomain(it) }
    }
}
```

#### Step 3: Create Presentation Layer
```kotlin
// 1. Create ViewModel
class NewFeatureViewModel @Inject constructor(
    private val repository: NewFeatureRepository
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun loadFeatures() {
        viewModelScope.launch {
            repository.getAllFeatures().collect { features ->
                _state.value = UiState.Success(features)
            }
        }
    }
}

// 2. Create Composables
@Composable
fun NewFeatureScreen(
    viewModel: NewFeatureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> FeatureList((state as UiState.Success).features)
        is UiState.Error -> ErrorScreen()
    }
}
```

#### Step 4: Update DI & Navigation
```kotlin
// Update RepositoryModule
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNewFeatureRepository(
        dao: NewFeatureDao
    ): NewFeatureRepository = NewFeatureRepositoryImpl(dao)
}

// Update navigation routes
sealed class NavRoutes(val route: String) {
    object NewFeature : NavRoutes("new_feature")
}

// Add to NavGraph
composable(NavRoutes.NewFeature.route) {
    NewFeatureScreen()
}
```

#### Step 5: Write Tests
```kotlin
// Write unit tests
class NewFeatureViewModelTest { ... }

// Write UI tests
class NewFeatureScreenTest { ... }

// Write database tests
class NewFeatureDaoTest { ... }
```

## Code Review & Quality Workflow

### Pre-Commit Checks

1. **Code Analysis**
   - Run: `./gradlew lint`
   - Fix issues before committing

2. **Unit Tests**
   - Run: `./gradlew test`
   - Ensure all tests pass

3. **Code Formatting**
   - Use IDE formatter: Ctrl+Alt+L
   - Apply consistent style

### Pull Request Workflow

1. Create feature branch
   ```bash
   git checkout -b feature/new-feature
   ```

2. Make changes and commit
   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

3. Push to remote
   ```bash
   git push origin feature/new-feature
   ```

4. Create pull request
   - Link related issues
   - Describe changes
   - Request reviewers

5. Address review comments
   - Make requested changes
   - Push updates (auto-updates PR)

6. Merge when approved
   - Squash commits if needed
   - Delete feature branch

## Debugging Workflow

### Enabling Debug Mode

#### Logcat Filtering
```bash
# View logs with tag filter
adb logcat tag:XpenseLedger

# View logs with level filter (E=Error, W=Warn, I=Info, D=Debug)
adb logcat *:D

# Save logs to file
adb logcat > log.txt
```

#### Android Studio Debugger
1. Set breakpoints (left margin click)
2. Run app in debug mode: Shift+F9
3. When breakpoint hit:
   - Inspect variables
   - Evaluate expressions
   - Step through code

### Debugging Common Issues

**Database Issues**
```kotlin
// Enable Room query logging
Room.databaseBuilder(...)
    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
    .build()
```

**Compose Recomposition**
```kotlin
// Enable recomposition debugging
@Composable
fun MyScreen() {
    Layout(
        modifier = Modifier.layout { measurable, constraints ->
            println("${Thread.currentThread().name}: measure")
            measurable.measure(constraints)
        }
    )
}
```

**Memory Issues**
- Android Studio → Profiler → Memory
- Record allocations
- Identify memory leaks
- Check for object retention

## Release Workflow

### Preparing Release Build

#### 1. Version Management
```gradle
android {
    defaultConfig {
        versionCode = 2  // Increment
        versionName = "1.1"  // Follow semver
    }
}
```

#### 2. Signing Configuration
```gradle
android {
    signingConfigs {
        release {
            storeFile = file("path/to/keystore.jks")
            storePassword = "password"
            keyAlias = "alias"
            keyPassword = "password"
        }
    }
}
```

#### 3. Build Release APK
```bash
./gradlew assembleRelease
# or
./gradlew bundleRelease  # For Play Store
```

#### 4. Release Checklist
- [ ] All tests passing
- [ ] Code review completed
- [ ] Version numbers updated
- [ ] Release notes prepared
- [ ] ProGuard rules verified
- [ ] Signing configuration set
- [ ] Build generated successfully

### Distribution

#### Google Play Store
1. Build AAB (App Bundle)
2. Sign release bundle
3. Upload to Play Console
4. Configure release notes
5. Select supported devices
6. Publish to production/beta/alpha track

#### Direct APK Distribution
1. Sign APK
2. Share APK file
3. Users install from Settings → Security → Unknown Sources

## Continuous Integration (Optional)

### GitHub Actions Example
```yaml
name: Build and Test
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: ./gradlew build
      - run: ./gradlew test
      - run: ./gradlew connectedAndroidTest
```

## Performance Optimization Workflow

### Profiling

#### CPU Profiling
- Android Studio → Profiler → CPU
- Record method traces
- Identify bottlenecks

#### Memory Profiling
- Android Studio → Profiler → Memory
- Capture heap dump
- Find memory leaks

#### Frame Rendering
- Android Studio → Profiler → Frames
- Monitor jank (frames > 16ms)
- Optimize Compose recomposition

### Optimization Best Practices
1. Minimize recomposition
2. Use remember for expensive calculations
3. Lazy load lists with LazyColumn
4. Batch database operations
5. Compress images and assets

## Documentation Maintenance

### Updating Documentation
- Keep README.md current
- Update architecture docs with major changes
- Document new features in FRAMEWORK.md
- Update build commands in WORKFLOW.md

### Documentation Location
- Main docs: `app/src/main/resources/docs/`
- Code comments: Inline in source
- API docs: Generated with Dokka (optional)

---

**Last Updated**: March 2026  
**Workflow Version**: 1.0
