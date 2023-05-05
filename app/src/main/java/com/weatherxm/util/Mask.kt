package com.weatherxm.util

object Mask {

    private const val DEFAULT_OFFSET_START = 5
    private const val DEFAULT_OFFSET_END = 5
    private const val DEFAULT_MASKED_CHARACTERS_TO_SHOW = 5
    private const val DEFAULT_MASK_CHARACTER = '*'

    public fun maskHash(
        hash: String,
        offsetStart: Int = DEFAULT_OFFSET_START,
        offsetEnd: Int = DEFAULT_OFFSET_END,
        maxMaskedChars: Int = DEFAULT_MASKED_CHARACTERS_TO_SHOW,
        maskCharacter: Char = DEFAULT_MASK_CHARACTER
    ): String {
        var addressToShow = ""
        var counter = 0
        var maskedCharacters = 0
        hash.forEach {
            if (counter <= offsetStart || counter > hash.length - offsetEnd) {
                addressToShow += it
            } else if (maskedCharacters++ < maxMaskedChars) {
                addressToShow += maskCharacter
            }
            counter++
        }
        return addressToShow
    }
}
