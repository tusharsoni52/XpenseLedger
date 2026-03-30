# 📊 Income & Balance Feature - Implementation TODO

## Overview
Complete strategic roadmap for implementing monthly income tracking and net balance calculation feature in XpenseLedger.

**Status**: 🔵 Not Started  
**Priority**: 🔴 High  
**Estimated Timeline**: 4-6 weeks (phased)

---

## PHASE 1: FEATURE REQUIREMENTS & DESIGN
**Status**: ⚪ Not Started | **Est. Duration**: 3-5 days | **Owner**: TBD

### 1.1 Core Feature: Monthly Income Entry
- [ ] **1.1.1** Define Income entity structure with all required fields
  - Fields: id, title, amount, category, subcategory, categoryId, subcategoryId, timestamp, isRecurring, notes
  - Validation rules: amount > 0, required title, valid timestamp
  
- [ ] **1.1.2** Create income categories taxonomy
  - Base categories: Salary, Freelance, Investment, Bonus, Other
  - Support subcategories (e.g., Salary → Base Salary, Overtime)
  - Align with existing category system using same DB structure
  
- [ ] **1.1.3** Design income entry workflow
  - Full CRUD operations (Add, Read, Update, Delete)
  - Edit mode detection for button label ("Add Income" vs "Save Changes")
  - Confirmation dialogs for destructive actions

### 1.2 Core Feature: Balance Dashboard View
- [ ] **1.2.1** Design balance metrics card layout
  - Show: Income | Expense | Balance (three-column layout)
  - Color scheme: Emerald (income) | Red (expense) | Cyan (balance)
  - Dynamic balance color: Green if positive, Red if negative, Cyan if zero
  
- [ ] **1.2.2** Define balance calculation logic
  - Formula: Balance = Total Income - Total Expense for selected month
  - Handle edge cases: No income, no expenses, equal amounts
  - Rounding: 2 decimal places for display
  
- [ ] **1.2.3** Plan income list display
  - Collapsible section in HomeScreen (below header, above expenses)
  - Show income items by category with totals
  - Support long-press/touch actions for edit/delete
  - Separate from expense list visually

- [ ] **1.2.4** Design trend indicator (optional but recommended)
  - Show: "↑ 15% vs last month" or "↓ 5% vs last month"
  - Calculate: Percentage change from previous month
  - Handle: First month (no baseline to compare)

### 1.3 Database Schema Design
- [ ] **1.3.1** Define IncomeEntity with SQLCipher encryption
  - Table name: `incomes`
  - Primary key: auto-increment id
  - Indexes: Create index on timestamp for fast filtering
  - Review: Align with ExpenseEntity structure for consistency
  
- [ ] **1.3.2** Design IncomeDao interface
  - Methods: insert, update, delete, getAll, getByDateRange
  - Return types: suspend (insert/update/delete), Flow (reads)
  - Add methods: getByMonth(monthKey: String), getByCategory
  
- [ ] **1.3.3** Plan category support for income
  - Decision: Reuse existing Category entity or create IncomeCategory?
  - Recommendation: Reuse existing system, add "type" filter for income-only categories
  - Migration: Add income categories to DefaultCategories.kt

### 1.4 Repository & UseCase Layer
- [ ] **1.4.1** Create IncomeRepository interface
  - Contract: CRUD + Flow-based queries
  - Location: `domain/repository/IncomeRepository.kt`
  - Methods: mirror ExpenseRepository pattern
  
- [ ] **1.4.2** Create Income domain model
  - Location: `domain/model/Income.kt`
  - Make @Serializable for backup/export compatibility
  - Match Expense structure for consistency
  
- [ ] **1.4.3** Create IncomeRepositoryImpl
  - Location: `data/repository/IncomeRepositoryImpl.kt`
  - Implement: Entity → Domain model mapping
  - Use Room DAO for all DB operations

### 1.5 ViewModel State Management
- [ ] **1.5.1** Extend ExpenseViewModel with income flows
  - Add: `incomes: StateFlow<List<Income>>`
  - Add: `monthlyIncomeTotal: StateFlow<Map<String, Double>>`
  - Add: `monthlyBalance: StateFlow<Map<String, Double>>`
  - Inject: IncomeRepository via Hilt
  
