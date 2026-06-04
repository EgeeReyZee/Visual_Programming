package com.egeereyzee.multiapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.*
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class Sockets2Activity : AppCompatActivity() {

    private lateinit var tvLatitude:     TextView
    private lateinit var tvLongitude:    TextView
    private lateinit var tvAltitude:     TextView
    private lateinit var tvAccuracy:     TextView
    private lateinit var tvTime:         TextView
    private lateinit var tvStatus:       TextView
    private lateinit var tvLog:          TextView
    private lateinit var etPort:         EditText
    private lateinit var etServerIp:     EditText
    private lateinit var btnGetLocation: Button
    private lateinit var btnScan:        Button
    private lateinit var btnConnect:     Button
    private lateinit var btnStop:        Button

    private var cellService: CellMonitorService? = null
    private var serviceBound = false

    companion object {
        private const val LOC_PERM_CODE = 100
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as CellMonitorService.LocalBinder
            cellService = b.getService()
            serviceBound = true
            applyConnectionParams()
            setStatus("Service connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            cellService = null
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CellMonitorService.ACTION_STATUS -> {
                    val msg = intent.getStringExtra(CellMonitorService.EXTRA_MSG) ?: return
                    setStatus(msg)
                }
                CellMonitorService.ACTION_LOG -> {
                    val msg = intent.getStringExtra(CellMonitorService.EXTRA_MSG) ?: return
                    appendLog(msg)
                }
                CellMonitorService.ACTION_LOCATION -> {
                    val lat = intent.getDoubleExtra("lat", 0.0)
                    val lon = intent.getDoubleExtra("lon", 0.0)
                    val alt = intent.getDoubleExtra("alt", 0.0)
                    val acc = intent.getFloatExtra("acc", 0f)
                    val ts  = intent.getStringExtra("ts") ?: ""
                    tvLatitude.text  = "Latitude:   $lat"
                    tvLongitude.text = "Longitude:  $lon"
                    tvAltitude.text  = "Altitude:   ${"%.1f".format(alt)} m"
                    tvAccuracy.text  = "Accuracy:   ${"%.1f".format(acc)} m"
                    tvTime.text      = "Time:       $ts"
                    btnConnect.isEnabled = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets2)
        bindViews()
        setupButtons()
        requestPermissions()

        val filter = IntentFilter().apply {
            addAction(CellMonitorService.ACTION_STATUS)
            addAction(CellMonitorService.ACTION_LOG)
            addAction(CellMonitorService.ACTION_LOCATION)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, CellMonitorService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
    }

    private fun bindViews() {
        tvLatitude     = findViewById(R.id.tvLatitude)
        tvLongitude    = findViewById(R.id.tvLongitude)
        tvAltitude     = findViewById(R.id.tvAltitude)
        tvAccuracy     = findViewById(R.id.tvAccuracy)
        tvTime         = findViewById(R.id.tvTime)
        tvStatus       = findViewById(R.id.tvStatus)
        tvLog          = findViewById(R.id.tvLog)
        etPort         = findViewById(R.id.etPort)
        etServerIp     = findViewById(R.id.etServerIp)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        btnScan        = findViewById(R.id.btnScan)
        btnConnect     = findViewById(R.id.btnConnect)
        btnStop        = findViewById(R.id.btnStop)
    }

    private fun setupButtons() {
        btnGetLocation.setOnClickListener { cellService?.requestLocation() }
        btnScan.setOnClickListener        { scanNetwork() }
        btnConnect.setOnClickListener     { startSending() }
        btnStop.setOnClickListener        { stopSending() }
        btnStop.isEnabled    = false
        btnConnect.isEnabled = false
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), LOC_PERM_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOC_PERM_CODE) {
            cellService?.requestLocation()
        }
    }

    private fun applyConnectionParams() {
        val ip   = etServerIp.text.toString().trim()
        val port = etPort.text.toString().trim()
        if (ip.isNotEmpty() && port.isNotEmpty()) {
            cellService?.setConnectionParams(ip, port.toIntOrNull() ?: 5555)
        }
    }

    private fun startSending() {
        val ip   = etServerIp.text.toString().trim()
        val port = etPort.text.toString().trim()
        if (ip.isEmpty())   { toast("Enter or scan server IP"); return }
        if (port.isEmpty()) { toast("Enter port"); return }
        cellService?.setConnectionParams(ip, port.toIntOrNull() ?: 5555)
        cellService?.startSending()
        btnConnect.isEnabled = false
        btnStop.isEnabled    = true
        btnScan.isEnabled    = false
    }

    private fun stopSending() {
        cellService?.stopSending()
        btnConnect.isEnabled = true
        btnStop.isEnabled    = false
        btnScan.isEnabled    = true
    }

    private fun scanNetwork() {
        val port = etPort.text.toString().trim()
        if (port.isEmpty()) { toast("Enter port first"); return }
        setStatus("Scanning network…")
        appendLog("── Scan started (port $port) ──")
        btnScan.isEnabled = false
        cellService?.scanNetwork(port.toInt()) { found ->
            runOnUiThread {
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

    private fun setStatus(msg: String) { tvStatus.text = msg }

    private fun appendLog(msg: String) {
        val current = tvLog.text.toString()
        val lines   = current.lines().takeLast(80)
        tvLog.text  = (lines + msg).joinToString("\n")
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}