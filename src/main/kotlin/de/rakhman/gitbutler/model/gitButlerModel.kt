package de.rakhman.gitbutler.model

import kotlinx.serialization.Serializable


@Serializable
class ButStatus(
    @Serializable
    val unassignedChanges: List<ButAssignedChange>,
    @Serializable
    val stacks: List<ButStack>
)

@Serializable
class ButStack(
    @Serializable
    val assignedChanges: List<ButAssignedChange>,
    @Serializable
    val branches: List<ButBranch>
)

@Serializable
class ButBranch(
    @Serializable
    val name: String
)

@Serializable
class ButAssignedChange(
    @Serializable
    val cliId: String,
    @Serializable
    val filePath: String,
)

@Serializable
class ButError(
    @Serializable
    val message: String
)