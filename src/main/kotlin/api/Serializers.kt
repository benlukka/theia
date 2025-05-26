package com.benlukka.theia.api

import api.AnimationComponent
import api.ChartComponent
import api.LayoutUpdate
import api.TextComponent
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.http4k.format.Jackson
import org.http4k.lens.BiDiBodyLens

object Json {
    init {
        Jackson.mapper.registerKotlinModule()

        Jackson.mapper.registerSubtypes(
            NamedType(ChartComponent::class.java, "chart"),
            NamedType(TextComponent::class.java, "text"),
            NamedType(AnimationComponent::class.java, "animation")
        )
    }
    val instance = Jackson
}

val layoutLens: BiDiBodyLens<LayoutUpdate> = Json.instance.autoBody<LayoutUpdate>().toLens()

