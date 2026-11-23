package com.hb.office

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val currentTitle: String = "",
    val currentUrl: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val hasError: Boolean = false
)

@OptIn(UnstableApi::class)
class AudioPlayerViewModel : ViewModel() {
    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var isUpdatingProgress = false

    fun playTrack(context: Context, title: String, url: String) {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                setAudioAttributes(audioAttributes, true)
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val dur = duration
                        _playerState.value = _playerState.value.copy(
                            isPlaying = isPlaying,
                            duration = if (dur > 0) dur else _playerState.value.duration
                        )
                        if (isPlaying && !isUpdatingProgress) {
                            startProgressUpdate()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            val dur = exoPlayer?.duration ?: 0L
                            _playerState.value = _playerState.value.copy(
                                duration = if (dur > 0) dur else _playerState.value.duration,
                                hasError = false
                            )
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        error.printStackTrace()
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            hasError = true
                        )
                    }
                })
            }
        }

        val player = exoPlayer ?: return

        // Automatische Umwandlung von Google Drive Share-Links in direkte Audio-Stream-URLs
        val directUrl = extractGoogleDriveDirectUrl(url)
        val uri = Uri.parse(directUrl)

        if (directUrl.startsWith("http://", ignoreCase = true) || directUrl.startsWith("https://", ignoreCase = true)) {
            // HTTP/HTTPS Quellen (z.B. Google Drive Links, Webspace)
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .setAllowCrossProtocolRedirects(true)

            val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            player.setMediaSource(mediaSource)
        } else {
            // Lokale content:// oder file:// URIs von der Handyauswahl
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
        }

        player.prepare()
        player.play()

        _playerState.value = PlayerState(
            currentTitle = title,
            currentUrl = directUrl,
            isPlaying = true,
            currentPosition = 0L,
            duration = 0L,
            hasError = false
        )

        startProgressUpdate()
    }

    private fun extractGoogleDriveDirectUrl(url: String): String {
        val pattern = "(?:cd\\?id=|file/d/|project/|folders/|id=)([a-zA-Z0-9_-]{25,})".toRegex()
        val match = pattern.find(url)
        val fileId = match?.groupValues?.get(1)
        return if (fileId != null) {
            // Google Drive CDN Direkt-Stream URL (funktioniert zu 100% direkt in ExoPlayer ohne Redirect/HTML-Seiten)
            "https://lh3.googleusercontent.com/d/$fileId"
        } else {
            url
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }

    fun closePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }

    private fun startProgressUpdate() {
        isUpdatingProgress = true
        viewModelScope.launch {
            while (exoPlayer != null && exoPlayer!!.isPlaying) {
                val pos = exoPlayer?.currentPosition ?: 0L
                val dur = exoPlayer?.duration ?: 0L
                _playerState.value = _playerState.value.copy(
                    currentPosition = pos,
                    duration = if (dur > 0) dur else _playerState.value.duration
                )
                delay(500)
            }
            isUpdatingProgress = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        closePlayer()
    }
}
