package chad.orionsoft.sendit

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet

class AppEditText : androidx.appcompat.widget.AppCompatEditText {

    constructor(ctx: Context) :super(ctx) {
        val tface= Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }

    constructor(ctx: Context, attrs: AttributeSet) :super(ctx,attrs) {
        val tface= Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyle:Int) : super(ctx,attrs,defStyle) {
        val tface= Typeface.createFromAsset(ctx.assets,"fonts/opensans-regular.ttf")
        this.typeface=tface
    }
}