- [ ] **1.5.2** Extend DashboardUiState sealed class
  - Add to Success: monthlyIncome, incomeList, incomeByCategory, monthlyBalance
  - Add optional: BalanceTrend (previous month comparison)
  - Update calculation: combine expenses + incomes flows
  
- [ ] **1.5.3** Add public commands to ExpenseViewModel
  - addIncome(title, amount, category, timestamp)
  - updateIncome(income: Income)
  - deleteIncome(income: Income)
  - Follow existing pattern: viewModelScope.launch with error handling
  
- [ ] **1.5.4** Add income editing state (similar to expense editing)
  - Add: `_editingIncome: MutableStateFlow<Income?>(null)`
  - Add: `editingIncome: StateFlow<Income?>`
  - Add: `setEditingIncome()` and `clearEditingIncome()` methods

### 1.6 UI Components Design
- [ ] **1.6.1** Design IncomeHeaderMetrics composable
  - Three-column card: Income | Expense | Balance
  - Responsive layout (handle RTL, tablet layouts)
  - Support month navigation arrows (< MARCH 2026 >)
  - Apply Theme colors: ColorIncome, ColorExpense, XpensePrimary
  
- [ ] **1.6.2** Design AddIncomeScreen composable
  - Mirror AddExpenseScreen structure (parallel component)
  - Fields: amount, category, subcategory, description, date
  - Validation: amount > 0, required fields
  - Button logic: conditional "Add Income" / "Save Changes"
  - Dialog vs full-screen mode decision
  
- [ ] **1.6.3** Design IncomeListSection composable
  - Collapsible header: "INCOME" with toggle state
  - List items: income entries grouped by category
  - Category totals and percentages
  - Touch interactions: long-press (edit), swipe (delete)
  
- [ ] **1.6.4** Design income detail/edit dialog
  - Reuse AddIncomeScreen in dialog mode
  - Handle state passing (editingIncome from ViewModel)
  - Dismiss handling: clear editing state on cancel

---

## PHASE 2: IMPLEMENTATION ARCHITECTURE
**Status**: ⚪ Not Started | **Est. Duration**: 4-6 days | **Owner**: TBD

### 2.1 Create Domain & Data Models
- [ ] **2.1.1** Create `domain/model/Income.kt`
  - Data class with @Serializable annotation
  - Fields: id, title, amount, category, subcategory, categoryId, subcategoryId, timestamp, notes
  
- [ ] **2.1.2** Create `data/local/entity/IncomeEntity.kt`
  - Room @Entity with table name "incomes"
  - Primary key auto-increment
  - All fields matching Income model
  
- [ ] **2.1.3** Create mapping extension functions
  - `IncomeEntity.toIncome(): Income`
  - `Income.toEntity(): IncomeEntity`
  - Location: `data/repository/IncomeRepositoryImpl.kt` or separate file

### 2.2 Create Repository Layer
- [ ] **2.2.1** Create `domain/repository/IncomeRepository.kt` interface
  - Methods: insert, update, delete, getAll, getByMonth, getByCategory
  - Return types as per specification in Phase 1.4
  
- [ ] **2.2.2** Create `data/local/dao/IncomeDao.kt`
  - @Dao interface with CRUD + custom query methods
  - Indexes on timestamp and categoryId for performance
  - Return Flow<> for reactive updates
  
- [ ] **2.2.3** Create `data/repository/IncomeRepositoryImpl.kt`
  - Inject IncomeDao via constructor
  - Implement all interface methods
  - Handle Entity ↔ Model mapping
  - Mark with @Singleton scope for Hilt

### 2.3 Update Database Configuration
- [ ] **2.3.1** Add IncomeDao to AppDatabase.kt
  - Add to @Database(entities = [..., IncomeEntity::class])
  - Add abstract function: `abstract fun incomeDao(): IncomeDao`
  - Increment database version (1 → 2)
  
- [ ] **2.3.2** Create migration 1→2
  - Write SQL to create `incomes` table with all columns
  - Create index: `CREATE INDEX idx_incomes_timestamp ON incomes(timestamp)`
  - Test migration: verify existing expenses unaffected
  - Document: what changed in migration
  
