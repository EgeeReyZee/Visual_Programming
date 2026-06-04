package com.egeereyzee.multiapp

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.telephony.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class CellMonitorService : Service() {

    inner class LocalBinder : Binder() {
        fun getService() = this@CellMonitorService
    }

    private val binder      = LocalBinder()
    private val handler     = Handler(Looper.getMainLooper())
    private val gson        = Gson()

    private var serverIp    = ""
    private var serverPort  = 5555
    private var lastLocation: LocationData? = null

    @Volatile private var stopFlag = false
    private var isRunning = false

    companion object {
        const val ACTION_STATUS   = "com.egeereyzee.multiapp.STATUS"
        const val ACTION_LOG      = "com.egeereyzee.multiapp.LOG"
        const val ACTION_LOCATION = "com.egeereyzee.multiapp.LOCATION"
        const val EXTRA_MSG       = "msg"
        private const val CHANNEL_ID       = "cell_monitor_channel"
        private const val NOTIF_ID         = 1
        private const val SEND_INTERVAL_MS = 2000L
        private const val SCAN_TIMEOUT_MS  = 300
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Running…"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        stopFlag = true
    }


    fun setConnectionParams(ip: String, port: Int) {
        serverIp   = ip
        serverPort = port
    }

    fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            broadcast(ACTION_STATUS, "⚠ Location permission not granted")
            return
        }
        broadcast(ACTION_STATUS, "Getting location…")
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val data = buildLocationData(loc)
                    lastLocation = data
                    broadcastLocation(data)
                    broadcast(ACTION_STATUS, "Location acquired ✓")
                } else {
                    broadcast(ACTION_STATUS, "⚠ Location unavailable")
                }
            }
    }

    fun startSending() {
        if (isRunning) return
        stopFlag  = false
        isRunning = true

        broadcast(ACTION_LOG, "── Connecting to $serverIp:$serverPort ──")

        thread {
            var msgCount = 0
            var ctx: ZContext? = null
            var sock: ZMQ.Socket? = null

            fun connect() {
                ctx?.close()
                ctx  = ZContext()
                sock = ctx!!.createSocket(SocketType.REQ)
                sock!!.receiveTimeOut = 5000
                sock!!.sendTimeOut    = 5000
                sock!!.connect("tcp://$serverIp:$serverPort")
                broadcast(ACTION_LOG, "Connected to $serverIp:$serverPort")
            }

            try {
                connect()
                while (!stopFlag) {
                    fetchLocationSync { loc -> if (loc != null) lastLocation = loc }

                    val data = lastLocation
                    if (data == null) {
                        broadcast(ACTION_STATUS, "Waiting for location…")
                        Thread.sleep(SEND_INTERVAL_MS)
                        continue
                    }

                    val enriched = data.copy(network = collectCellInfo())
                    val json     = gson.toJson(enriched)
                    msgCount++

                    try {
                        val sent = sock!!.send(json.toByteArray(ZMQ.CHARSET), 0)
                        if (!sent) throw Exception("send() returned false")
                        val reply = sock!!.recvStr(0) ?: throw Exception("No reply (timeout)")
                        broadcast(ACTION_STATUS, "✓ Sent #$msgCount → $reply")
                        broadcast(ACTION_LOG,    "#$msgCount → $reply")
                    } catch (e: Exception) {
                        broadcast(ACTION_LOG, "⚠ ${e.message} — reconnecting…")
                        try { connect() } catch (re: Exception) {
                            broadcast(ACTION_LOG, "Reconnect failed: ${re.message}")
                            Thread.sleep(3000)
                        }
                    }

                    Thread.sleep(SEND_INTERVAL_MS)
                }
            } catch (e: Exception) {
                broadcast(ACTION_LOG, "Fatal: ${e.message}")
                broadcast(ACTION_STATUS, "⚠ ${e.message}")
            } finally {
                sock?.close(); ctx?.close()
                isRunning = false
                broadcast(ACTION_STATUS, "Stopped")
                broadcast(ACTION_LOG, "── Stopped ──")
            }
        }
    }

    fun stopSending() { stopFlag = true }

    fun scanNetwork(port: Int, callback: (List<String>) -> Unit) {
        thread {
            val subnet = getLocalSubnet()
            if (subnet == null) {
                callback(emptyList()); return@thread
            }
            broadcast(ACTION_LOG, "Subnet: $subnet.0/24")
            val found = mutableListOf<String>()
            val threads = (1..254).map { i ->
                val host = "$subnet.$i"
                thread {
                    if (isZmqReachable(host, port)) synchronized(found) { found.add(host) }
                }
            }
            threads.forEach { it.join() }
            callback(found)
        }
    }

    private fun collectCellInfo(): NetworkData {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val hasPerm = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val rawType = if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                tm.dataNetworkType
            else
                @Suppress("DEPRECATION") tm.networkType
        } else TelephonyManager.NETWORK_TYPE_UNKNOWN

        val networkType = networkTypeName(rawType)

        val operatorNumeric = tm.networkOperator ?: ""
        val mcc = if (operatorNumeric.length >= 3) operatorNumeric.substring(0, 3).toIntOrNull() ?: 0 else 0
        val mnc = if (operatorNumeric.length >  3) operatorNumeric.substring(3).toIntOrNull() ?: 0 else 0

        var lteData: LteCellData?  = null
        var gsmData: GsmCellData?  = null
        var nrData:  NrCellData?   = null

        var signalDbm     = 0
        var cellId        = 0L
        var lac           = 0
        var visibleTowers = 0

        if (hasPerm) {
            val latch = java.util.concurrent.CountDownLatch(1)
            var cells: List<CellInfo>? = null
            handler.post {
                cells = tm.allCellInfo
                latch.countDown()
            }
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)

            visibleTowers = cells?.size ?: 0

            cells?.forEach { cell ->
                when {
                    cell is CellInfoLte -> {
                        val id  = cell.cellIdentity
                        val sig = cell.cellSignalStrength
                        if (cell.isRegistered) {
                            cellId    = id.ci.let { if (it != Int.MAX_VALUE) it.toLong() else 0L }
                            lac       = id.tac.let { if (it != Int.MAX_VALUE) it else 0 }
                            signalDbm = sig.dbm
                        }
                        val lteBand: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            @Suppress("NewApi")
                            (id.bands as? IntArray)?.firstOrNull() ?: 0
                        } else 0
                        val lteCqi: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            @Suppress("NewApi")
                            sig.cqi.let { if (it != Int.MAX_VALUE) it else 0 }
                        } else 0
                        val lteRssi: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            @Suppress("NewApi")
                            sig.rssi.let { if (it != Int.MAX_VALUE) it else 0 }
                        } else 0
                        val lteTa: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            @Suppress("NewApi")
                            sig.timingAdvance.let { if (it != Int.MAX_VALUE) it else 0 }
                        } else 0
                        lteData = LteCellData(
                            band         = lteBand,
                            cellIdentity = id.ci.let { if (it != Int.MAX_VALUE) it else 0 },
                            earfcn       = id.earfcn.let { if (it != Int.MAX_VALUE) it else 0 },
                            mcc          = id.mccString?.toIntOrNull() ?: mcc,
                            mnc          = id.mncString?.toIntOrNull() ?: mnc,
                            pci          = id.pci.let { if (it != Int.MAX_VALUE) it else 0 },
                            tac          = id.tac.let { if (it != Int.MAX_VALUE) it else 0 },
                            asuLevel     = sig.asuLevel,
                            cqi          = lteCqi,
                            rsrp         = sig.rsrp.let { if (it != Int.MAX_VALUE) it else 0 },
                            rsrq         = sig.rsrq.let { if (it != Int.MAX_VALUE) it else 0 },
                            rssi         = lteRssi,
                            rssnr        = sig.rssnr.let { if (it != Int.MAX_VALUE) it else 0 },
                            timingAdvance = lteTa,
                            isRegistered = cell.isRegistered
                        )
                    }
                    cell is CellInfoGsm -> {
                        val id  = cell.cellIdentity
                        val sig = cell.cellSignalStrength
                        if (cell.isRegistered) {
                            cellId    = id.cid.takeIf { it != Int.MAX_VALUE }?.toLong() ?: 0L
                            lac       = id.lac.takeIf { it != Int.MAX_VALUE } ?: 0
                            signalDbm = sig.dbm
                        }
                        gsmData = GsmCellData(
                            cellIdentity  = id.cid.takeIf { it != Int.MAX_VALUE } ?: 0,
                            bsic          = id.bsic.takeIf { it != Int.MAX_VALUE } ?: 0,
                            arfcn         = id.arfcn.takeIf { it != Int.MAX_VALUE } ?: 0,
                            lac           = id.lac.takeIf { it != Int.MAX_VALUE } ?: 0,
                            mcc           = id.mccString?.toIntOrNull() ?: mcc,
                            mnc           = id.mncString?.toIntOrNull() ?: mnc,
                            psc           = id.psc.takeIf { it != Int.MAX_VALUE } ?: 0,
                            dbm           = sig.dbm,
                            rssi          = sig.rssi.takeIf { it != Int.MAX_VALUE } ?: 0,
                            timingAdvance = run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    @Suppress("NewApi")
                                    sig.timingAdvance.let { if (it != Int.MAX_VALUE) it else 0 }
                                } else 0
                            },
                            isRegistered  = cell.isRegistered
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr -> {
                        @Suppress("NewApi")
                        run {
                            val id = cell.cellIdentity as CellIdentityNr
                            val sig = cell.cellSignalStrength as CellSignalStrengthNr

                            if (cell.isRegistered) {
                                cellId    = id.nci.let { if (it != Long.MAX_VALUE) it else 0L }
                                lac       = id.tac.let { if (it != Int.MAX_VALUE) it else 0 }
                                signalDbm = sig.dbm
                            }

                            val bandVal: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                @Suppress("NewApi")
                                (id.bands as? IntArray)?.firstOrNull() ?: 0
                            } else 0

                            val nrArfcnVal: Int = try {
                                val method = id.javaClass.getMethod("getNrArfcn")
                                val v = method.invoke(id) as? Int ?: 0
                                if (v != Int.MAX_VALUE) v else 0
                            } catch (e: Exception) {
                                0
                            }

                            val ssSinrVal: Int = try {
                                @Suppress("NewApi")
                                val sinr = sig.ssSinr
                                if (sinr != Int.MAX_VALUE) sinr else 0
                            } catch (e: Exception) {
                                0
                            }

                            val ssRsrpVal: Int = try {
                                @Suppress("NewApi")
                                val rsrp = sig.ssRsrp
                                if (rsrp != Int.MAX_VALUE) rsrp else 0
                            } catch (e: Exception) {
                                0
                            }

                            val ssRsrqVal: Int = try {
                                @Suppress("NewApi")
                                val rsrq = sig.ssRsrq
                                if (rsrq != Int.MAX_VALUE) rsrq else 0
                            } catch (e: Exception) {
                                0
                            }

                            val taVal: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    @Suppress("NewApi")
                                    val ta = sig.timingAdvanceMicros
                                    if (ta != Int.MAX_VALUE) ta else 0
                                } catch (e: Exception) {
                                    0
                                }
                            } else 0

                            val nciVal: Long = try {
                                @Suppress("NewApi")
                                val nci = id.nci
                                if (nci != Long.MAX_VALUE) nci else 0L
                            } catch (e: Exception) {
                                0L
                            }

                            val pciVal: Int = try {
                                @Suppress("NewApi")
                                val pci = id.pci
                                if (pci != Int.MAX_VALUE) pci else 0
                            } catch (e: Exception) {
                                0
                            }

                            val tacVal: Int = try {
                                @Suppress("NewApi")
                                val tac = id.tac
                                if (tac != Int.MAX_VALUE) tac else 0
                            } catch (e: Exception) {
                                0
                            }

                            val mccVal: Int = try {
                                id.mccString?.toIntOrNull() ?: mcc
                            } catch (e: Exception) {
                                mcc
                            }

                            val mncVal: Int = try {
                                id.mncString?.toIntOrNull() ?: mnc
                            } catch (e: Exception) {
                                mnc
                            }

                            nrData = NrCellData(
                                band = bandVal,
                                nci = nciVal,
                                pci = pciVal,
                                nrArfcn = nrArfcnVal,
                                tac = tacVal,
                                mcc = mccVal,
                                mnc = mncVal,
                                ssSinr = ssSinrVal,
                                ssRsrp = ssRsrpVal,
                                ssRsrq = ssRsrqVal,
                                timingAdvanceMicros = taVal,
                                isRegistered = cell.isRegistered
                            )
                        }
                    }
                }
            }
        }

        return NetworkData(
            operator_name    = tm.networkOperatorName ?: "",
            operator_numeric = operatorNumeric,
            network_type     = networkType,
            signal_dbm       = signalDbm,
            cell_id          = cellId,
            lac              = lac,
            mcc              = mcc,
            mnc              = mnc,
            is_roaming       = tm.isNetworkRoaming,
            visible_towers   = visibleTowers,
            lte              = lteData,
            gsm              = gsmData,
            nr               = nrData
        )
    }

    private fun networkTypeName(rawType: Int) = when (rawType) {
        TelephonyManager.NETWORK_TYPE_LTE      -> "LTE"
        TelephonyManager.NETWORK_TYPE_NR       -> "NR"
        TelephonyManager.NETWORK_TYPE_UMTS     -> "UMTS"
        TelephonyManager.NETWORK_TYPE_HSDPA    -> "HSDPA"
        TelephonyManager.NETWORK_TYPE_HSUPA    -> "HSUPA"
        TelephonyManager.NETWORK_TYPE_HSPA     -> "HSPA"
        TelephonyManager.NETWORK_TYPE_HSPAP    -> "HSPA+"
        TelephonyManager.NETWORK_TYPE_EDGE     -> "EDGE"
        TelephonyManager.NETWORK_TYPE_GPRS     -> "GPRS"
        else -> "UNKNOWN"
    }


    private fun buildLocationData(loc: Location): LocationData {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return LocationData(
            latitude  = loc.latitude,
            longitude = loc.longitude,
            altitude  = loc.altitude,
            accuracy  = loc.accuracy,
            provider  = loc.provider ?: "fused",
            timestamp = fmt.format(Date()),
            network   = collectCellInfo()
        )
    }

    private fun fetchLocationSync(callback: (LocationData?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) { callback(null); return }
        val latch  = java.util.concurrent.CountDownLatch(1)
        var result: LocationData? = null
        handler.post {
            LocationServices.getFusedLocationProviderClient(this)
                .lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) result = buildLocationData(loc)
                    latch.countDown()
                }.addOnFailureListener { latch.countDown() }
        }
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        callback(result)
    }

    private fun broadcastLocation(d: LocationData) {
        val intent = Intent(ACTION_LOCATION).apply {
            putExtra("lat", d.latitude)
            putExtra("lon", d.longitude)
            putExtra("alt", d.altitude)
            putExtra("acc", d.accuracy)
            putExtra("ts",  d.timestamp)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    private fun isZmqReachable(host: String, port: Int): Boolean {
        return try {
            val ctx  = ZContext()
            val sock = ctx.createSocket(SocketType.REQ)
            sock.receiveTimeOut = SCAN_TIMEOUT_MS
            sock.sendTimeOut    = SCAN_TIMEOUT_MS
            sock.connect("tcp://$host:$port")
            val sent  = sock.send("PING".toByteArray(ZMQ.CHARSET), 0)
            val reply = if (sent) sock.recvStr(0) else null
            sock.close(); ctx.close()
            reply != null
        } catch (e: Exception) { false }
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

    private fun broadcast(action: String, msg: String) {
        val intent = Intent(action).putExtra(EXTRA_MSG, msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Cell Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Collects cell & location data" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, Sockets2Activity::class.java),
            pendingFlags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cell Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .build()
    }

    data class LteCellData(
        val band:          Int,
        val cellIdentity:  Int,
        val earfcn:        Int,
        val mcc:           Int,
        val mnc:           Int,
        val pci:           Int,
        val tac:           Int,

        val asuLevel:      Int,
        val cqi:           Int,
        val rsrp:          Int,
        val rsrq:          Int,
        val rssi:          Int,
        val rssnr:         Int,
        val timingAdvance: Int,
        val isRegistered:  Boolean
    )

    data class GsmCellData(
        val cellIdentity:  Int,
        val bsic:          Int,
        val arfcn:         Int,
        val lac:           Int,
        val mcc:           Int,
        val mnc:           Int,
        val psc:           Int,

        val dbm:           Int,
        val rssi:          Int,
        val timingAdvance: Int,
        val isRegistered:  Boolean
    )

    data class NrCellData(
        val band:                  Int,
        val nci:                   Long,
        val pci:                   Int,
        val nrArfcn:               Int,
        val tac:                   Int,
        val mcc:                   Int,
        val mnc:                   Int,

        val ssSinr:                Int,
        val ssRsrp:                Int,
        val ssRsrq:                Int,
        val timingAdvanceMicros:   Int,
        val isRegistered:          Boolean
    )

    data class NetworkData(
        val operator_name:    String,
        val operator_numeric: String,
        val network_type:     String,
        val signal_dbm:       Int,
        val cell_id:          Long,
        val lac:              Int,
        val mcc:              Int,
        val mnc:              Int,
        val is_roaming:       Boolean,
        val visible_towers:   Int,

        val lte:              LteCellData?  = null,
        val gsm:              GsmCellData?  = null,
        val nr:               NrCellData?   = null
    )

    data class LocationData(
        val latitude:  Double,
        val longitude: Double,
        val altitude:  Double,
        val accuracy:  Float,
        val provider:  String,
        val timestamp: String,
        val network:   NetworkData? = null
    )
}