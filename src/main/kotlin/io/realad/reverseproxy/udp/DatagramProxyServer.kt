package io.realad.reverseproxy.udp

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.util.concurrent.DefaultThreadFactory
import java.net.InetSocketAddress

class DatagramProxyServer(
    private val port: Int,
    private val threads: Int,
    private val targets: List<InetSocketAddress>,
    private val verbose: Boolean = false
) {
    private val acceptFactory = DefaultThreadFactory("accept")
    private val acceptGroup = NioEventLoopGroup(threads, acceptFactory)

    fun run() {
        Bootstrap().apply {
            group(acceptGroup)
                .channel(NioDatagramChannel::class.java)
                .handler(DatagramProxyInitializer(targets, verbose))
            bind(port).sync()
        }
    }

    fun shutdown() {
        acceptGroup.shutdownGracefully()
    }
}
