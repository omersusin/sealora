package com.omersusin.sealora.di

import android.content.Context
import androidx.room.Room
import com.omersusin.sealora.data.local.*
import com.omersusin.sealora.data.remote.ai.AiService
import com.omersusin.sealora.data.remote.scraper.WeatherScraper
import com.omersusin.sealora.data.repository.AiRepositoryImpl
import com.omersusin.sealora.data.repository.WeatherRepositoryImpl
import com.omersusin.sealora.domain.repository.AiRepository
import com.omersusin.sealora.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.BODY
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        defaultRequest {
            headers.append("Accept", "application/json")
            headers.append("User-Agent", "Sealora/1.0.0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SealoraDatabase {
        return Room.databaseBuilder(
            context,
            SealoraDatabase::class.java,
            SealoraDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProviderDao(database: SealoraDatabase): ProviderDao {
        return database.providerDao()
    }

    @Provides
    @Singleton
    fun provideAiConfigDao(database: SealoraDatabase): AiConfigDao {
        return database.aiConfigDao()
    }

    @Provides
    @Singleton
    fun provideChatSessionDao(database: SealoraDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }

    @Provides
    @Singleton
    fun provideCachedWeatherDao(database: SealoraDatabase): CachedWeatherDao {
        return database.cachedWeatherDao()
    }

    @Provides
    @Singleton
    fun provideWeatherScraper(client: HttpClient): WeatherScraper {
        return WeatherScraper(client)
    }

    @Provides
    @Singleton
    fun provideAiService(client: HttpClient): AiService {
        return AiService(client)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        providerDao: ProviderDao,
        scraper: WeatherScraper
    ): WeatherRepository {
        return WeatherRepositoryImpl(providerDao, scraper)
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        aiConfigDao: AiConfigDao,
        chatSessionDao: ChatSessionDao,
        aiService: AiService
    ): AiRepository {
        return AiRepositoryImpl(aiConfigDao, chatSessionDao, aiService)
    }
}
