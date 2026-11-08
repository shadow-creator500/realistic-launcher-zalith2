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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.movtery.cardgrid.model.CardRect
import com.movtery.cardgrid.state.CardGridState
import com.movtery.cardgrid.state.CardSeed
import com.movtery.cardgrid.ui.CardGrid
import com.movtery.cardgrid.ui.CardGridAutoScroll
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardSettingsHost
import com.movtery.zalithlauncher.ui.screens.content.home.version.VersionCardManager
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.flow.distinctUntilChanged

/** 主页网格的卡片操作 */
private sealed interface HomeCardOperation {
    data object None : HomeCardOperation
    /** 打开服务器卡片的设置对话框 */
    data class ServerCardSettings(val cardId: String) : HomeCardOperation
}

/**
 * 主页网格：系统卡片列 + 卡片网格库容器，
 * 负责布局的播种、持久化、卡片记录同步与服务器卡片的可见性上报。
 */
@Composable
fun HomeGrid(
    state: CardGridState,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    //长按调整工具栏触发的卡片操作
    var operation by remember { mutableStateOf<HomeCardOperation>(HomeCardOperation.None) }

    // 播种持久化的用户卡片布局
    LaunchedEffect(Unit) {
        val snapshot = HomeGridStore.load()
        state.seed(
            types = HomeCards.userCardTypes,
            seeds = snapshot?.cards?.map { entry ->
                CardSeed(
                    id = entry.id,
                    typeId = entry.type,
                    layout = CardRect(
                        id = entry.id,
                        x = entry.x,
                        y = entry.y,
                        width = entry.width,
                        height = entry.height
                    )
                )
            } ?: emptyList(),
            storedColumns = snapshot?.columns ?: 0
        )
    }

    // 布局结算后持久化
    LaunchedEffect(Unit) {
        state.onLayoutCommitted = {
            HomeGridStore.save(
                cards = state.cards,
                columns = state.geometry.columns
            )
        }
    }

    // 卡片移除回调：同步各卡片管理器的记录
    LaunchedEffect(Unit) {
        state.onCardRemoved = { cardId ->
            VersionCardManager.removeCard(cardId)
            ServerCardManager.removeCard(cardId)
        }
    }

    // 与版本卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        VersionCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.versionCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 首次准备完成后，自动把三个 REALISTIC Fabric 版本加入主页快捷卡片。
    LaunchedEffect(Unit) {
        val supportedProfiles = setOf(
            "REALISTIC-1.21.1-Fabric",
            "REALISTIC-1.20.1-Fabric",
            "REALISTIC-1.19.2-Fabric"
        )
        VersionsManager.versions.collect { versions ->
            versions
                .filter { it.getVersionName() in supportedProfiles && it.isValid() }
                .forEach { version ->
                    VersionCardManager.addCard(version)
                }
        }
    }

    // 与服务器卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        ServerCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.serverCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 服务器卡片可见性：可见 = 屏幕可见 ∧ App 前台 ∧ 卡片矩形与滚动视口相交，
    // 新进入可见集合的卡片会触发一次静默刷新（15s 冷却由管理器约束）
    val isVisibleState = rememberUpdatedState(isVisible)
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            val top = state.viewportTopPx
            val bottom = top + state.viewportHeightPx
            if (!isVisibleState.value || !resumed || bottom <= 0f) {
                emptySet()
            } else {
                state.cards.mapNotNullTo(mutableSetOf()) { card ->
                    if (card.type.typeId != HomeCards.SERVER_CARD_TYPE_ID) return@mapNotNullTo null
                    val rect = state.rootRectOf(card)
                    if (rect.bottom >= top && rect.top <= bottom) card.id else null
                }
            }
        }.distinctUntilChanged().collect { ids ->
            ServerCardManager.onVisibleCardsChanged(ids)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                state.onViewportPositioned(
                    topPx = coordinates.positionInRoot().y,
                    heightPx = coordinates.size.height.toFloat()
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Hero principal: vidro escuro, contorno rubi e brilho discreto.
            val heroShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .shadow(elevation = 10.dp, shape = heroShape)
                    .clip(heroShape)
                    .backgroundGlass(
                        blur = AllSettings.backgroundBlur.state,
                        color = Color(0xCC171316),
                        enabled = true
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xE63A171B),
                                Color(0xDD211719),
                                Color(0xEA151515)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x99FF777F),
                                Color(0x33FFFFFF),
                                Color(0x665D2027)
                            )
                        ),
                        shape = heroShape
                    )
            ) {
                // Luz ambiente decorativa; não interfere nos toques nem no conteúdo.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(84.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x55E34A53),
                                    Color(0x00E34A53)
                                )
                            ),
                            CircleShape
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFB92C36),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MINECRAFT JAVA",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "REALISTIC\nLAUNCHER",
                            color = Color(0xFFFFF7F7),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            lineHeight = 24.sp,
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "Seu mundo. Suas regras.",
                            color = Color(0xFFD7C9CA),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(R.string.welcome_screen_unofficial_notice),
                            color = Color(0xFFE9BFC2),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFFE34A53), CircleShape)
                            )
                            Text(
                                text = "PERFIS E INSTALAÇÕES",
                                color = Color(0xFFE9BFC2),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE34A53),
                                        Color(0xFF8F202B)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0x66FFFFFF),
                                shape = RoundedCornerShape(17.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RL",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .size(9.dp)
                                .background(Color(0xFFFFD6D8), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SUAS INSTALAÇÕES",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                    Text(
                        text = "Selecione um perfil para jogar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = Color(0xFF351C20),
                    contentColor = Color(0xFFFFA6AC),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "JAVA",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            val systemCards = HomeCards.systemCards()
            if (systemCards.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    systemCards.forEach { systemCard ->
                        key(systemCard.id) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                                systemCard.content()
                            }
                        }
                    }
                }
            }

            CardGrid(
                state = state,
                containerColor = cardColor(),
                contentColor = onCardColor(),
                cardBackground = { base ->
                    base.backgroundGlass(
                        blur = AllSettings.backgroundBlur.state,
                        color = cardColor(),
                        enabled = true
                    )
                },
            )
        }
        CardGridAutoScroll(
            state = state,
            scrollState = scrollState
        )

        //调整态底部操作栏
        AnimatedVisibility(
            visible = state.isAdjusting,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 9.dp)
        ) {
            AdjustingBar(
                state = state,
                onShowSettings = {
                    operation = HomeCardOperation.ServerCardSettings(it)
                }
            )
        }
    }

    HomeCardOperation(
        operation = operation,
        onChange = { operation = it }
    )
}

