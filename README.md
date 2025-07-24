# Barcode Scanner App

A modern Android application built with Jetpack Compose that scans both QR codes and various barcode formats. 
The app supports JSON-formatted QR codes for invoice data and standard barcodes, providing a clean and user-friendly interface.

# Features

- Supports multiple barcode formats:
  - QR Code
  - Aztec
  - Codabar
  - Code 39
  - Code 93
  - Code 128
  - Data Matrix
  - EAN-8
  - EAN-13
  - ITF
  - PDF417
  - UPC-A
  - UPC-E

- Real-time barcode scanning
- JSON QR code parsing for invoice data
- Clean Material Design 3 UI
- Proper camera permission handling
- Automatic camera cleanup
- Error handling and user feedback

# Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Barcode Scanning**: ML Kit
- **Camera**: CameraX
- **JSON Parsing**: KotlinX Serialization
- **Minimum SDK**: 26 (Android 8.0)

# JSON Format for QR Codes

The app expects QR codes containing invoice data to be in the following JSON format:
```json
{
    "invoiceNumber": "INV12345",
    "client": {
        "name": "John Doe",
        "email": "john.doe@example.com",
        "address": "123 Main St, Cityville, Country"
    },
    "purchase": [
        {
            "item": "Laptop",
            "quantity": 1,
            "price": 1000
        },
        {
            "item": "Mouse",
            "quantity": 2,
            "price": 25
        }
    ],
    "totalAmount": 1050
}
```

# Architecture

The app follows the MVVM architecture pattern:

- **Model**: Data classes for invoice data (BarModel, Client, PurchaseItem)
- **View**: Compose UI components (BarcodeScannerScreen, CameraPreview)
- **ViewModel**: BarCodeScannerViewModel for handling business logic and state management
- **State**: BarScanState for managing UI states

# Usage

1. Launch the app
2. Grant camera permission when prompted
3. Point the camera at a barcode or QR code
4. The app will automatically detect and process the code:
   - For JSON QR codes: Displays invoice details
   - For regular barcodes: Shows the format and raw value
5. Click "Scan Another" to scan more codes
