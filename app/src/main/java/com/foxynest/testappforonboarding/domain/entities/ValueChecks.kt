package com.foxynest.testappforonboarding.domain.entities

/** "null" – наследие старых версий, где String.valueOf(null) попадал в хранилище. */
fun String?.isNotBlankValue(): Boolean = !isNullOrBlank() && this != "null"