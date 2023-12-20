package com.example.kursovaya

import java.io.Serializable

data class Message(
    val text: String,
    val date: String,
    val audioPath: String
) : Serializable