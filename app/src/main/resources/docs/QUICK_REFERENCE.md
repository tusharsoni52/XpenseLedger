# Quick Reference Guide

A condensed reference guide for common development tasks in XpenseLedger.

## Build Commands

```bash
# Clean and rebuild
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Build with verbose output
./gradlew build --info

# Update dependencies
./gradlew dependencyUpdates
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/xpenseledger/app/
│   │   ├── data/              # Database, DAOs, Repositories
│   │   ├── domain/            # Models, Use Cases, Interfaces
│   │   ├── ui/                # Screens, ViewModels, Navigation
│   │   ├── security/          # Encryption, Auth, Biometrics
│   │   └── di/                # Dependency Injection Modules
│   └── resources/docs/        # ← Documentation (you are here)
└── build.gradle.kts           # Build configuration
```

## Feature Development Checklist

- [ ] Create domain model in `domain/model/`
- [ ] Create repository interface in `domain/repository/`
- [ ] Create entity + DAO in `data/local/`
- [ ] Create mapper in `data/mapper/`
- [ ] Implement repository in `data/repository/`
- [ ] Create ViewModel in `ui/viewmodel/`
- [ ] Create Composable screens in `ui/screen/`
- [ ] Add route in `ui/navigation/NavRoutes.kt`
- [ ] Add navigation in `ui/navigation/AppNavGraph.kt`
- [ ] Wire dependencies in `di/` modules
- [ ] Write unit tests
- [ ] Write UI tests

## Navigation Routes

```kotlin
// Add new route
sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Detail : NavRoutes("detail/{id}")
}

// Navigate
navController.navigate(NavRoutes.Home.route)

// Navigate with argument
navController.navigate("detail/${id}")

// Pop back
navController.popBackStack()
```

## ViewModel Pattern

```kotlin
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            try {
                repository.getData().collect { data ->
                    _state.value = UiState.Success(data)
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

## Database Operations

```kotlin
// Create entity
@Entity(tableName = "my_table")
data class MyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// Create DAO
@Dao
interface MyDao {
    @Insert
    suspend fun insert(entity: MyEntity): Long
    
    @Query("SELECT * FROM my_table")
    fun getAll(): Flow<List<MyEntity>>
    
    @Update
    suspend fun update(entity: MyEntity)
    
    @Delete
    suspend fun delete(entity: MyEntity)
}

// Create mapper
object MyMapper {
    fun toDomain(entity: MyEntity) = MyModel(entity.id, entity.name)
    fun toEntity(model: MyModel) = MyEntity(model.id, model.name)
}
```

## Compose UI Pattern

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> {
            val data = (state as UiState.Success).data
            ContentScreen(data)
        }
        is UiState.Error -> {
            val message = (state as UiState.Error).message
            ErrorScreen(message)
        }
    }
}

@Composable
fun ContentScreen(data: List<MyModel>) {
    LazyColumn {
        items(data.size) { index ->
            MyItem(data[index])
        }
    }
}
```

## Testing Pattern

```kotlin
// Unit test
class MyViewModelTest {
    private lateinit var viewModel: MyViewModel
    private val mockRepository = mockk<MyRepository>()
    
    @Before
    fun setUp() {
        viewModel = MyViewModel(mockRepository)
    }
    
    @Test
    fun testLoadData() = runTest {
        // Arrange
        val mockData = listOf(mockk<MyModel>())
        every { mockRepository.getData() } returns flowOf(mockData)
        
        // Act
        viewModel.loadData()
        advanceUntilIdle()
        
        // Assert
        assert(viewModel.state.value is UiState.Success)
    }
}

// UI test
@HiltAndroidTest
class MyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testScreenDisplayed() {
        composeTestRule.setContent {
            MyScreen(MockMyViewModel())
        }
        
        composeTestRule.onNodeWithText("Expected Text").assertIsDisplayed()
    }
}
```

## Dependency Injection Patterns

```kotlin
// Module
@Module
@InstallIn(SingletonComponent::class)
object MyModule {
    
    @Provides
    @Singleton
    fun provideMyRepository(
        dao: MyDao
    ): MyRepository = MyRepositoryImpl(dao)
}

// Inject in Activity/Fragment
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var myService: MyService
}

// Inject in ViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()

// Inject in Composable
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
)
```

## Debugging Shortcuts

```bash
# View logs
adb logcat

# Filter logs with tag
adb logcat tag:XpenseLedger

# Save logs to file
adb logcat > log.txt

# Clear logcat
adb logcat -c

# Install and run app
adb install app-debug.apk
adb shell am start -n com.xpenseledger.app/.ui.activity.MainActivity

# Get app info
adb shell dumpsys package com.xpenseledger.app
```

## Common Gradle Issues

