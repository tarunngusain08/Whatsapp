package com.whatsappclone.feature.profile.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the :feature:profile module.
 *
 * The profile feature depends on:
 *  - [MediaRepository] (bound in :feature:media's [MediaModule])
 *  - [UserApi], [UserDao] (provided by core modules)
 *
 * No additional bindings are needed here at this time, but the module is
 * declared so future profile-specific providers/bindings have a home.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfileModule
