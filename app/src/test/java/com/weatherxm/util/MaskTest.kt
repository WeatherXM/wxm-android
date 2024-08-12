package com.weatherxm.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MaskTest : BehaviorSpec({
    val hash = "0x0123456789ABCDEF000000000000000000000000"

    given("a hash, mask it by including a mask character in the middle") {
        then("the masked hash should be returned") {
            Mask.maskHash(hash) shouldBe "0x0123*****0000"
        }
    }
})
