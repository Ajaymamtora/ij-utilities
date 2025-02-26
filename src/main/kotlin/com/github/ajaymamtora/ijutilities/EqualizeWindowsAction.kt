package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import java.awt.Container
import javax.swing.SwingUtilities

class EqualizeWindowsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Equalize all splitters in the editor
        equalizeSplitters(project)
    }

    private fun equalizeSplitters(project: Project) {
        // Get the editor manager
        val editorManager = FileEditorManager.getInstance(project)

        // Get the editor component
        val editorComponent = editorManager.selectedEditor?.component ?: return

        // Get the root component (usually an IdeFrameImpl)
        val rootComponent = SwingUtilities.getRoot(editorComponent) as? Container ?: return

        // Find and equalize all splitters
        val splitters = findAllSplitters(rootComponent)
        splitters.forEach { equalizeMainSplitter(it) }
    }

    private fun findAllSplitters(container: Container): List<Splitter> {
        val splitters = mutableListOf<Splitter>()

        // Process all components in the container
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)

            // If this component is a splitter, add it to our list
            if (component is Splitter) {
                splitters.add(component)
            }

            // If this component is a container, recursively process it
            if (component is Container) {
                splitters.addAll(findAllSplitters(component))
            }
        }

        return splitters
    }

    private fun equalizeMainSplitter(splitter: Splitter) {
        // Set proportion to 0.5 to equalize the windows
        splitter.proportion = 0.5F
    }
}
