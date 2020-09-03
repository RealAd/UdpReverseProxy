package io.realad.reverseproxy.udp

import io.netty.channel.*
import io.netty.channel.socket.DatagramPacket

@ChannelHandler.Sharable
class DatagramProxyOutboundHandler(private val inboundChannel: Channel) : ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        routeMap[(msg as DatagramPacket).recipient().port]?.let {
            val timestamp = System.currentTimeMillis()
            println("${timestamp}: Route found for port ${msg.recipient().port} out of ${routeMap.size} routes: $it (integration latency: ${timestamp - it.createdAt} ms.)")
            inboundChannel.writeAndFlush(DatagramPacket(msg.content(), it.origin))
                .addListener(ChannelFutureListener { future ->
                    if (future.isSuccess) {
                        ctx.channel().read()
                    } else {
                        future.channel().close()
                    }
                })
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        DatagramProxyInboundHandler.closeOnFlush(inboundChannel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        DatagramProxyInboundHandler.closeOnFlush(ctx.channel())
    }

}
