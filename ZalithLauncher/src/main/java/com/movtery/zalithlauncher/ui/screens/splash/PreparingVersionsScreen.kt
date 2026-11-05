/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery and contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PreparingVersionsScreen(
    items: StateFlow<List<VersionPreparationItem>>,
    screenViewModel: SplashBackStackViewModel,
    onRetry: () -> Unit
) {
    val currentItems by items.collectAsStateWithLifecycle()
    val isPreparing = currentItems.any { it.status == PreparationStatus.PREPARING }
    val hasFailed = currentItems.any { it.status == PreparationStatus.FAILED }
    val completed = currentItems.count { it.status == PreparationStatus.COMPLETE }

    BaseScreen(
        screenKey = SplashOperation.PreparingVersions,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { 
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "PREPARANDO O REALISTIC LAUNCHER",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Estamos preparando o Minecraft Java com Fabric. Mantenha a internet conectada e tenha espaço livre no armazenamento.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$completed de 3 versões preparadas",
                style = MaterialTheme.typography.titleMedium
            )
            if (isPreparing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentItems, key = { it.version }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (item.status) {
                                PreparationStatus.PREPARING -> CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                PreparationStatus.COMPLETE -> androidx.compose.material3.Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                PreparationStatus.FAILED -> androidx.compose.material3.Icon(
                                    painter = painterResource(R.drawable.ic_warning_filled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                PreparationStatus.PENDING -> Spacer(Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (hasFailed) {
                Text(
                    "Algumas versões não foram preparadas. Verifique a conexão e toque em tentar novamente.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRetry
                ) {
                    Text("TENTAR NOVAMENTE")
                }
            }
        }
    }
}
