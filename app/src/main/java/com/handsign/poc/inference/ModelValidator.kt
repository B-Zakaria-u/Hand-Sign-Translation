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
    private val ZIP_PK_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)  // ZIP/PK header (.task, .ptl, .keras)
    private val HDF5_MAGIC   = byteArrayOf(0x89.toByte(), 0x48, 0x44, 0x46) // \x89HDF (.h5)

    private val VALID_EXTENSIONS = setOf("tflite", "task", "onnx", "ptl", "lite", "keras", "h5")
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

        // Read first 8 bytes for magic bytes check to account for potential padding
        val header = file.inputStream().use { stream -> ByteArray(8).also { stream.read(it) } }
        val validMagics: List<ByteArray>? = when (ext) {
            "tflite", "lite" -> listOf(TFLITE_MAGIC)
            "task", "keras"  -> listOf(TFLITE_MAGIC, ZIP_PK_MAGIC)
            "h5"             -> listOf(HDF5_MAGIC)
            "onnx"           -> listOf(ONNX_MAGIC)
            "ptl"            -> listOf(ZIP_PK_MAGIC)
            else             -> null
        }
        if (validMagics != null) {
            val matches = validMagics.any { magic ->
                // Check if the magic bytes exist anywhere in the first 8 bytes
                (0..header.size - magic.size).any { offset ->
                    header.copyOfRange(offset, offset + magic.size).contentEquals(magic)
                }
            }
            if (!matches) {
                return Result.failure(SecurityException(
                    "File header mismatch for .$ext model — file may be corrupt or misnamed"
                ))
            }
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
