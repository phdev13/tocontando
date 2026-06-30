package com.phdev.quantofalta.core.database

enum class SyncState {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    CONFLICT,
    FAILED
}
