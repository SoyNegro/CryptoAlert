package alert

import kotlinx.serialization.Contextual
import kotlinx.serialization.*
import java.time.LocalTime



@Serializable
data class DailyAlert(
    val coinId: String,
    val userId: Long,
    val localTime: String,

)


@Serializable
data class UserAlert(
    val price: Double,
    val coinId: String,
    val coinSymbol:String,
    val userId: Long,
    val lowerOrUpper: String
)

