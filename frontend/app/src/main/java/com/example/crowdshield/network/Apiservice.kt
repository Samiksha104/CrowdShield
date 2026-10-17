package com.example.crowdshield.network

import com.example.crowdshield.network.dto.AlertDto
import com.example.crowdshield.network.dto.CameraDto
import com.example.crowdshield.network.dto.RiskPredictionDto
import com.example.crowdshield.network.dto.ZoneDto
import retrofit2.http.GET

/**
 * Scoped to the five endpoints this integration pass covers. Evacuation
 * routes intentionally NOT included yet — /api/evacuation/routes and
 * /api/evacuation-route both still return [] with no confirmed shape.
 */
interface ApiService {

    @GET("api/zones")
    suspend fun getZones(): List<ZoneDto>

    @GET("api/cameras")
    suspend fun getCameras(): List<CameraDto>

    @GET("api/alerts")
    suspend fun getAlerts(): List<AlertDto>

    @GET("api/risk-prediction")
    suspend fun getRiskPrediction(): RiskPredictionDto
}