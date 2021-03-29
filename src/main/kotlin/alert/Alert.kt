package alert

import kotlinx.serialization.Contextual
import kotlinx.serialization.*
import java.time.LocalTime



@Serializable
data class DailyAlert(
    @Contextual
    val localTime: LocalTime,
    val coinId: String,
    val userId: Long
)


@Serializable
data class UserAlert(
    val price: Double,
    val coinId: String,
    val userId: Long
)

