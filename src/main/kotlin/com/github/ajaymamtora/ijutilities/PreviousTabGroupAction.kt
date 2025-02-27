package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Action to navigate to the previous tab group
 */
class PreviousTabGroupAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            TabGroupManager.getInstance(project).navigateToPreviousTabGroup()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
