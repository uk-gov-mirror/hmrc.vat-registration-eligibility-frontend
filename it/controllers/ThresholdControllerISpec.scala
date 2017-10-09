
package controllers

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import helpers.RequestsFinder
import models.api.{VatEligibilityChoice, VatServiceEligibility}
import models.api.VatEligibilityChoice.{NECESSITY_OBLIGATORY, NECESSITY_VOLUNTARY}
import models.view.VoluntaryRegistration.{REGISTER_NO, REGISTER_YES}
import models.view.TaxableTurnover.{TAXABLE_NO, TAXABLE_YES}
import models.view.VoluntaryRegistrationReason.{NEITHER, SELLS}
import models.{S4LVatEligibility, S4LVatEligibilityChoice}
import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.JsString
import support.AppAndStubs


class ThresholdControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {
  "GET Taxable Turnover page" should {
    "return 200" when {
      "user is authorised and all conditions are fulfilled" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice](S4LVatEligibilityChoice.vatChoice).isEmpty

        val response = buildClient("/vat-taxable-sales-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Taxable Turnover page" should {
    "return 303" when {
      "user choose taxable turnover YES value and save data to backend" in {
        val eligibility = VatServiceEligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false), None)
        val s4lEligibility = S4LVatEligibility(Some(eligibility))

        val startedS4LData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_NO)), None, None, None)
        val s4lData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_YES)), None, None, None)
        val updatedS4LData = s4lData.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)))

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
          .currentProfile.withProfile
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(startedS4LData, Some(STARTED))
          .s4lContainerInScenario[S4LVatEligibilityChoice].isUpdatedWith(s4lData, Some(STARTED), Some("Taxable Turnover updated"))
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(s4lData, Some("Taxable Turnover updated"))
          .s4lContainerInScenario[S4LVatEligibilityChoice].isUpdatedWith(updatedS4LData, Some("Taxable Turnover updated"), Some("Voluntary Registration updated"))
          .vatScheme.isBlank
          .s4lContainerInScenario[S4LVatEligibility].contains(s4lEligibility, Some("Voluntary Registration updated"), Some("Eligibility returned"))
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(updatedS4LData, Some("Eligibility returned"))
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
        val startedS4LData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_YES)), None, None, None)
        val s4lData = S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_NO)), None, None, None)

        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(startedS4LData, Some(STARTED))
          .s4lContainer[S4LVatEligibilityChoice].isUpdatedWith(s4lData)
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
          .currentProfile.withProfileAndIncorpDate
          .s4lContainer[S4LVatEligibilityChoice].isEmpty
          .audit.writesAudit()

        val response = buildClient("/vat-taxable-turnover-gone-over").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Over Threshold page" should{
    "return 303" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice].isEmpty
          .s4lContainer[S4LVatEligibilityChoice].isUpdatedWith(OverThresholdView(selection = false))

        val response = buildClient("/vat-taxable-turnover-gone-over").post(Map("overThresholdRadio" ->Seq("false")))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ThresholdSummaryController.show().url)
        }
      }
    }
  }

  "GET Voluntary Registration page" should {
    "return 200" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice].isEmpty

        val response = buildClient("/register-voluntarily").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Voluntary Registration page" should {
    "return 303" when {
      "the user choose value YES and save to S4L" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice].isEmpty
          .s4lContainer[S4LVatEligibilityChoice].isUpdatedWith(VoluntaryRegistration(REGISTER_YES))

        val response = buildClient("/register-voluntarily").post(Map("voluntaryRegistrationRadio" -> Seq(REGISTER_YES)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.VoluntaryRegistrationReasonController.show().url)
        }
      }

      "the user choose value NO and delete the registration in backend" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibility].cleared
          .vatScheme.deleted

        val response = buildClient("/register-voluntarily").post(Map("voluntaryRegistrationRadio" -> Seq(REGISTER_NO)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri")
        }
      }
    }
  }

  "GET Voluntary Registration Reason page" should {
    "return 200" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice](S4LVatEligibilityChoice.vatChoice).isEmpty

        val response = buildClient("/applies-company").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "POST Voluntary Registration Reason page" should {
    "return 303" when {
      "the user selects a valid reason" in {
        val optVatChoice = Some(VatEligibilityChoice(necessity = NECESSITY_VOLUNTARY))
        val eligibility = VatServiceEligibility(Some(true), Some(false), Some(false), Some(false), Some(false), Some(false), optVatChoice)

        val s4lEligibility = S4LVatEligibility(Some(eligibility))

        val s4lData = S4LVatEligibilityChoice(
          Some(TaxableTurnover(TAXABLE_NO)),
          Some(VoluntaryRegistration(REGISTER_YES)),
          None,
          None
        )

        val updatedS4LData = s4lData.copy(voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(SELLS)))

        val postEligibilityData = VatServiceEligibility(
          haveNino = Some(true),
          doingBusinessAbroad = Some(false),
          doAnyApplyToYou = Some(false),
          applyingForAnyOf = Some(false),
          companyWillDoAnyOf = Some(false),
          vatEligibilityChoice = Some(VatEligibilityChoice(
            necessity = NECESSITY_VOLUNTARY,
            reason = Some(SELLS),
            vatThresholdPostIncorp = None
          ))
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(s4lData, Some(STARTED))
          .s4lContainerInScenario[S4LVatEligibilityChoice].isUpdatedWith(updatedS4LData, Some(STARTED), Some("Eligibility Choice updated"))
          .vatScheme.isBlank
          .s4lContainerInScenario[S4LVatEligibility].contains(s4lEligibility, Some("Eligibility Choice updated"), Some("Eligibility returned"))
          .s4lContainerInScenario[S4LVatEligibilityChoice].contains(updatedS4LData, Some("Eligibility returned"))
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
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibility].cleared
          .vatScheme.deleted

        val response = buildClient("/applies-company").post(Map("voluntaryRegistrationReasonRadio" -> Seq(NEITHER)))
        whenReady(response) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION) mustBe Some("/vat-uri")
        }
      }
    }
  }
}
