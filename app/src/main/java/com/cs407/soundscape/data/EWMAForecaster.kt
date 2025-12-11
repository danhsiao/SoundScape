package com.cs407.soundscape.data

import com.cs407.soundscape.data.ForecastTrend
import kotlin.math.abs

/**
 * EWMA-based noise level forecaster
 * Predicts short-term noise levels using Exponentially Weighted Moving Average
 */
class EWMAForecaster(
    private val alpha: Float = 0.3f, // Smoothing factor
    private val forecastHorizonMinutes: Int = 20 // Predict 20 minutes ahead (between 15-30)
) {
    
    /**
     * Calculate EWMA forecast for a location based on historical events
     * 
     * @param locationEvents List of SoundEvents for a location, sorted by timestamp (oldest first)
     * @param averageDecibel Optional average decibel to compare forecast against for trend determination
     * @return Triple of (forecastedDecibel, trend, confidence) or null if insufficient data
     */
    fun calculateForecast(locationEvents: List<SoundEvent>, averageDecibel: Float? = null): Triple<Float, ForecastTrend, Float>? {
        if (locationEvents.isEmpty()) return null
        
        // Filter to recent events (last 2 hours for short-term forecast)
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)
        val recentEvents = locationEvents
            .filter { it.timestamp >= twoHoursAgo }
            .sortedBy { it.timestamp }
        
        // Need at least 2 events for forecasting
        // If we have fewer than 2 recent events, try using all events (even older ones)
        val eventsForForecast = if (recentEvents.size < 2) {
            // Fallback: use all events if we have at least 2 total
            if (locationEvents.size >= 2) {
                locationEvents.sortedBy { it.timestamp }
            } else {
                // Still not enough data
                return null
            }
        } else {
            recentEvents
        }
        
        // Calculate EWMA values
        val ewmaValues = calculateEWMA(eventsForForecast.map { it.decibelLevel })
        
        // Current decibel (most recent)
        val currentDecibel = eventsForForecast.last().decibelLevel
        
        // Forecasted decibel (last EWMA value)
        val forecastedDecibel = ewmaValues.last()
        
        // Determine trend: compare forecast to average (if provided) or to current
        val trend = if (averageDecibel != null) {
            determineTrendFromComparison(forecastedDecibel, averageDecibel)
        } else {
            determineTrend(ewmaValues, currentDecibel)
        }
        
        // Calculate confidence based on:
        // 1. Amount of data (more data = higher confidence)
        // 2. Variance (lower variance = higher confidence)
        // 3. Recency (more recent data = higher confidence)
        val confidence = calculateConfidence(eventsForForecast, ewmaValues)
        
        return Triple(forecastedDecibel, trend, confidence)
    }
    
    /**
     * Calculate EWMA values for a series of decibel readings
     */
    private fun calculateEWMA(readings: List<Float>): List<Float> {
        if (readings.isEmpty()) return emptyList()
        
        val ewmaValues = mutableListOf<Float>()
        var ewma = readings[0].toFloat() // Initialize with first value
        
        for (i in readings.indices) {
            ewma = alpha * readings[i] + (1 - alpha) * ewma
            ewmaValues.add(ewma)
        }
        
        return ewmaValues
    }
    
    /**
     * Determine trend by comparing forecast to average decibel
     * This is the primary method - trend should indicate if forecast is higher/lower than average
     */
    private fun determineTrendFromComparison(forecastedDecibel: Float, averageDecibel: Float): ForecastTrend {
        val difference = forecastedDecibel - averageDecibel
        val threshold = 0.5f // Threshold for trend detection (0.5 dB change)
        
        return when {
            difference > threshold -> ForecastTrend.UP
            difference < -threshold -> ForecastTrend.DOWN
            else -> ForecastTrend.STABLE
        }
    }
    
    /**
     * Determine trend based on recent EWMA values (fallback method)
     */
    private fun determineTrend(ewmaValues: List<Float>, currentDecibel: Float): ForecastTrend {
        if (ewmaValues.size < 3) return ForecastTrend.STABLE
        
        // Compare last 3 EWMA values to detect trend
        val recentEWMA = ewmaValues.takeLast(3)
        val first = recentEWMA[0]
        val last = recentEWMA[2]
        val change = last - first
        
        // Threshold for trend detection (1 dB change)
        val threshold = 1.0f
        
        return when {
            change > threshold -> ForecastTrend.UP
            change < -threshold -> ForecastTrend.DOWN
            else -> ForecastTrend.STABLE
        }
    }
    
    /**
     * Calculate confidence score (0.0 to 1.0)
     */
    private fun calculateConfidence(
        events: List<SoundEvent>,
        ewmaValues: List<Float>
    ): Float {
        // Factor 1: Amount of data (0.0 to 0.4)
        val dataAmountScore = (events.size.coerceAtMost(20) / 20.0f) * 0.4f
        
        // Factor 2: Variance (lower variance = higher confidence) (0.0 to 0.3)
        val variance = calculateVariance(ewmaValues)
        val varianceScore = (1.0f - (variance / 50.0f).coerceIn(0f, 1f)) * 0.3f
        
        // Factor 3: Recency (more recent = higher confidence) (0.0 to 0.3)
        val now = System.currentTimeMillis()
        val mostRecentAge = now - events.last().timestamp
        val recencyScore = (1.0f - (mostRecentAge / (60 * 60 * 1000).toFloat()).coerceIn(0f, 1f)) * 0.3f
        
        return (dataAmountScore + varianceScore + recencyScore).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate variance of a list of values
     */
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val squaredDiffs = values.map { (it - mean) * (it - mean) }
        return squaredDiffs.average().toFloat()
    }
}

