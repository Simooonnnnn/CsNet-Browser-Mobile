package com.csnet.browser

data class TabInfo(
    val id: String,
    val title: String,
    val url: String,
    val isActive: Boolean = false,
    val favicon: String? = null,
    val csNetContent: String? = null
)