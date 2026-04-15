package de.rakhman.gitbutler

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.ui.Messages
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
        return ButCommitSession()
    }
}

class ButCommitSession : CommitSession {
    override fun execute(changes: Collection<Change>, commitMessage: @NlsSafe String?) {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val filePath = ChangesUtil.getFilePath(changes.first())
        val vcsRoot =
            ProjectLevelVcsManager.getInstance(project).getVcsRootFor(filePath)?.path
                ?: return

        if (commitMessage == null) return

        val coroutineScope = project.lifetime.coroutineScope
        coroutineScope.launch {
            withBackgroundProgress(project, "Running but commit") {
                reportSequentialProgress { reporter ->
                    reporter.indeterminateStep("Getting File IDs")

                    val status = executeButStatus(vcsRoot) ?: return@reportSequentialProgress

                    val idsByFile = getFileIds(status)
                    val branches = status.stacks.flatMap { it.branches.map { it.name } }

                    val branch = if (branches.size > 1) {
                        showPopup("Select Branch for Commit", branches, project) ?: return@reportSequentialProgress
                    } else {
                        branches.singleOrNull() ?: return@reportSequentialProgress
                    }

                    reporter.indeterminateStep("Committing")

                    val command = buildList {
                        add("but")
                        add("commit")

                        for (change in changes) {
                            val virtualFile = change.virtualFile ?: continue
                            val path =
                                virtualFile.path.removePrefix(vcsRoot).removePrefix("/")
                            add("-p")
                            add(idsByFile.getOrElse(path) { path })
                        }

                        add("--message")
                        add(commitMessage)
                        add(branch)
                    }

                    val result = runCliAndWait(vcsRoot, command)

                    if (result.exitCode != 0) {
                        withContext(Dispatchers.EDT) {
                            Messages.showErrorDialog(project, result.stdout, "GitButler")
                        }
                    }
                }
            }
        }
    }

    private fun getFileIds(status: ButStatus): Map<String, String> {
        return buildMap {
            for (change in status.unassignedChanges) {
                put(change.filePath, change.cliId)
            }
            for (stack in status.stacks) {
                for (change in stack.assignedChanges) {
                    put(change.filePath, change.cliId)
                }
            }
        }
    }

    private fun executeButStatus(vcsRoot: String): ButStatus? {
        val output = runCliAndWait(vcsRoot, listOf("but", "status", "--json"))
        return if (output.exitCode == 0) {
            jsonIgnoreUnknownKeys.decodeFromString<ButStatus>(output.stdout)
        } else {
            null
        }
    }
}
