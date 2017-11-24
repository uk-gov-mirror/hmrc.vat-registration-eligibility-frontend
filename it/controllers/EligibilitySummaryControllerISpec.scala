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

package controllers


import helpers.RequestsFinder
import models.api.VatServiceEligibility

import models.S4LVatEligibility
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import support.AppAndStubs

class EligibilitySummaryControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {

  val validEligibilityData = VatServiceEligibility(
    haveNino                = Some(true),
    doingBusinessAbroad     = Some(false),
    doAnyApplyToYou         = Some(false),
    applyingForAnyOf        = Some(false),
    applyingForVatExemption = Some(false),
    companyWillDoAnyOf      = Some(false)
  )

  val s4lData = Json.toJson(S4LVatEligibility(
    vatEligibility = Some(validEligibilityData)
  ))

  def validateEligibilitySummaryPage(document: Document) = {
    document.title() mustBe "Check and confirm your answers"
    document.getElementById("nationalInsurance.hasNinoAnswer").text mustBe "Yes"
    document.getElementById("nationalInsurance.hasNinoChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "haveNino").url

    document.getElementById("internationalBusiness.sellGoodsAnswer").text mustBe "No"
    document.getElementById("internationalBusiness.buyGoodsAnswer").text mustBe "No"
    document.getElementById("internationalBusiness.sellAssetsAnswer").text mustBe "No"
    document.getElementById("internationalBusiness.sellGoodsServicesAnswer").text mustBe "No"
    document.getElementById("internationalBusiness.doBusinessAnswer").text mustBe "No"
    document.getElementById("internationalBusiness.sellGoodsChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad").url
    document.getElementById("internationalBusiness.buyGoodsChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad").url
    document.getElementById("internationalBusiness.sellAssetsChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad").url
    document.getElementById("internationalBusiness.sellGoodsServicesChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad").url
    document.getElementById("internationalBusiness.doBusinessChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doingBusinessAbroad").url

    document.getElementById("otherBusiness.soleTraderAnswer").text mustBe "No"
    document.getElementById("otherBusiness.vatGroupAnswer").text mustBe "No"
    document.getElementById("otherBusiness.makingProfitAnswer").text mustBe "No"
    document.getElementById("otherBusiness.limitedCompanyAnswer").text mustBe "No"
    document.getElementById("otherBusiness.soleTraderChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou").url
    document.getElementById("otherBusiness.vatGroupChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou").url
    document.getElementById("otherBusiness.makingProfitChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou").url
    document.getElementById("otherBusiness.limitedCompanyChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "doAnyApplyToYou").url

    document.getElementById("otherVatScheme.agriculturalFlatAnswer").text mustBe "No"
    document.getElementById("otherVatScheme.accountingSchemeAnswer").text mustBe "No"
    document.getElementById("otherVatScheme.agriculturalFlatChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "applyingForAnyOf").url
    document.getElementById("otherVatScheme.accountingSchemeChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "applyingForAnyOf").url

    document.getElementById("vatExemption.vatExceptionAnswer").text mustBe "No"
    document.getElementById("vatExemption.vatExemptionAnswer").text mustBe "No"
    document.getElementById("vatExemption.vatExceptionChangeLink").attr("href") mustBe controllers.routes.EligibilityController.showExemptionCriteria().url
    document.getElementById("vatExemption.vatExemptionChangeLink").attr("href") mustBe controllers.routes.EligibilityController.showExemptionCriteria().url

    document.getElementById("resources.companyOwnAnswer").text mustBe "No"
    document.getElementById("resources.companySellAnswer").text mustBe "No"
    document.getElementById("resources.companyOwnChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "companyWillDoAnyOf").url
    document.getElementById("resources.companySellChangeLink").attr("href") mustBe controllers.routes.ServiceCriteriaQuestionsController.show(question = "companyWillDoAnyOf").url
  }

  "GET Eligibility Summary page" should {
    "return 200" when {
      "page is rendered with all data passed in if data is in s4l" in {

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibility].contains(s4lData)

        val response = buildClient("/check-confirm-eligibility").get()
        whenReady(response) { res =>
          res.status mustBe 200
          val document = Jsoup.parse(res.body)
          validateEligibilitySummaryPage(document)
        }
      }
      "page is rendered with all data passed in if data is in backend" in {

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibility].isEmpty
          .vatScheme.hasValidEligibilityData

        val response = buildClient("/check-confirm-eligibility").get()
        whenReady(response) { res =>
          res.status mustBe 200
          val document = Jsoup.parse(res.body)
          validateEligibilitySummaryPage(document)
        }
      }
    }
  }
  "Save and continue on eligibility summary" should {
    "save the data to the back end and redirect to the you can register page" in {
      given()
        .user.isAuthorised
        .currentProfile.withProfileAndIncorpDate()
        .vatScheme.isBlank
        .audit.writesAudit()
        .s4lContainer[S4LVatEligibility].contains(s4lData)
        .vatScheme.isUpdatedWith(validEligibilityData)

      val response = buildClient("/check-confirm-eligibility").post(Map("" -> Seq("")))
      whenReady(response) { res =>
        res.status mustBe 303
        res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.EligibilitySuccessController.show().url)
      }
    }
  }
}
