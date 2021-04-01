package api


import alert.DailyAlert
import alert.UserAlert
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class DB {
    companion object {
        val client = KMongo.createClient().coroutine
        val database = client.getDatabase("coinbase")
        val coinCollection = database.getCollection<CoinGecko>()
        val userAlertCollection = database.getCollection<UserAlert>()
        val dailyAlert = database.getCollection<DailyAlert>()
    }
}

@Serializable
data class CoinGecko(var id: String, val symbol:String,val name: String,)

val coinGeckoEndpoint = "https://api.coingecko.com/api/v3/simple/price?ids="
val coinGeckoApiOptions = "&vs_currencies=usd&include_24hr_change=true"
val cryptoCompareApi = "https://min-api.cryptocompare.com/data/pricemulti?fsyms="
val cryptoCompareApiKey = "0e5bf504e0aeb598bbcf92aaa9503bc8c88edea178161bd87ae17133268ca726"
val cryptoCompareOpt = "&tsyms=USD&api_key="
val jsonClient = HttpClient {
    install(JsonFeature) { serializer = JacksonSerializer() }
}

suspend fun getCoinGeckoPrice(name: String): Map<String, Map<String, Double>> {
    return jsonClient.get(urlString = coinGeckoEndpoint + name + coinGeckoApiOptions)
}

suspend fun getCryptoCompareListPrice(coinIdList: String):Map<String, Map<String, Double>>{
    return jsonClient.get(urlString = cryptoCompareApi+coinIdList+ cryptoCompareOpt+ cryptoCompareApiKey)
}

suspend fun nameInList(nameOrSymbol: String): CoinGecko?{
    return DB.coinCollection.findOne(
        or(
            CoinGecko::id eq nameOrSymbol,
            CoinGecko::name eq nameOrSymbol, CoinGecko::symbol eq nameOrSymbol
        )
    )
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

suspend fun existDailyAlert(userId: Long, coinId: String):Boolean{
    return DB.dailyAlert.findOne(
             DailyAlert::coinId eq coinId,
             DailyAlert::userId eq userId
    ) != null
}

suspend fun justOnce(): List<CoinGecko> {
    return jsonClient.get(urlString = "https://api.coingecko.com/api/v3/coins/list")
}

