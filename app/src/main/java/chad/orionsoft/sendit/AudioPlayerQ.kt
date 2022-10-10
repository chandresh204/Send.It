package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Size
import android.view.View
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import chad.orionsoft.sendit.databinding.ActivityAudioPlayerBinding
import java.lang.Exception

class AudioPlayerQ : AppCompatActivity() {

    private lateinit var title: String
    private lateinit var artist: String
    private var albumArt: Uri? = null
    private var uri: Uri? = null
    private var duration:Int=0
    private lateinit var mPlayer:MediaPlay
    private lateinit var seekBarHandler: Handler
    private lateinit var seekBarRunnable: Runnable
    private lateinit var binding: ActivityAudioPlayerBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = intent.getStringExtra("title")!!
        artist = intent.getStringExtra("artist")!!
        albumArt = Uri.parse(intent.getStringExtra("albumArt"))
        uri = Uri.parse(intent.getStringExtra("uri"))
        duration=intent.getIntExtra("duration",-1)
        binding.titleTextAudioPlayer.text = "$title - $artist"
        binding.titleTextAudioPlayer.isSelected = true

        val bitmap = try {
            contentResolver.loadThumbnail(albumArt!!, Size(800,800), null)
        } catch (e: Exception) {
            BitmapFactory.decodeResource(resources, R.drawable.music_icon)
        }
        binding.albumArtPlaying.setImageBitmap(bitmap)

        mPlayer = MediaPlay()
        mPlayer.play(uri!!)

        binding.audioSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, p2: Boolean) { }

            override fun onStartTrackingTouch(p0: SeekBar?) { }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val fullPlay = duration
                val currentSeekTo = fullPlay * seekBar!!.progress / 100
                mPlayer.seekTo(currentSeekTo)
            }

        })

        seekBarHandler = Handler(mainLooper)
        seekBarRunnable = object : Runnable {
            override fun run() {
                val progress = mPlayer.getCurrentPosition() * 100 / duration
                binding.audioSeek.progress = progress
                binding.durationText.text = Connection.formatDuration(duration)
                binding.playingTime.text= Connection.formatDuration(mPlayer.getCurrentPosition())
                seekBarHandler.postDelayed(this, 500)
            }
        }
        seekBarHandler.post(seekBarRunnable)

        binding.cancelPlay.setOnClickListener {
            mPlayer.stop()
            finish()
        }

        binding.playAgainButton.setOnClickListener {
            mPlayer=MediaPlay()
            mPlayer.play(uri!!)
        }
    }

    override fun onBackPressed() {
        mPlayer.stop()
        super.onBackPressed()
    }

    inner class MediaPlay {

        private val player = MediaPlayer()

        fun play(source:Uri) {
            player.setDataSource(applicationContext, source)

            player.prepareAsync()

            binding.cancelPlay.visibility = View.VISIBLE
            binding.playAgainButton.visibility = View.GONE

            player.setOnPreparedListener {
                it.start()
                player.setOnCompletionListener {
                    binding.cancelPlay.visibility = View.GONE
                    binding.playAgainButton.visibility = View.VISIBLE
                }
            }
        }

        fun seekTo(mSec: Int) {
            player.seekTo(mSec)
        }

        fun stop() {
            player.stop()
        }

        fun getCurrentPosition() = player.currentPosition
    }
}