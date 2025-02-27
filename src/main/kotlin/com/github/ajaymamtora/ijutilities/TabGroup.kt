package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager

/**
 * Represents a tab group containing multiple editor tabs
 */
class TabGroup(
    val id: Int,
    var name: String = "Group $id",
    val project: Project
) {
    private val files = mutableListOf<VirtualFile>()

    // Add a file to this tab group
    fun addFile(file: VirtualFile) {
        if (!files.contains(file)) {
            files.add(file)
        }
    }

    // Remove a file from this tab group
    fun removeFile(file: VirtualFile) {
        files.remove(file)
    }

    // Get all files in this tab group
    fun getFiles(): List<VirtualFile> {
        return files.toList()
    }

    // Show all files in this tab group in the editor
    fun showFiles() {
        ApplicationManager.getApplication().invokeLater {
            val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)

            // Remember which files we currently have open
            val currentlyOpenFiles = fileEditorManager.openFiles.toList()

            // For each file that's not in our group, close it
            currentlyOpenFiles.forEach { file ->
                if (!files.contains(file)) {
                    fileEditorManager.closeFile(file)
                }
            }

            // For each file in our group, open it if it's not already open
            files.forEach { file ->
                if (file.isValid && !currentlyOpenFiles.contains(file)) {
                    fileEditorManager.openFile(file, true)
                }
            }
        }
    }
}
