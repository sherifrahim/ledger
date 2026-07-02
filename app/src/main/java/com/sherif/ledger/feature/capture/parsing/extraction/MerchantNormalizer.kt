package com.sherif.ledger.feature.capture.parsing.extraction

import javax.inject.Inject

/**
 * Normalizes raw merchant strings into canonical identities using a deterministic dictionary.
 */
class MerchantNormalizer @Inject constructor() {

    private val normalizationMap = mapOf(
        "AMAZON" to "Amazon",
        "AMZN" to "Amazon",
        "CARREFOUR" to "Carrefour",
        "COSTA" to "Costa Coffee",
        "MASHREQ" to "Mashreq",
        "ETISALAT" to "Etisalat",
        "DU " to "Du",
        "NOON" to "Noon",
        "BOTIM" to "Botim",
        "TALABAT" to "Talabat",
        "ZOMATO" to "Zomato",
        "UBER" to "Uber",
        "CAREEM" to "Careem",
        "SHEIN" to "Shein",
        "ADCB" to "ADCB",
        "DEWA" to "DEWA",
        "SEWA" to "SEWA",
        "FEWA" to "FEWA",
        "LULU" to "Lulu Hypermarket",
        "VIVA" to "Viva Supermarket",
        "WEST ZONE" to "West Zone",
        "AL MAYA" to "Al Maya",
        "CHOITHRAMS" to "Choithrams",
        "SPINNEYS" to "Spinneys",
        "WAITROSE" to "Waitrose"
    )

    fun normalize(rawMerchant: String): String {
        val upper = rawMerchant.uppercase().trim()
        
        for ((pattern, canonical) in normalizationMap) {
            if (upper.contains(pattern)) {
                return canonical
            }
        }
        
        // Title case fallback if no match found
        return rawMerchant.trim().lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
