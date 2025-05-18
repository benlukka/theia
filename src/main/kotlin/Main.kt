package com.benlukka.theia

import com.benlukka.theia.api.api
import com.benlukka.theia.api.generateOpenApiSpec
import com.benlukka.theia.api.corsFilter
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.io.File

fun main(args: Array<String>) {
    // Check if we're running in spec generation mode
    if (args.isNotEmpty() && args[0] == "generateSpecToFile") {
        // Generate spec directly to a file without starting a server
        val outputPath = if (args.size > 1) args[1] else "build/openapi/swagger.json"
        val spec = generateOpenApiSpec().description.asJsonObject().toString()
        println("Generating spec $spec")
        File(outputPath).apply {
            parentFile.mkdirs()
            writeText(spec)
            println("📚 Generated OpenAPI spec to ${absolutePath}")
        }
        return
    }

    // Normal server operation
    val server: Http4kServer = corsFilter.then(api).asServer(Undertow(8080))
    server.start()
    println("⚡️ DashboardService HTTP server running on port 8080")
    println("📚 API documentation available at http://localhost:8080/docs/swagger.json")
}