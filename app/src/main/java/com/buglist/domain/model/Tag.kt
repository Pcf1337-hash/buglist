package com.buglist.domain.model

/**
 * Domain model representing a label/tag that can be attached to [DebtEntry] records.
 *
 * Tags are user-defined short strings (max. 20 characters) shown as chips in the
 * debt entry UI. Default tags are pre-seeded at first app launch.
 *
 * @param id        Auto-generated primary key (0 = not yet persisted).
 * @param name      Display name — max. 20 characters.
 * @param createdAt Record creation timestamp (Unix ms).
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
