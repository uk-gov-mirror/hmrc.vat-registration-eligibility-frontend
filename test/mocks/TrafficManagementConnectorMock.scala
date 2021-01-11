/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.{AllocationResponse, TrafficManagementConnector}
import models.RegistrationInformation
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait TrafficManagementConnectorMock extends MockitoSugar {
  self: Suite =>

  val mockTrafficManagementConnector = mock[TrafficManagementConnector]

  def mockAllocation(regId: String)(response: Future[AllocationResponse]) =
    when(mockTrafficManagementConnector.allocate(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockGetRegistrationInformation()(response: Future[Option[RegistrationInformation]]) =
    when(mockTrafficManagementConnector.getRegistrationInformation()(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockUpsertRegistrationInformation(regInfo: RegistrationInformation)(response: Future[RegistrationInformation]) =
    when(mockTrafficManagementConnector.upsertRegistrationInformation(ArgumentMatchers.eq(regInfo))(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any()))
      .thenReturn(response)

}
