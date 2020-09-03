package io.realad.reverseproxy.udp

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.net.InetSocketAddress

data class Route(
    val origin: InetSocketAddress,
    val target: InetSocketAddress
) {
    val createdAt: Long = System.currentTimeMillis()
}

class LaunchArgs(parser: ArgParser) {
    val verbose by parser.flagging(
        "-v", "--verbose",
        help = "enable verbose mode"
    ).default(false)

    val port by parser.storing(
        "-p", "--port",
        help = "port of the proxy"
    ) { toInt() }.default(5300)

    val threads by parser.storing(
        "--threads",
        help = "threads of the proxy"
    ) { toInt() }.default(0)

    val targets by parser.adding(
        "-t", "--target",
        help = "address of the target"
    )
}

/**
 * Key - target address
 * Value - route data
 */
val routeMap = hashMapOf<Int, Route>()

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::LaunchArgs).run {
        println("Udp server port $port defined.")
        targets.forEach { println("Target $it is defined.") }
        DatagramProxyServer(
            port = port,
            threads = threads,
            targets = targets.map {
                InetSocketAddress(
                    it.substringBefore(':'),
                    it.substringAfter(':').toInt()
                )
            },
            verbose = verbose
        ).run()
    }
}
