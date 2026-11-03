package com.jsf.app.pravin.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores per-rep form analysis data. Linked to a WorkoutSession for later
 * display in the session summary and eventual trainer-facing drill-down.
 *
 * formErrors is a JSON array of {message, severity} objects — stored as a string
 * to keep the schema simple. Deserialised in the UI layer using Gson.
 */
@Entity(
    tableName = "rep_form_records",
    indices = [Index("sessionId")]
)
data class RepFormRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val repNumber: Int,
    val exerciseType: String,
    val formErrors: String,    // JSON: [{message, severity}]
    val repScore: Float,
    val timestamp: Long = System.currentTimeMillis()
)
