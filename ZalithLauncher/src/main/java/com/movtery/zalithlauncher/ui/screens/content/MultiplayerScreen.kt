/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import java.net.NetworkInterface
import java.net.Inet4Address
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.URL_EASYTIER
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.AnimatedRow
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.NotificationCheck
import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.influencedByBackgroundColor
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.theme.cardTitleColor
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast

@Composable
fun MultiplayerScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current

    BaseScreen(
        screenKey = NormalNavKey.Multiplayer,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { isVisible ->
        Column(modifier = Modifier.fillMaxSize()) {
            BackgroundCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "REALISTIC LAN",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "JOGUE COM AMIGOS • WI-FI, HOTSPOT E CONVITES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "LAN LOCAL e rede virtual em uma única central.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (maxWidth < 600.dp) {
                    // Layout vertical para celulares em modo retrato.
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MainMenu(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.9f)
                                .padding(horizontal = 12.dp),
                            eventViewModel = eventViewModel,
                            onShareLogs = {
                                val logFile = PathManager.FILE_TERRACOTTA_LOG
                                if (logFile.exists()) {
                                    shareFile(context, logFile)
                                } else {
                                    eventViewModel.sendToast(androidText(R.string.terracotta_export_log_share_null))
                                }
                            }
                        )
                        TutorialMenu(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f)
                                .padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    AnimatedRow(
                        modifier = Modifier.fillMaxSize(),
                        isVisible = isVisible,
                        stagger = false,
                    ) {
                        AnimatedItem { ctx ->
                            TutorialMenu(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .animated(ctx, from = AnimatedDirection.Left)
                                    .padding(start = 12.dp)
                            )
                        }

                        AnimatedItem { ctx ->
                            MainMenu(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .animated(ctx, from = AnimatedDirection.Right)
                                    .padding(end = 12.dp),
                                eventViewModel = eventViewModel,
                                onShareLogs = {
                                    val logFile = PathManager.FILE_TERRACOTTA_LOG
                                    if (logFile.exists()) {
                                        shareFile(context, logFile)
                                    } else {
                                        eventViewModel.sendToast(androidText(R.string.terracotta_export_log_share_null))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Primeira ferramenta do modo LAN local: informa e copia o IPv4 privado
 * da interface de rede ativa. A porta continua sendo a anunciada pelo Minecraft.
 */
@Composable
private fun LocalLanCard(
    context: Context,
    eventViewModel: EventViewModel
) {
    var localAddress by remember { mutableStateOf(findLocalLanAddress()) }
    var gamePort by remember { mutableStateOf("") }
    val validPort = gamePort.toIntOrNull()?.takeIf { it in 1..65535 }

    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "LAN LOCAL • WI-FI / HOTSPOT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Conecte os celulares à mesma rede. O anfitrião deve abrir o mundo para LAN no menu de pausa do Minecraft.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = localAddress ?: "Não foi possível identificar um IP local. Verifique o Wi-Fi ou hotspot.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Digite a porta exibida pelo Minecraft no chat depois de abrir o mundo para LAN.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OwnOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = gamePort,
                onValueChange = { value ->
                    gamePort = value.filter(Char::isDigit).take(5)
                },
                label = { Text("Porta LAN do Minecraft") },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = localAddress != null && validPort != null,
                    onClick = {
                        val address = localAddress ?: return@Button
                        val port = validPort ?: return@Button
                        val connectionAddress = "$address:$port"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("REALISTIC LAN ADDRESS", connectionAddress))
                        eventViewModel.sendToast(androidText("Endereço LAN copiado"))
                    }
                ) {
                    Text("COPIAR ENDEREÇO")
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        localAddress = findLocalLanAddress()
                        if (localAddress == null) {
                            eventViewModel.sendToast(androidText("Nenhum IP local encontrado"))
                        }
                    }
                ) {
                    Text("ATUALIZAR IP")
                }
            }
        }
    }
}

private fun findLocalLanAddress(): String? {
    return runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { networkInterface ->
                runCatching { networkInterface.isUp && !networkInterface.isLoopback }.getOrDefault(false)
            }

        val candidates = interfaces.flatMap { networkInterface ->
            networkInterface.inetAddresses.toList()
                .filterIsInstance<Inet4Address>()
                .filter { address ->
                    !address.isLoopbackAddress && !address.isLinkLocalAddress && address.isSiteLocalAddress
                }
                .mapNotNull { address ->
                    address.hostAddress?.let { host -> networkInterface.name to host }
                }
        }

        candidates.sortedBy { (interfaceName, _) ->
            when {
                interfaceName.startsWith("wlan", ignoreCase = true) -> 0
                interfaceName.startsWith("ap", ignoreCase = true) -> 1
                interfaceName.startsWith("wifi", ignoreCase = true) -> 2
                else -> 3
            }
        }.firstOrNull()?.second
    }.getOrNull()
}

private sealed interface MultiplayerOperation {
    data object None : MultiplayerOperation
    data object Notice : MultiplayerOperation
    /** 没有通知权限，提醒用户 */
    data object WarningNotification : MultiplayerOperation
}

@Composable
private fun MultiplayerOperation(
    operation: MultiplayerOperation,
    onChange: (MultiplayerOperation) -> Unit,
    onNoticeRead: () -> Unit,
    onNoticeRefused: () -> Unit
) {
    when (operation) {
        is MultiplayerOperation.None -> {}
        is MultiplayerOperation.Notice -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.terracotta_status_uninitialized_desc),
                dismissByDialog = false,
                onDismiss = onNoticeRefused,
                onConfirm = onNoticeRead
            )
        }
        is MultiplayerOperation.WarningNotification -> {
            NotificationCheck(
                text = stringResource(R.string.notification_data_terracotta_message),
                onGranted = {
                    onChange(MultiplayerOperation.None)
                },
                onIgnore = {
                    onChange(MultiplayerOperation.None)
                },
                onDismiss = {
                    onChange(MultiplayerOperation.None)
                }
            )
        }
    }
}

