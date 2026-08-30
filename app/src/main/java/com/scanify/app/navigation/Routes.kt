package com.scanify.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Routes {
    @Serializable
    data object HomeScreen: Routes()

    @Serializable
    data object FileScreen: Routes()

    @Serializable
    data object TemplateScreen : Routes()

    @Serializable
    data object SettingScreen: Routes()

    @Serializable
    data class PreviewScreen(val id: Long)

    @Serializable
    data object SearchScreen: Routes()

    @Serializable
    data object OnBoardingScreen: Routes()
    @Serializable
    data class IdCardPreviewScreen(val frontUri: String, val backUri: String? = null) : Routes()

}