package com.phdev.quantofalta.domain.model.mode

/**
 * Common interface implemented by all card style enums (Standard, Relationship, Salary).
 * This allows a single unified CardStyleSelector composable to work for all modes.
 */
interface CardStyleItem {
    val styleId: String
    val displayName: String
    val isPremium: Boolean
}
