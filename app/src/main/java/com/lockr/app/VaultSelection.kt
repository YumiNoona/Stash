package com.lockr.app

/** Selection mode is independent of its count: entering never silently selects records. */
data class VaultSelection(val active: Boolean = false, val ids: Set<String> = emptySet()) {
    fun enter() = copy(active = true)
    fun toggle(id: String) = copy(active = true, ids = if (id in ids) ids - id else ids + id)
    fun all(visible: Set<String>) = copy(active = true, ids = if (visible.isNotEmpty() && ids.containsAll(visible)) ids - visible else visible)
    fun reconcile(visible: Set<String>) = copy(ids = ids.intersect(visible))
}
