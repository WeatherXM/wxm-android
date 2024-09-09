import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.weatherxm.R
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Rewards.formatTokens

@SuppressLint("ViewConstructor")
class TooltipMarkerView(
    context: Context,
    private val dates: List<String>,
) : MarkerView(context, R.layout.view_chart_tooltip) {
    private var dateView: TextView = findViewById(R.id.date)
    private var totalView: TextView = findViewById(R.id.totalValue)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    override fun refreshContent(e: Entry, highlight: Highlight?) {
        /**
         * We find the relevant timestamp by using the same index
         * as the entry's x to get the value in the dates list
         */
        dateView.text = dates.getOrNull(e.x.toInt()) ?: String.empty()
        totalView.text = formatTokens(e.y)
        super.refreshContent(e, highlight)
    }

    @Suppress("MagicNumber")
    override fun draw(canvas: Canvas, posx: Float, posy: Float) {
        // translate to the correct position and draw
        var newPosX = posx
        var newPosY = posy
        // Prevent overflow to the right
        if (posx > canvas.width / 2) {
            newPosX = (canvas.width / 2).toFloat()
        }

        // We do this as for continuous 0 values on the y Axis the marker view hides those values
        if (posy > canvas.height / 2) {
            newPosY = (canvas.height / 2).toFloat()
        }

        canvas.translate(newPosX, newPosY)
        draw(canvas)
    }
}
