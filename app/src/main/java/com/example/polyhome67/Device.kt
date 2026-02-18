package com.example.polyhome67

data class Device(
    val id: String,
    val type: String,
    val availableCommands: List<String>,
    val opening: Int?,
    val power: Int?
)
