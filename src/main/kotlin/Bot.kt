import kotlinx.coroutines.*
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main(args: Array<String>){
 val telegramBotApi = TelegramBotsApi(DefaultBotSession::class.java)
    val bot = CryptoAlertBot()
    telegramBotApi.registerBot(bot)
    runBlocking{
       val c = async{ bot.checkUserAlert() }
       //val u = async{ bot.checkDailyAlert() }
      
    }

}
