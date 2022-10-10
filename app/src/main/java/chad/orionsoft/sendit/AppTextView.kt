package chad.orionsoft.sendit


import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet

class AppTextView : androidx.appcompat.widget.AppCompatTextView {


    constructor(ctx:Context) :super(ctx) {
        val tface=Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }

    constructor(ctx:Context,attrs: AttributeSet) :super(ctx,attrs) {
        val tface=Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }

    constructor(ctx:Context,attrs:AttributeSet,defStyle:Int) : super(ctx,attrs,defStyle) {
        val tface=Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }
}
