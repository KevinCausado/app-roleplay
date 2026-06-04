package com.elara.app.ui.camera

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectorHelper(context: Context) {

    private val detector: ObjectDetector

    private var lastReportedObjects: Set<String> = emptySet()
    private var lastReportTime: Long = 0
    private val THROTTLE_MS = 5_000L

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        detector = ObjectDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy, onContext: (String?) -> Unit) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { objects ->
                val labels = objects
                    .flatMap { it.labels }
                    .filter { it.confidence >= 0.45f }
                    .map { it.text }
                    .toSet()

                val now = System.currentTimeMillis()
                val changed = labels != lastReportedObjects && labels.isNotEmpty()
                val timedOut = (now - lastReportTime) > THROTTLE_MS

                if (changed || timedOut && labels.isNotEmpty()) {
                    lastReportedObjects = labels
                    lastReportTime = now
                    onContext(labels.joinToString(", "))
                } else {
                    onContext(null)
                }
            }
            .addOnFailureListener { onContext(null) }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() = detector.close()
}
