package api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// Base interface for UI Components with Jackson polymorphism
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ChartComponent::class, name = "chart"),
    JsonSubTypes.Type(value = TextComponent::class, name = "text"),
    JsonSubTypes.Type(value = AnimationComponent::class, name = "animation")
)
sealed class UIComponent {
    abstract val id: String
}

data class ChartDataPoint(
    val title: String,
    val value: Double,
    val unit: String
)

data class ChartDataset(
    val data: List<ChartDataPoint>,
    val borderColor: String
)

data class ChartComponent(
    override val id: String,
    val chartType: String,
    val data: ChartDataset
) : UIComponent()

data class TextComponent(
    override val id: String,
    val text: String
) : UIComponent()

data class AnimationComponent(
    override val id: String,
    val animationName: String,
    val params: AnimationParams
) : UIComponent()

data class AnimationParams(
    val duration: Int,
    val delay: Int,
    val easing: String
)

data class LayoutUpdate(
    val timestamp: Long,
    val components: List<UIComponent>
)