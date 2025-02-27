package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Action to create a new tab group
 */
class NewTabGroupAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            TabGroupManager.getInstance(project).createTabGroup()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
