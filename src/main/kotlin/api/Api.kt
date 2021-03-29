package api


import alert.DailyAlert
import alert.UserAlert
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class DB {
    companion object {
        val client = KMongo.createClient().coroutine
        val database = client.getDatabase("coinbase")
        val coinCollection = database.getCollection<Coin>()
        val userAlertCollection = database.getCollection<UserAlert>()
        val dailyAlert = database.getCollection<DailyAlert>()
    }
}


@Serializable
data class Coin(val id: String, val symbol: String, val name: String)

val endpoint = "https://api.coingecko.com/api/v3/simple/price?ids="
val vs = "&vs_currencies=usd&include_24hr_change=true"
val jsonClient = HttpClient {
    install(JsonFeature) { serializer = JacksonSerializer() }
}

suspend fun getCoinPrice(name: String): Map<String, Map<String, Double>> {
    return jsonClient.get(urlString = endpoint + name + vs)
}

suspend fun nameInList(nameOrSymbol: String): String? {
    return DB.coinCollection.findOne(
        or(
            Coin::id eq nameOrSymbol,
            Coin::name eq nameOrSymbol, Coin::symbol eq nameOrSymbol
        )
    )?.id
}

suspend fun existUserAlert(userId: Long, coinId: String, price: Double = 0.0): Boolean {
    return DB.userAlertCollection.findOne(
        and(
            UserAlert::coinId eq coinId,
            UserAlert::userId eq userId,
            or(UserAlert::price eq price)
        )
    ) != null
}

suspend fun justOnce(): List<Coin> {
    return jsonClient.get(urlString = "https://api.coingecko.com/api/v3/coins/list")
}

