package com.jsf.app.pravin.data.di

import android.content.Context
import com.jsf.app.pravin.data.db.FitnessDatabase
import com.jsf.app.pravin.data.repository.CommunityRepository
import com.jsf.app.pravin.data.repository.FitnessRepository

object DatabaseModule {

    fun provideDatabase(context: Context): FitnessDatabase =
        FitnessDatabase.getInstance(context)

    fun provideFitnessRepository(context: Context): FitnessRepository =
        FitnessRepository(provideDatabase(context))

    fun provideCommunityRepository(context: Context): CommunityRepository =
        CommunityRepository(provideDatabase(context))
}
