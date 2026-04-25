package cn.huacheng.safebaiyun.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import cn.huacheng.safebaiyun.R
import cn.huacheng.safebaiyun.UnlockActivity
import cn.huacheng.safebaiyun.model.DoorDevice
import cn.huacheng.safebaiyun.unlock.DataRepo

/**
 * 3x3 网格小组件 - 支持放置3个门禁
 */
class GridReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = GridWidget
}

object GridWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val devices = DataRepo.getWidgetDevices()
        provideContent {
            GlanceTheme {
                WidgetContent(devices)
            }
        }
    }

    @Composable
    private fun WidgetContent(devices: List<DoorDevice>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .padding(12.dp)
        ) {
            // 标题
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "白云通",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "点击开门",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 3个门禁按钮（垂直排列）
            if (devices.isEmpty()) {
                // 未配置门禁
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity<UnlockActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击配置门禁",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                // 显示已配置的门禁
                devices.forEachIndexed { index, device ->
                    DoorButton(
                        device = device,
                        isLast = index == devices.size - 1
                    )
                    if (index < devices.size - 1) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }

                // 如果不足3个，填充空白
                repeat(3 - devices.size) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    EmptySlot()
                }
            }
        }
    }

    @Composable
    private fun DoorButton(device: DoorDevice, isLast: Boolean) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity<UnlockActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标区域
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    style = TextStyle(fontSize = 20.sp)
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // 文字区域
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = device.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onPrimaryContainer
                    ),
                    maxLines = 1
                )
                if (device.isDefault) {
                    Text(
                        text = "默认",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.primary
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptySlot() {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    fontSize = 24.sp,
                    color = GlanceTheme.colors.outline
                )
            )
        }
    }
}
