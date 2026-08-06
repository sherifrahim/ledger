package com.sherif.ledger.feature.update

/** A GitHub Release newer than the running build (see CheckForUpdateUseCase). */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
)
