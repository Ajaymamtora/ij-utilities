package com.github.ajaymamtora.ijutilities

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * UI component for displaying tab groups
 */
class TabGroupBarUI(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val tabPane = JBTabbedPane()
    private val tabGroupManager = TabGroupManager.getInstance(project)
    private var updatingUI = false

    init {
        // Set up UI
        configureUI()

        // Subscribe to tab group changes
        val connection = project.messageBus.connect(this)
        connection.subscribe(TabGroupManager.TAB_GROUP_TOPIC, object : TabGroupManager.TabGroupListener {
            override fun tabGroupsChanged() {
                updateTabs()
            }

            override fun activeTabGroupChanged(oldIndex: Int, newIndex: Int) {
                updateTabSelection()
            }
        })

        // Subscribe to theme changes
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            LafManagerListener.TOPIC,
            LafManagerListener { updateUI() }
        )

        // Initial update
        updateTabs()
    }

    private fun configureUI() {
        // Configure tab pane
        tabPane.border = JBUI.Borders.empty()
        tabPane.setTabPlacement(JBTabbedPane.TOP)

        // Add change listener
        tabPane.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent) {
                if (!updatingUI) {
                    val selectedIndex = tabPane.selectedIndex
                    if (selectedIndex >= 0) {
                        tabGroupManager.setActiveTabGroup(selectedIndex)
                    }
                }
            }
        })

        // Add to panel
        add(tabPane, BorderLayout.CENTER)
    }

    private fun updateTabs() {
        updatingUI = true
        try {
            // Remember selected index
            val selectedIndex = tabPane.selectedIndex

            // Clear existing tabs
            tabPane.removeAll()

            // Add tabs for each tab group
            tabGroupManager.getTabGroups().forEach { tabGroup ->
                tabPane.addTab(tabGroup.name, null)
            }

            // Select active tab
            updateTabSelection()
        } finally {
            updatingUI = false
        }

        // Update UI
        revalidate()
        repaint()
    }

    private fun updateTabSelection() {
        updatingUI = true
        try {
            val activeIndex = tabGroupManager.getActiveTabGroupIndex()
            if (activeIndex >= 0 && activeIndex < tabPane.tabCount) {
                tabPane.selectedIndex = activeIndex
            }
        } finally {
            updatingUI = false
        }
    }

    override fun dispose() {
        // Clean up resources if needed
    }
}