/**
 * 主菜单：所有主要操作都在这里
 */
@Composable
private fun MainMenu(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel,
    onShareLogs: () -> Unit
) {
    val context = LocalContext.current
    var operation by remember { mutableStateOf<MultiplayerOperation>(MultiplayerOperation.None) }

    MultiplayerOperation(
        operation = operation,
        onChange = { operation = it },
        onNoticeRead = {
            AllSettings.terracottaNoticeVer.save(Terracotta.TERRACOTTA_USER_NOTICE_VERSION)
            operation = if (!NotificationManager.checkNotificationEnabled(context)) {
                MultiplayerOperation.WarningNotification
            } else {
                MultiplayerOperation.None
            }
        },
        onNoticeRefused = {
            AllSettings.enableTerracotta.save(false)
            operation = MultiplayerOperation.None
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LocalLanCard(context = context, eventViewModel = eventViewModel)

//        //关于地区的警告
//        BackgroundCard(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors().copy(
//                containerColor = MaterialTheme.colorScheme.secondaryContainer,
//                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//            ),
//            shape = MaterialTheme.shapes.extraLarge
//        ) {
//            Text(
//                modifier = Modifier.padding(all = 16.dp),
//                text = stringResource(R.string.terracotta_warning_region),
//                style = MaterialTheme.typography.titleSmall
//            )
//        }

        //多人联机设置菜单
        SettingsCardColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            //启用多人联机
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                unit = AllSettings.enableTerracotta,
                title = stringResource(R.string.terracotta_enable),
                verticalAlignment = Alignment.CenterVertically,
                onCheckedChange = { value ->
                    if (value) {
                        when {
                            AllSettings.terracottaNoticeVer.getValue() < Terracotta.TERRACOTTA_USER_NOTICE_VERSION -> {
                                //未阅读公告
                                operation = MultiplayerOperation.Notice
                            }
                            !NotificationManager.checkNotificationEnabled(context) -> {
                                operation = MultiplayerOperation.WarningNotification
                            }
                        }
                    }
                }
            )

            // 自定义服务器节点
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.enableTerracottaNodes,
                title = stringResource(R.string.terracotta_custom_note_list),
                verticalAlignment = Alignment.CenterVertically,
                enabled = AllSettings.enableTerracotta.state,
                columnLayout = {
                    AnimatedVisibility(
                        visible = AllSettings.enableTerracotta.state && AllSettings.enableTerracottaNodes.state,
                    ) {
                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = AllSettings.terracottaNodes.state,
                            onValueChange = { value ->
                                AllSettings.terracottaNodes.save(value)
                            },
                            label = {
                                Text(text = stringResource(R.string.terracotta_custom_note_list_hint))
                            },
                            textStyle = MaterialTheme.typography.labelMedium,
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                        )
                    }
                }
            )

            val terracottaEnabled = AllSettings.enableTerracotta.state

            //分享联机核心日志
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                title = stringResource(R.string.terracotta_export_log_share),
                innerPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                onClick = onShareLogs,
                enabled = terracottaEnabled
            )

            //关于 EasyTier
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                title = stringResource(R.string.terracotta_easytier),
                innerPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                onClick = {
                    eventViewModel.sendEvent(EventViewModel.Event.OpenLink(URL_EASYTIER))
                }
            )
        }
    }
}

