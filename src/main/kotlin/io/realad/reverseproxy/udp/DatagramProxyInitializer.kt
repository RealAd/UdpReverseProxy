package io.realad.reverseproxy.udp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.logging.ByteBufFormat
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import java.net.InetSocketAddress

class DatagramProxyInitializer(
    private val nodes: List<InetSocketAddress>,
    private val verbose: Boolean = false
) :
    ChannelInitializer<NioDatagramChannel>() {
    public override fun initChannel(ch: NioDatagramChannel) {
        if (verbose) ch.pipeline().addLast(LoggingHandler(LogLevel.INFO, ByteBufFormat.SIMPLE))
        ch.pipeline().addLast(DatagramProxyInboundHandler(nodes))
    }
}
