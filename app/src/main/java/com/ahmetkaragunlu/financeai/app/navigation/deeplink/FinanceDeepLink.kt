package com.ahmetkaragunlu.financeai.app.navigation.deeplink

import com.ahmetkaragunlu.financeai.core.deeplink.FinanceLinkContract
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface FinanceDeepLink {
    data class PasswordReset(val code: String) : FinanceDeepLink
    data class Schedule(val ownerId: String? = null) : FinanceDeepLink
}

/** External URI validation is independent of navigation serialization and account readiness. */
fun parseFinanceDeepLink(value: String): FinanceDeepLink? =
    try {
        val uri = URI(value)
        if (
            uri.scheme != FinanceLinkContract.SCHEME ||
                uri.rawUserInfo != null ||
                uri.port != -1 ||
                uri.rawFragment != null
        )
            null
        else
            when (uri.host) {
                FinanceLinkContract.FINANCE_HOST -> {
                    val parameters = parseParameters(uri.rawQuery)
                    if (
                        uri.path != FinanceLinkContract.SCHEDULE_PATH ||
                            parameters == null ||
                            parameters.keys.any {
                                it !in
                                    setOf(
                                        FinanceLinkContract.OWNER_QUERY,
                                        FinanceLinkContract.RECORD_QUERY,
                                    )
                            } ||
                            parameters.values.any(String::isBlank)
                    )
                        null
                    else FinanceDeepLink.Schedule(parameters[FinanceLinkContract.OWNER_QUERY])
                }
                FinanceLinkContract.RESET_HOST -> {
                    if (!uri.path.isNullOrEmpty() && uri.path != "/") null
                    else {
                        val parameters = parseParameters(uri.rawQuery)
                        val code = parameters?.get("oobCode")
                        if (
                            parameters == null ||
                                parameters["mode"]?.let { it != "resetPassword" } == true ||
                                code.isNullOrBlank() ||
                                code.any(Char::isISOControl)
                        )
                            null
                        else FinanceDeepLink.PasswordReset(code)
                    }
                }
                else -> null
            }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.net.URISyntaxException) {
        null
    }

private fun parseParameters(query: String?): Map<String, String>? {
    val parts =
        query.orEmpty().split('&').filter(String::isNotEmpty).map { part ->
            val pair = part.split('=', limit = 2)
            URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(pair.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
        }
    return parts.toMap().takeIf { it.size == parts.size }
}
