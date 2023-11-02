package org.mozilla.tiktokreporter.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.tiktokreporter.data.model.Policy
import org.mozilla.tiktokreporter.data.model.StudyDetails
import org.mozilla.tiktokreporter.data.model.StudyOverview
import org.mozilla.tiktokreporter.data.model.toPolicy
import org.mozilla.tiktokreporter.data.model.toStudyDetails
import org.mozilla.tiktokreporter.data.model.toStudyOverview
import org.mozilla.tiktokreporter.data.remote.TikTokReporterService
import org.mozilla.tiktokreporter.util.sharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TikTokReporterRepository @Inject constructor(
    private val tikTokReporterService: TikTokReporterService,
    @ApplicationContext private val context: Context
) {

    var selectedStudyId by context.sharedPreferences(name = "selected_study", defaultValue = "")
        private set
    var onboardingCompleted by context.sharedPreferences(name = "onboarding_completed", defaultValue = false)
        private set

    private var selectedStudy: StudyDetails? = null

    suspend fun getAppTermsAndConditions(): Result<Policy?> {
        val policies = try {
            withContext(Dispatchers.IO) {
                return@withContext tikTokReporterService.getAppTermsAndConditions()
                    .map { it.toPolicy() }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val policy = policies.firstOrNull { it.type == Policy.Type.TermsAndConditions }
        return Result.success(policy)
    }

    suspend fun fetchStudies(): Result<List<StudyOverview>> {
        val remoteStudies = try {
            tikTokReporterService.getStudies()
                .map {
                    it.toStudyOverview(
                        isSelected = it.id == selectedStudyId
                    )
                }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.success(remoteStudies)
    }

    suspend fun fetchStudyById(studyId: String): Result<StudyDetails> {
        val remoteStudy = try {
            tikTokReporterService.getStudyById(studyId)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.success(remoteStudy.toStudyDetails())
    }

    suspend fun getSelectedStudy(): Result<StudyDetails> {
        if (selectedStudy == null) {
            val study = fetchStudyById(selectedStudyId)

            if (study.isSuccess) {
                selectedStudy = study.getOrNull()!!
            }

            return study
        }

        return Result.success(selectedStudy!!)
    }

    suspend fun selectStudy(studyId: String) {
        withContext(Dispatchers.IO) {
            selectedStudyId = studyId
        }
    }
}