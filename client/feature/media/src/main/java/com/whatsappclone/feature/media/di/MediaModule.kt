package com.whatsappclone.feature.media.di

import com.whatsappclone.feature.media.data.MediaRepository
import com.whatsappclone.feature.media.data.MediaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
