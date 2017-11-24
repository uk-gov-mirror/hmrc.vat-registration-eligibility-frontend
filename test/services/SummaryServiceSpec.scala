/*
 * Copyright 2017 HM Revenue & Customs
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

import common.enums.VatRegStatus
import helpers.VatRegSpec
import models.S4LVatEligibility
import models.api.VatScheme
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when

import scala.concurrent.Future

class SummaryServiceSpec extends VatRegSpec {



  class Setup {
    val service = new SummaryService(
      s4LService = mockS4LService,
      vatRegService = mockVatRegistrationService
    )
  }

  "Calling Eligibility Summary" should {
    "return a valid summary with the right amount of sections from s4l if present" in new Setup {
      when(mockS4LService.fetchAndGet[S4LVatEligibility]()(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(S4LVatEligibility(Some(validServiceEligibilityNoChoice)))))

      val response = await(service.getEligibilitySummary())

      response.sections.length shouldBe 6
      response.sections(0).id shouldBe "nationalInsurance"
      response.sections(1).id shouldBe "internationalBusiness"
      response.sections(2).id shouldBe "otherBusiness"
      response.sections(3).id shouldBe "otherVatScheme"
      response.sections(4).id shouldBe "vatExemption"
      response.sections(5).id shouldBe "resources"
    }
    "return a valid summary with the right amount of sections from vatScheme" in new Setup {
      when(mockS4LService.fetchAndGet[S4LVatEligibility]()(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockVatRegistrationService.getVatScheme()(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(VatScheme("foobar",Some(validServiceEligibilityNoChoice),VatRegStatus.draft)))

      val response = await(service.getEligibilitySummary())

      response.sections.length shouldBe 6
      response.sections(0).id shouldBe "nationalInsurance"
      response.sections(1).id shouldBe "internationalBusiness"
      response.sections(2).id shouldBe "otherBusiness"
      response.sections(3).id shouldBe "otherVatScheme"
      response.sections(4).id shouldBe "vatExemption"
      response.sections(5).id shouldBe "resources"
    }
  }
}