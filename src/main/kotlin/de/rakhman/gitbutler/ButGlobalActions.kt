package de.rakhman.gitbutler

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.actions.branch.GitBranchActionsUtil

abstract class ButBaseGlobalAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repositories = GitBranchActionsUtil.getAffectedRepositories(e)
        val vcsRoot = repositories.firstOrNull()?.root?.path ?: return
        actionPerformed(project, vcsRoot)
    }

    protected abstract fun actionPerformed(project: Project, vcsRoot: String)
}

class ButPullAction : ButBaseGlobalAction() {
    override fun actionPerformed(project: Project, vcsRoot: String) {
        runCommandWithProgress(project, vcsRoot, listOf("but", "pull"))
    }
}

class ButBranchAction : ButBaseGlobalAction() {
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

