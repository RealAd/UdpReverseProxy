package io.realad.reverseproxy.udp

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.socket.DatagramPacket
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class DatagramProxyInboundHandler(
    private val nodes: List<InetSocketAddress>
) : ChannelInboundHandlerAdapter() {

    private val nodeChannelMap = HashMap<InetSocketAddress, ArrayList<Channel>>()

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    override fun channelActive(ctx: ChannelHandlerContext) {
        nodes.forEach {
            Bootstrap().apply {
                group(ctx.channel().eventLoop())
                    .channel(ctx.channel().javaClass)
                    .handler(DatagramProxyOutboundHandler(ctx.channel()))
                    .option(ChannelOption.AUTO_READ, false)
                for (i in 1..512) {
                    connect(it).apply {
                        if (nodeChannelMap.containsKey(it)) nodeChannelMap[it]?.add(channel())
                        else nodeChannelMap[it] = arrayListOf(channel())
                        addListener(ChannelFutureListener { future ->
                            if (future.isSuccess) {
                                // connection complete start to read first data
                                ctx.channel().read()
                            } else {
                                // Close the connection if the connection attempt has failed.
                                ctx.channel().close()
                            }
                        })
                    }
                }
            }
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val address = nodeChannelMap.keys.toTypedArray().random()
        val channel = nodeChannelMap[address]?.random() ?: return

        val source = msg as DatagramPacket

        val srcIpByteBuf = ctx.alloc().buffer().writeLong(ByteBuffer.wrap(source.sender().address.address).int.toLong())
        val srcContentByteBuf = source.content()
        val content = ctx.alloc().compositeBuffer(2)
            .addComponent(srcIpByteBuf)
            .addComponent(srcContentByteBuf)
            .writerIndex(srcIpByteBuf.readableBytes() + srcContentByteBuf.readableBytes())
        if (channel.isActive) {
            channel.writeAndFlush(DatagramPacket(content, address)).addListener(ChannelFutureListener { future ->
                if (future.isSuccess) {
                    val route = Route(source.sender(), future.channel().remoteAddress() as InetSocketAddress)
                    routeMap[(future.channel().localAddress() as InetSocketAddress).port] = route
                    println(
                        "${System.currentTimeMillis()}: Route created for port ${
                            (future.channel().localAddress() as InetSocketAddress).port
                        }: $route"
                    )
                    // was able to flush out data, start to read the next chunk
                    ctx.channel().read()
                } else {
                    future.channel().close()
                }
            })
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        nodeChannelMap.forEach { entry -> entry.value.forEach { item -> closeOnFlush(item) } }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        closeOnFlush(ctx.channel())
    }

    companion object {
        /**
         * Closes the specified channel after all queued write requests are flushed.
         */
        fun closeOnFlush(ch: Channel) {
            if (ch.isActive) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }
    }
}
