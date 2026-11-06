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

import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionComparator
import com.movtery.zalithlauncher.game.version.installed.VersionType
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.installed.cleanup.GameAssetCleaner
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.activities.MainActivity
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.EdgeDirection
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.components.topBarWheelFallback
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CleanupOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.GamePathItemLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.GamePathOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionCategory
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionCategoryItem
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionItemLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionsOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.canHandlePermission
import com.movtery.zalithlauncher.utils.checkStoragePermissions
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.backgroundVisible
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

private class VersionsScreenViewModel : ViewModel() {
    /** 版本类别分类 */
    var versionCategory by mutableStateOf(VersionCategory.ALL)
        private set
    /** 重排序刷新key */
    var resortKey by mutableIntStateOf(0)
        private set

    /** 游戏路径相关操作 */
    var gamePathOperation by mutableStateOf<GamePathOperation>(GamePathOperation.None)

    /** 全部版本的数量 */
    var allVersionsCount by mutableIntStateOf(0)
    /** 原版版本数量 */
    var vanillaVersionsCount by mutableIntStateOf(0)
    /** 模组加载器版本数量 */
    var modloaderVersionsCount by mutableIntStateOf(0)

    fun startRefreshVersions() {
        if (!VersionsManager.isRefreshing.value) {
            VersionsManager.refresh("VersionsScreenViewModel.startRefreshVersions")
        }
    }

    private var currentJob: Job? = null
    private var mutex: Mutex = Mutex()

    /**
     * 变更当前版本列表的过滤类型
     */
    fun changeCategory(category: VersionCategory) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            mutex.withLock {
                this@VersionsScreenViewModel.versionCategory = category
            }
        }
    }

    /**
     * 重新排序当前版本列表
     */
    fun resortVersions() {
        resortKey++
    }

    /** 清理游戏文件操作 */
    var cleanupOperation by mutableStateOf<CleanupOperation>(CleanupOperation.None)

    /** 游戏无用资源清理者 */
    var cleaner by mutableStateOf<GameAssetCleaner?>(null)

    fun cleanUnusedFiles(
        onStart: () -> Unit = {},
        onStop: () -> Unit = {}
    ) {
        cleaner = GameAssetCleaner(
            scope = viewModelScope
        ).also {
            cleanupOperation = CleanupOperation.Clean
            it.start(
                onEnd = { count, size ->
                    cleaner = null
                    cleanupOperation = CleanupOperation.Success(count, size)
                    onStop()
                },
                onThrowable = { th ->
                    cleaner = null
                    cleanupOperation = CleanupOperation.Error(th)
                    onStop()
                }
            )
        }
        onStart()
    }

    fun cancelCleaner() {
        cleaner?.cancel()
        cleaner = null
        cleanupOperation = CleanupOperation.None
    }

    override fun onCleared() {
        cancelCleaner()
        currentJob?.cancel()
    }
}

@Composable
private fun rememberVersionViewModel() : VersionsScreenViewModel {
    return viewModel(
        key = NormalNavKey.VersionsManager.toString()
    ) {
        VersionsScreenViewModel()
    }
}

@Composable
private fun rememberVersions(
    versions: StateFlow<List<Version>>,
    viewModel: VersionsScreenViewModel,
): State<List<Version>> {
    val vers by versions.collectAsStateWithLifecycle()
    val category = viewModel.versionCategory
    val resortKey = viewModel.resortKey

    return remember(vers, category, resortKey) {
        derivedStateOf {
            viewModel.allVersionsCount = vers.size

            val vanillaVersions = vers
                .filter { ver -> ver.versionType == VersionType.VANILLA }
                .also { viewModel.vanillaVersionsCount = it.size }
            val modloaderVersions = vers
                .filter { ver -> ver.versionType == VersionType.MODLOADERS }
                .also { viewModel.modloaderVersionsCount = it.size }

            when (category) {
                VersionCategory.ALL -> vers
                VersionCategory.VANILLA -> vanillaVersions
                VersionCategory.MODLOADER -> modloaderVersions
            }.sortedWith(VersionComparator)
        }
    }
}

