package cn.huacheng.safebaiyun.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.huacheng.safebaiyun.model.DoorDevice

/**
 * 门禁编辑对话框（底部弹窗）
 *
 * @param device 要编辑的门禁，null表示添加新门禁
 * @param onDismiss 取消回调
 * @param onSave 保存回调 (name, mac, key, isDefault)
 * @param onDelete 删除回调（仅编辑时可用）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditDialog(
    device: DoorDevice?,
    onDismiss: () -> Unit,
    onSave: (name: String, mac: String, key: String, isDefault: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEdit = device != null

    var name by remember { mutableStateOf(device?.name ?: "") }
    var mac by remember { mutableStateOf(device?.macAddress ?: "") }
    var key by remember { mutableStateOf(device?.key ?: "") }
    var isDefault by remember { mutableStateOf(device?.isDefault ?: false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var macError by remember { mutableStateOf<String?>(null) }
    var keyError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 标题
            Text(
                text = if (isEdit) "编辑门禁" else "添加门禁",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 名称输入
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("门禁名称") },
                placeholder = { Text("如：家门、公司") },
                isError = nameError != null,
                supportingText = nameError?.let { Text(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // MAC地址输入
            OutlinedTextField(
                value = mac,
                onValueChange = {
                    // 自动格式化为大写并添加冒号
                    val formatted = formatMacInput(it)
                    mac = formatted
                    macError = null
                },
                label = { Text("MAC地址") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                isError = macError != null,
                supportingText = macError?.let { Text(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Key输入
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    keyError = null
                },
                label = { Text("加密密钥") },
                placeholder = { Text("从数据库中提取的PRODUCT_KEY") },
                isError = keyError != null,
                supportingText = keyError?.let { Text(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 设为默认（编辑时显示）
            if (isEdit && !device!!.isDefault) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("设为默认门禁")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 删除按钮（仅编辑时显示）
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 取消按钮
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 保存按钮
                Button(
                    onClick = {
                        // 验证输入
                        var valid = true

                        if (name.isBlank()) {
                            nameError = "请输入门禁名称"
                            valid = false
                        }

                        if (!isValidMac(mac)) {
                            macError = "MAC地址格式错误"
                            valid = false
                        }

                        if (key.isBlank()) {
                            keyError = "请输入加密密钥"
                            valid = false
                        }

                        if (valid) {
                            onSave(name.trim(), mac.trim().uppercase(), key.trim(), isDefault)
                        }
                    }
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 格式化MAC地址输入
 */
private fun formatMacInput(input: String): String {
    // 移除所有非十六进制字符
    val cleaned = input.uppercase().filter { it.isDigit() || it in 'A'..'F' }
    // 每两个字符添加冒号
    val formatted = StringBuilder()
    cleaned.take(12).forEachIndexed { index, char ->
        if (index > 0 && index % 2 == 0) {
            formatted.append(':')
        }
        formatted.append(char)
    }
    return formatted.toString()
}

/**
 * 验证MAC地址格式
 */
private fun isValidMac(mac: String): Boolean {
    val pattern = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
    return pattern.matches(mac)
}
