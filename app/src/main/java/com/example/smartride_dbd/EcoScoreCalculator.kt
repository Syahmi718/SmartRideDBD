package com.example.smartride_dbd

import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for calculating Eco Scores based on driving behavior metrics
 * The Eco Score represents how environmentally friendly the driving behavior is
 */
class EcoScoreCalculator {

    companion object {
        // Default thresholds and constants (can be adjusted based on real-world testing)
        private const val SMOOTH_DRIVING_THRESHOLD = 1.5f  // m/s³
        private const val ACCELERATION_SPIKE_THRESHOLD = 3.0f  // m/s²
        private const val MAX_SMOOTHNESS_SCORE = 30f
        private const val MAX_SPIKE_SCORE = 30f
        private const val MAX_AGGRESSION_SCORE = 30f
        private const val ZERO_AGGRESSION_BONUS = 10f
        
        /**
         * Calculate the overall Eco Score based on multiple factors
         * @return a score from 0-100 where 100 is the most eco-friendly
         */
        fun calculateEcoScore(
            accelerationData: List<Triple<Float, Float, Float>>,
            deltaTimeMillis: List<Long>,
            aggressivePredictions: Int,
            totalPredictions: Int
        ): Int {
            // Convert acceleration data to magnitudes
            val accMagnitudes = calculateAccelerationMagnitudes(accelerationData)
            
            // Calculate jerk values (rate of change of acceleration)
            val jerkValues = calculateJerkValues(accMagnitudes, deltaTimeMillis)
            
            // Calculate the smoothness score (0-30)
            val smoothnessScore = calculateSmoothnessScore(jerkValues)
            
            // Calculate acceleration spike score (0-30)
            val spikeScore = calculateSpikeScore(accMagnitudes, deltaTimeMillis)
            
            // Calculate aggression score (0-30)
            val aggressionScore = calculateAggressionScore(aggressivePredictions, totalPredictions)
            
            // Calculate bonus score
            val bonusScore = if (aggressivePredictions == 0 && totalPredictions > 0) ZERO_AGGRESSION_BONUS else 0f
            
            // Calculate final score
            val finalScore = smoothnessScore + spikeScore + aggressionScore + bonusScore
            
            // Normalize to 0-100 scale (maximum possible is 30+30+30+10=100)
            return finalScore.toInt().coerceIn(0, 100)
        }
        
        /**
         * Calculate a simplified Eco Score when accelerometer data is not available
         */
        fun calculateSimplifiedEcoScore(
            aggressivePredictions: Int,
            totalPredictions: Int
        ): Int {
            // When only ML predictions are available, allocate more weight to aggression score
            val aggressionScore = calculateAggressionScore(aggressivePredictions, totalPredictions) * 3f
            
            // Calculate bonus score
            val bonusScore = if (aggressivePredictions == 0 && totalPredictions > 0) ZERO_AGGRESSION_BONUS else 0f
            
            // Calculate final score (maximum possible is 90+10=100)
            val finalScore = aggressionScore + bonusScore
            
            // Normalize to 0-100 scale
            return finalScore.toInt().coerceIn(0, 100)
        }
        
        /**
         * Calculate acceleration magnitudes from 3D acceleration data
         */
        private fun calculateAccelerationMagnitudes(
            accelerationData: List<Triple<Float, Float, Float>>
        ): List<Float> {
            return accelerationData.map { (x, y, z) ->
                sqrt(x*x + y*y + z*z)
            }
        }
        
        /**
         * Calculate jerk values from acceleration magnitudes
         */
        private fun calculateJerkValues(
            accMagnitudes: List<Float>,
            deltaTimeMillis: List<Long>
        ): List<Float> {
            if (accMagnitudes.size <= 1 || deltaTimeMillis.size < accMagnitudes.size - 1) {
                return emptyList()
            }
            
            val jerkValues = mutableListOf<Float>()
            for (i in 1 until accMagnitudes.size) {
                val deltaT = deltaTimeMillis[i-1] / 1000f  // Convert milliseconds to seconds
                if (deltaT > 0) {
                    val jerk = (accMagnitudes[i] - accMagnitudes[i-1]) / deltaT
                    jerkValues.add(jerk)
                }
            }
            
            return jerkValues
        }
        
        /**
         * Calculate smoothness score based on jerk values
         * @return score from 0-30
         */
        private fun calculateSmoothnessScore(jerkValues: List<Float>): Float {
            if (jerkValues.isEmpty()) {
                return 0f
            }
            
            // Calculate average absolute jerk
            val avgJerk = jerkValues.map { kotlin.math.abs(it) }.average().toFloat()
            
            // Map average jerk to smoothness score (lower jerk = higher score)
            // Scale inversely: if avgJerk = 0, score = 30; if avgJerk >= 2*THRESHOLD, score = 0
            val mappedScore = (1 - (avgJerk / (2 * SMOOTH_DRIVING_THRESHOLD)).coerceIn(0f, 1f)) * MAX_SMOOTHNESS_SCORE
            
            return mappedScore
        }
        
        /**
         * Calculate acceleration spike score
         * @return score from 0-30
         */
        private fun calculateSpikeScore(
            accMagnitudes: List<Float>,
            deltaTimeMillis: List<Long>
        ): Float {
            if (accMagnitudes.isEmpty() || deltaTimeMillis.isEmpty()) {
                return 0f
            }
            
            // Count acceleration spikes
            val spikeCount = accMagnitudes.count { it > ACCELERATION_SPIKE_THRESHOLD }
            
            // Calculate total time in seconds
            val totalTimeSeconds = deltaTimeMillis.sum() / 1000f
            
            // Calculate spike rate (spikes per minute)
            val spikeRate = if (totalTimeSeconds > 0) {
                spikeCount / (totalTimeSeconds / 60f)
            } else {
                0f
            }
            
            // Map spike rate to score (lower rate = higher score)
            // If spikeRate = 0, score = 30; if spikeRate >= 10, score = 0
            val mappedScore = (1 - (spikeRate / 10f).coerceIn(0f, 1f)) * MAX_SPIKE_SCORE
            
            return mappedScore
        }
        
        /**
         * Calculate aggression score based on ML predictions
         * @return score from 0-30
         */
        private fun calculateAggressionScore(
            aggressivePredictions: Int,
            totalPredictions: Int
        ): Float {
            if (totalPredictions == 0) {
                return 0f
            }
            
            // Calculate aggression percentage
            val aggressionPercentage = (aggressivePredictions.toFloat() / totalPredictions) * 100
            
            // Map aggression percentage to score (lower percentage = higher score)
            // If aggressionPercentage = 0, score = 30; if aggressionPercentage >= 100, score = 0
            val mappedScore = (1 - (aggressionPercentage / 100f).coerceIn(0f, 1f)) * MAX_AGGRESSION_SCORE
            
            return mappedScore
        }
    }
} 