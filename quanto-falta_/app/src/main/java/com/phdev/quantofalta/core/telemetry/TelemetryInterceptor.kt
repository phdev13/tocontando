package com.phdev.quantofalta.core.telemetry

import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import com.phdev.quantofalta.core.analytics.AnalyticsEvent.ApiRequest
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import okhttp3.Interceptor
import okhttp3.Response

class TelemetryInterceptor(
    private val analyticsManager: AnalyticsManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        var response: Response? = null
        var exception: Exception? = null
        
        try {
            response = chain.proceed(request)
            return response
        } catch (e: Exception) {
            exception = e
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val urlPath = request.url.encodedPath
            
            // Não logar endpoints de telemetria
            if (!urlPath.contains("/telemetry") && !urlPath.contains("/performance/runs")) {
                analyticsManager.track(
                    ApiRequest(
                        endpoint = urlPath,
                        method = request.method,
                        status = response?.code ?: 0,
                        durationMs = duration,
                        success = response?.isSuccessful == true,
                        error = exception?.javaClass?.simpleName ?: ""
                    )
                )
            }
        }
    }
}
