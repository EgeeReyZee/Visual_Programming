package com.egeereyzee.multiapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.egeereyzee.multiapp.RangeSeekBar
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit
import java.io.File

class MediaPlayerActivity : AppCompatActivity() {
    private lateinit var imageViewAlbumArt: ImageView
    private lateinit var textViewTrackName: TextView
    private lateinit var textViewArtistName: TextView
    private lateinit var textViewCurrentTime: TextView
    private lateinit var textViewTotalTime: TextView
    private lateinit var textViewStartTime: TextView
    private lateinit var textViewEndTime: TextView

    private lateinit var buttonPlayPause: Button
    private lateinit var buttonBackwardStep: Button
    private lateinit var buttonForwardStep: Button
    private lateinit var buttonBackward: Button
    private lateinit var buttonForward: Button
    private lateinit var buttonRepeat: Button
    private lateinit var buttonShuffle: Button
    private lateinit var buttonPlaylist: Button
    private lateinit var buttonVolumeUp: Button
    private lateinit var buttonVolumeDown: Button

    private lateinit var seekBarProgress: SeekBar
    private lateinit var seekBarVolume: SeekBar
    private lateinit var rangeSeekBarPlayBackLoop: RangeSeekBar

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackIndex = 0
    private var trackList = mutableListOf<TrackInfo>()
    private var isPlaying = false
    private var repeatMode = RepeatMode.NONE
    private var isShuffleEnabled = false
    private var startPosition = 0
    private var endPosition = 0

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private val PERMISSION_REQUEST_CODE = 100
    private val FILE_PICKER_REQUEST_CODE = 101
    private var playlistDialog: AlertDialog? = null
    enum class RepeatMode {
        NONE, ONE, ALL
    }

    data class TrackInfo(
        val uri: String,
        val title: String,
        val artist: String,
        val albumArt: ByteArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TrackInfo
            if (uri != other.uri) return false
            return true
        }

        override fun hashCode(): Int {
            return uri.hashCode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test)

