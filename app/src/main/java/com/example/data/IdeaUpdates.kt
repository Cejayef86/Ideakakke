package com.example.data

import com.example.model.Idea
import com.google.firebase.firestore.FieldValue

// The editor owns these fields only; research and team membership have separate writes.
internal fun ideaDetailUpdates(idea: Idea): Map<String, Any?> = hashMapOf<String, Any?>(
    "title" to idea.title.trim(),
    "description" to idea.description.trim(),
    "category" to idea.category.trim(),
    "tags" to idea.tags,
    "priority" to idea.priority,
    "matrixQuadrant" to idea.matrixQuadrant,
    "isPublic" to idea.isPublic,
    "updatedAt" to FieldValue.serverTimestamp()
)
