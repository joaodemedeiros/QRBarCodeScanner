package com.jm.qrbarcodescanner.presentation

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jm.qrbarcodescanner.model.BarCodeAnalyzer
import com.jm.qrbarcodescanner.model.json.BarModel

/**
 * Main Composable that manages camera permission requests and displays
 * either the scanner screen or a message to grant permission.
 *
 * @param viewModel Instance of [com.codegalaxy.barcodescanner.presentation.BarCodeScannerViewModel] to handle scanning logic.
 */
@Composable
fun BarcodeScannerScreen( // This is the main entry point Composable for the barcode scanning feature.
    viewModel: BarCodeScannerViewModel
) {
    val context = LocalContext.current // Gets the current Android Context.
    // State to track if camera permission has been granted.
    var hasCameraPermission by remember { // `remember` stores the state across recompositions.
        mutableStateOf( // `mutableStateOf` creates an observable state; when it changes, Compose reruns relevant Composables.
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher for requesting camera permission.
    val launcher =
        rememberLauncherForActivityResult( // Handles the result of the permission request.
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasCameraPermission =
                isGranted // Updates the permission state based on user's response.
        }

    // Effect that runs once when the Composable enters the composition.
    // If permission is not granted, it launches the permission request.
    LaunchedEffect(Unit) { // `LaunchedEffect` is used for side effects that need a coroutine scope. `Unit` means it runs once.
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA) // Triggers the permission dialog.
        }
    }

    // Decides what to display based on whether camera permission is granted.
    if (hasCameraPermission) {
        CameraPreview(viewModel) // Shows the camera preview if permission is granted.
    } else {
        // Shows a message and a button to request permission if not granted.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission is required for scanning barcodes")
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant Permission")
            }
        }
    }
}

/**
 * Composable that displays the camera preview and the UI for barcode scanning.
 * It also handles the different scanning states.
 *
 * @param viewModel Instance of [BarCodeScannerViewModel] to interact with scanning logic.
 */
