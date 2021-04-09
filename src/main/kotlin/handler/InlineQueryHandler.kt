package main.kotlin.handler

import api.getCoinGeckoPrice
import api.nameInList
import handler.inlineKeyboardMarkup
import kotlinx.coroutines.runBlocking
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle

class InlineQueryHandler {

    fun onPriceInlineQuery(inlineQuery: InlineQuery): AnswerInlineQuery{
        var inlineQueryResultArticle = InlineQueryResultArticle("unkown","Unkown",InputTextMessageContent("Coin not found"))
        runBlocking {
            val coinGecko = nameInList(inlineQuery.query)
            val c = coinGecko?.id?.let { getCoinGeckoPrice(it) }
            if (coinGecko != null){
                inlineQueryResultArticle = InlineQueryResultArticle.builder()
                    .id(inlineQuery.id)
                    .title(coinGecko.symbol)
                    .inputMessageContent(
                        InputTextMessageContent
                            .builder()
                            .messageText("Current ${coinGecko.name} price in usd is $" + "${
                        c?.get(coinGecko.id)?.get("usd")}" +
                            "\n Search powered by <a href = 'https://coingecko.com'>Coingecko</a>")
                            .disableWebPagePreview(true)
                            .parseMode(ParseMode.HTML)
                            .build())
                    .replyMarkup(inlineKeyboardMarkup(coinGecko.id))
                    .thumbHeight(1)
                    .description("Get current price")
                    .build()

            }
        }
        val resultArticle = mutableListOf<InlineQueryResult>()
        resultArticle.add(inlineQueryResultArticle)

        return AnswerInlineQuery(inlineQuery.id,resultArticle)
    }

}