// TabManager.kt
package com.csnet.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TabManager {
    private val _tabs = MutableStateFlow<List<TabInfo>>(listOf())
    val tabs: StateFlow<List<TabInfo>> = _tabs.asStateFlow()

    private var activeTabId: String? = null

    fun createNewTab(title: String = "New Tab", url: String = ""): String {
        val newTabId = UUID.randomUUID().toString()
        val newTab = TabInfo(
            id = newTabId,
            title = title,
            url = url,
            isActive = true
        )

        // Deactivate current active tab
        val updatedTabs = _tabs.value.map { it.copy(isActive = false) } + newTab
        _tabs.value = updatedTabs
        activeTabId = newTabId

        return newTabId
    }

    fun updateTab(tabId: String, title: String? = null, url: String? = null) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    title = title ?: tab.title,
                    url = url ?: tab.url
                )
            } else tab
        }
    }

    fun closeTab(tabId: String) {
        _tabs.value = _tabs.value.filter { it.id != tabId }
        if (activeTabId == tabId) {
            activeTabId = _tabs.value.lastOrNull()?.id
            _tabs.value = _tabs.value.map { it.copy(isActive = it.id == activeTabId) }
        }
    }

    fun getActiveTab(): TabInfo? {
        return _tabs.value.find { it.id == activeTabId }
    }

    fun setActiveTab(tabId: String) {
        _tabs.value = _tabs.value.map { tab ->
            tab.copy(isActive = tab.id == tabId)
        }
        activeTabId = tabId
    }
}