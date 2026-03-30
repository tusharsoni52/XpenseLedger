# Android Framework & Components Documentation

## Android Framework Overview

XpenseLedger is built on the Android Framework with heavy use of Jetpack libraries for modern development patterns.

## Application Entry Point

### XpenseLedgerApp.class
```kotlin
@HiltAndroidApp
class XpenseLedgerApp : Application()
```

**Purpose**: Application subclass that initializes Hilt dependency injection

**Initialization Order**:
1. Hilt generates DI code
2. DI components are available to rest of app
3. Application-scoped singletons created

### MainActivity
```kotlin
class MainActivity : AppCompatActivity() {
    // Entry activity for the application
    // Sets content using Compose setContent
    // Initializes app navigation
}
```

**Manifest Declaration**:
```xml
<activity
    android:name=".ui.activity.MainActivity"
    android:exported="true"
    android:label="@string/app_name"
    android:theme="@style/Theme.XpenseLedger">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## Jetpack Compose

### Core Concepts

#### Composable Functions
- Lightweight functions that describe UI
- Automatically recompose when state changes
- Immutable and functional

#### Example Composable
```kotlin
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel = hiltViewModel()) {
    val expenseState by viewModel.state.collectAsState()
    
    when (expenseState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> ExpenseList((expenseState as UiState.Success).expenses)
        is UiState.Error -> ErrorScreen((expenseState as UiState.Error).message)
    }
}
```

### Compose Compiler Features
- **Smart Recomposition** - Only recompose when inputs change
- **Compose Stability** - Optimizations for stable types
- **Comparison Framework** - Efficient equality checks

### Material Design 3 Components
- `Scaffold` - Basic layout structure
- `NavigationBar` - Bottom navigation
- `Card` - Content containers
- `Button`, `FilledButton` - User actions
- `TextField` - Text input
- `Dialog`, `AlertDialog` - Dialogs
- `TopAppBar` - App header

## Navigation System

### Jetpack Navigation Compose

#### Navigation Graph Structure
```kotlin
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.Login.route) {
        composable(NavRoutes.Login.route) { LoginScreen() }
        composable(NavRoutes.Dashboard.route) { DashboardScreen() }
        composable(NavRoutes.ExpenseDetail.route) { ExpenseDetailScreen() }
        // ... more routes
    }
}
```

#### Navigation Routes
```kotlin
sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Dashboard : NavRoutes("dashboard")
    object ExpenseDetail : NavRoutes("expense/{expenseId}")
    object Settings : NavRoutes("settings")
    object Profile : NavRoutes("profile")
    object Categories : NavRoutes("categories")
}
```

#### Navigation Operations
```kotlin
// Navigate to screen
navController.navigate(NavRoutes.Dashboard.route)

// Navigate with argument
navController.navigate("expense/${expenseId}")

// Pop back stack
navController.popBackStack()

