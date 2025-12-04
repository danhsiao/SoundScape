package com.cs407.soundscape.ml

import com.cs407.soundscape.data.model.SoundType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Simple rule-based sound classifier
 * This is a placeholder that will be replaced with TensorFlow Lite when we have training data.
 *
 * Features used:
 * 1. Average Level - Overall loudness
 * 2. Variance - How much the sound fluctuates
 * 3. Peak Count - Number of loud bursts
 * 4. Steadiness - How consistent the sound is over time
 */
class SoundClassifier {

    private val recentReadings = mutableListOf<Float>()
    private val maxHistorySize = 30 // 3 seconds at 100ms intervals

    /**
     * Add a new decibel reading to the classifier
     */
    fun addReading(decibel: Float) {
        recentReadings.add(decibel)
        if (recentReadings.size > maxHistorySize) {
            recentReadings.removeAt(0)
        }
    }

    /**
     * Classify sound using 4 simple features.
     * TODO: Replace with TensorFlow Lite model inference
     */
    fun classifySound(): SoundType {
        if (recentReadings.size < 10) {
            return SoundType.OTHER
        }

        // Extract 4 core features
        val avgLevel = recentReadings.average().toFloat()
        val variance = calculateVariance(recentReadings)
        val peakCount = countPeaks(recentReadings)
        val steadiness = calculateSteadiness(recentReadings)

        // Simple rule-based classification
        return when {
            // Traffic: loud + steady + low variance
            avgLevel > 70f && steadiness > 0.7f && variance < 10f ->
                SoundType.TRAFFIC

            // Construction: very loud + high variance + many peaks
            avgLevel > 85f && variance > 15f && peakCount > 5 ->
                SoundType.CONSTRUCTION

            // Music: medium-high level + moderate variance + some rhythm
            avgLevel in 60f..80f && variance in 8f..20f && peakCount in 3..8 ->
                SoundType.MUSIC

            // Voice: medium level + high variance + not too steady
            avgLevel in 50f..70f && variance > 12f && steadiness < 0.6f ->
                SoundType.VOICE

            // Nature: low-medium level + moderate variance + few peaks
            avgLevel in 30f..55f && variance in 5f..15f && peakCount < 6 ->
                SoundType.NATURE

            // Animal: medium level + high variance + sporadic peaks
            avgLevel in 45f..70f && peakCount in 2..5 && steadiness < 0.5f ->
                SoundType.ANIMAL

            else -> SoundType.OTHER
        }
    }

    /**
     * Get confidence based on how much data we have
     */
    fun getConfidence(): Float {
        if (recentReadings.size < maxHistorySize) {
            return (recentReadings.size.toFloat() / maxHistorySize) * 0.8f
        }
        return 0.8f
    }

    fun reset() {
        recentReadings.clear()
    }

    /**
     * Variance - measures how much sound fluctuates
     */
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    /**
     * Peak Count - number of loud bursts above threshold
     */
    private fun countPeaks(values: List<Float>): Int {
        if (values.size < 3) return 0

        val threshold = values.average().toFloat() + sqrt(calculateVariance(values))
        var peakCount = 0

        for (i in 1 until values.size - 1) {
            // A peak is higher than neighbors and above threshold
            if (values[i] > values[i - 1] &&
                values[i] > values[i + 1] &&
                values[i] > threshold) {
                peakCount++
            }
        }

        return peakCount
    }

    /**
     * Feature 4: Steadiness - how consistent the sound is (0 = variable, 1 = steady)
     */
    private fun calculateSteadiness(values: List<Float>): Float {
        if (values.size < 2) return 0.5f

        // Calculate how much consecutive values differ
        val differences = values.zipWithNext { a, b -> abs(b - a) }
        val avgDifference = differences.average().toFloat()

        // Normalize: lower difference = higher steadiness
        // Typical differences range from 0-20 dB
        return (1.0f - (avgDifference / 20f)).coerceIn(0f, 1f)
    }

    companion object {
        /**
         * Get a human-readable description for each sound type
         */
        fun getSoundTypeDescription(type: SoundType): String {
            return when (type) {
                SoundType.TRAFFIC -> "Vehicle traffic, engines, horns"
                SoundType.NATURE -> "Birds, wind, water, rustling"
                SoundType.CONSTRUCTION -> "Heavy machinery, drilling, hammering"
                SoundType.MUSIC -> "Musical instruments, songs, melodies"
                SoundType.VOICE -> "Human speech, conversation"
                SoundType.ANIMAL -> "Animal sounds, barking, meowing"
                SoundType.OTHER -> "Unclassified sound"
            }
        }

        /**
         * Get an emoji icon for each sound type
         */
        fun getSoundTypeIcon(type: SoundType): String {
            return when (type) {
                SoundType.TRAFFIC -> "🚗"
                SoundType.NATURE -> "🌿"
                SoundType.CONSTRUCTION -> "🏗️"
                SoundType.MUSIC -> "🎵"
                SoundType.VOICE -> "💬"
                SoundType.ANIMAL -> "🐾"
                SoundType.OTHER -> "🔊"
            }
        }
    }
}