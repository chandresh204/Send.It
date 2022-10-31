package chad.orionsoft.sendit

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import chad.orionsoft.sendit.databinding.ActivityVideoPlayerBinding

class VideoPlayerQ : AppCompatActivity() {

    lateinit var title:String
    var duration:Int=0
    lateinit var uri:Uri
    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title=intent.getStringExtra("title") ?: "title"
        val uriStr=intent.getStringExtra("uri")
        uri = Uri.parse(uriStr)
        duration=intent.getIntExtra("duration",0)

        binding.videoTitleText.text=title
        binding.videoDuration.text=Connection.formatDuration(duration)

        //video_view.setVideoPath(path)

        binding.videoView.setVideoURI(uri)
        binding.videoView.start()

        binding.videoSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress=seekBar?.progress ?: 0
                binding.videoView.seekTo(duration*progress/100)
            }

        })

        val handler= Handler(mainLooper)
        val runnable=object:Runnable {
            override fun run() {
                val progress= binding.videoView.currentPosition*100/duration
                binding.videoSeek.progress=progress
                binding.videoPlayingTime.text=Connection.formatDuration(binding.videoView.currentPosition)
                handler.postDelayed(this,500)
            }
        }
        handler.post(runnable)

        binding.videoView.setOnCompletionListener {
            binding.stopVideoPlayButton.visibility= View.GONE
            binding.replayVideoButton.visibility= View.VISIBLE
        }

        binding.stopVideoPlayButton.setOnClickListener {
            binding.videoView.stopPlayback()
            handler.removeCallbacks(runnable)
            finish()
        }

        binding.replayVideoButton.setOnClickListener {
            binding.videoView.stopPlayback()
            binding.videoView.setVideoURI(uri)
            binding.videoView.start()
            binding.stopVideoPlayButton.visibility= View.VISIBLE
            binding.replayVideoButton.visibility= View.GONE
        }
    }
}
