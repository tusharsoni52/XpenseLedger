package com.xpenseledger.app.notification.service

/**
 * Merchant name → (category, subCategory, categoryId, subCategoryId) lookup table.
 *
 * Keys are lowercase for case-insensitive matching.
 * Falls back to "Other" / "Miscellaneous" when no match is found.
 */
object CategoryMatcher {

    data class CategoryMatch(
        val category: String,
        val subCategory: String,
        val categoryId: Long,
        val subCategoryId: Long?
    )

    private val DEFAULT = CategoryMatch("Other", "Miscellaneous", 8L, 82L)

    // Merchant keyword → category
    // Keys are substrings to match against (lowercased merchant name)
    private val rules: List<Pair<String, CategoryMatch>> = listOf(
        // Food Delivery
        "swiggy"      to CategoryMatch("Food", "Food Delivery", 1L, 12L),
        "zomato"      to CategoryMatch("Food", "Food Delivery", 1L, 12L),
        "dunzo"       to CategoryMatch("Food", "Food Delivery", 1L, 12L),
        "blinkit"     to CategoryMatch("Food", "Food Delivery", 1L, 12L),
        "zepto"       to CategoryMatch("Household", "Grocery", 9L, 13L),
        "bigbasket"   to CategoryMatch("Household", "Grocery", 9L, 13L),
        "grofers"     to CategoryMatch("Household", "Grocery", 9L, 13L),
        "instamart"   to CategoryMatch("Household", "Grocery", 9L, 13L),
        // Dining
        "mcdonald"    to CategoryMatch("Food", "Dining Out", 1L, 10L),
        "domino"      to CategoryMatch("Food", "Dining Out", 1L, 10L),
        "kfc"         to CategoryMatch("Food", "Dining Out", 1L, 10L),
        "subway"      to CategoryMatch("Food", "Dining Out", 1L, 10L),
        "burger king" to CategoryMatch("Food", "Dining Out", 1L, 10L),
        "pizza hut"   to CategoryMatch("Food", "Dining Out", 1L, 10L),
        // Coffee
        "starbucks"   to CategoryMatch("Food", "Coffee & Snacks", 1L, 11L),
        "cafe coffee" to CategoryMatch("Food", "Coffee & Snacks", 1L, 11L),
        "barista"     to CategoryMatch("Food", "Coffee & Snacks", 1L, 11L),
        // Transport
        "ola"         to CategoryMatch("Transport", "Cab / Auto", 2L, 21L),
        "uber"        to CategoryMatch("Transport", "Cab / Auto", 2L, 21L),
        "rapido"      to CategoryMatch("Transport", "Cab / Auto", 2L, 21L),
        "namma yatri" to CategoryMatch("Transport", "Cab / Auto", 2L, 21L),
        "irctc"       to CategoryMatch("Transport", "Travel", 2L, 23L),
        "redbus"      to CategoryMatch("Transport", "Travel", 2L, 23L),
        "abhibus"     to CategoryMatch("Transport", "Travel", 2L, 23L),
        "makemytrip"  to CategoryMatch("Travel", "Travel", 200L, 203L),
        "goibibo"     to CategoryMatch("Travel", "Travel", 200L, 203L),
        "yatra"       to CategoryMatch("Travel", "Travel", 200L, 203L),
        "indigo"      to CategoryMatch("Travel", "Flights", 200L, 201L),
        "air india"   to CategoryMatch("Travel", "Flights", 200L, 201L),
        "vistara"     to CategoryMatch("Travel", "Flights", 200L, 201L),
        "spicejet"    to CategoryMatch("Travel", "Flights", 200L, 201L),
        "oyo"         to CategoryMatch("Travel", "Hotels", 200L, 202L),
        "treebo"      to CategoryMatch("Travel", "Hotels", 200L, 202L),
        // Entertainment
        "netflix"     to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "hotstar"     to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "amazon prime" to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "zee5"        to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "sony liv"    to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "sonyliv"     to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "jiocinema"   to CategoryMatch("Entertainment", "OTT Subscriptions", 6L, 60L),
        "bookmyshow"  to CategoryMatch("Entertainment", "Movies / Events", 6L, 61L),
        "pvr"         to CategoryMatch("Entertainment", "Movies / Events", 6L, 61L),
        "inox"        to CategoryMatch("Entertainment", "Movies / Events", 6L, 61L),
        // Shopping
        "amazon"      to CategoryMatch("Shopping", "Misc Shopping", 4L, 43L),
        "flipkart"    to CategoryMatch("Shopping", "Misc Shopping", 4L, 43L),
        "myntra"      to CategoryMatch("Shopping", "Clothing", 4L, 40L),
        "ajio"        to CategoryMatch("Shopping", "Clothing", 4L, 40L),
        "nykaa"       to CategoryMatch("Shopping", "Personal Care", 4L, 42L),
        "meesho"      to CategoryMatch("Shopping", "Misc Shopping", 4L, 43L),
        "croma"       to CategoryMatch("Shopping", "Electronics", 4L, 41L),
        "reliance digital" to CategoryMatch("Shopping", "Electronics", 4L, 41L),
        // Health
        "apollo"      to CategoryMatch("Health", "Medicines", 5L, 51L),
        "medplus"     to CategoryMatch("Health", "Medicines", 5L, 51L),
        "netmeds"     to CategoryMatch("Health", "Medicines", 5L, 51L),
        "1mg"         to CategoryMatch("Health", "Medicines", 5L, 51L),
        "pharmeasy"   to CategoryMatch("Health", "Medicines", 5L, 51L),
        "cult"        to CategoryMatch("Health", "Gym / Fitness", 5L, 52L),
        "fitternity"  to CategoryMatch("Health", "Gym / Fitness", 5L, 52L),
        // Bills & Utilities
        "jio"         to CategoryMatch("Bills", "Mobile Bills", 3L, 36L),
        "airtel"      to CategoryMatch("Bills", "Mobile Bills", 3L, 36L),
        "vi "         to CategoryMatch("Bills", "Mobile Bills", 3L, 36L),
        "bsnl"        to CategoryMatch("Bills", "Mobile Bills", 3L, 36L),
        "tata sky"    to CategoryMatch("Bills", "Electricity", 3L, 30L),
        "d2h"         to CategoryMatch("Bills", "Electricity", 3L, 30L),
        "bescom"      to CategoryMatch("Bills", "Electricity", 3L, 30L),
        "msedcl"      to CategoryMatch("Bills", "Electricity", 3L, 30L),
        "mahadiscom"  to CategoryMatch("Bills", "Electricity", 3L, 30L),
        "indane"      to CategoryMatch("Bills", "Gas", 3L, 32L),
        "hp gas"      to CategoryMatch("Bills", "Gas", 3L, 32L),
        "bharat gas"  to CategoryMatch("Bills", "Gas", 3L, 32L),
        // Finance
        "lic"         to CategoryMatch("Finance", "Insurance", 7L, 53L),
        "hdfc life"   to CategoryMatch("Finance", "Insurance", 7L, 53L),
        "sbi life"    to CategoryMatch("Finance", "Insurance", 7L, 53L),
        "mutual fund" to CategoryMatch("Finance", "Investments", 7L, 73L),
        "zerodha"     to CategoryMatch("Finance", "Investments", 7L, 73L),
        "groww"       to CategoryMatch("Finance", "Investments", 7L, 73L),
        "upstox"      to CategoryMatch("Finance", "Investments", 7L, 73L),
        "emi"         to CategoryMatch("Finance", "EMI / Loans", 7L, 70L),
    )

    fun match(merchant: String): CategoryMatch {
        if (merchant.isBlank()) return DEFAULT
        val lower = merchant.lowercase()
        return rules.firstOrNull { (keyword, _) -> lower.contains(keyword) }?.second ?: DEFAULT
    }
}

