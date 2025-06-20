package io.github.brahyam.gateway.client

import io.ktor.client.engine.HttpClientEngine

internal expect fun createPinnedEngine(): HttpClientEngine
