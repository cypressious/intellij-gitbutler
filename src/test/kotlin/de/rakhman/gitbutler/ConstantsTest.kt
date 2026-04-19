package de.rakhman.gitbutler

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstantsTest {
    @Test
    fun `workspace branch check accepts exact branch name`() {
        assertTrue(isWorkspaceBranch(BRANCH_NAME_GITBUTLER_WORKSPACE))
    }

    @Test
    fun `workspace branch check trims branch output`() {
        assertTrue(isWorkspaceBranch("  $BRANCH_NAME_GITBUTLER_WORKSPACE\n"))
    }

    @Test
    fun `workspace branch check rejects other branch names`() {
        assertFalse(isWorkspaceBranch("main"))
    }
}
