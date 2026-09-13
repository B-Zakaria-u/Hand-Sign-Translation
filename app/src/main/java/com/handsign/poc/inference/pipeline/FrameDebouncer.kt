package com.handsign.poc.inference.pipeline

/**
 * Debounces gesture predictions: a letter is only "committed" (emitted to the
 * caller) once it appears in [requiredFrames] consecutive frames. This prevents
 * flickering letters from noise or hand motion.
 */
class FrameDebouncer(private val requiredFrames: Int = 1) {

    private var currentLetter = ""
    private var frameCount = 0

    /**
     * Submit the latest predicted letter.
     * @return the committed letter string, or null if still accumulating frames.
     */
    fun submit(letter: String): String? {
        return if (letter == currentLetter) {
            frameCount++
            if (frameCount == requiredFrames) letter else null
        } else {
            currentLetter = letter
            frameCount = 1
            null
        }
    }

    /** Reset the debouncer (e.g. when user presses Backspace or clears text). */
    fun reset() {
        currentLetter = ""
        frameCount = 0
    }

    /** Current stable candidate (may not yet be committed). */
    val candidate: String get() = currentLetter

    /** How many frames we have seen [candidate] in a row. */
    val currentCount: Int get() = frameCount
}
