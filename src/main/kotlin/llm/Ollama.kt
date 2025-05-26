package com.benlukka.theia.llm

import api.LayoutUpdate
import api.TextComponent
import com.benlukka.theia.llm.Tools.Sport
import com.benlukka.theia.api.service.Layout
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.ollama.OllamaChatModel
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration

fun main() {
    val model = OllamaChatModel.builder()
        .modelName("qwen3:8b")
        .baseUrl("http://192.168.178.198:11343")
        .responseFormat(ResponseFormat.JSON)
        .timeout(Duration.ofMinutes(5))
        .build()
println(getResponseFormat())
    val systemPrompt = """
You are an API-driven assistant. You must always respond with a JSON object that strictly matches the following schema:

${getResponseFormat()}

**Rules:**
- The top-level object must have a `timestamp` (current time in milliseconds) and a `components` array.
- Each component in `components` must have a `type` field, which can only be `"chart"`, `"text"`, or `"animation"`.
    - For `"chart"`: include `id`, `chartType` (only `"bar"` allowed), and `data` (must match the schema, no extra fields).
    - For `"text"`: include `id` and `text`.
    - For `"animation"`: include `id`, `animationName`, and a required `params` object.
- Do not include any fields or types not defined in the schema.
- Do not output explanations, natural language, or extra text—only the JSON object.
- When using a Tool, call it and then transform its output into the required JSON schema before responding.
- Never return raw API or tool responses.
- If you cannot produce a valid response, return a single `"text"` component with an error message.

**Strictly follow the schema and these rules. Any deviation will cause a system error.**
""".trimIndent()
    val userPrompt = "wie oft war ich insgesamt im fitx".trimIndent()

    fun getLLMResponse(messages: List<ChatMessage>): String {
        val toolSpecs = ToolSpecifications.toolSpecificationsFrom(Sport)
        var currentMessages = messages
        while (true) {
            val response = model.chat(
                ChatRequest.builder()
                    .toolSpecifications(toolSpecs)
                    .messages(currentMessages)
                    .build()
            )
            val aiMessage = response.aiMessage()
            if (aiMessage?.text() != null) {
                return aiMessage.text()
            }
            val toolRequests = aiMessage?.toolExecutionRequests()
            if (!toolRequests.isNullOrEmpty()) {
                val toolRequest = toolRequests.first()
                val toolResult = Sport.getCheckinHistory()
                println("Tool result: $toolResult") // Debug: log tool output
                currentMessages = currentMessages + dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                    toolRequest, toolResult
                )
            } else {
                error("No AI message text and no tool requests in response")
            }
        }
    }

    val initialMessages = listOf(
        SystemMessage.systemMessage(systemPrompt),
        UserMessage.from(userPrompt)
    )

    var llmResponse = getLLMResponse(initialMessages)
    var success = false
    var retryCount = 0
    val maxRetries = 5

    while (!success && retryCount <= maxRetries) {
        try {
            val url = URL("http://127.0.0.1:8080/update")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream).use { it.write(llmResponse) }
            println("LLM response: $llmResponse") // Debug: log LLM output
            val responseCode = connection.responseCode
            println("Backend response: $responseCode")
            connection.inputStream.close()
            success = true
        } catch (e: Exception) {
            println("Failed to parse LLM response: ${e.message}")
            e.printStackTrace()
            retryCount++
            if (retryCount <= maxRetries) {
                val errorPrompt = "The previous JSON response could not be processed due to this error: ${e.message}. Please try again and ensure the response strictly matches the required JSON schema."
                val retryMessages = initialMessages + UserMessage.from(errorPrompt)
                llmResponse = getLLMResponse(retryMessages)
            } else {
                val fallbackLayout = LayoutUpdate(
                    timestamp = System.currentTimeMillis(),
                    components = listOf(
                        TextComponent(
                            id = "error-message",
                            text = "Failed to parse LLM response"
                        )
                    )
                )
                Layout.update(fallbackLayout)
            }
        }
    }
}