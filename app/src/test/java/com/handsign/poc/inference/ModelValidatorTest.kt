package com.handsign.poc.inference

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelValidatorTest {

    @Test
    fun validate_stubFile_returnsFailure() {
        val file = File.createTempFile("stub", ".tflite").apply { deleteOnExit() }
        file.writeBytes(byteArrayOf(0x18, 0x00, 0x00, 0x00)) // 4 bytes

        val result = ModelValidator.validate(file)
        assertTrue(result.isFailure)
    }

    @Test
    fun validate_nonExistentFile_returnsFailure() {
        val file = File("nonexistent_model_file.tflite")
        val result = ModelValidator.validate(file)
        assertTrue(result.isFailure)
    }

    @Test
    fun validate_unsupportedExtension_returnsFailure() {
        val file = File.createTempFile("model", ".txt").apply { deleteOnExit() }
        file.writeBytes(ByteArray(64))

        val result = ModelValidator.validate(file)
        assertTrue(result.isFailure)
    }

    @Test
    fun validate_validTfliteHeader_returnsSuccess() {
        val file = File.createTempFile("valid", ".tflite").apply { deleteOnExit() }
        val content = ByteArray(64)
        content[0] = 0x18
        content[1] = 0x00
        content[2] = 0x00
        content[3] = 0x00
        file.writeBytes(content)

        val result = ModelValidator.validate(file)
        assertTrue(result.isSuccess)
    }

    @Test
    fun validate_validTaskZipHeader_returnsSuccess() {
        val file = File.createTempFile("gesture_recognizer", ".task").apply { deleteOnExit() }
        val content = ByteArray(64)
        content[0] = 0x50
        content[1] = 0x4B
        content[2] = 0x03
        content[3] = 0x04
        file.writeBytes(content)

        val result = ModelValidator.validate(file)
        assertTrue(result.isSuccess)
    }
}
