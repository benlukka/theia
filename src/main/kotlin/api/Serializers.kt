package com.benlukka.theia.api

import LayoutUpdate
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.http4k.format.Jackson

object Json {
    init {
        Jackson.mapper.registerKotlinModule()
    }
    val instance = Jackson
}

// Lens to extract LayoutUpdate from request bodies
val layoutLens = Json.instance.autoBody<LayoutUpdate>().toLens()