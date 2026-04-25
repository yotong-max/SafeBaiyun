package cn.huacheng.safebaiyun.unlock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cn.huacheng.safebaiyun.model.DoorDevice
import cn.huacheng.safebaiyun.util.ContextHolder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * 数据仓库 - 管理多个门禁设备
 */
object DataRepo {

    private val preferences: SharedPreferences by lazy {
        ContextHolder.get().getSharedPreferences("door_devices", Context.MODE_PRIVATE)
    }

    private const val KEY_DEVICES = "devices"
    private const val KEY_LEGACY_MAC = "mac"
    private const val KEY_LEGACY_KEY = "key"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 获取所有门禁设备
     */
    fun getAllDevices(): List<DoorDevice> {
        val devicesJson = preferences.getString(KEY_DEVICES, null)
        return if (devicesJson != null) {
            try {
                json.decodeFromString<List<DoorDevice>>(devicesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // 迁移旧数据
            migrateLegacyData()
        }
    }

    /**
     * 获取默认门禁
     */
    fun getDefaultDevice(): DoorDevice? {
        val devices = getAllDevices()
        return devices.find { it.isDefault } ?: devices.firstOrNull()
    }

    /**
     * 根据ID获取门禁
     */
    fun getDeviceById(id: String): DoorDevice? {
        return getAllDevices().find { it.id == id }
    }

    /**
     * 获取小组件显示的门禁（按顺序）
     */
    fun getWidgetDevices(): List<DoorDevice> {
        return getAllDevices()
            .filter { it.widgetOrder >= 0 }
            .sortedBy { it.widgetOrder }
            .take(3)
    }

    /**
     * 添加新门禁
     */
    fun addDevice(name: String, macAddress: String, key: String): DoorDevice {
        val devices = getAllDevices().toMutableList()
        val isFirst = devices.isEmpty()

        val newDevice = DoorDevice(
            id = UUID.randomUUID().toString(),
            name = name,
            macAddress = macAddress,
            key = key,
            isDefault = isFirst // 第一个自动设为默认
        )

        devices.add(newDevice)
        saveDevices(devices)
        return newDevice
    }

    /**
     * 更新门禁
     */
    fun updateDevice(updatedDevice: DoorDevice) {
        val devices = getAllDevices().toMutableList()
        val index = devices.indexOfFirst { it.id == updatedDevice.id }
        if (index != -1) {
            // 如果设为默认，取消其他设备的默认状态
            if (updatedDevice.isDefault) {
                devices.forEachIndexed { i, device ->
                    if (i != index && device.isDefault) {
                        devices[i] = device.copy(isDefault = false)
                    }
                }
            }
            devices[index] = updatedDevice
            saveDevices(devices)
        }
    }

    /**
     * 删除门禁
     */
    fun deleteDevice(deviceId: String) {
        val devices = getAllDevices().toMutableList()
        val removedDevice = devices.find { it.id == deviceId }
        devices.removeAll { it.id == deviceId }

        // 如果删除的是默认设备，将第一个设为默认
        if (removedDevice?.isDefault == true && devices.isNotEmpty()) {
            devices[0] = devices[0].copy(isDefault = true)
        }

        saveDevices(devices)
    }

    /**
     * 设置默认门禁
     */
    fun setDefaultDevice(deviceId: String) {
        val devices = getAllDevices().map {
            it.copy(isDefault = it.id == deviceId)
        }
        saveDevices(devices)
    }

    /**
     * 设置小组件显示的门禁
     */
    fun setWidgetDevices(deviceIds: List<String>) {
        val devices = getAllDevices().map { device ->
            val order = deviceIds.indexOf(device.id)
            device.copy(widgetOrder = order)
        }
        saveDevices(devices)
    }

    /**
     * 保存设备列表
     */
    private fun saveDevices(devices: List<DoorDevice>) {
        preferences.edit {
            putString(KEY_DEVICES, json.encodeToString(devices))
        }
    }

    /**
     * 迁移旧版本数据
     */
    private fun migrateLegacyData(): List<DoorDevice> {
        val oldPrefs = ContextHolder.get().getSharedPreferences("data", Context.MODE_PRIVATE)
        val mac = oldPrefs.getString(KEY_LEGACY_MAC, "") ?: ""
        val key = oldPrefs.getString(KEY_LEGACY_KEY, "") ?: ""

        return if (mac.isNotEmpty() && key.isNotEmpty()) {
            val device = DoorDevice(
                id = UUID.randomUUID().toString(),
                name = "门禁1",
                macAddress = mac,
                key = key,
                isDefault = true
            )
            saveDevices(listOf(device))
            listOf(device)
        } else {
            emptyList()
        }
    }
}