- [ ] **2.3.3** Add income categories to DefaultCategories.kt
  - Categories: Salary, Freelance, Investment, Bonus, Other
  - Set type="INCOME" (or similar) to distinguish from EXPENSE
  - Assign unique categoryId values

### 2.4 ViewModel Implementation
- [ ] **2.4.1** Inject IncomeRepository into ExpenseViewModel
  - Constructor parameter: incomeRepository: IncomeRepository
  - Lazy initialization with other repositories
  
- [ ] **2.4.2** Add income state flows to ExpenseViewModel
  - `_editingIncome: MutableStateFlow<Income?>(null)`
  - `editingIncome: StateFlow<Income?> = _editingIncome.asStateFlow()`
  - `incomes: StateFlow<List<Income>> = repo.getAll().stateIn(...)`
  
- [ ] **2.4.3** Calculate derived flows
  - `monthlyIncomeTotal: StateFlow<Map<String, Double>>`
  - `monthlyBalance: StateFlow<Map<String, Double>>`
  - Combine: incomes + expenses flows
  
- [ ] **2.4.4** Extend DashboardUiState.Success
  - Add fields: monthlyIncome, incomeList, incomeByCategory, monthlyBalance
  - Update calculation logic in dashboardUiState flow
  - Test: all calculations produce correct values
  
- [ ] **2.4.5** Implement action methods
  - `addIncome(title, amount, category, timestamp)`
  - `updateIncome(income: Income)`
  - `deleteIncome(income: Income)`
  - `setEditingIncome(income: Income?)`
  - `clearEditingIncome()`
  - All with viewModelScope.launch + error handling

### 2.5 Navigation Setup
- [ ] **2.5.1** Add income screen routes to Screen.kt
  - `object AddIncome : Screen("add_income")` (may share with edit)
  - Or separate: `object EditIncome : Screen("edit_income/{id}")`
  - Recommendation: Use AddIncome for both, pass editingIncome via ViewModel
  
- [ ] **2.5.2** Update AppNavGraph.kt
  - Add composable route for AddIncomeScreen
  - Implement: FAB short-tap (add expense) vs long-tap (show menu with Add Income)
  - Or: Use Bottom Sheet with Add Expense / Add Income options
  - Handle navigation: navController.navigate(Screen.AddIncome.route)
  
- [ ] **2.5.3** Add IncomeRepository to Hilt DI
  - Create @Module for income (or add to existing module)
  - Provide: @Provides @Singleton fun provideIncomeRepository(...)
  - Bind implementation to interface

### 2.6 Create Composable Components
- [ ] **2.6.1** Create `ui/components/IncomeHeaderMetrics.kt`
  - Composable: IncomeHeaderMetrics(income, expense, balance, month, onMonthChange)
  - Three-column layout with proper spacing
  - Color coordination: Emerald, Red, Cyan (dynamic based on balance)
  
- [ ] **2.6.2** Create `ui/screens/income/AddIncomeScreen.kt`
  - Reuse form pattern from AddExpenseScreen
  - Size consideration: Dialog vs full-screen
  - Form validation, category selection, date picker
  
- [ ] **2.6.3** Create `ui/screens/income/AddIncomeViewModel.kt` (optional)
  - Or reuse ExpenseViewModel logic in AddIncomeScreen directly
  - Consider: complexity of income form validation
  
- [ ] **2.6.4** Create `ui/components/IncomeListSection.kt`
  - Collapsible section with toggle state
  - Items listed by category with totals
  - Touch handlers for edit/delete

### 2.7 Update HomeScreen
- [ ] **2.7.1** Integrate IncomeHeaderMetrics at top
  - Collect monthlyIncome, monthlyExpense, monthlyBalance from ViewModel
  - Pass month navigation callback
  - Styling: match existing card elevation and spacing
  
- [ ] **2.7.2** Add IncomeListSection below header
  - Collect incomeList from DashboardUiState
  - Show only if in Success state
  - Handle edit mode: open AddIncomeScreen dialog on long-press
  
- [ ] **2.7.3** Update expenses section heading (optional)
  - Change from "EXPENSES" to "EXPENSES" with count
  - Or add divider between income and expense sections
  - Maintain existing expense list logic untouched

---

