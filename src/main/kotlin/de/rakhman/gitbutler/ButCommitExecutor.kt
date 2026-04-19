package de.rakhman.gitbutler

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.*
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import de.rakhman.gitbutler.model.ButStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Nls

private val jsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

class ButCommitExecutor : CommitExecutor {
    override fun getActionText(): @Nls String {
        return "Commit to Virtual Branch"
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
            withBackgroundProgress(project, "Committing to Virtual Branch") {
                reportSequentialProgress { reporter ->
                    reporter.indeterminateStep("Checking Workspace")
                    if (!isCurrentBranchWorkspace(vcsRoot)) {
                        notifyError(project, "Commit to Virtual Branch is only available in workspace mode.")
                        return@reportSequentialProgress
                    }

                    reporter.indeterminateStep("Getting Status")

                    val status = executeButStatus(vcsRoot) ?: return@reportSequentialProgress

                    val idsByFile = getFileIds(status)
                    val branches = status.stacks.flatMap { it.branches.map { it.name } }

                    val branch = if (branches.size > 1) {
                        showPopup("Select Virtual Branch for Commit", branches, project) ?: return@reportSequentialProgress
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
                        notifyError(project, result.stdout.ifEmpty { "Process exited with code ${result.exitCode}." })
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

    private fun isCurrentBranchWorkspace(vcsRoot: String): Boolean {
        val output = runCliAndWait(vcsRoot, listOf("git", "branch", "--show-current"))
        return output.exitCode == 0 && isWorkspaceBranch(output.stdout)
    }
}
