package com.benlukka.theia.llm


import api.*
import com.benlukka.theia.api.Json
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.full.findAnnotation
import com.fasterxml.jackson.annotation.JsonSubTypes
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


fun getResponseFormat(): String {
    val mapper = Json.instance.mapper

    fun placeholderForType(type: KClass<*>): Any? = when (type) {
        String::class -> "<String>"
        Int::class -> 0
        Long::class -> 0L
        Double::class -> 0.0
        Float::class -> 0.0f
        Number::class -> 0
        Boolean::class -> true
        else -> null
    }

    fun buildExample(clazz: KClass<*>): Any? {
        // Handle UIComponent subtypes
        if (clazz == UIComponent::class) {
            val subTypes = UIComponent::class.findAnnotation<JsonSubTypes>()?.value.orEmpty()
            return subTypes.mapNotNull { subType ->
                when (subType.value) {
                    ChartComponent::class -> buildExample(ChartComponent::class)
                    TextComponent::class -> buildExample(TextComponent::class)
                    AnimationComponent::class -> buildExample(AnimationComponent::class)
                    else -> null
                }
            }
        }

        // Handle lists
        if (clazz == List::class) return listOf("<...>")

        // Handle data classes
        val ctor = clazz.primaryConstructor ?: return null
        val args = ctor.parameters.associateWith { param ->
            val type = param.type.classifier as? KClass<*>
            when {
                type == null -> null
                type == List::class -> {
                    val argType = param.type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
                    if (argType != null) listOf(buildExample(argType)) else listOf("<...>")
                }
                type == Map::class -> mapOf("required" to "field")
                type.java.isEnum -> type.java.enumConstants?.firstOrNull()?.toString() ?: "<Enum>"
                placeholderForType(type) != null -> placeholderForType(type)
                else -> buildExample(type)
            }
        }
        return ctor.callBy(args)
    }

    // Build example LayoutUpdate
    val example = LayoutUpdate(
        timestamp = System.currentTimeMillis(),
        components = buildExample(UIComponent::class) as List<UIComponent>
    )

    // Serialize with placeholders
    val node = mapper.valueToTree<ObjectNode>(example)
    // Replace timestamp with placeholder
    node.put("timestamp", "<Long>")
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
}