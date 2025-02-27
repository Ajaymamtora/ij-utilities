package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.SwingUtilities

/**
 * Listener for file editor events
 */
class TabGroupFileEditorListener(private val project: Project) : FileEditorManagerListener {

    private val tabGroupManager = TabGroupManager.getInstance(project)

    override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
        // Add file to active tab group
        SwingUtilities.invokeLater {
            if (file.isValid) {
                tabGroupManager.addFileToActiveTabGroup(file)
            }
        }
    }

    override fun fileClosed(source: com.intellij.openapi.fileEditor.FileEditorManager, file: VirtualFile) {
        // We don't remove the file from tab groups when it's closed
        // This allows tabs to persist between IDE sessions
        // The file will be reopened when switching back to a tab group containing it
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        // Handle file selection changes if needed
    }
}
