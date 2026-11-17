package com.macdeck.keyboard

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Keeps a TCP connection to the Mac and sends key events as text lines. */
class Link(private val onStatus: (String, Boolean) -> Unit) {
    @Volatile private var gen = 0
    @Volatile private var connected = false
    @Volatile private var sock: Socket? = null
    @Volatile private var queue: LinkedBlockingQueue<String>? = null

    private fun post(my: Int, msg: String, ok: Boolean) {
        if (my == gen) onStatus(msg, ok)
    }

    fun start(host: String, port: Int, pin: String) {
        stop()
        val my = gen
        val t = Thread {
            while (my == gen) {
                try {
                    post(my, "Connecting…", false)
                    val s = Socket()
                    sock = s
                    s.connect(InetSocketAddress(host, port), 3000)
                    s.tcpNoDelay = true
                    s.soTimeout = 4000
                    val o = s.getOutputStream()
                    o.write("HELLO $pin\n".toByteArray())
                    o.flush()
                    val resp = s.getInputStream().bufferedReader().readLine()
                    if (resp != "OK") {
                        post(my, "Wrong PIN", false)
                        s.close()
                        Thread.sleep(3000)
                        continue
                    }
                    s.soTimeout = 0
                    val q = LinkedBlockingQueue<String>()
                    queue = q
                    connected = true
                    post(my, "Connected", true)
                    while (my == gen) {
                        val line = q.poll(2, TimeUnit.SECONDS) ?: "P"
                        o.write((line + "\n").toByteArray())
                        o.flush()
                    }
                    s.close()
                } catch (e: Exception) {
                    connected = false
                    if (my == gen) {
                        post(my, "Not connected – is the Mac app running?", false)
                        try { Thread.sleep(2000) } catch (e2: Exception) { }
                    }
                }
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun stop() {
        gen++
        connected = false
        queue = null
        try { sock?.close() } catch (e: Exception) { }
    }

    fun isConnected() = connected

    fun send(line: String) {
        if (connected) queue?.offer(line)
    }
}
