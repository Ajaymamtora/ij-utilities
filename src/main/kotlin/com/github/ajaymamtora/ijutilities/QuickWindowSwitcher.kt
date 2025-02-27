package com.github.ajaymamtora.ijutilities

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*


class QuickWindowSwitcher : AnAction() {

    // Add a companion object to track active instances
    companion object {
        private var activeOverlays: List<JDialog>? = null
        private var activePopup: JBPopup? = null

        // Method to dispose of any active window pickers
        private fun disposeActiveOverlays() {
            activeOverlays?.let { overlays ->
                overlays.forEach {
                    it.isVisible = false
                    it.dispose()
                }
                activeOverlays = null
            }

            activePopup?.cancel()
            activePopup = null
        }
    }

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
        // First dispose of any active window pickers
        disposeActiveOverlays()

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
        activeOverlays = overlays  // Store in companion object
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
        activeOverlays = null  // Clear the static reference
    }

    private fun forceFocusComponentWithMultipleMethods(project: Project, component: JComponent) {
        // Use FileEditorManagerEx which has more functionality
        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)

        // Find which file editor contains our component
        var targetFile: com.intellij.openapi.vfs.VirtualFile? = null

        // Step 1: Find the file that contains our component
        for (file in fileEditorManager.openFiles) {
            for (editor in fileEditorManager.getAllEditors(file)) {
                if (isComponentInHierarchy(editor.component, component)) {
                    targetFile = file
                    break
                }
            }
            if (targetFile != null) break
        }

        // Track focus attempts for debugging
        val attemptsCounter = AtomicInteger(0)
        val maxAttempts = 5

        // Schedule multiple focus attempts with different strategies
        val timer = Timer(50, null)
        timer.addActionListener { _ ->
            val attempt = attemptsCounter.incrementAndGet()

            // Stop after max attempts
            if (attempt > maxAttempts) {
                timer.stop()
                return@addActionListener
            }

            // Run on UI thread to ensure proper focus handling
            ApplicationManager.getApplication().invokeLater {
                try {
                    when (attempt) {
                        1 -> {
                            // First attempt: Direct component focus
                            component.requestFocusInWindow()
                            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
                                IdeFocusManager.getGlobalInstance().requestFocus(component, true)
                            }
                        }
                        2 -> {
                            // Second attempt: Try to open the file if we found it
                            if (targetFile != null) {
                                fileEditorManager.openFile(targetFile, true)

                                // Also try to force active status
                                fileEditorManager.currentWindow?.setAsCurrentWindow(true)
                            }
                        }
                        3 -> {
                            // Third attempt: Get the text editor and focus directly on its content component
                            val textEditor = fileEditorManager.selectedEditors.firstOrNull { it is TextEditor } as? TextEditor
                            if (textEditor != null) {
                                textEditor.editor.contentComponent.requestFocusInWindow()
                                textEditor.editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                            }
                        }
                        4 -> {
                            // Fourth attempt: Ensure no tool windows are active
                            ToolWindowManager.getInstance(project).activeToolWindowId?.let { windowId ->
                                ToolWindowManager.getInstance(project).getToolWindow(windowId)?.hide(null)
                            }

                            // Then refocus editor
                            IdeFocusManager.getInstance(project).requestFocus(component, true)
                        }
                        5 -> {
                            // Fifth attempt: Try UI-level focus (this is our most aggressive approach)
                            val window = SwingUtilities.getWindowAncestor(component)
                            window?.toFront()

                            SwingUtilities.invokeLater {
                                // Find all focusable components and try them in sequence
                                findAllFocusableComponents(component).forEach { focusableComponent ->
                                    SwingUtilities.invokeLater {
                                        focusableComponent.requestFocusInWindow()
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    // Move to the next attempt if an exception occurs
                }
            }
        }

        // Set timer to repeat and start it
        timer.initialDelay = 50
        timer.delay = 200
        timer.isRepeats = true
        timer.start()
    }



    private fun isComponentInHierarchy(parent: Component, target: Component): Boolean {
        if (parent === target) return true

        if (parent is Container) {
            for (i in 0 until parent.componentCount) {
                if (isComponentInHierarchy(parent.getComponent(i), target)) {
                    return true
                }
            }
        }

        return false
    }

    private fun findDeepestFocusableComponent(component: Component): Component? {
        if (component is Container) {
            for (i in 0 until component.componentCount) {
                val child = component.getComponent(i)
                val deepChild = findDeepestFocusableComponent(child)

                if (deepChild != null) {
                    return deepChild
                }
            }

            // If no focusable children, check if this container is focusable
            if (component.isFocusable) {
                return component
            }
        } else if (component.isFocusable) {
            return component
        }

        return null
    }

    private fun findAllFocusableComponents(component: Component): List<Component> {
        val result = mutableListOf<Component>()

        if (component.isFocusable) {
            result.add(component)
        }

        if (component is Container) {
            for (i in 0 until component.componentCount) {
                result.addAll(findAllFocusableComponents(component.getComponent(i)))
            }
        }

        return result
    }

    private fun setupKeyListener(project: Project, editorWindows: List<Pair<JComponent, Rectangle>>, overlays: List<JDialog>) {
        val keyListenerPanel = JPanel()
        keyListenerPanel.isFocusable = true

        val escapeAction = ActionListener {
            hideOverlays(overlays)
            activePopup?.cancel()
            activePopup = null
        }

        val keyAdapter = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    escapeAction.actionPerformed(null)
                    e.consume()
                    return
                }

                val key = e.keyChar.uppercaseChar()
                val index = overlayKeys.indexOf(key)

                if (index != -1 && index < editorWindows.size) {
                    // Hide overlays immediately
                    hideOverlays(overlays)
                    activePopup?.cancel()
                    activePopup = null

                    // Get the component we want to focus
                    val (component, _) = editorWindows[index]

                    // Try to determine which file and editor window this component belongs to
                    forceFocusComponentWithMultipleMethods(project, component)
                } else if (e.keyCode != KeyEvent.VK_SHIFT &&
                    e.keyCode != KeyEvent.VK_CONTROL &&
                    e.keyCode != KeyEvent.VK_ALT) {
                    escapeAction.actionPerformed(null)
                }
            }
        }

        // Rest of the method remains the same...
        keyListenerPanel.addKeyListener(keyAdapter)

        val inputMap = keyListenerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = keyListenerPanel.actionMap

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeAction")
        actionMap.put("escapeAction", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                escapeAction.actionPerformed(e)
            }
        })

        keyListenerPanel.registerKeyboardAction(
            escapeAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(keyListenerPanel, keyListenerPanel)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setRequestFocus(true)
            .createPopup()

        activePopup = popup

        popup.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
            override fun onClosed(jbPopup: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                hideOverlays(overlays)
                activePopup = null
            }
        })

        val firstComponent = editorWindows.firstOrNull()?.first
        if (firstComponent != null) {
            popup.show(RelativePoint.getNorthWestOf(firstComponent))
            keyListenerPanel.requestFocusInWindow()
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
