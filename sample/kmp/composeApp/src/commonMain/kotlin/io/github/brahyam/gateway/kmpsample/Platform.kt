package io.github.brahyam.gateway.kmpsample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform