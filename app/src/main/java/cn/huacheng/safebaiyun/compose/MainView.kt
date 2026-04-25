package cn.huacheng.safebaiyun.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cn.huacheng.safebaiyun.R
import cn.huacheng.safebaiyun.model.DoorDevice
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.unlock.UnlockRepo
import cn.huacheng.safebaiyun.util.showToast

/**
 * 主界面 - 门禁列表
 */
@Composable
fun MainView(navController: NavHostController) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<DoorDevice?>(null) }
    var devices by remember { mutableStateOf(DataRepo.getAllDevices()) }

    // 权限检查
    LaunchedEffect(Unit) {
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // 刷新数据
    val refreshData = {
        devices = DataRepo.getAllDevices()
    }

    Scaffold(
        topBar = {
            MainTopBar(
                onEditClick = { /* 不再使用 */ },
                onHelperClick = { navController.navigate("helper") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加门禁")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                PermissionView { granted ->
                    hasPermission = granted
                }
            } else if (devices.isEmpty()) {
                EmptyView { showAddDialog = true }
            } else {
                DeviceList(
                    devices = devices,
                    onUnlock = { device ->
                        showToast("正在开门: ${device.name}")
                        UnlockRepo.unlock(device.macAddress, device.key)
                    },
                    onEdit = { device ->
                        showEditDialog = device
                    },
                    onDelete = { device ->
                        DataRepo.deleteDevice(device.id)
                        refreshData()
                        showToast("已删除: ${device.name}")
                    },
                    onSetDefault = { device ->
                        DataRepo.setDefaultDevice(device.id)
                        refreshData()
                        showToast("已设为默认: ${device.name}")
                    }
                )
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        DeviceEditDialog(
            device = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, mac, key, isDefault ->
                DataRepo.addDevice(name, mac, key)
                refreshData()
                showAddDialog = false
                showToast("添加成功")
            }
        )
    }

    // 编辑对话框
    showEditDialog?.let { device ->
        DeviceEditDialog(
            device = device,
            onDismiss = { showEditDialog = null },
            onSave = { name, mac, key, isDefault ->
                val updated = device.copy(
                    name = name,
                    macAddress = mac,
                    key = key,
                    isDefault = isDefault
                )
                DataRepo.updateDevice(updated)
                refreshData()
                showEditDialog = null
                showToast("保存成功")
            },
            onDelete = {
                DataRepo.deleteDevice(device.id)
                refreshData()
                showEditDialog = null
                showToast("已删除")
            }
        )
    }
}

@Composable
private fun DeviceList(
    devices: List<DoorDevice>,
    onUnlock: (DoorDevice) -> Unit,
    onEdit: (DoorDevice) -> Unit,
    onDelete: (DoorDevice) -> Unit,
    onSetDefault: (DoorDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                onUnlock = { onUnlock(device) },
                onEdit = { onEdit(device) },
                onDelete = { onDelete(device) },
                onSetDefault = { onSetDefault(device) }
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: DoorDevice,
    onUnlock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUnlock() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // 信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (device.isDefault) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "默认",
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "MAC: ${maskMac(device.macAddress)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            Row {
                if (!device.isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "设为默认",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑"
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除门禁"${device.name}"吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EmptyView(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无门禁",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "点击右下角按钮添加门禁",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onAdd,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("添加门禁")
        }
    }
}

@Composable
private fun PermissionView(onPermissionGranted: (Boolean) -> Unit) {
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionGranted(isGranted)
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要蓝牙权限",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "本应用需要蓝牙权限才能连接门禁设备",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        ) {
            Text(text = stringResource(id = R.string.request_permission))
        }
    }
}

/**
 * 脱敏显示MAC地址
 */
private fun maskMac(mac: String): String {
    return if (mac.length > 8) {
        mac.take(8) + ":**:**:**"
    } else {
        mac
    }
}
