package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Service for managing tab groups
 */
class TabGroupManager(private val project: Project) {

    private val tabGroups = CopyOnWriteArrayList<TabGroup>()
    private var activeTabGroupIndex = 0

    interface TabGroupListener {
        fun tabGroupsChanged()
        fun activeTabGroupChanged(oldIndex: Int, newIndex: Int)
    }

    companion object {
        val TAB_GROUP_TOPIC = Topic.create("TabGroupTopic", TabGroupListener::class.java)

        fun getInstance(project: Project): TabGroupManager {
            return project.getService(TabGroupManager::class.java)
        }
    }

    init {
        // Create default tab group
        createTabGroup()
    }

    // Create a new tab group
    fun createTabGroup(): TabGroup {
        val id = tabGroups.size + 1
        val tabGroup = TabGroup(id, "Group $id", project)
        tabGroups.add(tabGroup)

        // Notify listeners
        project.messageBus.syncPublisher(TAB_GROUP_TOPIC).tabGroupsChanged()

        return tabGroup
    }

    // Get all tab groups
    fun getTabGroups(): List<TabGroup> {
        return tabGroups.toList()
    }

    // Get active tab group
    fun getActiveTabGroup(): TabGroup {
        return if (tabGroups.isEmpty()) {
            createTabGroup()
        } else {
            tabGroups[activeTabGroupIndex]
        }
    }

    // Get active tab group index
    fun getActiveTabGroupIndex(): Int {
        return activeTabGroupIndex
    }

    // Set active tab group
    fun setActiveTabGroup(index: Int) {
        if (index in 0 until tabGroups.size && index != activeTabGroupIndex) {
            val oldIndex = activeTabGroupIndex
            activeTabGroupIndex = index

            // Show files in this tab group
            tabGroups[index].showFiles()

            // Notify listeners
            project.messageBus.syncPublisher(TAB_GROUP_TOPIC).activeTabGroupChanged(oldIndex, index)
        }
    }

    // Add file to active tab group
    fun addFileToActiveTabGroup(file: VirtualFile) {
        getActiveTabGroup().addFile(file)
    }

    // Remove file from all tab groups
    fun removeFileFromAllGroups(file: VirtualFile) {
        tabGroups.forEach { it.removeFile(file) }
    }

    // Navigate to next tab group
    fun navigateToNextTabGroup() {
        if (tabGroups.size > 1) {
            val nextIndex = (activeTabGroupIndex + 1) % tabGroups.size
            setActiveTabGroup(nextIndex)
        }
    }

    // Navigate to previous tab group
    fun navigateToPreviousTabGroup() {
        if (tabGroups.size > 1) {
            val prevIndex = (activeTabGroupIndex - 1 + tabGroups.size) % tabGroups.size
            setActiveTabGroup(prevIndex)
        }
    }
}
