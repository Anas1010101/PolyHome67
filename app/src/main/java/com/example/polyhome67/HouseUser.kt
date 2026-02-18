package com.example.polyhome67

data class HouseUser(
    val userLogin: String?,
    val owner: Int
) {
    fun safeLogin(): String = userLogin?.takeIf { it.isNotBlank() } ?: "(login inconnu)"
    fun isOwner(): Boolean = owner == 1
}
