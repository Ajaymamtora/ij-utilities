package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Startup activity to initialize tab groups
 */
class TabGroupStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Initialize tab group manager
        TabGroupManager.getInstance(project)

        // Set up file editor listener
        project.messageBus.connect().subscribe(
            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
            TabGroupFileEditorListener(project)
        )
    }
}
