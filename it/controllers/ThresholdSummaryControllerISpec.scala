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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import helpers.RequestsFinder
import models.api.{VatEligibilityChoice, VatExpectedThresholdPostIncorp, VatServiceEligibility, VatThresholdPostIncorp}
import models.api.VatEligibilityChoice.NECESSITY_OBLIGATORY
import models.view.VoluntaryRegistration.REGISTER_NO
import models.{S4LVatEligibility, S4LVatEligibilityChoice}
import models.view.{ExpectationOverThresholdView, OverThresholdView, VoluntaryRegistration}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.{JsBoolean, JsString, Json}
import support.AppAndStubs

class ThresholdSummaryControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {
  "GET Threshold Summary page" should {
    "return 200" when {
      "over threshold is false and Expected over threshold is false" in {
        val s4lData = Json.toJson(S4LVatEligibilityChoice(
          None,
          None,
          None,
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice].contains(s4lData)
        val response = buildClient("/check-confirm-answers").get()
        whenReady(response) { res =>
          res.status mustBe 200
          val document = Jsoup.parse(res.body)
          document.title() mustBe "Check and confirm your answers"
          document.getElementById("threshold.overThresholdSelectionQuestion").text must include ("05 August 2016")
          document.getElementById("threshold.overThresholdSelectionAnswer").text mustBe "No"
          document.getElementById("threshold.expectationOverThresholdSelectionAnswer").text mustBe "No"
        }
      }

      "over threshold is true with a date and Expected over threshold is true with a date" in {
        val s4lData = Json.toJson(S4LVatEligibilityChoice(
          None,
          None,
          None,
          Some(OverThresholdView(selection = true, Some(LocalDate.of(2016, 10, 25)))),
          Some(ExpectationOverThresholdView(selection = true,Some(LocalDate.of(2016,11,25))))))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice].contains(s4lData)

        val response = buildClient("/check-confirm-answers").get()
        whenReady(response) { res =>
          res.status mustBe 200
          val document = Jsoup.parse(res.body)
          document.title() mustBe "Check and confirm your answers"
          document.getElementById("threshold.overThresholdSelectionQuestion").text must include ("05 August 2016")
          document.getElementById("threshold.overThresholdSelectionAnswer").text mustBe "Yes"
          document.getElementById("threshold.overThresholdDateAnswer").text mustBe "October 2016"
          document.getElementById("threshold.expectationOverThresholdSelectionAnswer").text mustBe "Yes"
          document.getElementById("threshold.expectationOverThresholdDateAnswer").text mustBe "25 November 2016"
        }
      }
    }

    "return an error page if the company is not incorporated" in {
      given()
        .user.isAuthorised
        .currentProfile.withProfile
        .audit.writesAudit()

      val response = buildClient("/check-confirm-answers").get()
      whenReady(response)(_.status) mustBe 500
    }
  }

  "POST Threshold Summary page" should {
    "return 303" when {
      val s4lEligibilityData = S4LVatEligibility(
        vatEligibility = Some(VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = None
        ))
      )

      "over threshold is true with a date, and expected threshold is populated, save data to backend" in {
        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(VatEligibilityChoice(
            necessity = NECESSITY_OBLIGATORY,
            vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(true,Some(LocalDate.of(2016,11,24)))),
            vatThresholdPostIncorp = Some(VatThresholdPostIncorp(overThresholdSelection = true, Some(LocalDate.of(2016, 10, 25))))
          ))
        )

        val s4lData = S4LVatEligibilityChoice(
          expectationOverThreshold = Some(ExpectationOverThresholdView(true,Some(LocalDate.of(2016,11,24)))),
          overThreshold = Some(OverThresholdView(selection = true, Some(LocalDate.of(2016, 10, 25)))))
        val updatedS4LData = s4lData.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(s4lData, Some(STARTED))
          .s4lContainerInScenario[S4LVatEligibilityChoice].isUpdatedWith(updatedS4LData, Some(STARTED), Some("Eligibility Choice updated"))
          .vatScheme.isBlank
          .s4lContainerInScenario[S4LVatEligibility].contains(s4lEligibilityData, Some("Eligibility Choice updated"), Some("Eligibility returned"))
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(updatedS4LData, Some("Eligibility returned"))
          .vatScheme.isUpdatedWith(postEligibilityData)
          .audit.writesAudit()

        val response = buildClient("/check-confirm-answers").post(Map("" -> Seq("")))
        whenReady(response){ res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri/who-is-registering-the-company-for-vat")

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "vatThresholdPostIncorp" \ "overThresholdSelection").as[JsBoolean].value mustBe true
          (json \ "vatEligibilityChoice" \ "vatThresholdPostIncorp" \ "overThresholdDate").as[JsString].value mustBe "2016-10-25"
          (json \ "vatEligibilityChoice" \ "vatExpectedThresholdPostIncorp" \ "expectedOverThresholdSelection").as[JsBoolean].value mustBe true
          (json \ "vatEligibilityChoice" \ "vatExpectedThresholdPostIncorp" \ "expectedOverThresholdDate").as[JsString].value mustBe "2016-11-24"
        }
      }

      "over threshold is false and expectation is populated and redirect to Voluntary Registration page" in {
        val s4lData = S4LVatEligibilityChoice(
          expectationOverThreshold = Some(ExpectationOverThresholdView(false,None)),
          overThreshold = Some(OverThresholdView(selection = false)))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .s4lContainer[S4LVatEligibilityChoice].contains(s4lData)
          .audit.writesAudit()

        val response = buildClient("/check-confirm-answers").post(Map("" -> Seq("")))
        whenReady(response){ res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationController.show().url)
        }
      }
    }
  }

}
