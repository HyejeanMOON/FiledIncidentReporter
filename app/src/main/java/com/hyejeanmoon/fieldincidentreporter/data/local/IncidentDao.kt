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

    /** Rows from the first position through the last one in list order; a null [firstCreatedAt] starts at the top. */
    @Query(
        "SELECT * FROM incidents " +
            "WHERE (:firstCreatedAt IS NULL OR createdAt < :firstCreatedAt OR (createdAt = :firstCreatedAt AND id <= :firstId)) " +
            "AND (createdAt > :lastCreatedAt OR (createdAt = :lastCreatedAt AND id >= :lastId))",
    )
    abstract suspend fun range(firstCreatedAt: Long?, firstId: String?, lastCreatedAt: Long, lastId: String): List<IncidentEntity>

    /** Whether any row is at the given position or after it in list order. */
    @Query("SELECT EXISTS(SELECT 1 FROM incidents WHERE createdAt < :createdAt OR (createdAt = :createdAt AND id <= :id))")
    abstract suspend fun hasRowsFrom(createdAt: Long, id: String): Boolean

    @Query("DELETE FROM incidents WHERE createdAt < :createdAt OR (createdAt = :createdAt AND id < :id)")
    abstract suspend fun deleteAfter(createdAt: Long, id: String)

    @Query("DELETE FROM incidents")
    abstract suspend fun deleteAll()

    /**
     * Makes the cached rows covered by one server [page] match it, writing only what changed.
     * [previous] is the last report of the page before, or null for the first page.
     * Returns whether rows from the page's last report onward were already cached.
     */
    @Transaction
    open suspend fun syncPage(page: List<IncidentEntity>, previous: IncidentEntity?, isLastPage: Boolean): Boolean {
        val last = page.lastOrNull()
        val cachedFromLast = last != null && hasRowsFrom(last.createdAt, last.id)
        if (last != null) {
            // The first page covers everything above its last report; a later page covers only its own span.
            val first = page.first().takeIf { previous != null }
            val changes = pageChanges(range(first?.createdAt, first?.id, last.createdAt, last.id), page)
            if (changes.upserts.isNotEmpty()) upsert(changes.upserts)
            if (changes.deletedIds.isNotEmpty()) delete(changes.deletedIds)
        }
        if (isLastPage) {
            // Nothing follows the server's last report.
            val end = last ?: previous
            if (end == null) deleteAll() else deleteAfter(end.createdAt, end.id)
        }
        return cachedFromLast
    }
}

/** The writes that make the cached rows within one server page's span match that page. */
internal data class PageChanges(val upserts: List<IncidentEntity>, val deletedIds: List<String>)

internal fun pageChanges(cached: List<IncidentEntity>, page: List<IncidentEntity>): PageChanges {
    val cachedById = cached.associateBy(IncidentEntity::id)
    val pageIds = page.mapTo(HashSet(), IncidentEntity::id)
    return PageChanges(
        // Identical rows are skipped, and a newer copy saved by this app is not replaced by an older response.
        upserts = page.filter { row ->
            val old = cachedById[row.id]
            old == null || old != row && row.updatedAt >= old.updatedAt
        },
        // A cached row inside the page's span that the server no longer returns was deleted there.
        deletedIds = cached.map(IncidentEntity::id).filterNot(pageIds::contains),
    )
}
