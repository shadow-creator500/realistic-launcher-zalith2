/*
 * REALISTIC LAUNCHER
 * Automatic first-run preparation for supported Minecraft versions.
 */
package com.movtery.zalithlauncher.ui.screens.splash

data class VersionPreparationItem(
    val version: String,
    val title: String,
    val status: PreparationStatus = PreparationStatus.PENDING,
    val detail: String = "Aguardando preparação"
)

enum class PreparationStatus {
    PENDING,
    PREPARING,
    COMPLETE,
    FAILED
}
