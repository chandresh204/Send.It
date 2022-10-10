package chad.orionsoft.sendit

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.SeekBar
import chad.orionsoft.sendit.databinding.ActivityAudioPlayerBinding

class AudioPlayer : AppCompatActivity() {

    private lateinit var title: String
    private lateinit var artist: String
    private var albumArt: String? = null
    private var path: String? = null
    private var duration:Int=0
    private lateinit var mPlayer:MediaPlay
    private lateinit var seekBarHandler: Handler
    private lateinit var seekBarRunnable: Runnable
    private lateinit var binding: ActivityAudioPlayerBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = intent.getStringExtra("title")!!
        artist = intent.getStringExtra("artist")!!
        albumArt = intent.getStringExtra("albumArt")
        path = intent.getStringExtra("path")
        duration=intent.getIntExtra("duration",-1)
        binding.titleTextAudioPlayer.text = "$title - $artist"
        binding.titleTextAudioPlayer.isSelected = true

        if (albumArt != null)
            binding.albumArtPlaying.setImageBitmap(BitmapFactory.decodeFile(albumArt))

        mPlayer = MediaPlay()
        mPlayer.play(path!!)

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
            mPlayer.play(path!!)
        }
    }

    override fun onBackPressed() {
        mPlayer.stop()
        super.onBackPressed()
    }

    inner class MediaPlay {

        private val player = MediaPlayer()

        fun play(source:String) {
            player.setDataSource(source)

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

        fun seekTo(msec: Int) {
            player.seekTo(msec)
        }

        fun stop() {
            player.stop()
        }

        fun getCurrentPosition() = player.currentPosition
    }

}
