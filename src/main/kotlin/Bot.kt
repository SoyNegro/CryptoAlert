import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main(args: Array<String>){
 val telegramBotApi = TelegramBotsApi(DefaultBotSession::class.java)
    val bot = CryptoAlertBot()
    telegramBotApi.registerBot(bot)
    bot.checkDailyAlert()
    bot.checkUserAlert()
}