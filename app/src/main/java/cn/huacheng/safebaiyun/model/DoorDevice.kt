package cn.huacheng.safebaiyun.model

import kotlinx.serialization.Serializable

/**
 * 门禁设备数据模型
 *
 * @param id 唯一标识
 * @param name 门禁名称
 * @param macAddress MAC地址
 * @param key 加密密钥
 * @param isDefault 是否为默认门禁
 * @param widgetOrder 小组件显示顺序（0-2，-1表示不显示在小组件）
 */
@Serializable
data class DoorDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val key: String,
    val isDefault: Boolean = false,
    val widgetOrder: Int = -1
) {
    companion object {
        const val WIDGET_NOT_SHOW = -1
        const val WIDGET_POS_1 = 0
        const val WIDGET_POS_2 = 1
        const val WIDGET_POS_3 = 2
    }
}
