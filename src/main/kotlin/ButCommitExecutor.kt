package de.rakhman.gitbutler

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.*
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import de.rakhman.gitbutler.model.ButStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

private val jsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

class ButCommitExecutor : CommitExecutor {
    override fun getActionText(): @Nls String {
        return "but commit"
    }

    override fun areChangesRequired(): Boolean {
        return true
    }

    override fun supportsPartialCommit(): Boolean {
        return false
    }

    override fun createCommitSession(commitContext: CommitContext): CommitSession {
        return object : CommitSession {
            override fun execute(changes: Collection<Change>, commitMessage: @NlsSafe String?) {
                val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
                val filePath = ChangesUtil.getFilePath(changes.first())
                val vcsRoot =
                    ProjectLevelVcsManager.getInstance(project).getVcsRootFor(filePath)?.path
                        ?: return

                val commitMessageFilePath =
                    commitMessage?.takeUnless { it.isBlank() }?.let { message ->
                        Files.createTempFile("commit_message", null).also {
                            it.writeText(message)
                        }?.absolutePathString()
                    }

                project.lifetime.coroutineScope.launch {
                    withBackgroundProgress(project, "Running but commit") {
                        reportSequentialProgress { reporter ->
                            reporter.indeterminateStep("Getting File IDs")

                            val output = runCommand(vcsRoot, listOf("but", "status", "--json"))
                            if (output.exitCode != 0) return@reportSequentialProgress
                            val butStatus = jsonIgnoreUnknownKeys.decodeFromString<ButStatus>(output.stdout)

                            val idsByFile = buildMap {
                                for (stack in butStatus.stacks) {
                                    for (change in stack.assignedChanges) {
                                        put(change.filePath, change.cliId)
                                    }
                                }
                            }

                            reporter.indeterminateStep("Committing")

                            withContext(Dispatchers.EDT) {
                                val terminalWidget = TerminalToolWindowManager.getInstance(project)
                                    .createShellWidget(vcsRoot, "but commit", true, true)
                                terminalWidget.sendCommandToExecute(buildString {
                                    append("but commit ")

                                    for (change in changes) {
                                        val virtualFile = change.virtualFile ?: continue
                                        val path = virtualFile.path.removePrefix(vcsRoot).removePrefix("/")
                                        append("-p ")
                                        append(idsByFile.getOrElse(path) { path })
                                        append(" ")
                                    }

                                    if (commitMessageFilePath != null) {
                                        append("--message-file=\"")
                                        append(commitMessageFilePath)
                                        append("\"")
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
