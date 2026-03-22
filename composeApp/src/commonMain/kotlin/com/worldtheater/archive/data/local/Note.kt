package com.worldtheater.archive.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "notes")
@Serializable
data class Note(
    @PrimaryKey(autoGenerate = true) val _id: Int = 0,
    @ColumnInfo(name = "node_id") val nodeId: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "body") val body: String = "",
    @ColumnInfo(name = "create_time") val createTime: Long = 0L,
    @ColumnInfo(name = "update_time") val updateTime: Long = 0L,
    @ColumnInfo(name = "color") val color: Int = 0,
    @ColumnInfo(name = "item_type") val itemType: Int = 0,
    @ColumnInfo(name = "parent_node_id") val parentNodeId: String? = null,
    @ColumnInfo(name = "is_sensitive") val isSensitive: Boolean = false
)