@Composable
private fun HomeCardOperation(
    operation: HomeCardOperation,
    onChange: (HomeCardOperation) -> Unit
) {
    when (operation) {
        is HomeCardOperation.None -> {}
        is HomeCardOperation.ServerCardSettings -> {
            ServerCardSettingsHost(
                cardId = operation.cardId,
                onDismiss = { onChange(HomeCardOperation.None) }
            )
        }
    }
}

/**
 * 调整态底部操作栏
 */
@Composable
private fun AdjustingBar(
    state: CardGridState,
    onShowSettings: (String) -> Unit
) {
    val adjustingId = state.adjustingCardId
    val isServerCard = remember(state.cards, adjustingId) {
        state.cards.firstOrNull { it.id == adjustingId }?.type?.typeId == HomeCards.SERVER_CARD_TYPE_ID
    }

    Surface(
        modifier = Modifier
            .padding(all = 3.dp)
            .barGuard(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(all = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isServerCard && adjustingId != null) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    onClick = { adjustingId?.let(onShowSettings) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = stringResource(R.string.generic_setting)
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            IconButton(
                modifier = Modifier.size(34.dp),
                enabled = adjustingId != null,
                onClick = { adjustingId?.let(state::removeCard) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_filled),
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = stringResource(R.string.generic_delete)
                )
            }

            Spacer(Modifier.width(2.dp))

            IconButton(
                modifier = Modifier.size(34.dp),
                onClick = { state.exitAdjusting() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.generic_done)
                )
            }
        }
    }
}

/**
 * 消费落在自身范围内的全部指针事件
 */
private fun Modifier.barGuard(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).consume()
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) return@awaitEachGesture
        }
    }
}