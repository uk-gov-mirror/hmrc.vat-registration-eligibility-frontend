
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import common.enums.CacheKeys
import helpers.RequestsFinder
import models.api.{VatEligibilityChoice, VatExpectedThresholdPostIncorp, VatServiceEligibility, VatThresholdPostIncorp}
import models.api.VatEligibilityChoice.{NECESSITY_OBLIGATORY, NECESSITY_VOLUNTARY}
import models.view.VoluntaryRegistration.{REGISTER_NO, REGISTER_YES}
import models.view.TaxableTurnover.{TAXABLE_NO, TAXABLE_YES}
import models.view.VoluntaryRegistrationReason.{NEITHER, SELLS}
import models.{S4LVatEligibility, S4LVatEligibilityChoice}
import models.view._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.{JsBoolean, JsString, Json}
import support.AppAndStubs


class ThresholdControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {
  val s4lEligibility = S4LVatEligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false))

  "GET Taxable Turnover page" should {
    "return 200" when {
      "user is authorised and all conditions are fulfilled" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())

        val response = buildClient("/vat-taxable-sales-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Taxable Turnover page" should {
    "return 303" when {
      "user choose taxable turnover YES value and save data to backend" in {
        val startedS4LData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_NO)), None, None, None, None)

        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(VatEligibilityChoice(necessity = NECESSITY_OBLIGATORY))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, startedS4LData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared(Some("Eligibility returned"), Some("S4L cleared"))
          .vatScheme.isUpdatedWith(postEligibilityData)
          .audit.writesAudit()

        val response = buildClient("/vat-taxable-sales-over-threshold").post(Map("taxableTurnoverRadio" -> Seq(TAXABLE_YES)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri/who-is-registering-the-company-for-vat")

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_OBLIGATORY
        }
      }

      "user choose taxable turnover NO value and save to S4L" in {
        val startedS4LData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_YES)), None, None, None, None)
        val s4lData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_NO)), None, None, None, None)

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, startedS4LData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, s4lData, Some("Eligibility Choice returned"), Some("S4L Eligibility Choice updated"))
          .audit.writesAudit()

        val response = buildClient("/vat-taxable-sales-over-threshold").post(Map("taxableTurnoverRadio" -> Seq(TAXABLE_NO)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationController.show().url)
        }
      }
    }
  }

  "GET Over Threshold page" should {
    "return 200" when {
      "when user is authorised and has a date of incorporation" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfileAndIncorpDate()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())
          .audit.writesAudit()

        val response = buildClient("/vat-taxable-turnover-gone-over").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Over Threshold page" should{
    "return 303" when {
      "when the request is valid" in {
        val s4lData = S4LVatEligibilityChoice(None, None, None, Some(OverThresholdView(selection = false)), None)

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.isEmpty(Some(STARTED), Some("S4L Eligibility Choice returned"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice(), Some("S4L Eligibility Choice returned"), Some("S4L Default Eligibility Choice saved"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, s4lData, Some("S4L Default Eligibility Choice saved"), Some("S4L Eligibility Choice updated"))

        val response = buildClient("/vat-taxable-turnover-gone-over").post(Map("overThresholdRadio" ->Seq("false")))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdController.expectationOverShow().url)
        }
      }
    }
  }

  "GET Expected Over Threshold page" should {
    "return 200" when {
      "user is authorised and has a date of incorporation" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfileAndIncorpDate()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())
          .audit.writesAudit()

        val response = buildClient("/go-over-vat-threshold-period").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Expectation Over Threshold page" should{
    "return 303" when {
      "the request is valid" in {
        val s4lData = S4LVatEligibilityChoice(
          None,
          None,
          None,
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.isEmpty(Some(STARTED), Some("S4L Eligibility Choice returned"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice(), Some("S4L Eligibility Choice returned"), Some("S4L Default Eligibility Choice saved"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, s4lData, Some("S4L Default Eligibility Choice saved"), Some("S4L Eligibility Choice updated"))

        val response = buildClient("/go-over-vat-threshold-period").post(Map("expectationOverThresholdRadio" -> Seq("false")))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdSummaryController.show().url)
        }
      }

      "the request is valid with both over threshold values set to true" in {
        val s4lData = S4LVatEligibilityChoice(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(selection = true, Some(LocalDate.of(2016, 8, 6)))),
          Some(ExpectationOverThresholdView(selection = false, None))
        )

        val postEligibilityChoiceData = VatEligibilityChoice(
          necessity = NECESSITY_OBLIGATORY,
          reason = None,
          vatThresholdPostIncorp = Some(VatThresholdPostIncorp(true, Some(LocalDate.of(2016, 8, 6)))),
          vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(true, Some(LocalDate.of(2016, 8, 6))))
        )

        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(postEligibilityChoiceData)
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, s4lData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared()
          .vatScheme.isUpdatedWith(postEligibilityData)

        val response = buildClient("/go-over-vat-threshold-period").post(Map("expectationOverThresholdRadio" -> Seq("true"),
                                                                             "expectationOverThreshold.day" -> Seq("06"),
                                                                             "expectationOverThreshold.month" -> Seq("08"),
                                                                             "expectationOverThreshold.year" -> Seq("2016")))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdSummaryController.show().url)


          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_OBLIGATORY
          (json \ "vatEligibilityChoice" \ "vatThresholdPostIncorp" \ "overThresholdSelection").as[JsBoolean].value mustBe true
          (json \ "vatEligibilityChoice" \ "vatExpectedThresholdPostIncorp" \ "expectedOverThresholdSelection").as[JsBoolean].value mustBe true
        }
      }

      "the data is complete and saved into backend" in {
        val s4lData = S4LVatEligibilityChoice(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))
        )

        val postEligibilityChoiceData = VatEligibilityChoice(
          necessity = NECESSITY_VOLUNTARY,
          reason = Some(SELLS),
          vatThresholdPostIncorp = Some(VatThresholdPostIncorp(false, None)),
          vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(false, None))
        )

        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(postEligibilityChoiceData)
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, s4lData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared()
          .vatScheme.isUpdatedWith(postEligibilityData)

        val response = buildClient("/go-over-vat-threshold-period").post(Map("expectationOverThresholdRadio" -> Seq("false")))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdSummaryController.show().url)

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_VOLUNTARY
          (json \ "vatEligibilityChoice" \ "reason").as[JsString].value mustBe SELLS
          (json \ "vatEligibilityChoice" \ "vatThresholdPostIncorp" \ "overThresholdSelection").as[JsBoolean].value mustBe false
          (json \ "vatEligibilityChoice" \ "vatExpectedThresholdPostIncorp" \ "expectedOverThresholdSelection").as[JsBoolean].value mustBe false
        }
      }
    }
  }

  "GET Voluntary Registration page" should {
    "return 200" when {
      "the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())

        val response = buildClient("/register-voluntarily").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Voluntary Registration page" should {
    "return 303" when {
      "the user choose value YES and save to S4L" in {
        val startedS4LData = S4LVatEligibilityChoice(
          None,
          None,
          None,
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))
        )

        val s4lData = S4LVatEligibilityChoice(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          None,
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, startedS4LData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.isUpdatedWith(CacheKeys.EligibilityChoice, s4lData, Some("Eligibility Choice returned"), Some("S4L Eligibility Choice updated"))

        val response = buildClient("/register-voluntarily").post(Map("voluntaryRegistrationRadio" -> Seq(REGISTER_YES)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationReasonController.show().url)
        }
      }

      "the user choose value NO and save in backend" in {
        val startedS4LData = S4LVatEligibilityChoice(
          None,
          Some(VoluntaryRegistration(REGISTER_YES)),
          Some(VoluntaryRegistrationReason(SELLS)),
          Some(OverThresholdView(selection = false)),
          Some(ExpectationOverThresholdView(selection = false))
        )

        val postEligibilityChoiceData = VatEligibilityChoice(
          necessity = NECESSITY_OBLIGATORY,
          reason = None,
          vatThresholdPostIncorp = Some(VatThresholdPostIncorp(false, None)),
          vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(false, None))
        )

        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(postEligibilityChoiceData)
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, startedS4LData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared(Some("Eligibility returned"), Some("S4L cleared"))
          .vatScheme.isUpdatedWith(postEligibilityData)

        val response = buildClient("/register-voluntarily").post(Map("voluntaryRegistrationRadio" -> Seq(REGISTER_NO)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri")

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_OBLIGATORY
          (json \ "vatEligibilityChoice" \ "vatThresholdPostIncorp" \ "overThresholdSelection").as[JsBoolean].value mustBe false
          (json \ "vatEligibilityChoice" \ "vatExpectedThresholdPostIncorp" \ "expectedOverThresholdSelection").as[JsBoolean].value mustBe false
        }
      }
    }
  }

  "GET Voluntary Registration Reason page" should {
    "return 200" when {
      "the request is valid" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfile()
          .audit.writesAudit()
          .s4lContainer.isEmpty
          .s4lContainer.isUpdatedWith(CacheKeys.EligibilityChoice, S4LVatEligibilityChoice())

        val response = buildClient("/applies-company").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Voluntary Registration Reason page" should {
    "return 303" when {
      val eligibilityData = VatServiceEligibility(
        haveNino = Some(true),
        doingBusinessAbroad = Some(false),
        doAnyApplyToYou = Some(false),
        applyingForAnyOf = Some(false),
        companyWillDoAnyOf = Some(false),
        vatEligibilityChoice = None
      )

      val s4lData = S4LVatEligibilityChoice(
        Some(TaxableTurnover(TAXABLE_NO)),
        Some(VoluntaryRegistration(REGISTER_YES)),
        None,
        None
      )

      "the user selects a valid reason" in {
        val postEligibilityData = eligibilityData.copy(vatEligibilityChoice = Some(VatEligibilityChoice(
            necessity = NECESSITY_VOLUNTARY,
            reason = Some(SELLS),
            vatThresholdPostIncorp = None
          ))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, s4lData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared(Some("Eligibility returned"), Some("S4L cleared"))
          .vatScheme.isUpdatedWith(postEligibilityData)

        val response = buildClient("/applies-company").post(Map("voluntaryRegistrationReasonRadio" -> Seq(SELLS)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri/who-is-registering-the-company-for-vat")

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_VOLUNTARY
          (json \ "vatEligibilityChoice" \ "reason").as[JsString].value mustBe SELLS
        }
      }

      "the user selects an invalid reason" in {
        val postEligibilityData = eligibilityData.copy(vatEligibilityChoice = Some(VatEligibilityChoice(
            necessity = NECESSITY_VOLUNTARY,
            reason = Some(NEITHER),
            vatThresholdPostIncorp = None
          ))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile()
          .audit.writesAudit()
          .s4lContainerInScenario.contains(CacheKeys.EligibilityChoice, s4lData, Some(STARTED), Some("Eligibility Choice returned"))
          .s4lContainerInScenario.contains(CacheKeys.Eligibility, s4lEligibility, Some("Eligibility Choice returned"), Some("Eligibility returned"))
          .s4lContainerInScenario.cleared(Some("Eligibility returned"), Some("S4L cleared"))
          .vatScheme.isUpdatedWith(postEligibilityData)

        val response = buildClient("/applies-company").post(Map("voluntaryRegistrationReasonRadio" -> Seq(NEITHER)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri")

          val json = getPATCHRequestJsonBody(s"/vatreg/1/service-eligibility")
          (json \ "vatEligibilityChoice" \ "necessity").as[JsString].value mustBe NECESSITY_VOLUNTARY
          (json \ "vatEligibilityChoice" \ "reason").as[JsString].value mustBe NEITHER
        }
      }
    }
  }
}
