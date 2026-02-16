package com.whatsappclone.feature.chat.di

import com.whatsappclone.feature.chat.data.ChatRepository
import com.whatsappclone.feature.chat.data.ChatRepositoryImpl
import com.whatsappclone.feature.chat.data.MessageRepository
import com.whatsappclone.feature.chat.data.MessageRepositoryImpl
import com.whatsappclone.feature.chat.data.UserRepository
import com.whatsappclone.feature.chat.data.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
