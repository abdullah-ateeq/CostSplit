package com.example.costsplit.ui.state

/**
 * State for a single person row in the editor.
 */
data class PersonInputState(
    val id: String,
    val name: String = "",
    val shareInput: String = "1",
    val percentageInput: String = "25"
)
