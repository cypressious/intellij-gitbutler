package de.rakhman.gitbutler

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.GitBranch
import git4idea.actions.branch.GitSingleBranchAction
import git4idea.repo.GitRepository

class ButApplyAction : BaseButBranchAction("apply")
class ButUnapplyAction : BaseButBranchAction("unapply")
class ButPushAction : BaseButBranchAction("push")
class ButDeleteAction : BaseButBranchAction("delete") {
    override fun buildCommand(reference: GitBranch): List<String> {
        return listOf("but", "branch", "delete", reference.name, "-f")
    }
}

abstract class BaseButBranchAction(
    private val command: String,
) : GitSingleBranchAction() {
    override fun actionPerformed(
        e: AnActionEvent,
        project: Project,
        repositories: List<GitRepository>,
        reference: GitBranch
    ) {
        val vcsRoot = repositories.firstOrNull()?.root?.path ?: return
        val commands = buildCommand(reference)
        runCommandWithProgress(project, vcsRoot, commands)
    }

    protected open fun buildCommand(reference: GitBranch): List<String> {
        return listOf("but", command, reference.name)
    }
}

class ButPrAction : ButBaseBranchActionWithInput("PR Title") {
    override val disabledForRemote: Boolean get() = true

    override fun buildCommand(
        reference: GitBranch,
        answer: String
    ): List<String> = listOf("but", "pr", "new", reference.name, "-m", answer)

    override fun dialogText(reference: GitBranch): String = "Enter PR Title for '${reference.name}'"
}

abstract class ButBaseBranchActionWithInput(val dialogTitle: String) : GitSingleBranchAction() {
    override fun actionPerformed(
        e: AnActionEvent,
        project: Project,
        repositories: List<GitRepository>,
        reference: GitBranch
    ) {
        val vcsRoot = repositories.firstOrNull()?.root?.path ?: return
        val answer = Messages.showInputDialog(
            project,
            dialogText(reference),
            dialogTitle,
            Messages.getQuestionIcon()
        ).orEmpty().ifEmpty { return }

        runCommandWithProgress(project, vcsRoot, buildCommand(reference, answer))
    }

    protected abstract fun buildCommand(
        reference: GitBranch,
        answer: String
    ): List<String>

    protected abstract fun dialogText(reference: GitBranch): String
}