// Replace current screen
navController.navigate(NavRoutes.Login.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        inclusive = true
    }
}
```

### Bottom Navigation Bar
```kotlin
@Composable
fun BottomNavBar(navController: NavHostController, currentRoute: String) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, "Expenses") },
            label = { Text("Expenses") },
            selected = currentRoute == NavRoutes.Dashboard.route,
            onClick = { navController.navigate(NavRoutes.Dashboard.route) }
        )
        // ... more items
    }
}
```

## ViewModel & State Management

### ViewModel Architecture
```kotlin
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun loadExpenses() {
        viewModelScope.launch {
            try {
                expenseRepository.getAllExpenses().collect { expenses ->
                    _state.value = UiState.Success(expenses)
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### State Management Best Practices

1. **Single Source of Truth** - State managed in ViewModel
2. **Immutability** - Using StateFlow instead of mutable state
3. **Coroutine Safety** - Using viewModelScope
4. **Error Handling** - Proper exception handling in state
5. **Scope Compliance** - State follows ViewModel lifecycle

## Activity Lifecycle

### Activity Lifecycle Hooks in Compose
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                AppNavGraph()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh session, check authentication
    }
    
    override fun onPause() {
        super.onPause()
        // Pause timers, save state
    }
}
```

## Resource Management

### Resources Directory Structure
```
res/
├── drawable/        # Image assets
├── mipmap/          # Launcher icons
├── values/          # Colors, strings, dimensions
│   ├── strings.xml
│   ├── colors.xml
│   └── dimens.xml
├── values-night/    # Dark theme resources
└── layout/          # XML layouts (if any)
```

### String Resources
Located in `res/values/strings.xml`:
- App name: `@string/app_name`
- Screen titles
- Button labels
- Error messages
- Hint texts

### Color Resources
Located in `res/values/colors.xml`:
- Material Design 3 color palette
- Primary, secondary, tertiary colors
- Surface colors for dark/light themes

### Dimension Resources
Located in `res/values/dimens.xml`:
- Spacing values
- Text sizes
- Component dimensions

## Android Manifest

### Current Manifest Structure
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".XpenseLedgerApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.XpenseLedger">
        
        <activity
            android:name=".ui.activity.MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.XpenseLedger">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Required Permissions
```xml
<!-- Add based on features -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

## Theme System

### Material Design 3 Theme
- Color system with primary, secondary, tertiary colors
- Dynamic theming support (API 31+)
- Day/Night theme variants

### Theme Application
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
```

## Asset Management

### Icon Generation
Script: `generate_icons.py`

Purpose: Generate launcher icons in multiple densities
- xxhdpi (480x480)
- xhdpi (384x384)
- hdpi (288x288)
- mdpi (192x192)
- ldpi (128x128)

Output location: `app/src/main/res/mipmap-*/`

## Build Features

### Enabled Build Features
```kotlin
buildFeatures {
    compose = true  // Enable Jetpack Compose
}
```

### Kotlin Compilation Options
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
```

### KSP Configuration
```kotlin
ksp {
    arg("room.verifySchema", "false")  // Disable schema verification
}
```

## Testing Framework Integration

### Compose Testing
```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun testExpenseScreenLoading() {
    composeTestRule.setContent {
        ExpenseScreen(MockExpenseViewModel())
    }
    
    composeTestRule.onNodeWithText("Loading").assertIsDisplayed()
}
```

### Espresso Testing
```kotlin
@Test
fun testNavigationToDashboard() {
    onView(withId(R.id.dashboard_button)).perform(click())
    onView(withId(R.id.dashboard_screen)).check(matches(isDisplayed()))
}
```

## Jetpack Compose Key Features Used

### State & Effect
```kotlin
// State
val count = remember { mutableStateOf(0) }

// Effect
LaunchedEffect(key1) {
    // Side effect code
}

// Update effect on state change
DisposableEffect(state) {
    onDispose { cleanup() }
}
```

### Lists & Collections
```kotlin
LazyColumn {
    items(expenses.size) { index ->
        ExpenseItem(expenses[index])
    }
}
```

### Animations
```kotlin
val offsetAnimation by animateDpAsState(
    targetValue = if (expanded) 100.dp else 0.dp
)
```

## System Integration

### SharedPreferences
Used for simple key-value storage (optional)

### Content Providers
Not used in current implementation

### Services
Can be added for background tasks

### Broadcast Receivers
Can be added for system events

### Work Manager
Can be integrated for background work

## Hilt Integration with Components

### Activity Injection
```kotlin
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var someService: SomeService  // Automatically injected
}
```

### ViewModel Injection
```kotlin
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel()

// In Compose
val viewModel: ExpenseViewModel = hiltViewModel()
```

### Composable Injection
```kotlin
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    // ViewModel automatically scoped to screen
}
```

## Configuration Changes

### Handle Rotation
- ViewModels survive rotation (not recreated)
- Compose recomposes but doesn't restart
- No state loss on rotation

### Back Navigation
- Back stack properly maintained
- Pop behavior defined in navigation graph
- Deep links supported

## Accessibility

### Compose Accessibility
- Semantic modifiers for screen readers
- Content descriptions for images
- Proper touch target sizes

### Best Practices
- Descriptive button labels
- Alternative text for images
- Sufficient color contrast

---

**Last Updated**: March 2026  
**Framework Version**: 1.0
