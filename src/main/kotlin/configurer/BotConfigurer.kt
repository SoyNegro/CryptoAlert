package configurer

object BotConfigurer{
    const val botName = "btcpursuerbot"
    const val botToken = "1719170423:AAFUUd3BZI4Scii0dYkBMo4kK3Ed03VHY1o"
}

object BotCommand{
    private const val initCommandChar = '/'
    val start = initCommandChar + "start"
    val help = initCommandChar + "help"
    val price = initCommandChar + "p"
    val alertLower = initCommandChar + "al"
    val alertHigher = initCommandChar+"ah"
    val daily = initCommandChar + "d"
    val listalert = initCommandChar + "la"
    val listdaily = initCommandChar + "ld"
    val deleteAlert = initCommandChar + "da"
    val deleteDaily = initCommandChar + "dd"
    val about = initCommandChar + "about"
    val donate = initCommandChar + "donate"
    val disclaimer = initCommandChar + "disclaimer"
}

