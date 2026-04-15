package de.rakhman.gitbutler.model

import kotlinx.serialization.Serializable


@Serializable
class ButStatus(
    @Serializable
    val stacks: List<ButStack>
)

@Serializable
class ButStack(
    @Serializable
    val assignedChanges: List<ButAssignedChange>
)

@Serializable
class ButAssignedChange(
    @Serializable
    val cliId: String,
    @Serializable
    val filePath: String,
)
