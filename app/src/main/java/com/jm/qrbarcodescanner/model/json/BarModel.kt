package com.jm.qrbarcodescanner.model.json


import kotlinx.serialization.Serializable

@Serializable
data class BarModel(
    val invoiceNumber: String,
    val client: Client,
    val purchase: List<PurchaseItem>,
    val totalAmount: Double
)