@Composable
fun CameraPreview(viewModel: BarCodeScannerViewModel) { // This Composable handles the camera feed and UI updates based on scan state.
    val context = LocalContext.current
    val lifecycleOwner =
        LocalLifecycleOwner.current // Gets the LifecycleOwner to bind CameraX lifecycle.
    // State for the CameraX Preview instance.
    var preview by remember { mutableStateOf<Preview?>(null) }
    // State for the CameraX ProcessCameraProvider instance.
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // Gets the current scan state from the ViewModel. This is an observable State.
    val barScanState = viewModel.barScanState

    // Effect to unbind camera use cases when scanning is successful.
    // It runs every time `barScanState` changes.
    LaunchedEffect(barScanState) { // Keyed `LaunchedEffect`: reruns if `barScanState` changes.
        if (barScanState is BarScanState.ScanSuccess) {
            cameraProvider?.unbindAll() // Unbinds all use cases to release the camera.
        }
    }

    Scaffold {
        Column { // Arranges the preview and scan information vertically.
            Box(
                modifier = Modifier
                    .padding(it)
                    .size(400.dp) // Fixed size for the camera preview.
                    .padding(16.dp) // Adds padding around the preview.
            ) {
                // Shows the camera view only if scanning has not been successful yet.
                if (barScanState !is BarScanState.ScanSuccess) {
                    AndroidView( // Embeds a traditional Android View (PreviewView) into Compose.
                        factory = { androidViewContext ->
                            // Creates and configures the CameraX PreviewView.
                            PreviewView(androidViewContext).apply {
                                this.scaleType = PreviewView.ScaleType.FILL_START
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                        },
                        modifier = Modifier.fillMaxSize(), // The PreviewView occupies the entire Box.
                        update = { previewView -> // This block is called when the view needs to be (re)configured.
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Selects the back camera.
                                .build()
                            val cameraExecutor: ExecutorService =
                                Executors.newSingleThreadExecutor() // Executor for camera operations.
                            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                                ProcessCameraProvider.getInstance(context) // Gets the camera provider instance.

                            cameraProviderFuture.addListener(
                                { // Callback executed when ProcessCameraProvider is available.
                                    preview = Preview.Builder().build().also {
                                        it.surfaceProvider =
                                            previewView.surfaceProvider // Connects PreviewView to Preview use case.
                                    }

                                    val provider: ProcessCameraProvider = cameraProviderFuture.get()
                                    cameraProvider = provider // Saves the provider instance.
                                    val barcodeAnalyzer =
                                        BarCodeAnalyzer(viewModel) // Creates the barcode analyzer.
                                    val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Strategy for handling image frames.
                                        .build()
                                        .also {
                                            it.setAnalyzer(
                                                cameraExecutor,
                                                barcodeAnalyzer
                                            ) // Assigns the analyzer to the ImageAnalysis use case.
                                        }

                                    try {
                                        provider.unbindAll() // Unbinds any previous use cases.
                                        // Binds the use cases (Preview, ImageAnalysis) to the lifecycle.
                                        provider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        Log.d(
                                            "CameraPreview",
                                            "Error: ${e.localizedMessage}"
                                        ) // Logs errors during binding.
                                    }

                                    // Lifecycle observer to release the camera executor.
                                    lifecycleOwner.lifecycle.addObserver(object :
                                        DefaultLifecycleObserver {
                                        override fun onDestroy(owner: LifecycleOwner) { // Called when the LifecycleOwner is destroyed.
                                            cameraExecutor.shutdown() // Shuts down the executor.
                                        }
                                    })
                                },
                                ContextCompat.getMainExecutor(context)
                            ) // Runs the listener on the main thread.
                        }
                    )
                }
            }

            // 'when' block to display different UI based on the scan state.
            // This is a powerful Kotlin construct for handling different cases of a sealed interface or enum.
            when (barScanState) {
                is BarScanState.Ideal -> {
                    // Initial state: asks the user to position the barcode.
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Position the barcode in front of the camera.")
                    }
                }

                is BarScanState.Loading -> {
                    // Loading state: shows a progress indicator.
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning...")
                    }
                }

                is BarScanState.ScanSuccess -> {
                    // Success state: shows the scan results.
                    ScanResultContent( // Delegates UI for success to another Composable.
                        scanSuccess = barScanState, // Passes the successful scan information.
                        onRescan = { viewModel.resetState() } // Allows the user to scan again.
                    )
                }

                is BarScanState.Error -> {
                    // Error state: shows an error message and a retry button.
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${barScanState.error}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for displaying the content of a successful scan result.
 * It can display parsed JSON data or the raw barcode value.
 *
 * @param scanSuccess The [BarScanState.ScanSuccess] state containing scan data.
 * @param onRescan Callback for when the user wants to perform another scan.
 */
@Composable
fun ScanResultContent(
    scanSuccess: BarScanState.ScanSuccess,
    onRescan: () -> Unit
) { // Displays the result of a successful scan.

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (scanSuccess.barStateModel != null) {
            // If the barcode was JSON and parsed successfully, display model data.
            Text("Invoice Id: ${scanSuccess.barStateModel.invoiceNumber}")
            Text("Name: ${scanSuccess.barStateModel.client.name}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Purchases:", style = MaterialTheme.typography.titleMedium)
            scanSuccess.barStateModel.purchase.forEach { item ->
                Text("${item.item}: ${item.quantity} x $${item.price}")
            }
            Text("Total Amount: $${scanSuccess.barStateModel.totalAmount}")
        } else {
            // If not parseable as JSON or not JSON, display raw format and value.
            Text("Format: ${scanSuccess.format}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Value: ${scanSuccess.rawValue}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRescan) { // Button to scan another barcode.
            Text("Scan Another")
        }
    }

}

