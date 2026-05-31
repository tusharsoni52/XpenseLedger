package com.xpenseledger.app.notification.service

/**
 * Registry of all monitored app packages and their display labels.
 * Only notifications from these packages are parsed.
 */
object MonitoredApps {

    data class AppInfo(val packageName: String, val label: String, val type: String)

    val ALL = listOf(
        // UPI apps
        AppInfo("com.google.android.apps.nbu.paisa.user", "Google Pay",   "UPI"),
        AppInfo("com.phonepe.app",                        "PhonePe",      "UPI"),
        AppInfo("net.one97.paytm",                        "Paytm",        "UPI"),
        AppInfo("in.org.npci.upiapp",                     "BHIM",         "UPI"),
        AppInfo("in.amazon.mShop.android.shopping",       "Amazon Pay",   "UPI"),
        AppInfo("com.freecharge.android",                 "FreeCharge",   "UPI"),
        AppInfo("com.mobikwik_new",                       "MobiKwik",     "UPI"),
        // Bank apps
        AppInfo("com.hdfc.mobilebanking",                 "HDFC Bank",    "BANK"),
        AppInfo("com.sbi.lotusintouch",                   "SBI YONO",     "BANK"),
        AppInfo("com.csam.icici.bank.imobile",            "ICICI iMobile","BANK"),
        AppInfo("com.axis.mobile",                        "Axis Bank",    "BANK"),
        AppInfo("com.msf.kbank.mobile",                   "Kotak Bank",   "BANK"),
        AppInfo("com.barclays.bpb",                       "Barclays",     "BANK"),
        AppInfo("com.indusind.mobilindus",                "IndusInd Bank","BANK"),
        AppInfo("com.idfc.mfapp",                         "IDFC Bank",    "BANK"),
        AppInfo("com.pnb.mbanking",                       "PNB",          "BANK"),
        AppInfo("com.ubi.official",                       "Union Bank",   "BANK"),
        AppInfo("com.fss.citi",                           "Citi Bank",    "BANK"),
    )

    private val byPackage = ALL.associateBy { it.packageName }

    fun find(packageName: String): AppInfo? = byPackage[packageName]
    fun isKnown(packageName: String): Boolean = byPackage.containsKey(packageName)
}

