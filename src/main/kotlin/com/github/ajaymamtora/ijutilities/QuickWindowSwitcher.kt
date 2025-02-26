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
import javax.swing.JDialog

class QuickWindowSwitcher : AnAction() {

    private val overlayKeys = listOf('A', 'S', 'D', 'F', 'Q', 'W', 'E', 'R')
    private val overlayColors = listOf(
        JBColor(Color(232, 78, 64, 25), Color(232, 78, 64, 25)),   // Red - ~10% opaque
        JBColor(Color(46, 134, 193, 25), Color(46, 134, 193, 25)),  // Blue - ~10% opaque
        JBColor(Color(40, 180, 99, 25), Color(40, 180, 99, 25)),   // Green - ~10% opaque
        JBColor(Color(241, 196, 15, 25), Color(241, 196, 15, 25))   // Yellow - ~10% opaque
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
                    try {
                        // Check if component is showing before getting screen location
                        if (component.isShowing) {
                            // Convert bounds to screen coordinates
                            val locationOnScreen = component.locationOnScreen
                            val screenBounds = Rectangle(locationOnScreen.x, locationOnScreen.y, bounds.width, bounds.height)
                            result.add(Pair(component, screenBounds))
                        }
                    } catch (e: IllegalComponentStateException) {
                        // Skip this component if we can't get its screen location
                    }
                }
            }
        }

        return result
    }

    private fun createOverlays(editorWindows: List<Pair<JComponent, Rectangle>>): List<JDialog> {
        val overlays = mutableListOf<JDialog>()

        editorWindows.forEachIndexed { index, (_, bounds) ->
            if (index >= overlayKeys.size) return@forEachIndexed

            val key = overlayKeys[index]
            val color = overlayColors[index % overlayColors.size]

            // Create undecorated dialog for better transparency support
            val overlay = JDialog()
            overlay.isUndecorated = true
            overlay.background = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))

            // Make entire dialog much more transparent (30% opacity)
            overlay.opacity = 0.3f

            val panel = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // Set panel to be completely transparent initially
                    g2d.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR)
                    g2d.fillRect(0, 0, width, height)

                    // Create a thin border with the color
                    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
                    g2d.color = color
                    g2d.drawRect(0, 0, width - 1, height - 1)
                    g2d.drawRect(1, 1, width - 3, height - 3)

                    // Create small very transparent label area at the bottom
                    val labelHeight = height / 12
                    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f)
                    g2d.fillRect(0, height - labelHeight, width, labelHeight)

                    // Draw key letter with high contrast
                    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
                    g2d.color = JBColor.WHITE

                    val fontSize = labelHeight / 2
                    val font = Font("Arial", Font.BOLD, fontSize)
                    g2d.font = font

                    val metrics = g2d.fontMetrics
                    val x = (width - metrics.stringWidth(key.toString())) / 2
                    val y = height - (labelHeight / 2) + (metrics.height / 4)

                    // Add slight shadow to make the letter more visible
                    g2d.color = JBColor.BLACK
                    g2d.drawString(key.toString(), x + 1, y + 1)

                    g2d.color = JBColor.WHITE
                    g2d.drawString(key.toString(), x, y)
                }
            }

            panel.isOpaque = false
            overlay.contentPane = panel
            overlay.setLocation(bounds.x, bounds.y)
            overlay.size = Dimension(bounds.width, bounds.height)

            // Make sure it stays on top
            overlay.isAlwaysOnTop = true
            // Make it not focusable to avoid stealing focus
            overlay.focusableWindowState = false

            overlays.add(overlay)
        }

        return overlays
    }

    private fun showOverlays(overlays: List<JDialog>) {
        overlays.forEach { it.isVisible = true }
    }

    private fun hideOverlays(overlays: List<JDialog>) {
        overlays.forEach {
            it.isVisible = false
            it.dispose()
        }
    }

    private fun setupKeyListener(project: Project, editorWindows: List<Pair<JComponent, Rectangle>>, overlays: List<JDialog>) {
        val keyListenerPanel = JPanel()
        keyListenerPanel.isFocusable = true

        // Create a special key adapter to handle escape before creating the popup
        val keyAdapter = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                // Handle Escape key
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    hideOverlays(overlays)
                    SwingUtilities.getWindowAncestor(keyListenerPanel)?.dispose()
                    return
                }

                val key = e.keyChar.uppercaseChar()
                val index = overlayKeys.indexOf(key)

                if (index != -1 && index < editorWindows.size) {
                    // Hide overlays first to prevent display issues
                    hideOverlays(overlays)
                    SwingUtilities.getWindowAncestor(keyListenerPanel)?.dispose()

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
                    SwingUtilities.getWindowAncestor(keyListenerPanel)?.dispose()
                }
            }
        }

        // Add the key listener before creating the popup
        keyListenerPanel.addKeyListener(keyAdapter)

        // Create the popup without modifying cancel key properties
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(keyListenerPanel, keyListenerPanel)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setRequestFocus(true)
            .createPopup()

        // Alternative approach to handle escape key using key dispatcher
        keyListenerPanel.registerKeyboardAction(
            {
                hideOverlays(overlays)
                popup.cancel()
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )

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
