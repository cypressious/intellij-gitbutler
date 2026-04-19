package de.rakhman.gitbutler

const val BRANCH_NAME_GITBUTLER_WORKSPACE = "gitbutler/workspace"

fun isWorkspaceBranch(branchName: String?): Boolean {
    return branchName?.trim() == BRANCH_NAME_GITBUTLER_WORKSPACE
}
