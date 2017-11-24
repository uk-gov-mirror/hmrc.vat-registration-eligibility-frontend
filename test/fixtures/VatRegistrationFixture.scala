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

package fixtures

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import common.enums.VatRegStatus
import models.api._
import models.external.{IncorporationInfo, _}
import models.view.{Summary, SummaryRow, SummarySection}
import play.api.http.Status._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.http.HttpResponse

trait VatRegistrationFixture extends TradingDetailsFixture {

  //Test variables
  val testRegId = "VAT123456"
  val validHttpResponse = HttpResponse(OK)

  //Api models
  val validServiceEligibility          = VatServiceEligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false), Some(validVatChoice))
  val validServiceEligibilityNoChoice  = VatServiceEligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false))

  val validVatThresholdPostIncorp      = VatThresholdPostIncorp(overThresholdSelection = false, None)
  val validExpectedVatThresholdPostIncorp      = VatExpectedThresholdPostIncorp(expectedOverThresholdSelection = true, Some(LocalDate.now()))
  val emptyVatScheme = VatScheme(testRegId, status = VatRegStatus.draft)

  val validVatScheme = VatScheme(
    id = testRegId,
    status =VatRegStatus.draft,
    vatServiceEligibility = Some(validServiceEligibility)
  )

  val testIncorporationInfo = IncorporationInfo(
    IncorpSubscription(
      transactionId = "000-434-23",
      regime = "vat",
      subscriber = "scrs",
      callbackUrl = "http://localhost:9896/TODO-CHANGE-THIS"),
    IncorpStatusEvent(
      status = "accepted",
      crn = Some("90000001"),
      incorporationDate = Some(LocalDate.of(2016, 8, 5)),
      description = Some("Some description")))

  def vatScheme(id: String = testRegId, vatServiceEligibility: Option[VatServiceEligibility] = None): VatScheme =
    VatScheme(
      id = id,
      status = VatRegStatus.draft,
      vatServiceEligibility = vatServiceEligibility
    )

  val nationalInsuiranceSection = SummarySection(
    "nationalInsurance",
    rows = Seq((SummaryRow("nationalInsurance.hasNino","app.common.yes",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "haveNino"))),true))
  )

  val internationalBusinessSection = SummarySection(
    "internationalBusiness",
    rows = Seq(
      (SummaryRow("internationalBusiness.sellGoods","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad"))),true),
      (SummaryRow("internationalBusiness.buyGoods","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad"))),true),
      (SummaryRow("internationalBusiness.sellAssets","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad"))),true),
      (SummaryRow("internationalBusiness.sellGoodsServices","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad"))),true),
      (SummaryRow("internationalBusiness.doBusiness","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad"))),true)
    )
  )

  val otherBusinessSection = SummarySection(
    "otherBusiness",
    rows = Seq(
      (SummaryRow("otherBusiness.soleTrader","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou"))),true),
      (SummaryRow("otherBusiness.vatGroup","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou"))),true),
      (SummaryRow("otherBusiness.makingProfit","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou"))),true),
      (SummaryRow("otherBusiness.limitedCompany","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou"))),true)
    )
  )

  val otherVatSchemeSection = SummarySection(
    "otherVatScheme",
    rows = Seq(
      (SummaryRow("otherVatScheme.agriculturalFlat","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "applyingForAnyOf"))),true),
      (SummaryRow("otherVatScheme.accountingScheme","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "applyingForAnyOf"))),true)
    )
  )

  val vatExemptionSection = SummarySection(
    "vatExemption",
    rows = Seq(
      (SummaryRow("vatExemption.vatException","app.common.no",Some(controllers.routes.EligibilityController.showExemptionCriteria())),true),
      (SummaryRow("vatExemption.vatExemption","app.common.no",Some(controllers.routes.EligibilityController.showExemptionCriteria())),true)
    )
  )

  val resourcesSection = SummarySection(
    "resources",
    rows = Seq(
      (SummaryRow("resources.companyOwn","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "companyWillDoAnyOf"))),true),
      (SummaryRow("resources.companySell","app.common.no",Some(controllers.routes.ServiceCriteriaQuestionsController.show(question = "companyWillDoAnyOf"))),true)
    )
  )

  val validEligibilitySummary = Summary(Seq(
    nationalInsuiranceSection,
    internationalBusinessSection,
    otherBusinessSection,
    otherVatSchemeSection,
    vatExemptionSection,
    resourcesSection
  ))
}
