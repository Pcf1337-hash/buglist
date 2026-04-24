package com.buglist.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {

    @Provides
    @Singleton
    @Named("diagnostics")
    fun provideDiagnosticsHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }
}
