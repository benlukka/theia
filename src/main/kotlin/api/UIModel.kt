// src/main/kotlin/api/UIModel.kt
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ChartComponent::class, name = "chart"),
    JsonSubTypes.Type(TextComponent::class, name = "text"),
    JsonSubTypes.Type(AnimationComponent::class, name = "animation")
)
sealed class UIComponent {
    abstract val id: String
}

data class ChartComponent(
    override val id: String,
    val chartType: String,
    val data: Map<String, Number>
) : UIComponent()

data class TextComponent(
    override val id: String,
    val text: String
) : UIComponent()

data class AnimationComponent(
    override val id: String,
    val animationName: String,
    val params: Map<String, Any?>
) : UIComponent()

data class LayoutUpdate(
    val timestamp: Long,
    val components: List<UIComponent>
)