package de.rakhman.gitbutler

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.swing.ListSelectionModel
import kotlin.coroutines.resume

fun runCliAndWait(workingDir: String, command: List<String>): ProcessOutput {
    val cmd = GeneralCommandLine(command)
        .withWorkDirectory(workingDir)
        .withCharset(StandardCharsets.UTF_8)

    val handler = CapturingProcessHandler(cmd)
    return handler.runProcess()
}

suspend fun showPopup(title: String, options: List<String>, project: Project): String? {
    return withContext(Dispatchers.EDT) {
        suspendCancellableCoroutine { continuation ->
            val popup = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(options)
                .setTitle(title)
                .setItemChosenCallback { selected ->
                    continuation.resume(selected)
                }
                .addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        if (!event.isOk) {
                            continuation.resume(null)
                        }
                    }
                })
                .setNamerForFiltering { it }
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .createPopup()

            popup.showCenteredInCurrentWindow(project)

            continuation.invokeOnCancellation { popup.cancel() }
        }
    }
}
