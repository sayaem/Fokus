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
}
