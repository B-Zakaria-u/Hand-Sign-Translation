package com.handsign.poc.inference

import java.io.File
import java.security.MessageDigest

/**
 * Validates model files before loading them into any runtime.
 * Checks extension, file size, and magic bytes to prevent bad input from
 * crashing native runtimes or causing unexpected behavior.
 */
object ModelValidator {

    private val TFLITE_MAGIC = byteArrayOf(0x18, 0x00, 0x00, 0x00) // FlatBuffer schema
    private val ONNX_MAGIC   = byteArrayOf(0x08)                    // Protobuf field 1 varint
    private val PTL_MAGIC    = byteArrayOf(0x50, 0x4B, 0x03, 0x04)  // ZIP/PK header

    private val VALID_EXTENSIONS = setOf("tflite", "task", "onnx", "ptl", "lite")
    private const val MIN_FILE_BYTES = 32L                  // Minimum valid model size
    private const val MAX_FILE_BYTES = 200L * 1024 * 1024  // 200 MB

    /**
     * Validates [file] for correct extension, size, and magic bytes.
     * Returns [Result.success] if valid, [Result.failure] with an
     * [IllegalArgumentException] or [SecurityException] otherwise.
     */
    fun validate(file: File): Result<Unit> {
        if (!file.exists())
            return Result.failure(IllegalArgumentException("Model file not found: ${file.path}"))

        val ext = file.extension.lowercase()
        if (ext !in VALID_EXTENSIONS)
            return Result.failure(IllegalArgumentException(
                "Unsupported model extension '.$ext'. Supported: ${VALID_EXTENSIONS.joinToString()}"
            ))

        if (file.length() < MIN_FILE_BYTES)
            return Result.failure(IllegalArgumentException(
                "Model file is corrupt or too small (${file.length()} bytes). Minimum size: $MIN_FILE_BYTES bytes"
            ))

        if (file.length() > MAX_FILE_BYTES)
            return Result.failure(IllegalArgumentException(
                "Model file is too large (${file.length() / 1_048_576} MB). Max: 200 MB"
            ))

        // Read first 4 bytes for magic bytes check
        val header = file.inputStream().use { stream -> ByteArray(4).also { stream.read(it) } }
        val expectedMagic: ByteArray? = when (ext) {
            "tflite", "task", "lite" -> TFLITE_MAGIC
            "onnx"                   -> ONNX_MAGIC
            "ptl"                    -> PTL_MAGIC
            else                     -> null
        }
        if (expectedMagic != null) {
            val headerSlice = header.take(expectedMagic.size).toByteArray()
            if (!headerSlice.contentEquals(expectedMagic))
                return Result.failure(SecurityException(
                    "File header mismatch for .$ext model — file may be corrupt or misnamed"
                ))
        }

        return Result.success(Unit)
    }

    /**
     * Verifies the SHA-256 digest of [file] against the [expected] hex string.
     * Used when the user provides a hash for integrity checking after download.
     */
    fun verifySha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val actual = digest.joinToString("") { "%02x".format(it) }
        return actual.equals(expected.trim(), ignoreCase = true)
    }
}
