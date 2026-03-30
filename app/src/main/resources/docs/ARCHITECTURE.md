# Architecture & Design Documentation

## Architecture Overview

XpenseLedger follows a **Clean Architecture** pattern combined with **Model-View-ViewModel (MVVM)** for the presentation layer. This architecture ensures separation of concerns, testability, and maintainability.

## High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                │
│  (UI - Jetpack Compose, ViewModels, Navigation)    │
├─────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                     │
│  (Business Logic - Use Cases, Models, Repositories) │
├─────────────────────────────────────────────────────┤
│                      DATA LAYER                      │
│  (Data Sources - Room DB, DAOs, Repositories Impl)  │
└─────────────────────────────────────────────────────┘
```

## Layer Details

### 1. Presentation Layer (`ui/`)

Responsible for displaying data and capturing user interactions.

#### Components
- **Screens (Composables)** - UI layouts and interactions
- **ViewModels** - State management and business logic coordination
- **Navigation** - Screen routing and transitions
- **UiState Classes** - Data classes for UI state representation

#### Key Structure
```
ui/
├── screen/           # Composable screen functions
├── viewmodel/        # ViewModels for state management
├── navigation/       # Navigation routes and graph
├── activity/         # Activities (entry points)
└── security/         # UI-level security features
```

#### ViewModels
- **AuthViewModel** - Authentication and login flow
- **ExpenseViewModel** - Expense CRUD operations
- **CategoryViewModel** - Category management
- **SessionViewModel** - User session management
- **UserProfileViewModel** - User profile operations

#### State Management
- Uses `StateFlow` and `Flow` for reactive state
- ViewModels hold state that survives configuration changes
- Compose recomposition triggered by state changes
- Coroutines handle async operations

### 2. Domain Layer (`domain/`)

Contains the core business logic, independent of implementation details.

#### Components
- **Models** - Pure data classes representing domain entities
- **Repository Interfaces** - Contracts for data access
- **Use Cases** - Encapsulate specific business operations

#### Key Structure
```
domain/
├── model/            # Domain models (Expense, Category, etc.)
├── repository/       # Repository interfaces
└── usecase/          # Business logic use cases
```

#### Domain Models
- `Expense` - Represents a single expense transaction
- `Category` - Represents an expense category
- `User` - User profile information

#### Repository Interfaces
```kotlin
interface ExpenseRepository {
    suspend fun createExpense(expense: Expense): Long
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: Long)
    // ... more operations
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun createCategory(category: Category): Long
    // ... more operations
}
```

#### Use Cases
- **ExportExpensesUseCase** - Export expenses to file format
- **ImportExpensesUseCase** - Import expenses from file format
- Custom use cases for complex operations

### 3. Data Layer (`data/`)

Handles all data operations and provides repository implementations.

#### Components
- **Repositories Implementation** - Implement domain repository interfaces
- **Data Access Objects (DAOs)** - Database queries
- **Entities** - Room database entity classes
- **Mappers** - Convert between entities and domain models
- **Local Data Source** - Room database wrapper

#### Key Structure
```
data/
├── repository/       # Repository implementations
├── local/
│   ├── dao/          # Room DAOs
│   ├── db/           # Database definition
│   ├── entity/       # Room entities
│   └── datasource/   # Local data source wrapper
└── mapper/           # Entity to Domain model mappers
```

#### Database Design

**Database Name**: `xpenseledger_encrypted.db`

**Encryption**: SQLCipher with Android KeyStore passphrase

##### Tables

**ExpenseEntity**
```
- id: Long (Primary Key)
- amount: Double
- description: String
- categoryId: Long (Foreign Key)
- date: Long (timestamp)
- createdAt: Long
- updatedAt: Long
```

**CategoryEntity**
```
- id: Long (Primary Key)
- name: String
- color: Int
- icon: String
- isDefault: Boolean
- createdAt: Long
- updatedAt: Long
```

**UserEntity**
```
- id: Long (Primary Key)
- name: String
- email: String
- profilePicture: String?
- createdAt: Long
- updatedAt: Long
```

#### Data Access Pattern
```kotlin
@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long
    
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)
    
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
```

#### Mappers
```kotlin
object ExpenseMapper {
    fun toDomain(entity: ExpenseEntity): Expense = Expense(...)
    fun toDomain(entities: List<ExpenseEntity>): List<Expense> = entities.map { toDomain(it) }
    fun toEntity(domain: Expense): ExpenseEntity = ExpenseEntity(...)
}
```

## Dependency Injection Architecture

### Hilt Modules

#### DatabaseModule
Provides database instances and Room database setup

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase
    
    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao
    
    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao
}
```

