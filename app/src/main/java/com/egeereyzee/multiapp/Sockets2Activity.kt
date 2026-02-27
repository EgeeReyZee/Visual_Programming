package com.egeereyzee.multiapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class LocationSocketsActivity : AppCompatActivity() {

    // ── UI ──────────────────────────────────────────────────────────────
    private lateinit var tvLatitude:    TextView
    private lateinit var tvLongitude:   TextView
    private lateinit var tvAltitude:    TextView
    private lateinit var tvAccuracy:    TextView
    private lateinit var tvTime:        TextView
    private lateinit var tvStatus:      TextView
    private lateinit var tvLog:         TextView
    private lateinit var etPort:        EditText
    private lateinit var etServerIp:    EditText
    private lateinit var btnGetLocation:Button
    private lateinit var btnScan:       Button
    private lateinit var btnConnect:    Button
    private lateinit var btnStop:       Button

    // ── State ───────────────────────────────────────────────────────────
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val gson    = Gson()

    private var lastLocation: LocationData? = null
    private var isRunning   = false

    @Volatile private var stopFlag = false

    companion object {
        private const val LOC_PERM_CODE = 100
        private const val SCAN_TIMEOUT_MS = 300
        private const val SEND_INTERVAL_MS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets2)

        bindViews()
        setupButtons()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()
    }

    private fun bindViews() {
        tvLatitude    = findViewById(R.id.tvLatitude)
        tvLongitude   = findViewById(R.id.tvLongitude)
        tvAltitude    = findViewById(R.id.tvAltitude)
        tvAccuracy    = findViewById(R.id.tvAccuracy)
        tvTime        = findViewById(R.id.tvTime)
        tvStatus      = findViewById(R.id.tvStatus)
        tvLog         = findViewById(R.id.tvLog)
        etPort        = findViewById(R.id.etPort)
        etServerIp    = findViewById(R.id.etServerIp)
        btnGetLocation= findViewById(R.id.btnGetLocation)
        btnScan       = findViewById(R.id.btnScan)
        btnConnect    = findViewById(R.id.btnConnect)
        btnStop       = findViewById(R.id.btnStop)
    }

    private fun setupButtons() {
        btnGetLocation.setOnClickListener { fetchLocation() }
        btnScan.setOnClickListener       { scanNetwork()   }
        btnConnect.setOnClickListener    { startSending()  }
        btnStop.setOnClickListener       { stopSending()   }

        btnStop.isEnabled    = false
        btnConnect.isEnabled = false
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOC_PERM_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOC_PERM_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            setStatus("⚠ Location permission not granted")
            return
        }

        setStatus("Getting location…")
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                lastLocation = LocationData(
                    latitude  = loc.latitude,
                    longitude = loc.longitude,
                    altitude  = loc.altitude,
                    accuracy  = loc.accuracy,
                    provider  = loc.provider ?: "fused",
                    timestamp = fmt.format(Date())
                )
                updateLocationUI(lastLocation!!)
                setStatus("Location acquired ✓")
                btnConnect.isEnabled = true
            } else {
                setStatus("⚠ Location unavailable — move outdoors or enable GPS")
            }
        }
    }

    private fun updateLocationUI(d: LocationData) {
        tvLatitude.text  = "Latitude:   ${d.latitude}"
        tvLongitude.text = "Longitude:  ${d.longitude}"
        tvAltitude.text  = "Altitude:   ${"%.1f".format(d.altitude)} m"
        tvAccuracy.text  = "Accuracy:   ${"%.1f".format(d.accuracy)} m"
        tvTime.text      = "Time:       ${d.timestamp}"
    }

    private fun scanNetwork() {
        val port = etPort.text.toString().trim()
        if (port.isEmpty()) { toast("Enter port first"); return }

        setStatus("Scanning network…")
        appendLog("── Scan started (port $port) ──")
        btnScan.isEnabled = false

        thread {
            val subnet = getLocalSubnet()
            if (subnet == null) {
                ui { setStatus("⚠ Could not determine local subnet") }
                ui { btnScan.isEnabled = true }
                return@thread
            }

            appendLog("Subnet: $subnet.0/24")
            val found = mutableListOf<String>()

            val threads = (1..254).map { i ->
                val host = "$subnet.$i"
                thread {
                    if (isZmqReachable(host, port.toInt())) {
                        synchronized(found) { found.add(host) }
                    }
                }
            }
            threads.forEach { it.join() }

            ui {
                btnScan.isEnabled = true
                if (found.isEmpty()) {
                    setStatus("No server found on port $port")
                    appendLog("Scan finished — nothing found")
                } else {
                    val best = found.first()
                    etServerIp.setText(best)
                    setStatus("Found ${found.size} host(s) — using $best")
                    appendLog("Found: ${found.joinToString(", ")}")
                    btnConnect.isEnabled = true
                }
            }
        }
    }

    private fun isZmqReachable(host: String, port: Int): Boolean {
        return try {
            val ctx = ZContext()
            val sock = ctx.createSocket(SocketType.REQ)
            sock.receiveTimeOut = SCAN_TIMEOUT_MS
            sock.sendTimeOut    = SCAN_TIMEOUT_MS
            sock.connect("tcp://$host:$port")
            val sent = sock.send("PING".toByteArray(ZMQ.CHARSET), 0)
            val reply = if (sent) sock.recvStr(0) else null
            sock.close(); ctx.close()
            reply != null
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalSubnet(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (iface in interfaces.asSequence()) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.asSequence()) {
                    val ip = addr.hostAddress ?: continue
                    if (addr.isLoopbackAddress || ip.contains(':')) continue
                    if (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                        val parts = ip.split(".")
                        if (parts.size == 4) return "${parts[0]}.${parts[1]}.${parts[2]}"
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }

    private fun startSending() {
        val ip   = etServerIp.text.toString().trim()
        val port = etPort.text.toString().trim()

        if (ip.isEmpty())   { toast("Enter or scan server IP"); return }
        if (port.isEmpty()) { toast("Enter port"); return }
        if (isRunning)      { toast("Already running"); return }

        stopFlag = false
        isRunning = true
        btnConnect.isEnabled = false
        btnStop.isEnabled    = true
        btnScan.isEnabled    = false

        appendLog("── Connecting to $ip:$port ──")

        thread {
            var msgCount = 0
            var ctx: ZContext? = null
            var sock: org.zeromq.ZMQ.Socket? = null

            fun connect() {
                ctx?.close()
                ctx  = ZContext()
                sock = ctx!!.createSocket(SocketType.REQ)
                sock!!.receiveTimeOut = 5000
                sock!!.sendTimeOut    = 5000
                sock!!.connect("tcp://$ip:$port")
                appendLog("Connected to $ip:$port")
            }

            try {
                connect()

                while (!stopFlag) {
                    fetchLocationSync { loc ->
                        if (loc != null) lastLocation = loc
                    }

                    val data = lastLocation
                    if (data == null) {
                        ui { setStatus("Waiting for location…") }
                        Thread.sleep(SEND_INTERVAL_MS)
                        continue
                    }

                    val json = gson.toJson(data)
                    msgCount++

                    try {
                        val sent = sock!!.send(json.toByteArray(ZMQ.CHARSET), 0)
                        if (!sent) throw Exception("send() returned false")

                        val reply = sock!!.recvStr(0)
                        if (reply == null) throw Exception("No reply (timeout)")

                        ui {
                            setStatus("✓ Sent #$msgCount → $reply")
                            updateLocationUI(data)
                        }
                        appendLog("#$msgCount → $reply")

                    } catch (e: Exception) {
                        appendLog("⚠ Error: ${e.message} — reconnecting…")
                        ui { setStatus("Reconnecting…") }
                        try { connect() } catch (re: Exception) {
                            appendLog("Reconnect failed: ${re.message}")
                            Thread.sleep(3000)
                        }
                    }

                    Thread.sleep(SEND_INTERVAL_MS)
                }

            } catch (e: Exception) {
                appendLog("Fatal: ${e.message}")
                ui { setStatus("⚠ ${e.message}") }
            } finally {
                sock?.close()
                ctx?.close()
                isRunning = false
                ui {
                    btnConnect.isEnabled = true
                    btnStop.isEnabled    = false
                    btnScan.isEnabled    = true
                    setStatus("Stopped")
                }
                appendLog("── Stopped ──")
            }
        }
    }

    private fun stopSending() {
        stopFlag = true
    }

    private fun fetchLocationSync(callback: (LocationData?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            callback(null); return
        }
        val latch = java.util.concurrent.CountDownLatch(1)
        var result: LocationData? = null

        handler.post {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    result = LocationData(
                        latitude  = loc.latitude,
                        longitude = loc.longitude,
                        altitude  = loc.altitude,
                        accuracy  = loc.accuracy,
                        provider  = loc.provider ?: "fused",
                        timestamp = fmt.format(Date())
                    )
                }
                latch.countDown()
            }.addOnFailureListener { latch.countDown() }
        }

        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        callback(result)
    }

    private fun ui(block: () -> Unit) = handler.post(block)

    private fun setStatus(msg: String) {
        tvStatus.text = msg
    }

    private fun appendLog(msg: String) {
        ui {
            val current = tvLog.text.toString()
            val lines   = current.lines().takeLast(80)
            tvLog.text  = (lines + msg).joinToString("\n")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    data class LocationData(
        val latitude:  Double,
        val longitude: Double,
        val altitude:  Double,
        val accuracy:  Float,
        val provider:  String,
        val timestamp: String
    )
}