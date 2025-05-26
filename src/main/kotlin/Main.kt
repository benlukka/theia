package com.benlukka.theia

import api.LayoutUpdate
import com.benlukka.theia.api.Json
import com.benlukka.theia.api.service.DirectLayoutMessage
import com.benlukka.theia.api.service.Layout
import org.http4k.contract.*
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.ApiServer
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.lens.BiDiBodyLens
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import io.sentry.Sentry


fun main() {
    Sentry.init { options ->
        options.dsn = "https://ee192e480fa3d8f55d6d9398271b7208@o4509379436150789.ingest.de.sentry.io/4509386242654288"
        options.isDebug = true
    }
    val layoutLens: BiDiBodyLens<LayoutUpdate> = Json.instance.autoBody<LayoutUpdate>().toLens()
    val stringLens: BiDiBodyLens<String> = Json.instance.autoBody<String>().toLens()

    val getLayout: ContractRoute = "/api/layout-update" meta {
        summary     = "Get layout values"
        operationId = "getLayout"
        tags += Tag("Layout")
        returning(OK, layoutLens to Layout.get())
    } bindContract GET to { _: Request ->
        // handler returns a Response
        Response(OK).with(layoutLens of Layout.get())
    }

    val update: ContractRoute = "/update" meta {
        summary     = "update endpoint"
        operationId = "update"
        tags += Tag("Layout")
        receiving(layoutLens)
        // just register example responses, not lambdas
        returning(OK,         stringLens to "pong")
        returning(Status.BAD_REQUEST, stringLens to "Invalid request")
    } bindContract POST to { req: Request ->
        runCatching {
            val update = layoutLens(req)
            DirectLayoutMessage(update).applyTo(Layout)
            "pong"
        }.fold(
            onSuccess = { res -> Response(OK).with(stringLens of res) },
            onFailure = { err -> Response(Status.BAD_REQUEST).with(stringLens of "Invalid request: ${err.message}") }
        )
    }

    val app = contract {
        renderer = OpenApi3(
            ApiInfo(
                title       = "Layout Dashboard API",
                description = "API for managing dynamic dashboard layouts and components",
                version     = "1.0.0"
            ),
            Jackson,
            servers = listOf(ApiServer(Uri.of("http://localhost:8080"), "Local development server"))
        )
        descriptionPath = "/openapi.json"
        routes += getLayout
        routes += update
    }

    // 5) start
    val server: Http4kServer = app.asServer(Jetty(8080))
    server.start()
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Stopping server")
        server.stop()
        println("Server stopped")
    })
}
