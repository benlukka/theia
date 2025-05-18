package com.benlukka.theia.api

import com.benlukka.theia.api.service.LayoutService
import com.benlukka.theia.api.service.LayoutService.getCurrentLayout
import com.benlukka.theia.api.service.LayoutService.layoutUpdateLens
import org.http4k.contract.*
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.routing.*
import java.io.File


fun generateOpenApiSpec(): ContractRoutingHttpHandler {
    val apiInfo = ApiInfo(
        title = "Layout Dashboard API",
        version = "1.0.0",
        description = "API for managing dashboard layouts"
    )

    val openApi = OpenApi3(apiInfo, Jackson)
    return contract {
        renderer = openApi
        descriptionPath = "/swagger.json"

        // Add your existing routes
        routes += layoutUpdatePostRoute
        routes += layoutUpdateGetRoute
    }
}

val corsPolicy = CorsPolicy(
    originPolicy = OriginPolicy.AnyOf("http://localhost:3000"),
    headers = listOf("Content-Type", "Accept", "Authorization"),
    methods = listOf(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.OPTIONS),
    credentials = true
)

val corsFilter = ServerFilters.Cors(corsPolicy)

val layoutUpdateGetRoute = "/layout-update" meta {
    summary = "get layout values"
    returning(Status.OK to "Successfully retrieved layout")
} bindContract Method.GET to { req ->
    println("Layout update requested${getCurrentLayout()}")
    Response(Status.OK)
        .with(layoutUpdateLens of getCurrentLayout())
}
// Your existing route definitions remain unchanged
val layoutUpdatePostRoute = "/layout-update" meta {
    summary = "receive a layout update"
    receiving(layoutUpdateLens)
    returning(Status.OK)
} bindContract Method.POST to { req ->
    LayoutService.overrideLayout(layoutUpdateLens(req))
    Response(Status.OK)
}



val api = corsFilter.then(
    routes(
        "/api" bind routes(
            generateOpenApiSpec()
        ),
        "/docs" bind routes(
            "/swagger.json" bind Method.GET to {
                Response(Status.OK)
            },
            "/" bind Method.GET to {
                Response(Status.TEMPORARY_REDIRECT)
                    .header("Location", "/docs/swagger.json")
            }
        ),
        singlePageApp(ResourceLoader.Directory("frontend/build"))
    )
)