## PHASE 3: RECOMMENDED COMPLEMENTARY FEATURES
**Status**: ⚪ Not Started | **Est. Duration**: Varies per feature | **Owner**: TBD

### 3.1 Budget Management (High Priority)
**Description**: Set and track monthly budget limits per category

- [ ] **3.1.1** Design budget model
  - Entity: Budget(categoryId, monthKey, budgetAmount, alertThreshold)
  
- [ ] **3.1.2** Create budget storage (database table)
  
- [ ] **3.1.3** Add budget UI components
  - Circular progress indicator showing %used
  - Alert when approaching/exceeding budget
  
- [ ] **3.1.4** Add budget state to ViewModel
  - Track: budgetVsActual ratio per category
  
- [ ] **3.1.5** Integrate into HomeScreen/ComparisonScreen
  - Show progress for each category

### 3.2 Recurring Income/Expense (Medium Priority)
**Description**: Auto-generate recurring transactions

- [ ] **3.2.1** Add `isRecurring` and `recurringRule` to Income/Expense
  - Rule: monthly, weekly, daily, custom
  
- [ ] **3.2.2** Create recurrence engine
  - Job that runs daily to generate pending recurrences
  - User reviews and confirms before recording
  
- [ ] **3.2.3** Add recurrence UI to forms
  - Toggle: Recurring yes/no
  - Dropdown: Frequency selection
  
- [ ] **3.2.4** Notification on recurrence due
  - Prompt user to record/skip recurring entry

### 3.3 Financial Goals (Medium Priority)
**Description**: Savings targets and progress tracking

- [ ] **3.3.1** Create Goal entity
  - Fields: goalName, targetAmount, currentAmount, deadline, category
  
- [ ] **3.3.2** Add goal management screen
  - Add/Edit/Delete goals
  - Allocate balance to specific goals
  
- [ ] **3.3.3** Goal progress visualization
  - Linear progress bar per goal
  - Percentage completed
  
- [ ] **3.3.4** Goals dashboard view
  - Show all active goals
  - Highlight approaching deadlines

### 3.4 Cash Flow Forecast (Medium Priority)
**Description**: Project future balance based on trends

- [ ] **3.4.1** Implement forecasting algorithm
  - If recurring enabled: use actual schedules
  - Else: use average from last N months
  
- [ ] **3.4.2** Create forecast visualization
  - Line chart: projected balance 3/6/12 months
  - Warnings: "Running out in 6 months"
  
- [ ] **3.4.3** Add to Analytics screen
  - Show historical + forecast

### 3.5 Multi-Account Support (Low Priority)
**Description**: Track multiple accounts separately

- [ ] **3.5.1** Design account model
  - Accounts: Checking, Savings, Investment, etc.
  
- [ ] **3.5.2** Create account switching UI
  - Tab or dropdown to select account
  
- [ ] **3.5.3** Database schema for accounts
  - Add accountId to Expense/Income entities
  
- [ ] **3.5.4** Consolidated views
  - Total across all accounts

### 3.6 Income Tax Estimation (Low Priority)
**Description**: Estimate tax liability

- [ ] **3.6.1** Design tax configuration
  - Tax bracket selection
  - Deduction categories
  
- [ ] **3.6.2** Tax calculation engine
  - Year-to-date computation
  - Quarterly estimation
  
- [ ] **3.6.3** Tax planning screen
  - Estimated liability
  - Quarterly payment planner

### 3.7 Financial Reports (Medium Priority)
**Description**: Generate summary reports

- [ ] **3.7.1** Create report engine
  - Monthly/Quarterly/Annual summaries
  - Income statement format
  
- [ ] **3.7.2** Export functionality
  - PDF generation
  - CSV export
  
- [ ] **3.7.3** Email integration
  - Send reports to user email
  
- [ ] **3.7.4** Report UI screen
  - History of generated reports
  - Download/share options

### 3.8 Savings Rate Tracking (Low Priority)
**Description**: Calculate and track savings percentage

- [ ] **3.8.1** Formula: (Income - Expenses) / Income × 100%
  
- [ ] **3.8.2** Historical tracking
  - Store monthly savings rate
  - Chart showing trend
  
