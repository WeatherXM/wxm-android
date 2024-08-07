package com.weatherxm

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.names.DuplicateTestNameMode

object TestConfig : AbstractProjectConfig() {
    override val duplicateTestNameMode = DuplicateTestNameMode.Error
}
