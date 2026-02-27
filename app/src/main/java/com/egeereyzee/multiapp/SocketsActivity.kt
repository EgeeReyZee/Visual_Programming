package com.egeereyzee.multiapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZMQ
import org.zeromq.ZContext
import kotlin.concurrent.thread

class SocketsActivity : AppCompatActivity() {
    private lateinit var tvSockets: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStartClient: Button

    private var isClientRunning = false
    private lateinit var handler: Handler

    private val SERVER_IP = "10.146.144.115"
    private val SERVER_PORT = "5555"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        initViews()
        handler = Handler(Looper.getMainLooper())
    }

    private fun initViews() {
        tvSockets = findViewById(R.id.tvSockets)
        tvStatus = findViewById(R.id.tvStatus)
        btnStartClient = findViewById(R.id.btnStartClient)


        btnStartClient.setOnClickListener{
            startClientToComputer()
        }
    }

    private fun startClientToComputer(){
        if (isClientRunning){
            return
        }

        thread {
            isClientRunning = true
            try {
                val context = ZContext()
                val socket = context.createSocket(SocketType.REQ)
                socket.connect("tcp://$SERVER_IP:$SERVER_PORT")
                handler.post {
                    tvStatus.text = "Connecting to computer at $SERVER_IP:$SERVER_PORT"
                }
                for (i in 1..10){
                    try {
                        val message = "Hello from Android! Message #$i"
                        socket.send(message.toByteArray(ZMQ.CHARSET), 0)

                        val reply = socket.recvStr(0)

                        handler.post {
                            tvSockets.text = "Computer Client\nSent: $message\nReceived: $reply"
                            tvStatus.text = "Connected to computer. Message $i/10 sent"
                        }
                        Thread.sleep(1000)
                    } catch (e: Exception){
                        handler.post {
                            tvStatus.text = "Error: ${e.message}"
                        }
                        break
                    }
                }
                socket.close()
                context.close()
                handler.post{
                    tvStatus.text = "Finished sending messages"
                }

            } catch (e: Exception){
                handler.post{
                    tvStatus.text = "Connection failed: ${e.message}"
                }
            } finally {
                isClientRunning = false
            }
        }
    }
}