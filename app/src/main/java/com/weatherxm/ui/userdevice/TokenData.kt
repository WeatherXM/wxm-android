package com.weatherxm.ui.userdevice

data class TokenData(
    var tokens24h: TokenSummary,
    var tokens7d: TokenSummary,
    var tokens30d: TokenSummary
)

data class TokenSummary(
    var total: Float,
    var values: MutableList<Pair<String, Float>>
)
