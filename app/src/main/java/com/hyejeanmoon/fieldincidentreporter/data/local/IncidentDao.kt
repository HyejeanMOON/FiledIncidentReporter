package com.hyejeanmoon.fieldincidentreporter.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/** Rows are listed newest first, matching the server's `ORDER BY created_at DESC, id DESC`. */
@Dao
internal abstract class IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC, id DESC")
    abstract fun pagingSource(): PagingSource<Int, IncidentEntity>

    @Upsert
    abstract suspend fun upsert(incidents: List<IncidentEntity>)

    @Query("DELETE FROM incidents WHERE id IN (:ids)")
    abstract suspend fun delete(ids: List<String>)

    @Query("SELECT * FROM incidents WHERE id = :id")
    abstract suspend fun get(id: String): IncidentEntity?

    /** Rows after the [afterCreatedAt] position, or from the top when it is null, through the last position. */
    @Query(
        "SELECT * FROM incidents " +
            "WHERE (:afterCreatedAt IS NULL OR (createdAt, id) < (:afterCreatedAt, :afterId)) " +
            "AND (createdAt, id) >= (:lastCreatedAt, :lastId)",
    )
    abstract suspend fun range(afterCreatedAt: Long?, afterId: String?, lastCreatedAt: Long, lastId: String): List<IncidentEntity>

    /** Whether any row is at the given position or after it in list order. */
    @Query("SELECT EXISTS(SELECT 1 FROM incidents WHERE (createdAt, id) <= (:createdAt, :id))")
    abstract suspend fun hasRowsFrom(createdAt: Long, id: String): Boolean

    /** Deletes the rows after the given position, or every row when [createdAt] is null. */
    @Query("DELETE FROM incidents WHERE :createdAt IS NULL OR (createdAt, id) < (:createdAt, :id)")
    abstract suspend fun deleteAfter(createdAt: Long?, id: String?)

    /**
     * Makes the cached rows covered by one server [page] match it, writing only what changed.
     * [previous] is the last report of the page before, or null for the first page. A page covers
     * the rows after [previous] through its own last report, and the last page also every row after it.
     * Returns whether rows from the page's last report onward were already cached.
     */
    @Transaction
    open suspend fun syncPage(page: List<IncidentEntity>, previous: IncidentEntity?, isLastPage: Boolean): Boolean {
        val last = page.lastOrNull()
        val cachedFromLast = last != null && hasRowsFrom(last.createdAt, last.id)
        if (last != null) {
            val changes = pageChanges(range(previous?.createdAt, previous?.id, last.createdAt, last.id), page)
            if (changes.upserts.isNotEmpty()) upsert(changes.upserts)
            if (changes.deletedIds.isNotEmpty()) delete(changes.deletedIds)
        }
        if (isLastPage) {
            val end = last ?: previous
            deleteAfter(end?.createdAt, end?.id)
        }
        return cachedFromLast
    }

    /**
     * Replaces the cached copy of one report with a fresh server [incident]. A report that is not cached
     * is not added, since the reports between it and the cached ones would be missing from the list.
     */
    @Transaction
    open suspend fun refreshCached(incident: IncidentEntity) {
        val cached = get(incident.id) ?: return
        if (incident.replaces(cached)) upsert(listOf(incident))
    }
}

/** The writes that make the cached rows within one server page's span match that page. */
internal data class PageChanges(val upserts: List<IncidentEntity>, val deletedIds: List<String>)

internal fun pageChanges(cached: List<IncidentEntity>, page: List<IncidentEntity>): PageChanges {
    val cachedById = cached.associateBy(IncidentEntity::id)
    val pageIds = page.mapTo(HashSet(), IncidentEntity::id)
    return PageChanges(
        upserts = page.filter { row ->
            val old = cachedById[row.id]
            old == null || row.replaces(old)
        },
        // A cached row inside the page's span that the server no longer returns was deleted there.
        deletedIds = cached.map(IncidentEntity::id).filterNot(pageIds::contains),
    )
}

/** Identical copies are skipped, and a newer copy saved by this app is not replaced by an older response. */
private fun IncidentEntity.replaces(cached: IncidentEntity) = this != cached && updatedAt >= cached.updatedAt
