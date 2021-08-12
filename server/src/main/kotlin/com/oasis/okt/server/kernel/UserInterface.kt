package com.oasis.okt.server.kernel

import io.ktor.application.*
import io.ktor.auth.*

interface UserInterface : Principal

@Suppress("unused")
inline fun <reified U : UserInterface> ApplicationCall.getUser(): U? = principal()