/**
 * 教程Tab分区
 * @param text 板块标题字符串资源
 */
private data class TabItem(
    val text: Int
)

/**
 * 教程菜单
 */
@Composable
private fun TutorialMenu(
    modifier: Modifier = Modifier
) {
    BackgroundCard(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val tabs = remember {
            listOf(
                TabItem(R.string.terracotta_confirm_title),
                TabItem(R.string.terracotta_tutorial_host_tab),
                TabItem(R.string.terracotta_tutorial_guest_tab)
            )
        }

        val pagerState = rememberPagerState(pageCount = { tabs.size })
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }

        //顶贴标签栏
        SecondaryTabRow(
            containerColor = influencedByBackgroundColor(
                color = cardTitleColor(),
                influencedAlpha = 0.5f * (AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f)
            ),
            selectedTabIndex = selectedTabIndex
        ) {
            tabs.forEachIndexed { index, item ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = {
                        selectedTabIndex = index
                    },
                    text = {
                        MarqueeText(text = stringResource(item.text))
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when(page) {
                0 -> {
                    //用户须知
                    SingleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(R.string.terracotta_confirm_title),
                        text = {
                            BodyText(stringResource(R.string.terracotta_confirm_software))
                            BodyText(stringResource(R.string.terracotta_confirm_p2p))
                            BodyText(stringResource(R.string.terracotta_confirm_law))
                        }
                    )
                }
                1 -> {
                    //房主教程
                    DoubleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        firstTitle = stringResource(R.string.terracotta_tutorial_host_tip),
                        firstText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_enable_multiplayer))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_become_host))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_open_lan))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_vpn_permission))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_copy_invite))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_send_invite))
                        },
                        secondTitle = stringResource(R.string.terracotta_tutorial_note_title),
                        secondText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_offline_account_support))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_interoperability))
                        }
                    )
                }
                2 -> {
                    //房客教程
                    DoubleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        firstTitle = stringResource(R.string.terracotta_tutorial_guest_tip),
                        firstText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_enable_multiplayer))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_become_guest))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_vpn_permission))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_join_room))
                        },
                        secondTitle = stringResource(R.string.terracotta_tutorial_note_title),
                        secondText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_offline_account_support))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_interoperability))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_alternate_server))
                        }
                    )
                }
            }
        }
    }
}

/**
 * 单标题文本Column，标题+正文的布局
 */
@Composable
private fun SingleTitleColumn(
    modifier: Modifier = Modifier,
    title: String,
    text: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    TitleTextLayout(
        modifier = modifier
            .verticalScrollWithBar(scrollState)
            .padding(all = 16.dp),
        title = title,
        text = text
    )
}

/**
 * 双标题文本Column，第一个标题+文本+第二个标题+文本
 */
@Composable
private fun DoubleTitleColumn(
    modifier: Modifier = Modifier,
    firstTitle: String,
    secondTitle: String,
    firstText: @Composable ColumnScope.() -> Unit,
    secondText: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(
        modifier = modifier
            .verticalScrollWithBar(scrollState)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TitleTextLayout(firstTitle, firstText)
        TitleTextLayout(secondTitle, secondText)
    }
}

@Composable
private fun TitleTextLayout(
    title: String,
    text: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = text
        )
    }
}

@Composable
private fun BodyText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall
    )
}