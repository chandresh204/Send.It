package chad.orionsoft.sendit

import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.View
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView

class Animator {
    companion object {

        fun moveUP_IN(view: View, duration: Long ) {
            TranslateAnimation(0f,0f,500f,0f).also {
                it.duration=duration
                view.startAnimation(it)
            }
        }

        fun moveDown_OUT(view:View, duration: Long) {
            TranslateAnimation(0f,0f,0f,500f).also {
                it.duration=duration
                view.startAnimation(it)
            }
        }

        fun moveRight_IN(view:View,duration:Long) {
            TranslateAnimation(-1000f,0f,0f,0f).also {
                it.duration=duration
                view.startAnimation(it)
            }
        }

        fun flipDrawable(view: ImageView, drawable:Drawable?, duration: Long) {
            val pivotX=(view.width/2).toFloat()
            val pivotY=(view.height/2).toFloat()
            val scaleIn=ScaleAnimation(1f,0f,1f,1f,pivotX,pivotY).also {
                it.duration=duration/2
            }
            val scaleOut=ScaleAnimation(0f,1f,1f,1f,pivotX,pivotY).also {
                it.duration=duration/2
            }

            view.startAnimation(scaleIn)
            view.postDelayed({
                view.setImageDrawable(drawable)
                view.startAnimation(scaleOut)
            },(duration/2))
        }
    }
}