@Composable
fun VersionsManageScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val viewModel = rememberVersionViewModel()

    val versions by rememberVersions(VersionsManager.versions, viewModel)
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val horizontalVersionCards = AllSettings.horizontalVersionCards.state
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    GamePathOperation(
        gamePathOperation = viewModel.gamePathOperation,
        changeState = { viewModel.gamePathOperation = it },
        submitError = submitError
    )

    BaseScreen(
        screenKey = NormalNavKey.VersionsManager,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF020202),
                        Color(0xFF170307),
                        Color(0xFF050505),
                        Color(0xFF24040B),
                        Color(0xFF020202)
                    )
                )
            )
        ) {
            LeftMenu(
                isVisible = isVisible,
                isRefreshing = isRefreshing,
                swapToFileSelector = { path ->
                    backScreenViewModel.mainScreen.backStack.navigateToFileSelector(
                        startPath = path,
                        selectFile = false,
                        saveKey = NormalNavKey.VersionsManager
                    ) { path ->
                        viewModel.gamePathOperation = GamePathOperation.AddNewPath(path)
                    }
                },
                onCleanupGameFiles = {
                    if (viewModel.cleanupOperation == CleanupOperation.None) {
                        viewModel.cleanupOperation = CleanupOperation.Tip
                    }
                },
                changePathOperation = {
                    viewModel.gamePathOperation = it
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f)
            )

            VersionsLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(8f),
                isVisible = isVisible,
                isRefreshing = isRefreshing,
                versions = versions,
                currentVersion = currentVersion,
                versionCategory = viewModel.versionCategory,
                onCategoryChange = { viewModel.changeCategory(it) },
                allVersionsCount = viewModel.allVersionsCount,
                vanillaVersionsCount = viewModel.vanillaVersionsCount,
                modloaderVersionsCount = viewModel.modloaderVersionsCount,
                navigateToVersions = navigateToVersions,
                navigateToExport = navigateToExport,
                submitError = submitError,
                onRefresh = {
                    viewModel.startRefreshVersions()
                },
                onVersionPinned = {
                    viewModel.resortVersions()
                },
                onInstall = {
                    backScreenViewModel.navigateToDownload()
                }
            )

            CleanupOperation(
                operation = viewModel.cleanupOperation,
                changeOperation = { viewModel.cleanupOperation = it },
                cleaner = viewModel.cleaner,
                onClean = {
                    viewModel.cleanUnusedFiles(
                        onStart = {
                            eventViewModel.sendKeepScreen(true)
                        },
                        onStop = {
                            eventViewModel.sendKeepScreen(false)
                        }
                    )
                },
                onCancel = {
                    viewModel.cancelCleaner()
                    eventViewModel.sendKeepScreen(false)
                },
                submitError = submitError
            )
        }
    }
}

