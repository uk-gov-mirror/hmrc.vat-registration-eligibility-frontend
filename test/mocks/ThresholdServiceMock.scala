/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mocks

import models.view.Threshold
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import services.ThresholdService

import scala.concurrent.Future

trait ThresholdServiceMock {
this: MockitoSugar =>

  lazy val mockThresholdService: ThresholdService = mock[ThresholdService]

  def mockGetThreshold(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.getThreshold(any(), any())) thenReturn res
  }

  def mockCurrentVatThreshold(res: Future[String]): OngoingStubbing[Future[String]] = {
    when(mockThresholdService.fetchCurrentVatThreshold(any())) thenReturn res
  }

  def mockSaveOverThresholdThirtyPreIncorp(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveOverThresholdThirtyDaysPreIncorp(any())(any(), any())) thenReturn res
  }

  def mockSaveVoluntaryRegistration(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveVoluntaryRegistration(any())(any(), any())) thenReturn res
  }

  def mockSaveVoluntaryRegistrationReason(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveVoluntaryRegistrationReason(any())(any(), any())) thenReturn res
  }

  def mockSaveOverThreshold(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveOverThresholdThirtyDays(any())(any(), any())) thenReturn res
  }

  def mockSaveOverTwelveThreshold(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveOverThresholdTwelveMonths(any())(any(), any())) thenReturn res
  }

  def mockSaveOverSinceIncorp(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveOverThresholdSinceIncorp(any())(any(), any())) thenReturn res
  }

  def mockPastThirty(res: Future[Threshold]): OngoingStubbing[Future[Threshold]] = {
    when(mockThresholdService.saveOverThresholdPastThirtyDays(any())(any(), any())) thenReturn res
  }

  def mockFetchCurrentVatThreshold(returns: Future[String]): OngoingStubbing[Future[String]] = {
    when(mockThresholdService.fetchCurrentVatThreshold(any()))
      .thenReturn(returns)
  }
}
