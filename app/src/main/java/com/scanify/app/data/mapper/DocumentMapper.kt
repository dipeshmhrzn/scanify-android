package com.scanify.app.data.mapper

import com.scanify.app.data.local.entity.DocumentEntity
import com.scanify.app.domain.model.Document
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun DocumentEntity.toDomain(): Document = Document(
    id = id,
    name = name,
    fileType = fileType,
    fileSize = fileSize,
    filePath = filePath,
    createdAt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(createdAtTimeStamp),
        ZoneId.systemDefault()
    ),
    isImageBundle= isImageBundle
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    name = name,
    fileType = fileType,
    fileSize = fileSize,
    filePath = filePath,
    createdAtTimeStamp = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    isImageBundle = isImageBundle
)