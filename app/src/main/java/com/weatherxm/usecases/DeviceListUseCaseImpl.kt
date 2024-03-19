package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.DeviceProfile.Helium
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.UIDevice

class DeviceListUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : DeviceListUseCase {
    override suspend fun getUserDevices(): Either<Failure, List<UIDevice>> {
        return deviceRepository.getUserDevices().map { response ->
            val devices = response.map {
                val device = it.toUIDevice()
                val shouldShowOTAPrompt = deviceOTARepository.shouldShowOTAPrompt(
                    device.id, device.assignedFirmware
                ) && device.isOwned()
                val alerts = mutableListOf<DeviceAlert>()
                if (device.isActive == false) {
                    alerts.add(DeviceAlert.OFFLINE)
                }

                // FIXME: Revert this before merging. For testing purposes. 
                if (true) {
                    alerts.add(DeviceAlert.NEEDS_UPDATE)
                }
                device.apply {
                    this.alerts = alerts
                }
            }

            with(getDevicesSortFilterOptions()) {
                /**
                 * 1. Apply Sort then...
                 * 2. Apply Filter then...
                 * 3. Apply Group By
                 * 4. Return the result
                 */
                applyGroupBy(applyFilter(applySort(devices)))
            }
        }
    }

    override fun getDevicesSortFilterOptions(): DevicesSortFilterOptions {
        val savedOptions = userPreferencesRepository.getDevicesSortFilterOptions()
        return if (savedOptions.isEmpty()) {
            // Default preference if user hasn't saved his own yet
            DevicesSortFilterOptions()
        } else {
            /**
             * 1 = sort order
             * 2 = filter type
             * 3 = group by
             * as can be seen in `getDevicesSortFilterOptions` in CacheService
             */
            DevicesSortFilterOptions(
                DevicesSortOrder.valueOf(savedOptions[0]),
                DevicesFilterType.valueOf(savedOptions[1]),
                DevicesGroupBy.valueOf(savedOptions[2])
            )
        }
    }

    override fun setDevicesSortFilterOptions(options: DevicesSortFilterOptions) {
        userPreferencesRepository.setDevicesSortFilterOptions(
            options.sortOrder.name,
            options.filterType.name,
            options.groupBy.name
        )
    }
}
