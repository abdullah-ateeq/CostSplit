package com.example.costsplit.model

/**
 * Represents a person participating in an expense allocation.
 *
 * @param id Unique identifier for the person
 * @param name Optional display name (falls back to "Person N" if blank)
 * @param share Weight used in Shares mode (must be whole number >= 1)
 */
data class CostPerson(
    val id: String,
    val name: String,
    val share: Long
)
