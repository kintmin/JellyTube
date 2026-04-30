package com.kintmin.platform.worker.di

import android.content.Context
import com.kintmin.domain.step.usecase.FetchLoadBalancingDelaySecondUseCase
import com.kintmin.domain.step.worker.*
import com.kintmin.platform.worker.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    fun bindRegisterDailyResetImmediatelyWorker(
        @ApplicationContext context: Context,
    ): RegisterDailyResetImmediatelyWorker {
        return RegisterDailyResetImmediatelyWorkerImpl(context)
    }

    @Provides
    fun bindRegisterLoadBalancedDailyResetWorker(
        @ApplicationContext context: Context,
        fetchLoadBalancingDelaySecondUseCase: FetchLoadBalancingDelaySecondUseCase,
    ): RegisterLoadBalancedDailyResetWorker {
        return RegisterLoadBalancedDailyResetWorkerImpl(context, fetchLoadBalancingDelaySecondUseCase)
    }
}