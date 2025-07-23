package com.jm.qrbarcodescanner.model.json

import kotlinx.serialization.Serializable


@Serializable
data class PurchaseItem(
    val item: String,
    val quantity: Int,
    val price: Double
)