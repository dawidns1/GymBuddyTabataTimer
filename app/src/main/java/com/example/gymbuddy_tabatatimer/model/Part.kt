package com.example.gymbuddy_tabatatimer.model

import java.io.Serializable
import java.util.*

class Part(
    var id: Int=0,
    var name: String = "TabataPart",
    var type: String = "Exercise",
    var duration: Int = 5,
    var imgID: Int = 0,
    var increment: Int = 0,
    var setBreak: Int = 0
) : Serializable
