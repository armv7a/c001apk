package com.example.c001apk.compose.logic.smid

/** TG-Twilight's explicit ShuzlmID import format. Other copied identifiers are ignored. */
object OfficialDeviceIdentity {
    fun parse(text: String): String {
        require(text.length <= 32768) { "复制内容过长，请重新从官方酷安复制" }
        val matches =
            Regex(
                    "^\\s*ShuzlmID\\s*[:：]\\s*([^\\r\\n]*)$",
                    setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
                )
                .findAll(text.replace("\uFEFF", ""))
                .toList()
        require(matches.size == 1) { "未识别到唯一的 ShuzlmID，请在官方同一页面点击“复制 OAID”5 次后粘贴完整内容" }
        return matches.single().groupValues[1].trim().also {
            require(valid(it)) { "数盟 ID 为空或格式不正确，请稍等后重新复制" }
        }
    }

    fun valid(id: String) =
        !id.equals("null", true) &&
            !id.equals("undefined", true) &&
            Regex("^[A-Za-z0-9_+/.=-]{8,4096}$").matches(id)
}
