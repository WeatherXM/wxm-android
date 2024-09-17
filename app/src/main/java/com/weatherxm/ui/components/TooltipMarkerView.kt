import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.weatherxm.R
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Rewards.formatTokens

@SuppressLint("ViewConstructor")
class TooltipMarkerView(
    context: Context,
    private val dates: List<String>,
    private val totals: List<Float> = mutableListOf(),
    private val baseData: LineChartData = LineChartData.empty(),
    private val betaData: LineChartData = LineChartData.empty(),
    private val othersData: LineChartData = LineChartData.empty(),
) : MarkerView(context, R.layout.view_chart_tooltip) {
    private var dateView: TextView = findViewById(R.id.date)
    private var totalView: TextView = findViewById(R.id.totalValue)
    private var baseTitleView: TextView = findViewById(R.id.baseTitle)
    private var baseView: TextView = findViewById(R.id.baseValue)
    private var betaTitleView: TextView = findViewById(R.id.betaTitle)
    private var betaView: TextView = findViewById(R.id.betaValue)
    private var othersTitleView: TextView = findViewById(R.id.othersTitle)
    private var othersView: TextView = findViewById(R.id.othersValue)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    override fun refreshContent(e: Entry, highlight: Highlight?) {
        /**
         * We find the relevant timestamp by using the same index
         * as the entry's x to get the value in the dates list
         */
        dateView.text = dates.getOrNull(e.x.toInt()) ?: String.empty()

        /**
         * As explained in the comment in RewardsUseCase, the "chart with filled layers" to work
         * has "beta = base + beta", "others = base + beta + others", so in here to show the correct
         * value in tooltip we need to make the respective subtractions.
         */
        var baseValue = 0F
        var betaValue = 0F
        var othersValue = 0F

        baseData.isDataValid().takeIf { it }?.also {
            baseTitleView.visible(true)
            baseView.visible(true)
            baseValue = baseData.getEntryValueForTooltip(e.x)
            baseView.text = formatTokens(baseValue)
        } ?: run {
            baseTitleView.visible(false)
            baseView.visible(false)
        }
        betaData.isDataValid().takeIf { it }?.also {
            betaTitleView.visible(true)
            betaView.visible(true)
            betaValue = betaData.getEntryValueForTooltip(e.x)
            betaView.text = formatTokens((betaValue - baseValue).coerceAtLeast(0F))
        } ?: run {
            betaTitleView.visible(false)
            betaView.visible(false)
        }
        othersData.isDataValid().takeIf { it }?.also {
            othersTitleView.visible(true)
            othersView.visible(true)
            othersValue = othersData.getEntryValueForTooltip(e.x)
            othersView.text = if (betaValue > 0) {
                formatTokens((othersValue - betaValue).coerceAtLeast(0F))
            } else {
                formatTokens((othersValue - baseValue).coerceAtLeast(0F))
            }
        } ?: run {
            othersTitleView.visible(false)
            othersView.visible(false)
        }

        /**
         * In Rewards Breakdown chart we cannot get the total from e.y that's why we use
         * the explicit `totals` list in order to get the correct number
         */
        if(totals.isEmpty()) {
            totalView.text = formatTokens(e.y)
        } else {
            totalView.text = formatTokens(totals.getOrNull(e.x.toInt()) ?: 0F)
        }

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
            /**
             * If baseEntries is not null or empty that means that we are in the "Rewards Breakdown"
             * chart which is significantly bigger so we should give it more space in the Y axis
             */
            newPosY = if (!baseData.isDataValid()) {
                (canvas.height / 2).toFloat()
            } else {
                (canvas.height / 3).toFloat()
            }
        }

        canvas.translate(newPosX, newPosY)
        draw(canvas)
    }
}
