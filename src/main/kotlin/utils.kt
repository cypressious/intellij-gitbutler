package de.rakhman.gitbutler

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import java.nio.charset.StandardCharsets

fun runCommand(workingDir: String, command: List<String>): ProcessOutput {
    val cmd = GeneralCommandLine(command)
        .withWorkDirectory(workingDir)
        .withCharset(StandardCharsets.UTF_8)

    val handler = CapturingProcessHandler(cmd)
    return handler.runProcess()
}