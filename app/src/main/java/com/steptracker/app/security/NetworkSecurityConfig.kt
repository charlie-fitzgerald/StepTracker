package com.steptracker.app.security

import android.content.Context
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NetworkSecurityConfig {
    
    companion object {
        // Certificate pinning for OpenWeatherMap API
        private const val OPENWEATHERMAP_HOST = "api.openweathermap.org"
        private const val OPENWEATHERMAP_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Replace with actual pin
        
        // Certificate pinning for Google APIs
        private const val GOOGLE_HOST = "www.googleapis.com"
        private const val GOOGLE_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Replace with actual pin
    }
    
    fun createSecureOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .certificatePinner(
                CertificatePinner.Builder()
                    .add(OPENWEATHERMAP_HOST, OPENWEATHERMAP_PIN)
                    .add(GOOGLE_HOST, GOOGLE_PIN)
                    .build()
            )
            .addInterceptor { chain ->
                val request = chain.request()
                // Ensure all requests use HTTPS
                if (!request.url.isHttps) {
                    throw SecurityException("HTTPS required for all API calls")
                }
                chain.proceed(request)
            }
            .build()
    }
    
    fun createTrustAllOkHttpClient(): OkHttpClient {
        // WARNING: Only use for development/testing
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
} 