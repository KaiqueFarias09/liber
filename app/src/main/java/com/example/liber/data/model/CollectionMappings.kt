package com.example.liber.data.model

data class Collection(
    val id: Long,
    val name: String,
    val createdAt: Long,
)

fun CollectionEntity.toDomain() = Collection(
    id = id,
    name = name,
    createdAt = createdAt,
)

fun Collection.toEntity() = CollectionEntity(
    id = id,
    name = name,
    createdAt = createdAt,
)
