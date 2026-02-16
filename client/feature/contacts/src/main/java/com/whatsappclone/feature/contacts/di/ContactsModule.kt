package com.whatsappclone.feature.contacts.di

import android.content.ContentResolver
import android.content.Context
import com.whatsappclone.feature.contacts.data.ContactRepository
import com.whatsappclone.feature.contacts.data.ContactRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    companion object {

        @Provides
        @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver
    }
}
