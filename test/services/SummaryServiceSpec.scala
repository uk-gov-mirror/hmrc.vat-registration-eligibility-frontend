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

package services

import java.time.LocalDate

import common.enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.FutureAssertions
import mocks.{EligibilityServiceMock, VatMocks}
import models.CurrentProfile
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SummaryServiceSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAwaits with DefaultAwaitTimeout
                         with FutureAssertions with VatRegistrationFixture with EligibilityServiceMock {
  implicit val hc = HeaderCarrier()

  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
    VatRegStatus.draft,Some(LocalDate.of(2016, 12, 21)))

  class Setup {
    val service = new SummaryService {
      override val  questionsService = mockEligibilityService
    }
  }

  "Calling Eligibility Summary" should {
    "return a valid summary with the right amount of sections from a valid api model" in new Setup {
      mockGetEligibility(Future.successful(validEligibility))
      val response = await(service.getEligibilitySummary)
      response.sections.length mustBe 6
      response.sections(0).id mustBe "nationalInsurance"
      response.sections(1).id mustBe "internationalBusiness"
      response.sections(2).id mustBe "otherBusiness"
      response.sections(3).id mustBe "otherVatScheme"
      response.sections(4).id mustBe "vatExemption"
      response.sections(5).id mustBe "resources"
    }
  }
}