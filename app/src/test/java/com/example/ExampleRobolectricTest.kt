package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LearningStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context matches FOKUS`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FOKUS", appName)
  }

  @Test
  fun `learning status progress calculation weight is correct`() {
    val completedWeight = when (LearningStatus.COMPLETED) {
      LearningStatus.COMPLETED -> 100
      LearningStatus.IN_PROGRESS -> 50
      LearningStatus.NOT_STARTED -> 0
      LearningStatus.SKIPPED -> 0
    }
    assertEquals(100, completedWeight)

    val inProgressWeight = when (LearningStatus.IN_PROGRESS) {
      LearningStatus.COMPLETED -> 100
      LearningStatus.IN_PROGRESS -> 50
      LearningStatus.NOT_STARTED -> 0
      LearningStatus.SKIPPED -> 0
    }
    assertEquals(50, inProgressWeight)
  }

  @Test
  fun `bulk chapter title cleaner strips numbering and prefixes`() {
    val raw1 = "1. Physical World & Measurement"
    val raw2 = "Chapter 4: Newtonian Mechanics"
    val raw3 = "অধ্যায় ৩: গতিবিদ্যা"

    val cleaned1 = raw1.trim().replaceFirst(Regex("^[0-9০-৯]+[:.)\\-]\\s*"), "")
    val cleaned2 = raw2.trim().replaceFirst(Regex("^(Chapter|অধ্যায়|Ch\\.?|No\\.?)\\s*[0-9০-৯]+[:.\\-]?\\s*", RegexOption.IGNORE_CASE), "")
    val cleaned3 = raw3.trim().replaceFirst(Regex("^(Chapter|অধ্যায়|Ch\\.?|No\\.?)\\s*[0-9০-৯]+[:.\\-]?\\s*", RegexOption.IGNORE_CASE), "")

    assertEquals("Physical World & Measurement", cleaned1)
    assertEquals("Newtonian Mechanics", cleaned2)
    assertEquals("গতিবিদ্যা", cleaned3)
  }

  @Test
  fun `admission countdown future deadline formats correctly in English and Bangla`() {
    val now = 1700000000000L // arbitrary fixed time
    val targetIn5Days = now + (5 * 24 * 3600 * 1000L)

    val event = com.example.data.model.AdmissionEventEntity(
      universityId = "uni_du",
      title = "DU A Unit Application Deadline",
      eventType = com.example.data.model.AdmissionEventType.APPLICATION_DEADLINE,
      deadlineDateTimeMillis = targetIn5Days,
      isDateConfirmed = true
    )

    val resultEn = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
      event = event,
      language = com.example.ui.localization.AppLanguage.ENGLISH,
      nowMillis = now
    )
    assertEquals("5 days left", resultEn.label)
    assertEquals(false, resultEn.isPassed)
    assertEquals(false, resultEn.isUnconfirmed)

    val resultBn = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
      event = event,
      language = com.example.ui.localization.AppLanguage.BANGLA,
      nowMillis = now
    )
    assertEquals("৫ দিন বাকি", resultBn.label)
  }

  @Test
  fun `admission countdown unconfirmed date returns localized not confirmed message`() {
    val event = com.example.data.model.AdmissionEventEntity(
      universityId = "uni_buet",
      title = "BUET Preliminary Test",
      eventType = com.example.data.model.AdmissionEventType.ADMISSION_TEST,
      isDateConfirmed = false
    )

    val resultEn = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
      event = event,
      language = com.example.ui.localization.AppLanguage.ENGLISH
    )
    assertEquals("Date not confirmed", resultEn.label)
    assertTrue(resultEn.isUnconfirmed)

    val resultBn = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
      event = event,
      language = com.example.ui.localization.AppLanguage.BANGLA
    )
    assertEquals("তারিখ এখনো নির্ধারিত হয়নি", resultBn.label)
    assertTrue(resultBn.isUnconfirmed)
  }

  @Test
  fun `admission countdown deadline passed with incomplete status flags urgent review`() {
    val now = 1700000000000L
    val pastDeadline = now - (2 * 3600 * 1000L) // 2 hours ago

    val event = com.example.data.model.AdmissionEventEntity(
      universityId = "uni_ru",
      title = "RU Application Deadline",
      eventType = com.example.data.model.AdmissionEventType.APPLICATION_DEADLINE,
      deadlineDateTimeMillis = pastDeadline,
      isDateConfirmed = true
    )

    val result = com.example.admission.AdmissionCountdownEngine.calculateCountdown(
      event = event,
      applicationStatus = com.example.data.model.ApplicationStatus.IN_PROGRESS,
      language = com.example.ui.localization.AppLanguage.ENGLISH,
      nowMillis = now
    )
    assertEquals("Deadline passed", result.label)
    assertTrue(result.isUrgent)
    assertTrue(result.isPassed)
  }

  @Test
  fun `admission application and payment statuses remain independent`() {
    val app = com.example.data.model.AdmissionApplicationEntity(
      universityId = "uni_cu",
      unitName = "A Unit (Science)",
      applicationStatus = com.example.data.model.ApplicationStatus.SUBMITTED,
      paymentStatus = com.example.data.model.PaymentStatus.PAYMENT_PENDING
    )

    assertEquals(com.example.data.model.ApplicationStatus.SUBMITTED, app.applicationStatus)
    assertEquals(com.example.data.model.PaymentStatus.PAYMENT_PENDING, app.paymentStatus)
  }
}

