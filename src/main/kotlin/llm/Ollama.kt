package com.benlukka.theia.llm

import api.LayoutUpdate
import api.TextComponent
import com.benlukka.theia.api.service.Layout
import dev.langchain4j.agent.tool.Tool
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.ollama.OllamaChatModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class DateTool {
    @Tool("Get the current weekday in German")
    fun getCurrentWeekday(): String {
        val today = LocalDate.now()
        val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        println("DateTool executed: Heute ist $weekday")
        return "Heute ist $weekday"
    }
}

fun main() {
    // Remove JSON response format to allow proper tool calling
    val model = OllamaChatModel.builder()
        .modelName("qwen3:8b")
        .baseUrl("http://192.168.178.198:11343")
        // Don't force JSON format initially - let tools work first
        .timeout(Duration.ofMinutes(5))
        .build()

    println("=== TOOL SETUP ===")
    val dateToolInstance = DateTool()
    val toolSpecs = ToolSpecifications.toolSpecificationsFrom(dateToolInstance)

    println("Registered tools: ${toolSpecs.size}")
    toolSpecs.forEach { spec ->
        println("Tool: ${spec.name()} - ${spec.description()}")
    }

    // Two-phase system prompt: First get tool data, then format as JSON
    val systemPrompt = """
You are a helpful assistant with access to tools.

Available tools:
- getCurrentWeekday(): Returns the current weekday in German

INSTRUCTION:
When the user asks for today’s weekday, you MUST:
1. Call getCurrentWeekday() immediately.
2. After receiving the tool result, respond with NOTHING but a single valid JSON object that exactly matches the schema below—no thoughts, no explanations, no extra text.

SCHEMA:
${getResponseFormat()}

RULES:
- Do not include any narrative or chain-of-thought. 
- Do not include any text outside the JSON.
- Use the current timestamp in milliseconds.
- Include the tool’s returned string in a TextComponent with id "weekday-info".
- Do not output your thought process or reasoning process.
Example:
If getCurrentWeekday() returns "Heute ist Mittwoch", respond with:
{
  "timestamp": 1717180800000,
  "components": [
    {
      "type": "text",
      "id": "weekday-info",
      "text": "Heute ist Mittwoch"
    }
  ]
}
""".trimIndent()

    val userPrompt = "welcher wochentag ist heute"

    fun getLLMResponse(messages: List<ChatMessage>): String {
        var currentMessages = messages
        var iterationCount = 0
        val maxIterations = 10
        var toolWasCalled = false

        while (iterationCount < maxIterations) {
            iterationCount++
            println("\n=== ITERATION $iterationCount ===")

            val response = model.chat(
                ChatRequest.builder()
                    .toolSpecifications(toolSpecs)
                    .messages(currentMessages)
                    .build()
            )

            val aiMessage = response.aiMessage()
            println("AI Response:")
            println("- Text: ${aiMessage?.text()}")
            println("- Tool requests: ${aiMessage?.toolExecutionRequests()?.size ?: 0}")

            // Handle tool execution requests
            val toolRequests = aiMessage?.toolExecutionRequests()
            if (!toolRequests.isNullOrEmpty()) {
                println("=== EXECUTING TOOLS ===")
                toolWasCalled = true

                for (toolRequest in toolRequests) {
                    println("Calling tool: ${toolRequest.name()}")

                    val toolResult = when (toolRequest.name()) {
                        "getCurrentWeekday" -> {
                            dateToolInstance.getCurrentWeekday()
                        }
                        else -> "Unknown tool: ${toolRequest.name()}"
                    }

                    println("Tool result: $toolResult")
                    currentMessages = currentMessages + ToolExecutionResultMessage.from(toolRequest, toolResult)
                }

                // Continue to get the formatted response
                continue
            }

            // Check if we have a text response
            val responseText = aiMessage?.text()?.removeHtmlTags()
            if (responseText != null && responseText.trim().isNotEmpty()) {
                println("Final response received")

                // If tool was called and we have a response, it should be JSON
                if (toolWasCalled) {
                    println("Tool was called, expecting JSON response")
                    return responseText
                }

                // If no tool was called but we have text, the model might have skipped the tool
                println("WARNING: Tool was not called, but we have a response")
                return responseText
            }

            println("No response received, continuing...")
        }

        // Fallback if no proper response
        println("Max iterations reached, creating fallback response")
        return """{
  "timestamp": ${System.currentTimeMillis()},
  "components": [
    {
      "type": "text",
      "id": "error",
      "text": "Could not determine current weekday"
    }
  ]
}"""
    }

    // Alternative approach: Try with a more direct prompt
    fun getLLMResponseDirect(messages: List<ChatMessage>): String {
        println("\n=== TRYING DIRECT APPROACH ===")

        // First, manually call the tool to get the weekday
        val weekdayResult = dateToolInstance.getCurrentWeekday()
        println("Manual tool call result: $weekdayResult")

        // Now ask LLM to format it as JSON
        val formatPrompt = """
Format this information as JSON matching the required schema:
Information: "$weekdayResult"

Required JSON format:
${getResponseFormat()}

Put the weekday information in a text component. Respond with ONLY the JSON, no other text.
""".trimIndent()

        val response = model.chat(
            ChatRequest.builder()
                .messages(listOf(
                    SystemMessage.systemMessage("You format information as JSON. Respond only with valid JSON matching the given schema."),
                    UserMessage.from(formatPrompt)
                ))
                .build()
        )

        return response.aiMessage()?.text() ?: """{
  "timestamp": ${System.currentTimeMillis()},
  "components": [
    {
      "type": "text", 
      "id": "weekday",
      "text": "$weekdayResult"
    }
  ]
}"""
    }

    val initialMessages = listOf(
        SystemMessage.systemMessage(systemPrompt),
        UserMessage.from(userPrompt)
    )

    println("\n=== STARTING CONVERSATION ===")
    println("User prompt: $userPrompt")

    // Try the tool-based approach first
    var llmResponse = getLLMResponse(initialMessages)

    // If that didn't work well, try the direct approach
    if (llmResponse.isBlank() || !llmResponse.trim().startsWith("{")) {
        println("\n=== TOOL APPROACH FAILED, TRYING DIRECT APPROACH ===")
        llmResponse = getLLMResponseDirect(initialMessages)
    }

    var success = false
    var retryCount = 0
    val maxRetries = 3

    while (!success && retryCount <= maxRetries) {
        try {
            // Validate it's proper JSON before sending
            if (!llmResponse.trim().startsWith("{")) {
                throw Exception("Response is not JSON format")
            }

            val url = URL("http://127.0.0.1:8080/update")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream).use { it.write(llmResponse) }
            println("LLM response sent: $llmResponse")

            val responseCode = connection.responseCode
            println("Backend response code: $responseCode")

            if (responseCode == 200) {
                success = true
            } else {
                throw Exception("Backend returned error code: $responseCode")
            }

            connection.inputStream.close()
        } catch (e: Exception) {
            println("Failed to send response: ${e.message}")
            retryCount++

            if (retryCount <= maxRetries) {
                // Try the direct approach as fallback
                llmResponse = getLLMResponseDirect(initialMessages)
            } else {
                println("All retries failed, using fallback")
                val fallbackLayout = LayoutUpdate(
                    timestamp = System.currentTimeMillis(),
                    components = listOf(
                        TextComponent(
                            id = "error-message",
                            text = "Failed to get weekday information"
                        )
                    )
                )
                Layout.update(fallbackLayout)
            }
        }
    }
}
fun String.removeHtmlTags(): String {
    val doc: Document = Jsoup.parse(this, "", Parser.xmlParser())
     doc.select("think").forEach(Element::remove)
    return doc.html()
}