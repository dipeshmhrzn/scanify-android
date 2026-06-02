package com.scanify.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fileType: String,
    val fileSize: String,
    val filePath: String,
    val createdAtTimeStamp: Long,
    val isImageBundle: Boolean
)