- [ ] **3.8.3** Benchmarking
  - Compare to user's average
  - Goal setting: "Target 30%"

### 3.9 Currency & Exchange Rates (Very Low Priority)
**Description**: Multi-currency support

- [ ] **3.9.1** Currency selection per transaction
  
- [ ] **3.9.2** Real-time exchange rate API integration
  
- [ ] **3.9.3** Base currency conversion
  - Display in selected base currency
  
- [ ] **3.9.4** Historical rate tracking
  - For past transactions

### 3.10 Expense Splitting & Sharing (Very Low Priority)
**Description**: Split expenses with others

- [ ] **3.10.1** Design split model
  - Expense: ₹30,000 split 2 ways (You: ₹15,000, Other: ₹15,000)
  
- [ ] **3.10.2** Settlement tracking
  - Who owes whom and how much
  
- [ ] **3.10.3** Payment integration
  - Link to PayTM, GPay for settlement
  
- [ ] **3.10.4** Group management
  - Create groups (roommates, trip friends, family)

---

## PHASE 4: DESIGN CONSISTENCY VERIFICATION
**Status**: ⚪ Not Started | **Est. Duration**: 2-3 days | **Owner**: TBD

### 4.1 Theme Integration Checklist
- [ ] **4.1.1** Colors
  - Verify ColorIncome (Emerald #34D399) used consistently
  - Verify ColorExpense (Red #F87171) used consistently
  - Verify balance color logic: Green (positive), Red (negative), Cyan (zero)
  - Test in dark mode and light mode
  
- [ ] **4.1.2** Cards and Elevation
  - Income metrics card uses same NeumorphicShadow as expense cards
  - Consistent shadow depth and blur
  - Test on multiple device sizes (phone, tablet, foldable)
  
- [ ] **4.1.3** Typography
  - Use Type.kt Material Design 3 styles
  - Income header: Headline/Title2 (match expense total style)
  - Income items: Body1/Body2 (match expense items)
  
- [ ] **4.1.4** Icons
  - Create income-related icons: Salary, Freelance, Investment, Bonus
  - Consistent icon style with existing icons
  - Accessible: sufficient contrast in all themes
  
- [ ] **4.1.5** Spacing & Padding
  - Follow existing rhythm: 16.dp, 8.dp, 4.dp
  - Income section padding: match expense section
  - Item spacing: consistent with expense items
  
- [ ] **4.1.6** Animations
  - Use existing transition library (fadeIn/slideInHorizontally)
  - Income section collapse/expand animation matches expense section
  - No jarring transitions on month change
  
- [ ] **4.1.7** Dark/Light Mode Testing
  - Test all income components in dark theme
  - Test all income components in light theme
  - Verify contrast ratios (WCAG AA minimum)

### 4.2 Navigation Flow Verification
- [ ] **4.2.1** Route Configuration
  - All income routes properly typed in Screen.kt
  - No hardcoded string routes
  
- [ ] **4.2.2** Back Stack Behavior
  - Adding income returns to HomeScreen
  - Editing income returns to HomeScreen
  - Navigation routes don't stack unnecessarily
  
- [ ] **4.2.3** FAB Behavior
  - Single FAB with menu behavior decided
  - Menu shows "Add Expense" and "Add Income" clearly
  
- [ ] **4.2.4** Deep Linking (if applicable)
  - Income routes support deep linking
  - External links open correct screen

### 4.3 Hilt Dependency Injection Verification
- [ ] **4.3.1** IncomeRepository binding
  - Interface properly bound to implementation
  - Scope: @Singleton verified
  
- [ ] **4.3.2** IncomeDao injection
  - Provided via @Database
  - Accessible to repository
  
- [ ] **4.3.3** ViewModel injection
  - ExpenseViewModel receives IncomeRepository
  - All Hilt annotations present

---

## PHASE 5: TESTING STRATEGY
**Status**: ⚪ Not Started | **Est. Duration**: 3-4 days | **Owner**: TBD

### 5.1 Unit Tests
- [ ] **5.1.1** IncomeRepository tests
  - insert(): Verify income saved and retrievable
  - update(): Verify changes applied
  - delete(): Verify income removed
  - getAll(): Verify correct order (timestamp DESC)
  - getByMonth(): Verify filtering works
  
- [ ] **5.1.2** ExpenseViewModel income calculation tests
  - monthlyIncome calculation: sum of all income in month = correct
  - monthlyBalance calculation: income - expense = balance
  - Edge case: No income (balance = -expense)
  - Edge case: No expense (balance = income)
  - Edge case: Equal income and expense (balance = 0)
  
- [ ] **5.1.3** Validation tests
  - Negative amount rejected
  - Zero amount rejected
  - Null title rejected
  - Valid income accepted
  
- [ ] **5.1.4** Date filtering tests
  - Income filtered by month-key correctly
  - Edge case: Month boundary (Feb → Mar)
  - Year boundary (Dec 2025 → Jan 2026)
  
- [ ] **5.1.5** Category aggregation tests
  - Income grouped by category correctly
  - Totals per category calculated properly
  - Category order by amount descending

### 5.2 UI Integration Tests
- [ ] **5.2.1** Add Income flow
  - Open Add Income screen
  - Fill form with valid data
  - Tap Save → Income appears in list
  - Verify display matches input
  
- [ ] **5.2.2** Edit Income flow
  - Long-press income item
  - Edit dialog opens with current data
  - Modify field
  - Save → List updates
  
- [ ] **5.2.3** Delete Income flow
  - Long-press income item
  - Delete action → Confirmation dialog
  - Confirm → Income removed from list
  
- [ ] **5.2.4** Month navigation flow
  - Click < button → Previous month loads
  - Click > button → Next month loads
  - Income and balance update for selected month
  - Available months list updates correctly
  
- [ ] **5.2.5** Balance display tests
  - Metrics card shows correct income
  - Metrics card shows correct expense
  - Metrics card shows correct balance
  - Colors match state (green/red/cyan)
  
- [ ] **5.2.6** Income list display
  - All income items visible
  - Grouped by category visually
  - Category totals correct
  - Items sorted by amount descending

### 5.3 Database Tests
- [ ] **5.3.1** Migration verification
  - Migration 1→2 runs without errors
  - Existing expenses unaffected after migration
  - New incomes table created with correct schema
  
- [ ] **5.3.2** Encryption verification
  - Income data encrypted with SQLCipher
  - Data unreadable without password
  - Decryption works after password entry
  
- [ ] **5.3.3** Index performance
  - Query by timestamp uses index (verify EXPLAIN QUERY PLAN)
  - Queries on large datasets perform acceptably
  
- [ ] **5.3.4** Foreign key integrity (if used)
  - Income.categoryId must reference valid Category
  - Constraint enforced by database
  
- [ ] **5.3.5** Transaction consistency
  - Concurrent operations don't corrupt data
  - No race conditions on multi-threaded access

### 5.4 Manual Testing Checklist
- [ ] **5.4.1** Device testing
  - Test on Android 7.0 (min SDK 24)
  - Test on Android 15 (target SDK 36)
  - Test on tablet (verify responsive layout)
  
- [ ] **5.4.2** Theme testing
  - Test in dark mode (system setting)
  - Test in light mode
  - Test with eye-protect mode (if applicable)
  
- [ ] **5.4.3** Edge case scenarios
  - Add income with same name as expense
  - Add income for future date (should still work)
  - Add income with very large amount (999,999,999)
  - Add income with special characters in title
  - Change system date after adding income (timestamp should persist)
  
- [ ] **5.4.4** Performance testing
  - Loading HomeScreen with 1000+ incomes
  - Month navigation with large datasets
  - Scrolling smooth (60 FPS)
  
- [ ] **5.4.5** Security testing
  - PIN protection still required to view income
  - Incomes not leaked in logs
  - Crash reports don't contain income data

---

## PHASE 6: SECURITY & PRIVACY COMPLIANCE
**Status**: ⚪ Not Started | **Est. Duration**: 1-2 days | **Owner**: TBD

### 6.1 Data Protection
- [ ] **6.1.1** SQLCipher encryption
  - Verify incomes table encrypted at rest
  - Verify encryption key from Android KeyStore used
  
- [ ] **6.1.2** Logging audit
  - No income amounts logged
  - No income details in crash reports
  - Remove any debug logs before release
  
- [ ] **6.1.3** Authentication
  - PIN protection required to access app
  - PIN must be entered to view/edit income
  - Session timeout applies to income operations

### 6.2 Input Validation
- [ ] **6.2.1** Amount validation
  - Minimum: > 0
  - Maximum: < 999,999,999 (prevent overflow)
  - Decimal places: Max 2 (₹X.XX)
  
- [ ] **6.2.2** Text input validation
  - Title: Max 255 characters
  - Sanitize special characters (prevent injection)
  - Category: Must exist in database
  
- [ ] **6.2.3** Date validation
  - Timestamp must be valid (not in future, not before 1970)
  - Handle daylight saving time transitions
  
- [ ] **6.2.4** Database constraints
  - Use UNIQUE constraints where applicable
  - Foreign key constraints enabled
  - NOT NULL constraints on required fields

### 6.3 Privacy Compliance
- [ ] **6.3.1** GDPR compliance
  - User can export all income data
  - User can delete all income data
  - No third-party sharing of income data
  
- [ ] **6.3.2** Data retention
  - Define: How long to keep deleted income data?
  - Soft delete vs hard delete logic
  - Backup retention policy

---

## PHASE 7: DOCUMENTATION & HANDOFF
**Status**: ⚪ Not Started | **Est. Duration**: 1-2 days | **Owner**: TBD

### 7.1 Code Documentation
- [ ] **7.1.1** KDoc comments
  - Document all public functions in Income[Repository, .kt, etc.]
  - Include: parameters, return values, exceptions, usage examples
  
- [ ] **7.1.2** Architecture decision documents
  - Why separate Income from Expense?
  - Why Flow-based architecture?
  - Database design decisions
  
- [ ] **7.1.3** API documentation
  - IncomeRepository contract
  - ViewModel state flows

### 7.2 User Documentation
- [ ] **7.2.1** In-app help
  - Tooltip on metrics card: "Income - Expenses = Balance"
  - Help screen: How to add income
  
- [ ] **7.2.2** User guide
  - Step-by-step: Add income
  - Step-by-step: Edit income
  - FAQ: Common questions
  
- [ ] **7.2.3** Release notes
  - What's new in this version?
  - How to use new feature?

### 7.3 Team Handoff
- [ ] **7.3.1** Code review checklist
  - All TODOs completed
  - Tests passing (100% pass rate)
  - Code style consistent
  
- [ ] **7.3.2** Deployment checklist
  - Build succeeds (no warnings)
  - ProGuard minification verified
  - Release APK size acceptable
  
- [ ] **7.3.3** Training materials
  - Architecture overview for new developers
  - How to add new income features in future
  - Troubleshooting common issues

---

## SUMMARY & TRACKING

### By Priority
1. **🔴 CRITICAL**: Phases 1 & 2 (Core feature)
2. **🟠 HIGH**: Phase 3.1 (Budget management)
3. **🟡 MEDIUM**: Phases 3.2, 3.3, 3.4, 3.7 (Recurring, Goals, Forecast, Reports)
4. **🟢 LOW**: Phases 3.5, 3.6, 3.8, 3.9, 3.10 (Multi-account, Tax, Savings Rate, Currency, Splitting)

### By Timeline
| Phase | Duration | Start Date | End Date | Status |
|-------|----------|-----------|----------|--------|
| 1: Design | 3-5 days | TBD | TBD | ⚪ |
| 2: Implementation | 4-6 days | TBD | TBD | ⚪ |
| 3: Features | Varies | TBD | TBD | ⚪ |
| 4: Design Review | 2-3 days | TBD | TBD | ⚪ |
| 5: Testing | 3-4 days | TBD | TBD | ⚪ |
| 6: Security | 1-2 days | TBD | TBD | ⚪ |
| 7: Documentation | 1-2 days | TBD | TBD | ⚪ |
| **TOTAL** | **~4-6 weeks** | TBD | TBD | 🔵 |

### Assignment Template
```
- [ ] **[Task ID]** [Task Description]
  **Assigned To**: [Name]
  **In Progress**: No
  **Blocked By**: [None/Task ID]
  **Notes**: 
```

---

**Document Version**: 1.0  
**Last Updated**: March 30, 2026  
**Next Review**: After Phase 1 completion

