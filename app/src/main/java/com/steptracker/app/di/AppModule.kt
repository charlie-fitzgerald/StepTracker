package com.steptracker.app.di

import android.content.Context
import androidx.room.Room
import com.steptracker.app.data.AppDatabase
import com.steptracker.app.data.api.WeatherApi
import com.steptracker.app.data.preferences.UserPreferences
import com.steptracker.app.auth.OAuthManager
import com.steptracker.app.security.SecurityManager
import com.steptracker.app.security.SecurePreferences
import com.steptracker.app.security.NetworkSecurityConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideStepDataDao(database: AppDatabase) = database.stepDataDao()
    
    @Provides
    @Singleton
    fun provideWalkSessionDao(database: AppDatabase) = database.walkSessionDao()
    
    @Provides
    @Singleton
    fun provideSavedRouteDao(database: AppDatabase) = database.savedRouteDao()
    
    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context
    ): SecurityManager {
        return SecurityManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSecurePreferences(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): SecurePreferences {
        return SecurePreferences(context, securityManager)
    }
    
    @Provides
    @Singleton
    fun provideOAuthManager(
        @ApplicationContext context: Context
    ): OAuthManager {
        return OAuthManager(context)
    }
    
    @Provides
    @Singleton
    fun provideNetworkSecurityConfig(): NetworkSecurityConfig {
        return NetworkSecurityConfig()
    }
    
    @Provides
    @Singleton
    fun provideSecureOkHttpClient(
        networkSecurityConfig: NetworkSecurityConfig
    ): OkHttpClient {
        return networkSecurityConfig.createSecureOkHttpClient()
    }
    
    @Provides
    @Singleton
    fun provideWeatherApi(secureOkHttpClient: OkHttpClient): WeatherApi {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(secureOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
} 