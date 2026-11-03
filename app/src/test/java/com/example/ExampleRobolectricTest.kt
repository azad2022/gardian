package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("گاردین", appName)
  }

  @Test
  fun `test jalali conversion`() {
    val result = com.example.utils.JalaliCalendarHelper.getJalaliDateString(1781614800000L) // June 16, 2026
    assertEquals("۱۴۰۵/۰۳/۲۶ - ۱۶:۳۰", result)
  }
}
