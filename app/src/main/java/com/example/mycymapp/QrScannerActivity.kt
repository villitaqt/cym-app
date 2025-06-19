package com.example.mycymapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mycymapp.databinding.ActivityQrScannerBinding // Binding para esta Activity
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isFlashOn = false

    companion object {
        private const val TAG = "QrScannerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.title = "Escanear DNI"
        binding.toolbar.setNavigationOnClickListener {
            finish() // Volver a la actividad anterior al hacer clic en la flecha
        }

        // Inicializar la cámara
        startCamera()

        // Configurar el botón de flash
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // Desvincular todos los casos de uso antes de vincular nuevos
        cameraProvider.unbindAll()

        // Caso de uso de vista previa
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        // Caso de uso de análisis de imagen
        val barcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // Solo escanear QR
            .build()
        val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720)) // Resolución de la imagen para análisis
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Solo procesar el último fotograma
            .build()

        imageAnalysis?.setAnalyzer(Executors.newSingleThreadExecutor(), { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        })

        try {
            // Vincular casos de uso a la cámara
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
            // Habilitar/deshabilitar el flash si está disponible
            binding.flashButton.isEnabled = camera.cameraInfo.hasFlashUnit()
            if (camera.cameraInfo.hasFlashUnit()) {
                camera.cameraControl.enableTorch(isFlashOn) // Asegura que el flash esté en el estado correcto
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Error al vincular casos de uso de cámara: ${exc.message}", exc)
            Toast.makeText(this, "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun processImageProxy(barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        // Convertir ImageProxy a InputImage para ML Kit
        imageProxy.image?.let { mediaImage ->
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        // Código QR detectado
                        val scannedValue = barcodes.first().rawValue
                        if (!scannedValue.isNullOrEmpty()) {
                            // Devolver el DNI escaneado a la actividad anterior
                            val resultIntent = Intent()
                            resultIntent.putExtra("SCANNED_DNI", scannedValue)
                            setResult(RESULT_OK, resultIntent)
                            finish() // Cierra la actividad del escáner
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al escanear código de barras: ${e.message}", e)
                    // No mostrar Toast aquí, ya que se repetiría muchas veces si hay error constante
                }
                .addOnCompleteListener {
                    // Cierra la imagen proxy después de procesar para liberar recursos
                    imageProxy.close()
                }
        } ?: imageProxy.close() // Si mediaImage es null, cierra la ImageProxy de todos modos
    }

    private fun toggleFlash() {
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector) // Re-vincular para obtener el control de la cámara
        isFlashOn = !isFlashOn
        camera.cameraControl.enableTorch(isFlashOn)
        // Actualizar el icono del botón de flash
        binding.flashButton.setImageResource(if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegúrate de que la cámara se desvincule al destruir la actividad
        cameraProvider.unbindAll()
    }
}