        initializeViews()
        checkPermissions()
        setupSeekBars()
        setupButtons()
    }

    private fun initializeViews() {
        imageViewAlbumArt = findViewById(R.id.imageViewMain)
        textViewTrackName = findViewById(R.id.textViewTrackName)
        textViewArtistName = findViewById(R.id.textViewArtistName)
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime)
        textViewTotalTime = findViewById(R.id.textViewTotalTime)
        textViewStartTime = findViewById(R.id.textViewStartTime)
        textViewEndTime = findViewById(R.id.textViewEndTime)

        buttonPlayPause = findViewById(R.id.buttonPause)
        buttonBackwardStep = findViewById(R.id.buttonBackwardStep)
        buttonForwardStep = findViewById(R.id.buttonForwardStep)
        buttonBackward = findViewById(R.id.buttonBackward)
        buttonForward = findViewById(R.id.buttonForward)
        buttonRepeat = findViewById(R.id.buttonRepeat)
        buttonShuffle = findViewById(R.id.buttonShuffle)
        buttonPlaylist = findViewById(R.id.buttonPlaylist)
        buttonVolumeUp = findViewById(R.id.buttonVolumeUp)
        buttonVolumeDown = findViewById(R.id.buttonVolumeDown)

        seekBarProgress = findViewById(R.id.seekBarMusic)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        rangeSeekBarPlayBackLoop = findViewById(R.id.rangeSeekBarPlayBackLoop)
    }

    private fun checkPermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. Cannot add tracks.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTrackToList(uri: Uri) {
        try {
            val file = File(uri.path ?: "")
            if (file.isDirectory) {
                Toast.makeText(this, "Выбрана директория, а не файл", Toast.LENGTH_SHORT).show()
                return
            }

            val mimeType = contentResolver.getType(uri)
            if (mimeType == null || !mimeType.startsWith("audio/")) {
                Toast.makeText(this, "Выбранный файл не является аудиофайлом", Toast.LENGTH_SHORT).show()
                return
            }

            if (trackList.any { it.uri == uri.toString() }) {
                Toast.makeText(this, "Этот трек уже в плейлисте", Toast.LENGTH_SHORT).show()
                return
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"

            val albumArt = retriever.embeddedPicture

            val newTrack = TrackInfo(
                uri = uri.toString(),
                title = title,
                artist = artist,
                albumArt = albumArt
            )

            trackList.add(newTrack)

            if (trackList.size == 1) {
                loadTrack(0)
            }

            Toast.makeText(this, "Трек добавлен: $title", Toast.LENGTH_SHORT).show()

            playlistDialog?.let { dialog ->
                if (dialog.isShowing) {
                    val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewPlaylist)
                    recyclerView?.adapter?.notifyDataSetChanged()
                }
            }

            retriever.release()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при добавлении трека", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTrack(index: Int) {
        if (trackList.isEmpty()) return

        currentTrackIndex = index
        val track = trackList[currentTrackIndex]

        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer.create(this, Uri.parse(track.uri))

            mediaPlayer?.let { mp ->
                val duration = mp.duration

                textViewTrackName.text = track.title
                textViewArtistName.text = track.artist
                textViewTotalTime.text = formatTime(duration)
                textViewCurrentTime.text = "00:00"

                if (track.albumArt != null) {
                    val bitmap = BitmapFactory.decodeByteArray(track.albumArt, 0, track.albumArt.size)
                    imageViewAlbumArt.setImageBitmap(bitmap)
                } else {
                    imageViewAlbumArt.setImageResource(R.drawable.picturenotfound)
                }

                seekBarProgress.max = duration
                seekBarProgress.progress = 0
                rangeSeekBarPlayBackLoop.maxValue = duration
                rangeSeekBarPlayBackLoop.rightThumbValue = duration
                rangeSeekBarPlayBackLoop.leftThumbValue = 0

                startPosition = 0
                endPosition = duration

                textViewStartTime.text = "00:00"
                textViewEndTime.text = formatTime(duration)

                mp.setOnCompletionListener {
                    handleTrackCompletion()
                }

                val currentVolume = seekBarVolume.progress / 100f
                mp.setVolume(currentVolume, currentVolume)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при загрузке трека", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTrackCompletion() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                mediaPlayer?.seekTo(startPosition)
                mediaPlayer?.start()
            }
            RepeatMode.ALL -> {
                playNextTrack()
            }
            RepeatMode.NONE -> {
                if (currentTrackIndex < trackList.size - 1) {
                    playNextTrack()
                } else {
                    stopPlayback()
                }
            }
        }
    }

    private fun setupSeekBars() {
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    textViewCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarVolume.max = 100
        seekBarVolume.progress = 50
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                mediaPlayer?.setVolume(volume, volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rangeSeekBarPlayBackLoop.onRangeChangeListener = object : RangeSeekBar.OnRangeChangeListener {
            override fun onRangeChanged(leftValue: Int, rightValue: Int) {
                startPosition = leftValue
                endPosition = rightValue

                textViewStartTime.text = formatTime(startPosition)
                textViewEndTime.text = formatTime(endPosition)

                val minDifference = 1
                if (endPosition - startPosition < minDifference) {
                    if (startPosition + minDifference <= rangeSeekBarPlayBackLoop.maxValue) {
                        endPosition = startPosition + minDifference
                        rangeSeekBarPlayBackLoop.rightThumbValue = endPosition
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                pausePlayback()
            } else {
                startPlayback()
            }
        }

        buttonBackwardStep.setOnClickListener {
            playPreviousTrack()
        }

        buttonForwardStep.setOnClickListener {
            playNextTrack()
        }

        buttonBackward.setOnClickListener {
            mediaPlayer?.let { mp ->
                val newPosition = maxOf(mp.currentPosition - 10000, startPosition)
                mp.seekTo(newPosition)
            }
        }

        buttonForward.setOnClickListener {
            mediaPlayer?.let { mp ->
                val newPosition = minOf(mp.currentPosition + 10000, endPosition)
                mp.seekTo(newPosition)
            }
        }

        buttonRepeat.setOnClickListener {
            repeatMode = when (repeatMode) {
                RepeatMode.NONE -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.NONE
            }
            updateRepeatButton()
        }

        buttonShuffle.setOnClickListener {
            isShuffleEnabled = !isShuffleEnabled
            buttonShuffle.alpha = if (isShuffleEnabled) 1.0f else 0.5f
        }

        buttonPlaylist.setOnClickListener {
            showPlaylistDialog()
        }

        buttonVolumeUp.setOnClickListener {
            val newVolume = minOf(seekBarVolume.progress + 10, 100)
            seekBarVolume.progress = newVolume
        }

        buttonVolumeDown.setOnClickListener {
            val newVolume = maxOf(seekBarVolume.progress - 10, 0)
            seekBarVolume.progress = newVolume
        }
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                addTrackToList(uri)
            }
        }
    }

    private fun updateRepeatButton() {
        buttonRepeat.alpha = when (repeatMode) {
            RepeatMode.NONE -> 0.5f
            RepeatMode.ONE, RepeatMode.ALL -> 1.0f
        }
    }

    private fun startPlayback() {
        mediaPlayer?.let { mp ->
            if (mp.currentPosition < startPosition || mp.currentPosition > endPosition) {
                mp.seekTo(startPosition)
            }
            mp.start()
            isPlaying = true
            buttonPlayPause.text = getString(R.string.fa_pause_circle)
            startUpdatingSeekBar()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        buttonPlayPause.text = getString(R.string.fa_play_circle)
        stopUpdatingSeekBar()
    }

    private fun stopPlayback() {
        mediaPlayer?.let { mp ->
            mp.pause()
            mp.seekTo(startPosition)
            isPlaying = false
            buttonPlayPause.text = getString(R.string.fa_play_circle)
            seekBarProgress.progress = startPosition
            textViewCurrentTime.text = formatTime(startPosition)
            stopUpdatingSeekBar()
        }
    }

    private fun playNextTrack() {
        if (trackList.isEmpty()) return

        currentTrackIndex = if (isShuffleEnabled) {
            (0 until trackList.size).filter { it != currentTrackIndex }.randomOrNull() ?: 0
        } else {
            (currentTrackIndex + 1) % trackList.size
        }

        loadTrack(currentTrackIndex)
        if (isPlaying) {
            startPlayback()
        }
    }

    private fun playPreviousTrack() {
        if (trackList.isEmpty()) return

        currentTrackIndex = if (isShuffleEnabled) {
            (0 until trackList.size).filter { it != currentTrackIndex }.randomOrNull() ?: 0
        } else {
            if (currentTrackIndex - 1 < 0) trackList.size - 1 else currentTrackIndex - 1
        }

        loadTrack(currentTrackIndex)
        if (isPlaying) {
            startPlayback()
        }
    }

    private fun startUpdatingSeekBar() {
        updateRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (isPlaying) {
                        val currentPosition = mp.currentPosition
                        seekBarProgress.progress = currentPosition
                        textViewCurrentTime.text = formatTime(currentPosition)

                        if (currentPosition >= endPosition) {
                            handleTrackCompletion()
                        }

                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopUpdatingSeekBar() {
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun showPlaylistDialog() {
        if (playlistDialog != null && playlistDialog!!.isShowing) {
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPlaylist)
        val btnAddTrack = dialogView.findViewById<Button>(R.id.btnAddTrack)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PlaylistAdapter(trackList) { index ->
            loadTrack(index)
            if (isPlaying) {
                startPlayback()
            }
            playlistDialog?.dismiss()
        }
        recyclerView.adapter = adapter

        btnAddTrack.setOnClickListener {
            openFilePicker()
        }

        playlistDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        playlistDialog?.show()
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pausePlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdatingSeekBar()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

class PlaylistAdapter(
    private val tracks: List<MediaPlayerActivity.TrackInfo>,
    private val onTrackClick: (Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewTitle: TextView = view.findViewById(R.id.textViewTrackTitle)
        val textViewArtist: TextView = view.findViewById(R.id.textViewTrackArtist)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]
        holder.textViewTitle.text = track.title
        holder.textViewArtist.text = track.artist
        holder.itemView.setOnClickListener {
            onTrackClick(position)
        }
    }

    override fun getItemCount() = tracks.size
}