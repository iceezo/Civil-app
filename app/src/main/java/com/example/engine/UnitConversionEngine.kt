package com.example.engine

import java.util.Locale

object UnitConversionEngine {

    enum class UnitSystem(val label: String) {
        METRIC("Metric (m, mm, kN, MPa)"),
        IMPERIAL("Imperial (ft, in, kips, psi)")
    }

    // Length conversion
    fun mToFt(m: Double): Double = m * 3.28084
    fun ftToM(ft: Double): Double = ft / 3.28084
    fun mmToIn(mm: Double): Double = mm / 25.4
    fun inToMm(inch: Double): Double = inch * 25.4

    // Area conversion
    fun sqmToSqft(sqm: Double): Double = sqm * 10.7639
    fun sqftToSqm(sqft: Double): Double = sqft / 10.7639

    // Volume conversion
    fun cumToCuft(cum: Double): Double = cum * 35.3147
    fun cuftToCum(cuft: Double): Double = cuft / 35.3147

    // Force / Load conversion
    fun knToKips(kn: Double): Double = kn * 0.224809
    fun kipsToKn(kips: Double): Double = kips / 0.224809
    fun knPerMToKipsPerFt(knM: Double): Double = knM * 0.0685218
    fun kipsPerFtToKnPerM(kipsFt: Double): Double = kipsFt / 0.0685218

    // Stress / Pressure conversion
    fun mpaToPsi(mpa: Double): Double = mpa * 145.038
    fun psiToMpa(psi: Double): Double = psi / 145.038
    fun kpaToPsf(kpa: Double): Double = kpa * 20.8854
    fun psfToKpa(psf: Double): Double = psf / 20.8854

    // Weight conversion
    fun kgToLbs(kg: Double): Double = kg * 2.20462
    fun lbsToKg(lbs: Double): Double = lbs / 2.20462

    fun formatDouble(value: Double, decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun formatCurrency(amount: Double, currencyCode: String = "NGN"): String {
        val symbol = when {
            currencyCode.contains("NGN") || currencyCode.contains("₦") -> "₦"
            currencyCode.contains("USD") || currencyCode.contains("$") -> "$"
            currencyCode.contains("GBP") || currencyCode.contains("£") -> "£"
            currencyCode.contains("EUR") || currencyCode.contains("€") -> "€"
            currencyCode.contains("GHS") || currencyCode.contains("GH₵") -> "GH₵"
            currencyCode.contains("KES") || currencyCode.contains("KSh") -> "KSh "
            currencyCode.contains("INR") || currencyCode.contains("₹") -> "₹"
            else -> "₦"
        }
        return "$symbol ${String.format(Locale.US, "%,.2f", amount)}"
    }
}