#### RepositoryModule
Provides repository implementations

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        categoryDao: CategoryDao
    ): ExpenseRepository = ExpenseRepositoryImpl(expenseDao, categoryDao)
    
    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao
    ): CategoryRepository = CategoryRepositoryImpl(categoryDao)
}
```

#### SecurityModule
Provides security-related dependencies

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideEncryptionManager(): EncryptionManager = EncryptionManager()
    
    @Provides
    @Singleton
    fun providePinManager(@ApplicationContext context: Context): PinManager
    
    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager
}
```

## Security Architecture

### Components

```
security/
├── auth/              # Authentication logic
├── biometrics/        # Biometric authentication
├── crypto/            # Encryption and key management
├── pin/               # PIN management
├── profile/           # User profile management
└── session/           # Session management
```

### Key Security Features

#### 1. Database Encryption (SQLCipher)
- All data encrypted at rest
- Passphrase derived from Android KeyStore
- Transparent encryption/decryption

#### 2. Key Management (KeyStoreManager)
- Uses Android KeyStore for key storage
- AES key generation and retrieval
- Hardware-backed key storage when available

#### 3. Encryption Manager (EncryptionManager)
- Encrypts sensitive data
- Uses KeyStoreManager for key material
- AES-GCM encryption by default

#### 4. Biometric Authentication (BiometricAuthManager)
- Fingerprint/Face recognition
- Falls back to PIN if biometric fails
- Follows Android security guidelines

#### 5. PIN Management (PinManager)
- Secure PIN storage and verification
- Hash-based PIN comparison
- Rate limiting for invalid attempts

#### 6. Session Management (SessionManager)
- Track user login status
- Handle session timeout
- Auto-logout after inactivity

#### 7. User Profile Manager (UserProfileManager)
- Manage per-device user profiles
- Store user preferences securely
- Profile-specific settings

## Data Flow

### Expense Creation Flow

```
1. User enters expense in UI (Screen)
   ↓
2. ViewModel receives action (ExpenseViewModel)
   ↓
3. Use Case processes business logic
   ↓
4. Repository saves to database (ExpenseRepositoryImpl)
   ↓
5. DAO inserts into Room database (ExpenseDao)
   ↓
6. Data encrypted by SQLCipher
   ↓
7. DAO emits updated Flow
   ↓
8. Repository transforms and emits data
   ↓
9. ViewModel collects and updates state (StateFlow)
   ↓
10. Compose recomposes with new state
   ↓
11. UI displays updated expense list
```

## Navigation Architecture

### Navigation Graph
- Type-safe routing using NavRoutes
- Route definitions centralized in `NavRoutes.kt`
- Deep linking support
- State preservation during navigation

### Route Types
```kotlin
sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Dashboard : NavRoutes("dashboard")
    object ExpenseDetail : NavRoutes("expense/{expenseId}")
    object Settings : NavRoutes("settings")
    // ... more routes
}
```

### Navigation Flow
- AppNavGraph manages overall navigation
- BottomNavBar for primary navigation
- Individual screens handle internal navigation
- Back stack properly managed

## Testing Architecture

### Unit Tests (`src/test/`)
- Test ViewModels and Use Cases
- Mock repositories and dependencies
- Verify business logic

### Instrumentation Tests (`src/androidTest/`)
- Test UI composition
- Test database operations
- Test navigation flows
- Test security features

## State Management Strategy

### ViewModel State Pattern
```kotlin
class ExpenseViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    // State is only updated through ViewModel methods
    // Ensures single source of truth
}

sealed class UiState {
    object Loading : UiState()
    data class Success(val expenses: List<Expense>) : UiState()
    data class Error(val message: String) : UiState()
}
```

## Concurrency Model

### Coroutines
- Used throughout for async operations
- Structured concurrency with ViewModelScope
- Cancellation automatic when ViewModel cleared

### Flow
- Database operations emit Flows
- UI observes Flows in appropriate scope
- Backpressure handled automatically

## Performance Considerations

### Database
- Indexed columns for frequent queries
- Pagination for large datasets
- Transaction batching for bulk operations

### UI
- Lazy composition for list items
- Memoization of composables
- Only recompose on state changes

### Memory
- ViewModels hold references carefully
- Hilt manages lifecycle properly
- Coroutine cancellation prevents leaks

## Scalability

### Adding New Features
1. Define domain model
2. Create repository interface
3. Implement repository with data layer
4. Create use case if needed
5. Create ViewModel for feature
6. Create UI composables
7. Add navigation routes
8. Wire dependencies in DI modules

### Module Organization
- Each layer is independent
- Can add new data sources easily
- Easy to swap implementations
- Supports unit testing at each level

---

**Last Updated**: March 2026  
**Architecture Version**: 1.0
