package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.SwingUtilities

class EqualizeWindowsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Get the main splitter and equalize it
        val mainSplitter = getMainSplitter(project)
        mainSplitter?.let { equalizeMainSplitter(it) }
    }

    private fun equalizeMainSplitter(splitter: Splitter) {
        // Set proportion to 0.5 to equalize the windows
        splitter.proportion = 0.5F

        // If you have nested splitters, handle them recursively
        val firstComponent = splitter.firstComponent
        val secondComponent = splitter.secondComponent

        if (firstComponent is Splitter) {
            equalizeMainSplitter(firstComponent)
        }

        if (secondComponent is Splitter) {
            equalizeMainSplitter(secondComponent)
        }
    }

    private fun getMainSplitter(project: Project): Splitter? {
        // Get the editor manager
        val editorManager = FileEditorManager.getInstance(project)

        // Get the editor component
        val editorComponent = editorManager.selectedEditor?.component ?: return null

        // Find the parent component that is a Splitter
        val component = SwingUtilities.getAncestorOfClass(
            Splitter::class.java,
            editorComponent
        ) as? Splitter

        // If we found a splitter, return it
        if (component is Splitter) {
            return component
        }

        // Fallback: try to find any OnePixelSplitter in the UI
        val rootComponent = SwingUtilities.getRoot(editorComponent) as? JComponent
        return rootComponent?.let { UIUtil.findComponentOfType(it, OnePixelSplitter::class.java) }
    }
}
