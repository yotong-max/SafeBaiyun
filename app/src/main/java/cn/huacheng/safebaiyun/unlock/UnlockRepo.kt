package cn.huacheng.safebaiyun.unlock

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import cn.huacheng.safebaiyun.util.ContextHolder
import cn.huacheng.safebaiyun.util.LockBiz
import cn.huacheng.safebaiyun.util.showToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 开门仓库 - 支持传入指定门禁配置
 */
@SuppressLint("MissingPermission")
object UnlockRepo {

    private const val MAGIC_SERVICE = "14839ac4-7d7e-415c-9a42-167340cf2339"

    private var autoDisconnectJob: Job? = null

    private val activeGatts = mutableMapOf<String, BluetoothGatt>()

    /**
     * 使用默认门禁开门
     */
    fun unlockDefault() {
        val device = DataRepo.getDefaultDevice()
        if (device == null) {
            showToast("请先添加门禁")
            return
        }
        unlock(device.macAddress, device.key)
    }

    /**
     * 使用指定配置开门
     *
     * @param macAddress MAC地址
     * @param key 加密密钥
     */
    fun unlock(macAddress: String, key: String) {
        val bluetoothAdapter =
            (ContextHolder.get()
                .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            showToast("Mac地址格式错误")
            return
        }

        // 关闭之前的连接
        activeGatts[macAddress]?.close()

        connect(bluetoothAdapter, macAddress, key)

        autoDisconnectJob = GlobalScope.launch {
            delay(10000)
            if (isActive) {
                log("10s超时，自动断开链接")
                activeGatts[macAddress]?.disconnect()
                activeGatts[macAddress]?.close()
                activeGatts.remove(macAddress)
            }
        }
    }

    private fun connect(bluetoothAdapter: BluetoothAdapter, macAddress: String, key: String) {
        val remoteDevice = bluetoothAdapter.getRemoteDevice(macAddress)
        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteDevice.connectGatt(
                ContextHolder.get(),
                false,
                createCallback(macAddress, key),
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            remoteDevice.connectGatt(
                ContextHolder.get(),
                false,
                createCallback(macAddress, key)
            )
        }

        activeGatts[macAddress] = gatt
        log("尝试连接蓝牙 $macAddress")
    }

    private fun createCallback(macAddress: String, key: String): BluetoothGattCallback {
        return object : BluetoothGattCallback() {

            private lateinit var readableCharacteristic: BluetoothGattCharacteristic
            private lateinit var writeableCharacteristic: BluetoothGattCharacteristic

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                log("连接状态改变 status$status,newState$newState")
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    autoDisconnectJob?.cancel()
                    log("开始搜索服务")
                    gatt?.discoverServices()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    activeGatts.remove(macAddress)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                log("搜索服务成功 $status")
                log("搜索到以下服务：${gatt?.services?.map { it.uuid }?.joinToString(",")}")
                handleService(gatt, gatt?.services?.find { it.uuid.toString() == MAGIC_SERVICE })
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                //android13以上走这里
                log("特征码读取回调 $status,${value.size}")
                handleCharacteristicWrite(gatt, value, macAddress, key)
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                //android12及以下走这里
                val value = characteristic?.value ?: return
                log("特征码读取回调 $status,${value.size}")
                handleCharacteristicWrite(gatt, value, macAddress, key)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                log("特征码写入回调")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    showToast("开门成功")
                } else {
                    showToast("密钥写入失败")
                }
                gatt?.close()
                activeGatts.remove(macAddress)
            }

            /**
             * 找到对应的characteristic
             */
            private fun handleService(gatt: BluetoothGatt?, service: BluetoothGattService?) {
                if (service == null) {
                    return
                }

                log("开始处理服务，共${service.characteristics.size}个特征")
                val propCharacteristics = mutableListOf<BluetoothGattCharacteristic>()

                service.characteristics?.forEach {
                    log("特征${it.uuid},prop:${it.properties}")
                    val properties = it.properties
                    if ((properties and 2) != 0) {
                        readableCharacteristic = it
                    }
                    if ((properties and 8) != 0) {
                        writeableCharacteristic = it
                    }
                    if ((properties and 16) != 0) {
                        propCharacteristics.add(it)
                    }
                    if ((properties and 32) != 0) {
                        propCharacteristics.add(it)
                    }
                }

                handleCharacteristics(gatt, propCharacteristics)
            }

            private fun handleCharacteristics(
                gatt: BluetoothGatt?,
                propCharacteristics: MutableList<BluetoothGattCharacteristic>
            ) {
                log("开始处理特征,写入对应数据")
                propCharacteristics.forEach { characteristic ->
                    gatt?.setCharacteristicNotification(characteristic, true)
                    characteristic.descriptors.forEach {
                        if ((characteristic.properties and 16) != 0) {
                            characteristic.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else if ((characteristic.properties and 32) != 0) {
                            characteristic.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                        }

                        gatt?.writeDescriptor(it)
                    }
                }

                val result = gatt?.readCharacteristic(readableCharacteristic)
                log("特征写入结果 $result")
            }

            private fun handleCharacteristicWrite(
                gatt: BluetoothGatt?,
                value: ByteArray,
                macAddress: String,
                key: String
            ) {
                log("开始写入密钥")
                val encryptedKey = LockBiz.encryptData(value, LockBiz.hexToByteArray(macAddress), key)
                log(encryptedKey.joinToString())
                writeableCharacteristic.setValue(encryptedKey)
                writeableCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val result = gatt?.writeCharacteristic(writeableCharacteristic)
                log("密钥写入结果 $result")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun log(msg: String) {
        println(msg)
    }
}