@Composable
private fun LeftMenu(
    isVisible: Boolean,
    isRefreshing: Boolean,
    swapToFileSelector: (path: String) -> Unit,
    onCleanupGameFiles: () -> Unit,
    changePathOperation: (GamePathOperation) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    Column(
        modifier = modifier.animated(ctx, from = AnimatedDirection.Left),
    ) {
        val gamePaths by GamePathManager.gamePathData.collectAsStateWithLifecycle()
        val currentPath by GamePathManager.currentPath.collectAsStateWithLifecycle()
        val context = LocalContext.current

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                .border(1.dp, Color(0xFF75202F), RoundedCornerShape(18.dp)),
            color = Color(0xFF10090B),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_realistic_launcher),
                    contentDescription = "REALISTIC LAUNCHER",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                )
                Text(
                    text = "REALISTIC",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "LAUNCHER",
                    color = Color(0xFFFF2948),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "GERENCIADOR DE VERSÕES",
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Text(
            text = "PASTAS DO JOGO",
            color = Color(0xFFFF405B),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 12.dp,
                bottom = 12.dp
            )
        ) {
            items(gamePaths, key = { it.id }) { pathItem ->
                GamePathItemLayout(
                    item = pathItem,
                    selected = currentPath == pathItem.path,
                    enabled = canHandlePermission,
                    onClick = {
                        if (!isRefreshing) { //避免频繁刷新，防止currentGameInfo意外重置
                            if (pathItem.id == GamePathManager.DEFAULT_ID) {
                                GamePathManager.saveDefaultPath()
                            } else {
                                (context as? MainActivity)?.let { activity ->
                                    checkStoragePermissions(
                                        activity = activity,
                                        message = activity.getString(R.string.versions_manage_game_storage_permissions),
                                        messageSdk30 = activity.getString(R.string.versions_manage_game_storage_permissions_sdk30),
                                        hasPermission = {
                                            GamePathManager.saveCurrentPath(pathItem.id)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    onDelete = {
                        changePathOperation(GamePathOperation.DeletePath(pathItem))
                    },
                    onRename = {
                        changePathOperation(GamePathOperation.RenamePath(pathItem))
                    }
                )
            }
        }

        ScalingActionButton(
            modifier = Modifier
                .padding(start = 12.dp)
                .padding(top = 8.dp)
                .fillMaxWidth(),
            onClick = {
                (context as? MainActivity)?.let { activity ->
                    checkStoragePermissions(
                        activity = activity,
                        message = activity.getString(R.string.versions_manage_game_path_storage_permissions),
                        messageSdk30 = activity.getString(R.string.versions_manage_game_path_storage_permissions_sdk30),
                        hasPermission = {
                            swapToFileSelector(Environment.getExternalStorageDirectory().absolutePath)
                        }
                    )
                }
            },
            enabled = canHandlePermission
        ) {
            MarqueeText(text = stringResource(R.string.versions_manage_game_path_add_new))
        }

        ScalingActionButton(
            modifier = Modifier
                .padding(start = 12.dp, top = 8.dp, bottom = 12.dp)
                .fillMaxWidth(),
            onClick = onCleanupGameFiles
        ) {
            MarqueeText(text = stringResource(R.string.versions_manage_cleanup))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionsLayout(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isRefreshing: Boolean,
    versions: List<Version>,
    currentVersion: Version?,
    versionCategory: VersionCategory,
    onCategoryChange: (VersionCategory) -> Unit,
    allVersionsCount: Int,
    vanillaVersionsCount: Int,
    modloaderVersionsCount: Int,
    navigateToVersions: (Version) -> Unit,
    navigateToExport: (Version) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    onRefresh: () -> Unit,
    onVersionPinned: () -> Unit,
    onInstall: () -> Unit,
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)

    PullToRefreshBox(
        modifier = modifier.animated(ctx),
        isRefreshing = false,
        enabled = !isRefreshing,
        onRefresh = onRefresh
    ) {
        if (isRefreshing) { //版本正在刷新中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            var versionsOperation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
            VersionsOperation(
                versionsOperation = versionsOperation,
                updateVersionsOperation = { versionsOperation = it },
                submitError = submitError
            )

            // 操作栏滚动吸附
            val density = LocalDensity.current
            val topAppBarState = rememberTopAppBarState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
            val headerTopPaddingPx = with(density) { 12.dp.toPx() }
            var headerHeightPx by remember { mutableIntStateOf(0) }

            // 操作栏阴影跟随滚动线性过渡
            val listState = rememberLazyListState()
            val listScrolledFraction = remember(listState, headerTopPaddingPx) {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) 1f
                    else {
                        val rampPx = headerTopPaddingPx + (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0)
                        (listState.firstVisibleItemScrollOffset / rampPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    }
                }
            }
            val barShownFraction = remember(topAppBarState) {
                derivedStateOf { 1f - topAppBarState.collapsedFraction }
            }
            //背景可见时不使用阴影，因为卡片会半透明化
            val withShadow = !backgroundVisible()

            //悬浮操作栏的阴影在图层块内读取滚动比例，滚动时只更新图层不重组
            val barShape = MaterialTheme.shapes.large
            val barShadowModifier = Modifier.graphicsLayer {
                if (withShadow) {
                    shadowElevation = 5.dp.toPx() * listScrolledFraction.value * barShownFraction.value
                }
                shape = barShape
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (versions.isNotEmpty()) {
                    if (horizontalVersionCards.value) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = with(density) {
                                    (headerHeightPx + topAppBarState.heightOffset).coerceAtLeast(0f).toDp()
                                },
                                end = 12.dp,
                                bottom = 12.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(versions, key = { it.toString() }) { version ->
                                VersionItemLayout(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .border(
                                            width = 1.dp,
                                            color = if (version == currentVersion) Color(0xFFFF1738) else Color(0xFF54202A),
                                            shape = MaterialTheme.shapes.large
                                        ),
                                    version = version,
                                    selected = version == currentVersion,
                                    color = if (version == currentVersion) Color(0xFF2B080D) else Color(0xFF171717),
                                    contentColor = Color(0xFFF5F5F5),
                                    submitError = submitError,
                                    onSelected = {
                                        if (version == currentVersion) return@VersionItemLayout
                                        if (!VersionsManager.saveVersion(version)) {
                                            //不允许选择无效版本
                                            versionsOperation = VersionsOperation.InvalidDelete(version)
                                        }
                                    },
                                    onSettingsClick = {
                                        navigateToVersions(version)
                                    },
                                    onRenameClick = { versionsOperation = VersionsOperation.Rename(version) },
                                    onCopyClick = { versionsOperation = VersionsOperation.Copy(version) },
                                    onExportClick = { navigateToExport(version) },
                                    onDeleteClick = { versionsOperation = VersionsOperation.Delete(version) },
                                    onPinned = onVersionPinned
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .topBarWheelFallback(topAppBarState)
                                .nonInteractiveScrollbar(
                                    state = listState.scrollIndicatorState!!,
                                    orientation = Orientation.Vertical,
                                )
                                .clipToBounds()
                                .topFade {
                                    (headerHeightPx + headerTopPaddingPx + 80f) *
                                            listScrolledFraction.value *
                                            barShownFraction.value
                                },
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = with(density) {
                                    (headerHeightPx + topAppBarState.heightOffset).coerceAtLeast(0f).toDp()
                                },
                                end = 12.dp,
                                bottom = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            state = listState,
                        ) {
                            items(versions, key = { it.toString() }) { version ->
                                VersionItemLayout(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .border(
                                            width = 1.dp,
                                            color = if (version == currentVersion) Color(0xFFFF1738) else Color(0xFF54202A),
                                            shape = MaterialTheme.shapes.large
                                        ),
                                    version = version,
                                    selected = version == currentVersion,
                                    color = if (version == currentVersion) Color(0xFF2B080D) else Color(0xFF171717),
                                    contentColor = Color(0xFFF5F5F5),
                                    submitError = submitError,
                                    onSelected = {
                                        if (version == currentVersion) return@VersionItemLayout
                                        if (!VersionsManager.saveVersion(version)) {
                                            //不允许选择无效版本
                                            versionsOperation = VersionsOperation.InvalidDelete(version)
                                        }
                                    },
                                    onSettingsClick = {
                                        navigateToVersions(version)
                                    },
                                    onRenameClick = { versionsOperation = VersionsOperation.Rename(version) },
                                    onCopyClick = { versionsOperation = VersionsOperation.Copy(version) },
                                    onExportClick = { navigateToExport(version) },
                                    onDeleteClick = { versionsOperation = VersionsOperation.Delete(version) },
                                    onPinned = onVersionPinned
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScalingLabel(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.versions_manage_no_versions)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .onSizeChanged {
                            headerHeightPx = it.height
                            topAppBarState.heightOffsetLimit = -it.height.toFloat()
                        }
                        .offset { IntOffset(x = 0, y = topAppBarState.heightOffset.roundToInt()) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .border(1.dp, Color(0xFF75202F), MaterialTheme.shapes.large)
                            .background(Color(0xFF10090B), MaterialTheme.shapes.large)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Fala, meu cria! 👊",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Pronto pra escolher sua próxima aventura?",
                                color = Color(0xFFBDBDBD),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFF2948), RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF050505),
                                            Color(0xFF26060C),
                                            Color(0xFF590B19),
                                            Color(0xFF100508)
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 22.dp, vertical = 22.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SUAS VERSÕES",
                                    color = Color(0xFFFF405B),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "REALISTIC LAUNCHER",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.4.sp
                                )
                                Text(
                                    text = "GERENCIAMENTO DE VERSÕES  •  MINECRAFT JAVA",
                                    color = Color(0xFFFF405B),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Escolha, organize e prepare suas instalações para jogar.",
                                    color = Color(0xFFE0D5D7),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(width = 72.dp, height = 3.dp)
                                        .background(Color(0xFFFF2948), RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "${versions.size} VERSÕES INSTALADAS",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentVersion?.let { "ATUAL: ${it.getVersionName()}" }
                                        ?: "Nenhuma versão selecionada",
                                    color = Color(0xFFFF7185),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val blur = AllSettings.backgroundBlur.state
                            val versionToolbarColor = Color(0xFF191919)

                            Surface(
                                modifier = Modifier
                                    .height(IntrinsicSize.Max)
                                    .then(barShadowModifier),
                                color = versionToolbarColor,
                                contentColor = Color.White,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .backgroundGlass(blur, versionToolbarColor)
                                        .padding(all = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconTextButton(
                                        onClick = onRefresh,
                                        painter = painterResource(R.drawable.ic_refresh),
                                        contentDescription = stringResource(R.string.generic_refresh),
                                        text = stringResource(R.string.generic_refresh)
                                    )
                                    IconTextButton(
                                        onClick = onInstall,
                                        painter = painterResource(R.drawable.ic_download),
                                        contentDescription = stringResource(R.string.versions_manage_install_new),
                                        text = stringResource(R.string.versions_manage_install_new),
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .height(IntrinsicSize.Max)
                                    .then(barShadowModifier),
                                color = versionToolbarColor,
                                contentColor = Color.White,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                val scrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .backgroundGlass(blur, versionToolbarColor)
                                        .fadeEdge(
                                            state = scrollState,
                                            length = 32.dp,
                                            direction = EdgeDirection.Horizontal
                                        )
                                        .horizontalScroll(state = scrollState)
                                        .padding(all = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VersionCategoryItem(
                                        value = VersionCategory.ALL,
                                        versionsCount = allVersionsCount,
                                        selected = versionCategory == VersionCategory.ALL,
                                        onClick = { onCategoryChange(VersionCategory.ALL) }
                                    )
                                    VersionCategoryItem(
                                        value = VersionCategory.VANILLA,
                                        versionsCount = vanillaVersionsCount,
                                        selected = versionCategory == VersionCategory.VANILLA,
                                        onClick = { onCategoryChange(VersionCategory.VANILLA) }
                                    )
                                    VersionCategoryItem(
                                        value = VersionCategory.MODLOADER,
                                        versionsCount = modloaderVersionsCount,
                                        selected = versionCategory == VersionCategory.MODLOADER,
                                        onClick = { onCategoryChange(VersionCategory.MODLOADER) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 在组件顶部绘制指定像素高度的线性渐隐；高度在绘制阶段读取，随滚动只触发重绘
 */
private fun Modifier.topFade(heightPx: () -> Float): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val height = heightPx()
        if (height <= 0f) return@drawWithContent
        inset(
            left = 0f,
            top = 0f,
            right = 0f,
            bottom = (size.height - height).coerceAtLeast(0f)
        ) {
            drawRect(
                brush = TopFadeBrush,
                blendMode = BlendMode.DstOut
            )
        }
    }

/** 顶部渐隐使用的固定渐变 */
private val TopFadeBrush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent)