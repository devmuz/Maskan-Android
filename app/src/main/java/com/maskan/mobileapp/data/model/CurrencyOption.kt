package com.maskan.mobileapp.data.model

/**
 * Fixed currency list (08-landlord-settings.md's companion `Currency` model).
 * This picker shows symbols (identifying a currency in a list, not
 * displaying a monetary amount) — amounts elsewhere use the currency-code
 * convention instead (02-data-models.md), which is intentionally unchanged.
 */
data class CurrencyOption(val code: String, val displayName: String, val symbol: String)

object Currencies {
    val all = listOf(
        CurrencyOption("USD", "US Dollar", "$"),
        CurrencyOption("EUR", "Euro", "€"),
        CurrencyOption("GBP", "British Pound", "£"),
        CurrencyOption("AED", "UAE Dirham", "د.إ"),
        CurrencyOption("SAR", "Saudi Riyal", "ر.س"),
        CurrencyOption("PKR", "Pakistani Rupee", "₨"),
        CurrencyOption("INR", "Indian Rupee", "₹"),
        CurrencyOption("EGP", "Egyptian Pound", "E£"),
        CurrencyOption("QAR", "Qatari Riyal", "ر.ق"),
        CurrencyOption("KWD", "Kuwaiti Dinar", "د.ك"),
        CurrencyOption("OMR", "Omani Rial", "ر.ع."),
        CurrencyOption("BHD", "Bahraini Dinar", ".د.ب"),
        CurrencyOption("MYR", "Malaysian Ringgit", "RM"),
        CurrencyOption("NGN", "Nigerian Naira", "₦"),
        CurrencyOption("BDT", "Bangladeshi Taka", "৳"),
        CurrencyOption("TRY", "Turkish Lira", "₺"),
        CurrencyOption("IDR", "Indonesian Rupiah", "Rp"),
    )

    fun byCode(code: String?): CurrencyOption? = all.find { it.code == code }
}
