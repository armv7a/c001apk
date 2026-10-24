package com.example.c001apk.compose.logic.smid

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DigitalUnionDebugEntryPoint {
    fun digitalUnion(): DigitalUnionRepository

    fun preferences(): com.example.c001apk.compose.logic.repository.UserPreferencesRepository
}
