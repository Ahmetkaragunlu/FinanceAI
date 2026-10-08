package com.ahmetkaragunlu.financeai.app.navigation.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceDeepLinkTest {
    @Test
    fun existingScheduleAndResetLinksAreRecognised() {
        assertEquals(FinanceDeepLink.Schedule(), parseFinanceDeepLink("financeai://main/schedule"))
        assertEquals(
            FinanceDeepLink.Schedule("account-a"),
            parseFinanceDeepLink("financeai://main/schedule?owner=account-a&record=plan-id"),
        )
        assertEquals(
            FinanceDeepLink.PasswordReset("test-code"),
            parseFinanceDeepLink("financeai://resetPassword?oobCode=test-code"),
        )
        assertEquals(
            FinanceDeepLink.PasswordReset("a+b"),
            parseFinanceDeepLink("financeai://resetPassword?mode=resetPassword&oobCode=a%2Bb"),
        )
    }

    @Test
    fun unsupportedTargetsAndAmbiguousResetCodesAreRejected() {
        for (value in
            listOf(
                "https://main/schedule",
                "financeai://other/schedule",
                "financeai://main/schedule/other",
                "financeai://main/schedule?redirect=evil",
                "financeai://user@main/schedule",
                "financeai://main:123/schedule",
                "financeai://main/schedule#fragment",
                "financeai://resetPassword",
                "financeai://resetPassword?oobCode=",
                "financeai://resetPassword?oobCode=one&oobCode=two",
                "financeai://resetPassword?mode=verifyEmail&oobCode=code",
                "financeai://resetPassword/path?oobCode=code",
                "financeai://resetPassword?oobCode=%00",
                "financeai://resetPassword?oobCode=%ZZ",
            )) assertNull("Invalid link must not navigate", parseFinanceDeepLink(value))
    }
}
