package ui.util

import com.weatherxm.R
import com.weatherxm.ui.common.AnnotationGroupCode
import com.weatherxm.util.Rewards.formatTokens
import com.weatherxm.util.Rewards.getRewardIcon
import com.weatherxm.util.Rewards.getRewardScoreColor
import com.weatherxm.util.Rewards.isPoL
import com.weatherxm.util.Rewards.weiToETH
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RewardsTest : ShouldSpec() {
    init {
        should("Get Reward Score Color") {
            getRewardScoreColor(null) shouldBe R.color.reward_score_unknown
            getRewardScoreColor(-5) shouldBe R.color.reward_score_unknown
            getRewardScoreColor(0) shouldBe R.color.error
            getRewardScoreColor(5) shouldBe R.color.error
            getRewardScoreColor(15) shouldBe R.color.warning
            getRewardScoreColor(35) shouldBe R.color.warning
            getRewardScoreColor(55) shouldBe R.color.warning
            getRewardScoreColor(75) shouldBe R.color.warning
            getRewardScoreColor(95) shouldBe R.color.green
            getRewardScoreColor(100) shouldBe R.color.green
        }

        should("Get Reward Score Icon") {
            getRewardIcon(null) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(-5) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(0) shouldBe R.drawable.ic_error_hex_filled
            getRewardIcon(5) shouldBe R.drawable.ic_error_hex_filled
            getRewardIcon(15) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(35) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(55) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(75) shouldBe R.drawable.ic_warning_hex_filled
            getRewardIcon(95) shouldBe R.drawable.ic_checkmark_hex_filled
            getRewardIcon(100) shouldBe R.drawable.ic_checkmark_hex_filled
        }

        should("Format Tokens") {
            formatTokens(0F) shouldBe "0.00"
            formatTokens(10F) shouldBe "10.00"
            formatTokens(10.01F) shouldBe "10.01"
            formatTokens(10.001F) shouldBe "10.001"
        }

        should("Convert WEI -> ETH") {
            weiToETH(BigDecimal.ZERO) shouldBe BigDecimal.ZERO
            formatTokens(weiToETH(9.9999999E14F.toBigDecimal())) shouldBe "0.001"
        }

        should("Get if an AnnotationGroupCode is Proof-of-Location related") {
            AnnotationGroupCode.NO_WALLET.isPoL() shouldBe false
            AnnotationGroupCode.LOCATION_NOT_VERIFIED.isPoL() shouldBe true
            AnnotationGroupCode.NO_LOCATION_DATA.isPoL() shouldBe true
            AnnotationGroupCode.USER_RELOCATION_PENALTY.isPoL() shouldBe true
            AnnotationGroupCode.CELL_CAPACITY_REACHED.isPoL() shouldBe false
            AnnotationGroupCode.UNKNOWN.isPoL() shouldBe false
        }

        afterTest {
            println("[${it.b.name.uppercase()}] - ${it.a.name.testName}")
        }
    }
}
