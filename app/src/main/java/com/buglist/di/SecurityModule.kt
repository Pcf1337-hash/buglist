package com.buglist.di

import android.content.Context
import com.buglist.security.BiometricAuthManager
import com.buglist.security.KeystoreManager
import com.buglist.security.PassphraseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related singletons.
 *
 * [KeystoreManager] and [PassphraseManager] are singletons because:
 * - Keystore operations are expensive (hardware-backed).
 * - The same key alias and passphrase state must be shared across all callers.
 * - Multiple instances could race on key generation or passphrase creation.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(
        @ApplicationContext context: Context
    ): KeystoreManager = KeystoreManager(context)

    @Provides
    @Singleton
    fun providePassphraseManager(
        @ApplicationContext context: Context
    ): PassphraseManager = PassphraseManager(context)

    @Provides
    @Singleton
    fun provideBiometricAuthManager(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): BiometricAuthManager = BiometricAuthManager(context, keystoreManager)
}
