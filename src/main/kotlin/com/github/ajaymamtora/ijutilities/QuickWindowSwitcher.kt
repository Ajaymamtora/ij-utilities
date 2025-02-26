package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class QuickWindowSwitcher : AnAction() {

    private val overlayKeys = listOf('A', 'S', 'D', 'F', 'Q', 'W', 'E', 'R')
    private val overlayColors = listOf(
        JBColor(Color(232, 78, 64, 60), Color(232, 78, 64, 60)),   // Red - ~24% opaque
        JBColor(Color(46, 134, 193, 60), Color(46, 134, 193, 60)),  // Blue - ~24% opaque
        JBColor(Color(40, 180, 99, 60), Color(40, 180, 99, 60)),   // Green - ~24% opaque
        JBColor(Color(241, 196, 15, 60), Color(241, 196, 15, 60))   // Yellow - ~24% opaque
    )

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editors = fileEditorManager.allEditors

        if (editors.isEmpty()) {
            return
        }

        // Get all editor windows
        val editorWindows = getEditorWindows(editors)
        if (editorWindows.isEmpty()) {
            return
        }

        // Create overlays
        val overlays = createOverlays(editorWindows)
        showOverlays(overlays)

        // Set up key listener to handle selection
        setupKeyListener(project, editorWindows, overlays)
    }

    private fun getEditorWindows(editors: Array<com.intellij.openapi.fileEditor.FileEditor>): List<Pair<JComponent, Rectangle>> {
        val result = mutableListOf<Pair<JComponent, Rectangle>>()

        // Get all editor components and their bounds
        editors.forEach { editor ->
            val component = editor.component
            if (component.isVisible) {
                val bounds = component.bounds

                // Only add visible editors with non-zero bounds
                if (bounds.width > 0 && bounds.height > 0) {
                    // Convert bounds to screen coordinates
                    val locationOnScreen = component.locationOnScreen
                    val screenBounds = Rectangle(locationOnScreen.x, locationOnScreen.y, bounds.width, bounds.height)
                    result.add(Pair(component, screenBounds))
                }
            }
        }

        return result
    }

    private fun createOverlays(editorWindows: List<Pair<JComponent, Rectangle>>): List<JWindow> {
        val overlays = mutableListOf<JWindow>()

        editorWindows.forEachIndexed { index, (_, bounds) ->
            if (index >= overlayKeys.size) return@forEachIndexed

            val key = overlayKeys[index]
            val color = overlayColors[index % overlayColors.size]

            val overlay = JWindow()
            overlay.background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))

            val panel = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Draw semi-transparent background
                    g2d.color = color
                    g2d.fillRect(0, 0, width, height)

                    // Draw key letter
                    g2d.color = JBColor.WHITE
                    val font = Font("Arial", Font.BOLD, height / 3)
                    g2d.font = font

                    val metrics = g2d.fontMetrics
                    val x = (width - metrics.stringWidth(key.toString())) / 2
                    val y = ((height - metrics.height) / 2) + metrics.ascent

                    g2d.drawString(key.toString(), x, y)
                }
            }

            panel.isOpaque = false
            overlay.contentPane = panel
            overlay.setLocation(bounds.x, bounds.y)
            overlay.size = Dimension(bounds.width, bounds.height)

            overlays.add(overlay)
        }

        return overlays
    }

    private fun showOverlays(overlays: List<JWindow>) {
        overlays.forEach { it.isVisible = true }
    }

    private fun hideOverlays(overlays: List<JWindow>) {
        overlays.forEach {
            it.isVisible = false
            it.dispose()
        }
    }

    private fun setupKeyListener(project: Project, editorWindows: List<Pair<JComponent, Rectangle>>, overlays: List<JWindow>) {
        val keyListenerPanel = JPanel()
        keyListenerPanel.isFocusable = true

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(keyListenerPanel, keyListenerPanel)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelKeyEnabled(true)
            .setRequestFocus(true)
            .createPopup()

        keyListenerPanel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val key = e.keyChar.uppercaseChar()
                val index = overlayKeys.indexOf(key)

                if (index != -1 && index < editorWindows.size) {
                    // Hide overlays first to prevent display issues
                    hideOverlays(overlays)
                    popup.cancel()

                    // Focus the selected editor component
                    val (component, _) = editorWindows[index]

                    // Use a series of invokeLater calls to ensure proper focus handling
                    SwingUtilities.invokeLater {
                        try {
                            // Direct method to focus the specific fileEditor
                            val fileEditor = FileEditorManager.getInstance(project).selectedEditor
                            if (fileEditor != null) {
                                val manager = FileEditorManager.getInstance(project)
                                manager.openFile(fileEditor.file, true)
                            }

                            // Direct focus to component
                            component.requestFocusInWindow()

                            // As backup, use IdeFocusManager
                            IdeFocusManager.getInstance(project).requestFocus(component, true)

                            // Extra step to ensure editor cursor visibility
                            SwingUtilities.invokeLater {
                                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                                if (editor != null) {
                                    editor.contentComponent.requestFocusInWindow()
                                    editor.contentComponent.repaint()
                                    editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback if any errors occur
                            component.requestFocus()
                        }
                    }
                } else {
                    // If no valid key pressed, just close overlays
                    hideOverlays(overlays)
                    popup.cancel()
                }
            }
        })

        // Show the invisible popup to capture keyboard input
        val firstComponent = editorWindows.firstOrNull()?.first
        if (firstComponent != null) {
            popup.show(RelativePoint.getNorthWestOf(firstComponent))

            // Make sure we get focus
            SwingUtilities.invokeLater {
                keyListenerPanel.requestFocusInWindow()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null
    }
}