| Error | Solution |
|-------|----------|
| "Could not find android.jar" | Set SDK location in Project Structure |
| "Gradle sync failed" | Run `./gradlew clean` and invalidate IDE cache |
| "KSP compilation error" | Verify Kotlin version matches KSP version |
| "Room compilation failed" | Check @Entity and @Dao annotations |
| "Hilt injection error" | Rebuild project with `./gradlew clean build` |

## Kotlin Extensions

```kotlin
// Scope launching
viewModelScope.launch { }          // Coroutine scope bound to ViewModel
lifecycleScope.launch { }          // Coroutine scope bound to Lifecycle

// Flow operators
flow.collect { }                   // Collect emissions
flow.map { }                       // Transform emissions
flow.filter { }                    // Filter emissions
flow.distinctUntilChanged()        // Emit only when value changes
flow.stateIn()                     // Convert to StateFlow

// Extension functions
list.map { }                       // Transform list
list.filter { }                    // Filter list
list.find { }                      // Find first match
string.isNotEmpty()                // Check if string has content
```

## Security Best Practices

```kotlin
// Encrypt data
val encrypted = EncryptionManager().encrypt(sensitiveData)

// Decrypt data
val decrypted = EncryptionManager().decrypt(encryptedData)

// Use KeyStore
val key = KeyStoreManager().getOrCreateKey()

// Verify PIN
PinManager().verifyPIN(userInput)

// Biometric auth
BiometricAuthManager().authenticate()

// Session management
SessionManager().isSessionValid()
SessionManager().clearSession()
```

## File Locations by Purpose

| Purpose | Location |
|---------|----------|
| Screens/Composables | `ui/screen/` |
| ViewModels | `ui/viewmodel/` |
| Navigation Routes | `ui/navigation/NavRoutes.kt` |
| Database Tables | `data/local/entity/` |
| DAOs | `data/local/dao/` |
| Repositories | `data/repository/` |
| Domain Models | `domain/model/` |
| Use Cases | `domain/usecase/` |
| Dependency Injection | `di/` |
| String Resources | `res/values/strings.xml` |
| Colors | `res/values/colors.xml` |
| Dimensions | `res/values/dimens.xml` |

## State Classes Template

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<MyModel>) : UiState()
    data class Error(val message: String) : UiState()
    object Empty : UiState()
}

sealed class UiEvent {
    object ShowSuccess : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    object NavigateToDashboard : UiEvent()
}
```

## Resources & Links

- **Android Docs**: https://developer.android.com/
- **Compose**: https://developer.android.com/jetpack/compose
- **Room**: https://developer.android.com/training/data-storage/room
- **Hilt**: https://developer.android.com/training/dependency-injection/hilt-android
- **Material 3**: https://m3.material.io/
- **Kotlin**: https://kotlinlang.org/docs/

## IDE Shortcuts (Android Studio)

| Action | Windows | Mac |
|--------|---------|-----|
| Format code | Ctrl+Alt+L | Cmd+Option+L |
| Rename | Shift+F6 | Shift+F6 |
| Find usages | Alt+F7 | Option+F7 |
| Go to definition | Ctrl+B | Cmd+B |
| Search everywhere | Double Shift | Double Shift |
| Logcat | Alt+6 | Cmd+6 |
| Profiler | Alt+9 | Cmd+9 |
| Debug | Shift+F9 | Shift+F9 |

## Key Versions

- **Kotlin**: 2.0.21
- **Android SDK**: Min 24, Target 36
- **Gradle**: 8.5.2
- **Compose**: 2024.09.00
- **Room**: 2.6.1
- **Hilt**: 2.52
- **JDK**: 17

## Database Migrations

Location: `di/DatabaseModule.kt`

```kotlin
.addMigrations(
    AppDatabase.MIGRATION_1_2,
    AppDatabase.MIGRATION_2_3,
    AppDatabase.MIGRATION_3_4,
    AppDatabase.MIGRATION_4_5
)
```

When adding a new migration:
1. Increment database version
2. Create migration method
3. Add to DatabaseModule
4. Update schema if needed

## Troubleshooting Quick Checks

```bash
# Clear cache and rebuild
./gradlew clean

# Invalidate IDE cache
# File → Invalidate Caches / Restart → Invalidate and Restart

# Check Java version
java -version                      # Should be 17

# Verify Android SDK
echo %ANDROID_HOME%               # Should be set

# Check Gradle wrapper
./gradlew --version               # Should be 8.5.2

# Sync Gradle
./gradlew sync                     # Force sync
```

---

**Tip**: Keep this guide bookmarked for quick reference while developing.

**Full Docs**: For detailed information, refer to the complete documentation files (README.md, ARCHITECTURE.md, etc.)

**Last Updated**: March 2026
