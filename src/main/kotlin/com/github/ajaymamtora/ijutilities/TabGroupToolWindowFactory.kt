package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent

/**
 * Factory for creating the tab group tool window
 */
class TabGroupToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val tabGroupUI = TabGroupBarUI(project)
        val content = contentFactory.createContent(tabGroupUI, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)

        // Set up tool window to be more like a tab bar
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.setAutoHide(false)
        toolWindow.stripeTitle = "Tab Groups"

        // Hide the tool window header
        toolWindow.component.parent.parent?.let { container ->
            // Try to find and hide the header component
            for (i in 0 until container.componentCount) {
                val component = container.getComponent(i)
                if (component is JComponent && component.componentCount > 0) {
                    val firstChild = component.getComponent(0)
                    if (firstChild.javaClass.simpleName.contains("Header")) {
                        component.isVisible = false
                        break
                    }
                }
            }
        }
    }
}
