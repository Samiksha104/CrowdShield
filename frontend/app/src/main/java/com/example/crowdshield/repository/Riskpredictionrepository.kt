package com.example.crowdshield.repository

import com.example.crowdshield.network.RetrofitClient
import com.example.crowdshield.network.dto.RiskPredictionDto

/**
 * NOW LIVE — calls the confirmed /api/risk-prediction endpoint. Didn't
 * exist before; RiskPredictionActivity had its values hardcoded inline.
 */
object RiskPredictionRepository {
    suspend fun getPrediction(): RiskPredictionDto = RetrofitClient.apiService.getRiskPrediction()
}