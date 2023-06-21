package com.amazon.ivs.optimizations.di

import android.content.Context
import com.amazon.ivs.optimizations.cache.PreferenceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {
    @Provides
    @Singleton
    fun providePreferenceProvider(
        @ApplicationContext context: Context
    ) = PreferenceProvider(context)
}
