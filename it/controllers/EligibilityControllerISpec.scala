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

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import common.enums.CacheKeys
import common.enums.CacheKeys.IneligibilityReason
import helpers.RequestsFinder
import models.api.VatEligibilityChoice.NECESSITY_OBLIGATORY
import models.api.{VatServiceEligibility, VatEligibilityChoice}
import models.view.TaxableTurnover.TAXABLE_YES
import models.view.VoluntaryRegistration.REGISTER_NO
import models.view.{TaxableTurnover, VoluntaryRegistration}
import models.{S4LVatEligibility, S4LVatEligibilityChoice}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http._
import play.api.libs.json.{JsBoolean, JsString}
import support.AppAndStubs


class EligibilityControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {

  "Eligibility questions on GET" should {
    "return 200" when {
      "[Q1] /national-insurance-number The user is authorised, current prof is setup, vatscheme is blank,audit is successful, s4l 404's" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.Eligibility, S4LVatEligibility())
          .audit.writesAudit()
        val response = buildClient("/national-insurance-number").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
    "return 200" when {
      "[Q2] /international-business the user is authorised, current prof is setup, vatscheme is blank, audit fails, s4l returns valid data" in {

        val s4lData = S4LVatEligibility(haveNino = Some(true), doingBusinessAbroad = Some(true))

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .s4lContainer.contains(CacheKeys.Eligibility, s4lData)
          .audit.writesAudit()

        val response = buildClient("/international-business").get()
        val res = whenReady(response)(a => a)
        res.status mustBe 200
        val document = Jsoup.parse(res.body)
        document.title() mustBe "Will the company do international business once it's registered for VAT?"
        document.getElementById("doingBusinessAbroadRadio-true").attr("checked") mustBe "checked"
      }
    }
    "return 404" when {
      "[Q3] /involved-more-business-changing-status the user is not authorised to view the page but session is valid" in {
        given()
          .user.isNotAuthorised
          .audit.writesAudit()

        val response = buildClient("""/involved-more-business-changing-status"""").get()
        whenReady(response)(_.status) mustBe 404
      }
    }
    "return 303" when {
      "[Q4] /agricultural-flat-rate the user is authorised but the request is invalid" in {
        given()
          .user.isNotAuthorised
          .audit.writesAudit()

        val response = buildClient("/agricultural-flat-rate")(HeaderNames.COOKIE -> "foo").get()
        whenReady(response)(_.status) mustBe 303
      }
    }
    "return 200" when {
      "[Q5] /apply-exception-exemption  the user is authorised, current prof is setup, vatscheme is blank, s4l returns valid data, prepop field" in {

        val s4lData = S4LVatEligibility(haveNino = Some(true),
                                        doingBusinessAbroad = Some(true),
                                        doAnyApplyToYou = Some(false),
                                        applyingForAnyOf = Some(false),
                                        applyingForVatExemption = Some(true))

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.failsToWriteAudit()
          .s4lContainer.contains(CacheKeys.Eligibility, s4lData)

        val response = buildClient("/apply-exception-exemption").get()
        val res = whenReady(response)(a => a)
        res.status mustBe 200
        val document = Jsoup.parse(res.body)
        document.title() mustBe "Will the company apply for a VAT registration exception or exemption?"
        document.getElementById("applyingForVatExemptionRadio-true").attr("checked") mustBe "checked"
      }
    }
    "return 200" when {
      "[Q6] /apply-for-any the user is authorised and data is in vat reg backend. data is pulled from vat because S4l returns 404" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.hasValidEligibilityData
          .audit.writesAudit()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.Eligibility, S4LVatEligibility())

        val response = buildClient("/apply-for-any").get()
        whenReady(response) { res =>
          res.status mustBe 200
          Jsoup.parse(res.body).getElementById("companyWillDoAnyOfRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }

  "Eligibility questions on POST" should {
    "return 303 and redirects to next page of questions" when {
      "[Q1] /national-insurance-number the user submits an answer" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.contains(CacheKeys.Eligibility, VatServiceEligibility(haveNino = Some(false)))
          .s4lContainer.isUpdatedWith(CacheKeys.Eligibility, VatServiceEligibility(haveNino = Some(false)))

        val response = buildClient("/national-insurance-number").post(Map("haveNinoRadio" -> Seq("true")))
        val res = whenReady(response)(a => a)
        res.status mustBe 303
        res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/international-business")
      }
    }
    "return 303 and redirects to next page of you are not eligible" when {
      "[Q1] /national-insurance-number the user submits an answer which is false" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.contains(CacheKeys.Eligibility, VatServiceEligibility(haveNino = Some(false)))
          .s4lContainer.isUpdatedWith(CacheKeys.Eligibility, VatServiceEligibility(haveNino = Some(false)))
          .keystore.putKeyStoreValue("IneligibilityReason", """"haveNino"""")

        val response = buildClient("/national-insurance-number").post(Map("haveNinoRadio" -> Seq("false")))
        val res = whenReady(response)(a => a)
        res.status mustBe 303
        res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/cant-register")
      }
    }
    "return 400 when invalid form is posted" when {
      "[Q6] /apply-for-any the user submits an incorrect value in the form" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.isEmpty

        val response = buildClient("/apply-for-any").post(Map("haveNinoRadio" -> Seq("fooBars")))
        val res = whenReady(response)(a => a)
        res.status mustBe 400
      }
    }

    "return 303 when final question posted" when {
      "[Q6] /apply-for-any final question is submitted successfully" in {
        val s4lData = S4LVatEligibility(haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          applyingForVatExemption = Some(false))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.contains(CacheKeys.Eligibility, s4lData)
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())
          .vatScheme.isUpdatedWith(s4lData.copy(companyWillDoAnyOf = Some(false)))
          .s4lContainer.cleared

        val response = buildClient("/apply-for-any").post(Map("companyWillDoAnyOfRadio" -> Seq("false")))
        whenReady(response){
          res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/check-confirm-eligibility")

            val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
            (json \ "haveNino").as[JsBoolean].value mustBe true
            (json \ "doingBusinessAbroad").as[JsBoolean].value mustBe false
            (json \ "doAnyApplyToYou").as[JsBoolean].value mustBe false
            (json \ "applyingForAnyOf").as[JsBoolean].value mustBe false
            (json \ "applyingForVatExemption").as[JsBoolean].value mustBe false
            (json \ "companyWillDoAnyOf").as[JsBoolean].value mustBe false
        }
      }

      "[Q6] /apply-for-any final question is submitted successfully with some existing Eligibility Choice data" in {
        val s4lData = S4LVatEligibility(haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          applyingForVatExemption = Some(false))

        val s4lChoiceData = S4LVatEligibilityChoice(
          Some(TaxableTurnover(TAXABLE_YES)),
          Some(VoluntaryRegistration(REGISTER_NO))
        )

        val postEligibilityData = VatServiceEligibility(
          Some(true),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(false),
          Some(VatEligibilityChoice(
            NECESSITY_OBLIGATORY,
            None,
            None,
            None
          ))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lData, Some(STARTED), Some("S4L Eligibility returned"))
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, s4lChoiceData, Some("S4L Eligibility returned"), Some("S4L Eligibility Choice returned"))
          .vatScheme.isUpdatedWith(postEligibilityData)
          .s4lContainer.cleared

        val response = buildClient("/apply-for-any").post(Map("companyWillDoAnyOfRadio" -> Seq("false")))
        whenReady(response){
          res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/check-confirm-eligibility")

            val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
            (json \ "haveNino").as[JsBoolean].value mustBe true
            (json \ "doingBusinessAbroad").as[JsBoolean].value mustBe false
            (json \ "doAnyApplyToYou").as[JsBoolean].value mustBe false
            (json \ "applyingForAnyOf").as[JsBoolean].value mustBe false
            (json \ "applyingForVatExemption").as[JsBoolean].value mustBe false
            (json \ "companyWillDoAnyOf").as[JsBoolean].value mustBe false
            (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_OBLIGATORY
        }
      }
    }

  }
  "Eligibility /cant-register on GET" should {
    "return 200" when {
      "user hits it directly where a question has a ineligibility reason" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile(Some(STARTED), Some("Current Profile returned"))
          .audit.writesAudit()
          .keystoreInScenario.hasKeyStoreValue(IneligibilityReason.toString, s""""haveNino"""", Some("Current Profile returned"))

        val response = buildClient("/cant-register").get()
        whenReady(response)(_.status) mustBe 200
      }
    }

    "return 500" when {
      "user hits it directly but there's no ineligibility reason in keystore" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .audit.writesAudit()
          .keystore.hasKeyStoreValue(IneligibilityReason.toString, "")

        val response = buildClient("/cant-register").get()
        whenReady(response)(_.status) mustBe 500
      }
    }
  }
  "Eligibility /can-register on GET" should {
    "return 200" when {
      "user hits it directly with a current profile" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .audit.writesAudit()

        val response = buildClient("/can-register").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
    "return 200" when {
      "user hits it but does not have a current profile so one is built up" in {
        given()
          .user.isAuthorised
          .keystore.hasKeyStoreValue("foo","true")
          .currentProfile.setup()
          .audit.writesAudit()

        val response = buildClient("/can-register").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "Eligility /can-register on POST" should {
    "return 303 and redirect to gone over threshold" when {
      "user is authorised and has a current profile" in {
        given()
          .user.isAuthorised
          .currentProfile.setup()
          .currentProfile.withProfile()
          .audit.writesAudit()

        val response = buildClient("/can-register").post(Map("haveNinoRadio" -> Seq("fooBars")))
        whenReady(response){
          res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/vat-taxable-turnover-gone-over")
        }
      }

    }
  }
}
