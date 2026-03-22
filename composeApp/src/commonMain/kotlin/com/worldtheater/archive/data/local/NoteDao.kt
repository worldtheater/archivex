package com.worldtheater.archive.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes order by _id DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes order by _id DESC")
    fun allNotes(): Flow<List<Note>>

    @Query(
        "SELECT _id, node_id, title, CASE WHEN is_sensitive = 1 THEN '' ELSE substr(body, 1, :previewChars) END AS body, create_time, update_time, color, item_type, parent_node_id, is_sensitive " +
                "FROM notes ORDER BY _id DESC"
    )
    fun allNotesForList(previewChars: Int): Flow<List<Note>>

    @Query(
        "SELECT _id, node_id, title, CASE WHEN is_sensitive = 1 THEN '' ELSE substr(body, 1, :previewChars) END AS body, create_time, update_time, color, item_type, parent_node_id, is_sensitive " +
                "FROM notes ORDER BY item_type DESC, _id DESC LIMIT :limit"
    )
    suspend fun getRecentNotesForList(previewChars: Int, limit: Int): List<Note>

    @Query(
        "SELECT _id, node_id, title, CASE WHEN is_sensitive = 1 THEN '' ELSE substr(body, 1, :previewChars) END AS body, create_time, update_time, color, item_type, parent_node_id, is_sensitive " +
                "FROM notes WHERE ((:parentNodeId IS NULL AND parent_node_id IS NULL) OR parent_node_id = :parentNodeId) " +
                "ORDER BY item_type DESC, _id DESC LIMIT :limit"
    )
    suspend fun getRecentNotesForListByParent(
        parentNodeId: String?,
        previewChars: Int,
        limit: Int
    ): List<Note>

    @Query(
        "SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR (is_sensitive = 0 AND body LIKE '%' || :query || '%') ORDER BY _id DESC"
    )
    fun searchNotes(query: String): Flow<List<Note>>

    @Query(
        "SELECT _id, node_id, title, CASE WHEN is_sensitive = 1 THEN '' ELSE substr(body, 1, :previewChars) END AS body, create_time, update_time, color, item_type, parent_node_id, is_sensitive " +
                "FROM notes WHERE title LIKE '%' || :query || '%' OR (is_sensitive = 0 AND body LIKE '%' || :query || '%') ORDER BY _id DESC"
    )
    fun searchNotesForList(query: String, previewChars: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE _id = (:id)")
    suspend fun loadById(id: Int): List<Note>

    @Query(
        "SELECT _id, node_id, title, CASE WHEN is_sensitive = 1 THEN '' ELSE body END AS body, create_time, update_time, color, item_type, parent_node_id, is_sensitive " +
                "FROM notes WHERE _id = (:id)"
    )
    suspend fun loadByIdForView(id: Int): List<Note>

    @Insert
    suspend fun add(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE _id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET is_sensitive = :isSensitive, update_time = :updateTime WHERE _id = :id")
    suspend fun updateSensitiveById(id: Int, isSensitive: Boolean, updateTime: Long)

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(_id), 0) FROM notes")
    suspend fun maxId(): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAll(): Int

}
