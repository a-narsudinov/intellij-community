// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gitlab.mergerequest.diff.action

import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JButtonAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gitlab.mergerequest.ui.review.GitLabMergeRequestOnCurrentBranchService
import org.jetbrains.plugins.gitlab.mergerequest.ui.review.GitLabMergeRequestReviewViewModel
import org.jetbrains.plugins.gitlab.mergerequest.ui.review.GitLabMergeRequestSubmitReviewPopup
import org.jetbrains.plugins.gitlab.util.GitLabBundle

internal class GitLabMergeRequestDiffSubmitReviewAction
  : JButtonAction(CollaborationToolsBundle.message("review.start.submit.action")) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val vm = e.getData(GitLabMergeRequestReviewViewModel.DATA_KEY) ?: e.project?.let(::findGlobalVm)
    val review = vm?.submittableReview?.value
    e.presentation.isEnabledAndVisible = review != null

    if (ActionPlaces.isPopupPlace(e.place) && !e.place.contains("gitlab", true)) {
      e.presentation.text = GitLabBundle.message("merge.request.review.submit.action")
      return
    }

    val draftCommentsCount = review?.draftComments ?: 0
    e.presentation.text = if (draftCommentsCount <= 0) {
      CollaborationToolsBundle.message("review.start.submit.action")
    }
    else {
      CollaborationToolsBundle.message("review.start.submit.action.with.comments", draftCommentsCount)
    }
  }

  private fun findGlobalVm(project: Project): GitLabMergeRequestReviewViewModel? =
    project.serviceIfCreated<GitLabMergeRequestOnCurrentBranchService>()?.mergeRequestReviewVmState?.value

  override fun actionPerformed(e: AnActionEvent) {
    //TODO: show under branch widget component - requires fixing action popup context propagation
    val vm = e.getData(GitLabMergeRequestReviewViewModel.DATA_KEY) ?: e.project?.let(::findGlobalVm) ?: return
    val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY) ?: e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    // looks fishy but spares us the need to pass component to VM
    vm.submitReviewInputHandler = {
      withContext(Dispatchers.Main) {
        if (component != null) GitLabMergeRequestSubmitReviewPopup.show(it, component)
        else GitLabMergeRequestSubmitReviewPopup.show(it, e.project!!)
      }
    }
    vm.submitReview()
  }
}