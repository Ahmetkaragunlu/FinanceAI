package com.ahmetkaragunlu.financeai.feature.location.di

import com.ahmetkaragunlu.financeai.feature.location.data.AndroidAddressResolver
import com.ahmetkaragunlu.financeai.feature.location.data.AndroidLocationGateway
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds abstract fun addressResolver(value: AndroidAddressResolver): AddressResolver
    @Binds abstract fun locationGateway(value: AndroidLocationGateway): LocationGateway
}
