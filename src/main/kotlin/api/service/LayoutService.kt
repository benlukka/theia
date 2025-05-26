package com.benlukka.theia.api.service

import api.LayoutUpdate
import com.benlukka.theia.api.Json
import com.benlukka.theia.api.layoutLens
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.util.concurrent.atomic.AtomicReference

object Layout {
    private val currentLayout = AtomicReference(
        LayoutUpdate(
            components = listOf(),
            timestamp = System.currentTimeMillis()
        )
    )

    fun applyMessage(message: LayoutMessage) {
        message.applyTo(this)
    }

    fun get(): LayoutUpdate = currentLayout.get()
    fun toResponse(): Response =
        Response(Status.OK)
            .with(layoutLens of get())
    fun toJson(): String = Json.instance.mapper.writeValueAsString(get())

    fun reset() {
        applyMessage(DirectLayoutMessage(
            LayoutUpdate(
                components = listOf(),
                timestamp = System.currentTimeMillis()
            )
        ))
    }

    // Internal only
     internal fun update(newLayout: LayoutUpdate) {
        println("New layout update received: ${newLayout.components.size} components")
        currentLayout.set(newLayout)
    }

    internal fun updateFromJson(json: String) {
        try {
            val layout = Json.instance.mapper.readValue(json, LayoutUpdate::class.java)
            update(layout)
        } catch (e: Exception) {
            println("Failed to parse layout JSON: ${e.message}")
            throw IllegalArgumentException("Invalid layout JSON format", e)
        }
    }
}

sealed interface LayoutMessage {
    fun applyTo(layout: Layout)
}

data class JsonLayoutMessage(val json: String) : LayoutMessage {
    override fun applyTo(layout: Layout) = layout.updateFromJson(json)
}

data class DirectLayoutMessage(val update: LayoutUpdate) : LayoutMessage {
    override fun applyTo(layout: Layout) = layout.update(update)
}