package com.jm.qrbarcodescanner.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Provides a coroutine scope tied to the ViewModel's lifecycle.
import com.google.mlkit.vision.barcode.common.Barcode
import com.jm.qrbarcodescanner.model.json.BarModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch // Builder for launching coroutines.
import kotlinx.serialization.json.Json

/**
 * Sealed interface representing the different possible states of the barcode scan.
 * Allows for exhaustive handling of states in the UI.
 * A core Kotlin feature for creating restricted class hierarchies.
 */
sealed interface BarScanState {
    // `sealed interface` ensures all subtypes are known at compile time.
    /**
     * Initial state or when no scanning operation is in progress.
     * `data object` is a singleton instance for states without parameters.
     */
    data object Ideal : BarScanState

    /**
     * State indicating that the scan was successful.
     * @param barStateModel Data model if the barcode is parseable JSON.
     * @param rawValue The raw value of the barcode.
     * @param format The detected barcode format (e.g., QR_CODE, EAN_13).
     * `data class` automatically generates equals(), hashCode(), toString(), etc.
     */
    data class ScanSuccess(
        val barStateModel: BarModel? = null,
        val rawValue: String? = null,
        val format: String? = null
    ) : BarScanState

    /**
     * State indicating that an error occurred during scanning.
     * @param error Descriptive error message.
     */
    data class Error(val error: String) : BarScanState

    /**
     * State indicating that the scanning process is ongoing.
     */
    data object Loading : BarScanState
}

/**
 * ViewModel for the business logic of barcode scanning.
 * Manages the scan state and interaction with ML Kit.
 * Inherits from AndroidX `ViewModel` for lifecycle awareness.
 */
@HiltViewModel
class BarCodeScannerViewModel @Inject constructor() : ViewModel() {
    // JSON parser configuration, lenient to ignore unknown keys and minor errors.
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Private mutable state holding the current scan state.
    // Uses `getValue` and `setValue` for Compose property delegation.
    // `mutableStateOf` creates an observable state that Compose can react to.
    private var _barScanState by mutableStateOf<BarScanState>(BarScanState.Ideal)

    // Public, immutable state exposed to the UI.
    // The UI observes this `barScanState` for changes.
    val barScanState: BarScanState get() = _barScanState

    /**
     * Function called when the camera detects one or more barcodes.
     * Processes the first valid barcode found.
     * @param barcodes List of [Barcode] objects detected by ML Kit.
     * This function is the primary entry point for barcode data from the analyzer.
     */
    fun onBarCodeDetected(barcodes: List<Barcode>) {
        // Launches a coroutine in the viewModelScope for asynchronous operations.
        // `viewModelScope` ensures the coroutine is cancelled if the ViewModel is cleared.
        viewModelScope.launch {
            // If no barcodes are detected, set the error state.
            if (barcodes.isEmpty()) {
                _barScanState = BarScanState.Error("No barcode detected")
                return@launch // Exits the coroutine.
            }

            // Set state to Loading while processing.
            _barScanState = BarScanState.Loading

            // Iterate over detected barcodes (though typically only the first one is processed).
            barcodes.forEach { barcode ->
                // Get the raw value of the barcode. `let` is a Kotlin scope function.
                barcode.rawValue?.let { barcodeValue ->
                    try {
                        // Attempt to parse the barcode value as JSON.
                        try {
                            val barModel: BarModel = jsonParser.decodeFromString(barcodeValue)
                            // If parsing is successful, set ScanSuccess state with the parsed model.
                            _barScanState = BarScanState.ScanSuccess(barStateModel = barModel)
                        } catch (e: Exception) {
                            // If it's not valid JSON or parsing fails,
                            // set ScanSuccess state with the raw value and barcode format.
                            _barScanState = BarScanState.ScanSuccess(
                                rawValue = barcodeValue,
                                format = getBarcodeFormatName(barcode.format)
                            )
                        }
                    } catch (e: Exception) {
                        // If any other exception occurs during processing, log the error and set Error state.
                        Log.e("BarCodeScanner", "Error processing barcode", e)
                        _barScanState = BarScanState.Error("Error processing barcode: ${e.message}")
                    }
                    // After processing the first barcode with a value, exit the coroutine.
                    return@launch
                }
            }
            // If after iterating all barcodes, none have a rawValue, set an error.
            _barScanState = BarScanState.Error("No valid barcode value")
        }
    }

    /**
     * Converts the ML Kit barcode format integer to a human-readable name.
     * @param format The integer value of the barcode format (e.g., [Barcode.FORMAT_QR_CODE]).
     * @return A string representing the format name.
     * The `when` expression is Kotlin's powerful equivalent to switch statements.
     */
    private fun getBarcodeFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_CODE_39 -> "CODE 39"
            Barcode.FORMAT_CODE_93 -> "CODE 93"
            Barcode.FORMAT_CODE_128 -> "CODE 128"
            Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
            Barcode.FORMAT_EAN_8 -> "EAN 8"
            Barcode.FORMAT_EAN_13 -> "EAN 13"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_UPC_A -> "UPC A"
            Barcode.FORMAT_UPC_E -> "UPC E"
            else -> "Unknown" // Unknown or unhandled format.
        }
    }

    /**
     * Resets the scan state to its initial [BarScanState.Ideal] state.
     * Useful for allowing the user to perform a new scan.
     * This function is typically called from the UI (e.g., "Scan Another" button).
     */
    fun resetState() {
        _barScanState = BarScanState.Ideal
    }
}
