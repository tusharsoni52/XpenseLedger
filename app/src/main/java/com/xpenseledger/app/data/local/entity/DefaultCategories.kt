package com.xpenseledger.app.data.local.entity

object DefaultCategories {
    val all: List<CategoryEntity> = listOf(
        // ── MAIN ──────────────────────────────────────────────
        CategoryEntity(1, "Food",          "MAIN", null, "🍽"),
        CategoryEntity(2, "Transport",     "MAIN", null, "🚗"),
        CategoryEntity(3, "Bills",         "MAIN", null, "📋"),
        CategoryEntity(4, "Shopping",      "MAIN", null, "🛒"),
        CategoryEntity(5, "Health",        "MAIN", null, "💊"),
        CategoryEntity(6, "Entertainment", "MAIN", null, "🎬"),
        CategoryEntity(7, "Finance",       "MAIN", null, "💰"),
        CategoryEntity(8, "Other",         "MAIN", null, "📦"),
        CategoryEntity(9, "Household",     "MAIN", null, "🏠"),
        CategoryEntity(200, "Travel",      "MAIN", null, "✈️"),

        // ── Food ──────────────────────────────────────────────
        CategoryEntity(10, "Dining Out",      "SUB", 1),
        CategoryEntity(11, "Coffee & Snacks", "SUB", 1),
        CategoryEntity(12, "Food Delivery",   "SUB", 1),  // Moved from Food to Household

        // ── Transport ─────────────────────────────────────────
        CategoryEntity(20, "Fuel",             "SUB", 2),
        CategoryEntity(21, "Cab / Auto",       "SUB", 2),
        CategoryEntity(22, "Public Transport", "SUB", 2),
        CategoryEntity(23, "Travel",           "SUB", 2),
        CategoryEntity(24, "Road Toll",        "SUB", 2),
        CategoryEntity(25, "Vehicle Service",  "SUB", 2),  // NEW
        CategoryEntity(26, "Parking",          "SUB", 2),  // NEW

        // ── Bills ─────────────────────────────────────────────
        CategoryEntity(30, "Electricity",    "SUB", 3),
        CategoryEntity(31, "Water",          "SUB", 3),
        CategoryEntity(32, "Gas",            "SUB", 3),
        CategoryEntity(33, "Internet",       "SUB", 3),
        CategoryEntity(35, "Home Rent",      "SUB", 3),
        CategoryEntity(36, "Mobile Bills",   "SUB", 3),

        // ── Shopping ──────────────────────────────────────────
        CategoryEntity(40, "Clothing",      "SUB", 4),
        CategoryEntity(41, "Electronics",   "SUB", 4),
        CategoryEntity(42, "Personal Care", "SUB", 4),
        CategoryEntity(43, "Misc Shopping", "SUB", 4),

        // ── Health ────────────────────────────────────────────
        CategoryEntity(50, "Doctor",       "SUB", 5),
        CategoryEntity(51, "Medicines",    "SUB", 5),
        CategoryEntity(52, "Gym / Fitness","SUB", 5),

        // ── Entertainment ─────────────────────────────────────
        CategoryEntity(60, "OTT Subscriptions","SUB", 6),
        CategoryEntity(61, "Movies / Events",  "SUB", 6),
        CategoryEntity(62, "Games",            "SUB", 6),

        // ── Finance ───────────────────────────────────────────
        CategoryEntity(70, "EMI / Loans",           "SUB", 7),
        CategoryEntity(71, "Credit Card Payment",   "SUB", 7),
        CategoryEntity(72, "Taxes",                 "SUB", 7),
        CategoryEntity(73, "Investments",           "SUB", 7),
        CategoryEntity(74, "Recurring Deposit",     "SUB", 7),
        CategoryEntity(75, "Fixed Deposit",         "SUB", 7),
        CategoryEntity(53, "Insurance",             "SUB", 7),  // Moved from Health to Finance
        CategoryEntity(76, "Vehicle Insurance",     "SUB", 7),  // NEW
        CategoryEntity(77, "Bank Charges",          "SUB", 7),  // NEW
        CategoryEntity(78, "Family Support",        "SUB", 7),  // Transfer-aware subcategory

        // ── Household ─────────────────────────────────────────
        CategoryEntity(90, "Maid Salary",       "SUB", 9),
        CategoryEntity(91, "Cook Salary",       "SUB", 9),
        CategoryEntity(92, "Helper Salary",     "SUB", 9),  // NEW
        CategoryEntity(93, "House Maintenance", "SUB", 9),  // NEW
        CategoryEntity(13, "Grocery",           "SUB", 9),

        // ── Travel ────────────────────────────────────────────
        CategoryEntity(201, "Flights",   "SUB", 200),  // NEW
        CategoryEntity(202, "Hotels",    "SUB", 200),  // NEW
        CategoryEntity(203, "Vacation",  "SUB", 200),  // NEW

        // ── Other ─────────────────────────────────────────────
        CategoryEntity(80, "Gifts",        "SUB", 8),
        CategoryEntity(81, "Donations",    "SUB", 8),
        CategoryEntity(82, "Miscellaneous","SUB", 8),

        // ── Income ────────────────────────────────────────────
        CategoryEntity(300, "Income",      "MAIN", null, "💵"),
        CategoryEntity(301, "Salary",      "SUB",  300,  ""),
        CategoryEntity(302, "Bonus",       "SUB",  300,  ""),
        CategoryEntity(303, "Freelance",   "SUB",  300,  ""),
        CategoryEntity(304, "Interest",    "SUB",  300,  ""),
        CategoryEntity(305, "Other Income","SUB",  300,  ""),
    )
}
