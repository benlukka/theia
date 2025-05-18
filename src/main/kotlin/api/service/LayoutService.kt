package com.benlukka.theia.api.service

import LayoutUpdate
import com.benlukka.theia.api.Json
import java.nio.file.Files
import java.nio.file.Paths

object LayoutService {
    private var currentLayout: LayoutUpdate = LayoutUpdate(components = listOf(), timestamp = 0L)

    val layoutUpdateLens = Json.instance.autoBody<LayoutUpdate>().toLens()

    init {
        if (currentLayout.timestamp == 0L) {
            val json = String(Files.readAllBytes(Paths.get("src/main/resources/layouts/default.json")))
            currentLayout = Json.instance.mapper.readValue(json, LayoutUpdate::class.java)
        }
    }

    fun overrideLayout(newContent: LayoutUpdate) {
        println("New layout update received: ${newContent.components.size} components")
        currentLayout = newContent
    }

    fun getCurrentLayout(): LayoutUpdate = currentLayout
}