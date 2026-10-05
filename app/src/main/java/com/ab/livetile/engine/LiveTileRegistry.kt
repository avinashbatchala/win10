package com.ab.livetile.engine

import com.ab.livetile.api.LiveTileProvider
import com.ab.livetile.providers.BatteryLiveTileProvider
import com.ab.livetile.providers.ClockLiveTileProvider
import com.ab.livetile.providers.DateLiveTileProvider
import com.ab.livetile.providers.MetroClockLiveTileProvider
import com.ab.livetile.providers.WeatherLiveTileProvider
import java.util.concurrent.CopyOnWriteArrayList

class LiveTileRegistry {

    private val providers = CopyOnWriteArrayList<LiveTileProvider>()

    init {
        // Register built-in system & demonstration providers.
        // MetroClock's contract provider is registered before the generic Clock provider so
        // it wins for the MetroClock package (which contains "clock").
        registerProvider(MetroClockLiveTileProvider())
        registerProvider(ClockLiveTileProvider())
        registerProvider(DateLiveTileProvider())
        registerProvider(BatteryLiveTileProvider())
        registerProvider(WeatherLiveTileProvider())
    }

    fun registerProvider(provider: LiveTileProvider) {
        // Prevent duplicate registration by providerId
        providers.removeAll { it.providerId == provider.providerId }
        providers.add(provider)
    }

    fun unregisterProvider(providerId: String) {
        providers.removeAll { it.providerId == providerId }
    }

    fun findProvider(packageName: String, activityName: String? = null): LiveTileProvider? {
        return providers.firstOrNull { it.matchesComponent(packageName, activityName) }
    }

    fun getAllProviders(): List<LiveTileProvider> = providers.toList()
}
