package com.buglist.di

import com.buglist.data.repository.DebtRepositoryImpl
import com.buglist.data.repository.DividerRepositoryImpl
import com.buglist.data.repository.PaymentRepositoryImpl
import com.buglist.data.repository.PersonRepositoryImpl
import com.buglist.data.repository.TagRepositoryImpl
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.DividerRepository
import com.buglist.domain.repository.PaymentRepository
import com.buglist.domain.repository.PersonRepository
import com.buglist.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to their implementations.
 *
 * Uses @Binds rather than @Provides to avoid the extra wrapper object that
 * @Provides creates for interface bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPersonRepository(impl: PersonRepositoryImpl): PersonRepository

    @Binds
    @Singleton
    abstract fun bindDebtRepository(impl: DebtRepositoryImpl): DebtRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindDividerRepository(impl: DividerRepositoryImpl): DividerRepository
}
