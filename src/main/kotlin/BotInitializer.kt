import alert.DailyAlert
import alert.UserAlert
import api.DB
import api.getCoinGeckoPrice
import api.getCryptoCompareListPrice
import configurer.BotConfigurer
import handler.CommandHandler
import handler.defaultReply
import handler.sendMessageBuilding
import handler.unknownMessage
import kotlinx.coroutines.*
import org.litote.kmongo.eq
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CryptoAlertBot(
    private val commandHandler: CommandHandler = CommandHandler()
) : TelegramLongPollingBot() {

    override fun getBotToken() = BotConfigurer.botToken

    override fun getBotUsername() = BotConfigurer.botName

    override fun onUpdateReceived(update: Update) {
        when (update.hasMessage()) {
            update.message.isCommand -> caughtMessage(
                commandHandler.commandStrategy(
                    update.message
                )
            )
            update.message.hasText() -> caughtMessage(
                update.message.defaultReply()
            )
            else -> caughtMessage(update.message.unknownMessage())
        }
    }


    private fun caughtMessage(sendMessage: SendMessage) {
        try {
            execute(
                sendMessage
            )
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }

    }

    fun checkDailyAlert() {
        GlobalScope.async(Dispatchers.Default){
            while (true) {
                println("checkDailyAlert")
                delay(60000)
                val dailyAlertList = DB.dailyAlert.find(
                    DailyAlert::localTime eq LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                ).toList()
                if (!dailyAlertList.isNullOrEmpty()) {
                    dailyAlertList.forEach {
                        val c = getCoinGeckoPrice(it.coinId)
                        println(c.size)
                        val text = "current ${it.coinId} price in usd is $" + "${c[it.coinId]?.get("usd")}"
                        caughtMessage(sendMessageBuilding(it.userId.toString(), text))
                    }
                }
            }
        }
    }

    fun checkUserAlert() {
        GlobalScope.async(Dispatchers.Default) {
            while (true) {
                delay(60000)
                println("checkUserAlert")
                var coins = DB.userAlertCollection.distinct(UserAlert::coinSymbol).toList().joinToString(",")
                val userAlert = DB.userAlertCollection.find().toList()
                if (!userAlert.isNullOrEmpty()) {
                    var coinList = getCryptoCompareListPrice(coins)
                    userAlert.forEach {
                        var text = "Current ${it.coinId} price is ${coinList[it.coinSymbol.toUpperCase()]?.get("USD")} which is "
                        if (it.lowerOrUpper == "lower" && coinList[it.coinSymbol.toUpperCase()]?.get("USD")!! < it.price) {
                            text += "lower than ${it.price}"
                            caughtMessage(sendMessageBuilding(it.userId.toString(), text))
                        } else if (it.lowerOrUpper == "bigger" && coinList[it.coinSymbol.toUpperCase()]?.get("USD")!! > it.price ) {
                            text += "bigger than ${it.price}"
                        }

                    }
                }
            }
        }
    }
}



