package com.whatsappclone.feature.group.di

import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.group.domain.CreateGroupUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object GroupModule {

    @Provides
    fun provideCreateGroupUseCase(
        chatRepository: ChatRepository
    ): CreateGroupUseCase = CreateGroupUseCase(chatRepository)
}
