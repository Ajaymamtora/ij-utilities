package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import javax.swing.JComponent

/**
 * An action that focuses the terminal if it's open, or opens a new terminal and reveals
 * the terminal tool window if no terminal is currently open.
 */
class TerminalFocusAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        focusOrOpenTerminal(project)
    }

    override fun update(e: AnActionEvent) {
        // Enable the action only when we have a project
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    private fun focusOrOpenTerminal(project: Project) {
        // Get the Terminal tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val terminalToolWindow = toolWindowManager.getToolWindow("Terminal") ?: return

        // Check if terminal is already open
        val isTerminalVisible = terminalToolWindow.isVisible

        if (isTerminalVisible) {
            // Terminal is already visible, just focus it
            terminalToolWindow.activate(null)

            // Try to find and focus the terminal component
            val selectedContent = terminalToolWindow.contentManager.selectedContent
            if (selectedContent != null) {
                focusTerminalComponent(selectedContent)
            }
        } else {
            // Terminal is not visible, open it
            terminalToolWindow.activate(null, true)
        }
    }

    private fun focusTerminalComponent(content: Content) {
        val component = content.component

        // Find the terminal input component by traversing the component hierarchy
        findTerminalInputComponent(component)?.requestFocus()
    }

    private fun findTerminalInputComponent(component: JComponent): JComponent? {
        // Look for a component that might be the terminal input
        // This is a simplified approach that looks for components with specific class names
        val className = component.javaClass.name
        if (className.contains("Terminal") && className.contains("Widget")) {
            return component
        }

        // Recursively search through child components
        for (i in 0 until component.componentCount) {
            val child = component.getComponent(i)
            if (child is JComponent) {
                val result = findTerminalInputComponent(child)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }
}
