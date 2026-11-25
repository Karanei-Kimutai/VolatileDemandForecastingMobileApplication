package com.example.volatiledemandforecasting.feature.workspace.create

import com.example.volatiledemandforecasting.core.ui.components.SelectOption
import java.time.Instant
import java.time.ZoneId
import java.util.Currency
import java.util.Locale

/**
 * The choices for a shop's country, currency and time zone, from the platform's own lists,
 * so every choice passes [StoreFormValidation]. Names are in the phone's language.
 */
object StoreOptions {
    fun countries(locale: Locale = Locale.getDefault()): List<SelectOption> =
        Locale.getISOCountries()
            .map { code -> SelectOption(code, Locale.Builder().setRegion(code).build().getDisplayCountry(locale), code) }
            .sortedBy { it.label }

    fun currencies(locale: Locale = Locale.getDefault()): List<SelectOption> =
        Currency.getAvailableCurrencies()
            .map { SelectOption(it.currencyCode, it.getDisplayName(locale), it.currencyCode) }
            .sortedBy { it.label }

    /** Region/City zones only; the legacy aliases (EST, Etc/GMT+3, SystemV/…) only confuse the list. */
    fun timezones(now: Instant = Instant.now()): List<SelectOption> =
        ZoneId.getAvailableZoneIds()
            .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .map { id ->
                val offset = ZoneId.of(id).rules.getOffset(now).id.let { if (it == "Z") "+00:00" else it }
                SelectOption(id, id.replace('_', ' '), "UTC$offset", fieldText = id.replace('_', ' '))
            }
            .sortedBy { it.value }
}
