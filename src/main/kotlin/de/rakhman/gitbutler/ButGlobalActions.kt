package de.rakhman.gitbutler

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.actions.branch.GitBranchActionsUtil

abstract class ButBaseGlobalAction(
    private val enabledInWorkspace: Boolean
) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repositories = GitBranchActionsUtil.getAffectedRepositories(e)
        val vcsRoot = repositories.firstOrNull()?.root?.path ?: return
        actionPerformed(project, vcsRoot)
    }

    protected abstract fun actionPerformed(project: Project, vcsRoot: String)

    override fun update(e: AnActionEvent) {
        val repositories = GitBranchActionsUtil.getAffectedRepositories(e)
        e.presentation.isEnabledAndVisible = repositories.any { isWorkspaceBranch(it.currentBranch?.name) } == enabledInWorkspace
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

class ButPullAction : ButBaseGlobalAction(enabledInWorkspace = true) {
    override fun actionPerformed(project: Project, vcsRoot: String) {
        runCommandWithProgress(project, vcsRoot, listOf("but", "pull"))
    }
}

class ButBranchAction : ButBaseGlobalAction(enabledInWorkspace = true) {
    override fun actionPerformed(project: Project, vcsRoot: String) {
        val answer = Messages.showInputDialog(
            project,
            "Enter branch name.",
            "New Virtual Branch",
            Messages.getQuestionIcon()
        ).orEmpty().ifEmpty { return }

        runCommandWithProgress(project, vcsRoot, listOf("but", "branch", "new", answer))
    }
}

class ButSetupAction : ButBaseGlobalAction(enabledInWorkspace = false) {
    override fun actionPerformed(project: Project, vcsRoot: String) {
        runCommandWithProgress(project, vcsRoot, listOf("but", "setup"))
    }
}

class ButTeardownAction : ButBaseGlobalAction(enabledInWorkspace = true) {
    override fun actionPerformed(project: Project, vcsRoot: String) {
        runCommandWithProgress(project, vcsRoot, listOf("but", "teardown"))
    }
}
