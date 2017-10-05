
package controllers

import models.api.{VatEligibilityChoice, VatServiceEligibility}
import models.{S4LKey, S4LVatEligibilityChoice, S4LVatEligibility}
import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import support.AppAndStubs


class ThresholdControllerISpec extends PlaySpec with AppAndStubs with ScalaFutures {

  "/vat-taxable-sales-over-threshold GET" should {
    "return 200" when {
      "user is authorised and all conditions are fulfilled" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[TaxableTurnover](S4LKey("VatChoice")).isEmpty

        val response = buildClient("/vat-taxable-sales-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/vat-taxable-sales-over-threshold POST" should {
    "return 303" when {
      "user successfully submits a valid form" in {
        val optVatChoice = Some(VatEligibilityChoice(necessity = "obligatory"))

        val eligibility = VatServiceEligibility(Some(true),Some(false),Some(false),Some(false),Some(false),optVatChoice)

        val s4lEligibility = Json.toJson(S4LVatEligibility(Some(eligibility)))


        val s4lData = Json.toJson(S4LVatEligibilityChoice(Some(
          TaxableTurnover("TAXABLE_YES"))
          ,Some(VoluntaryRegistration("REGISTER_NO"))
          ,None
          ,Some(OverThresholdView(false))))

        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .s4lContainer[TaxableTurnover](S4LKey("VatChoice")).contains(s4lData)
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).contains(s4lEligibility)
          .s4lContainer[TaxableTurnover](S4LKey("VatChoice")).isUpdatedWith(TaxableTurnover("TAXABLE_YES"))(S4LKey("VatChoice"),TaxableTurnover.format)
          .s4lContainer[TaxableTurnover](S4LKey("VatChoice")).isUpdatedWith(VoluntaryRegistration("REGISTER_NO"))(S4LKey("VatChoice"),VoluntaryRegistration.format)
          .vatScheme.isUpdatedWith(eligibility)
          .audit.writesAudit()

         val response = buildClient("/vat-taxable-sales-over-threshold").post(Map("taxableTurnoverRadio" -> Seq("TAXABLE_YES")))
        whenReady(response)(_.status) mustBe 303
      }
    }
  }
  "/vat-taxable-turnover-gone-over  GET" should {
    "return 200" when {
      "when user is authorised and has a date of incorporation" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfileAndIncorpDate
          .audit.writesAudit()

        val response = buildClient("/vat-taxable-turnover-gone-over").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "/vat-taxable-turnover-gone-over POST" should{
    "return 303" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[OverThresholdView](S4LKey("VatChoice")).isEmpty
          .s4lContainer[OverThresholdView](S4LKey("VatChoice")).isUpdatedWith(OverThresholdView(false))(S4LKey("VatChoice"),OverThresholdView.format)

        val response = buildClient("/vat-taxable-turnover-gone-over").post(Map("overThresholdRadio" ->Seq("false")))
        whenReady(response)(_.status) mustBe 303
      }
    }
  }
  "/check-confirm-answers GET" should {
    "return 200" when {
      "when the request is valid" in {
        val s4lData = Json.toJson(S4LVatEligibilityChoice(Some(
          TaxableTurnover("TAXABLE_YES"))
          ,Some(VoluntaryRegistration("REGISTER_NO"))
          ,None
          ,Some(OverThresholdView(false))))

          given()
            .user.isAuthorised
            .currentProfile.withProfileAndIncorpDate
            .vatScheme.isBlank
            .audit.writesAudit()
            .s4lContainer[S4LVatEligibilityChoice](S4LKey("VatChoice")).contains(s4lData)

        val response = buildClient("/check-confirm-answers").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/check-confirm-answers POST" should {
    "return 303" when {
      "when the request is valid" in {
        val s4lData = Json.toJson(S4LVatEligibilityChoice(Some(
          TaxableTurnover("TAXABLE_YES"))
          ,Some(VoluntaryRegistration("REGISTER_NO"))
          ,None
          ,Some(OverThresholdView(false))))

        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[S4LVatEligibilityChoice](S4LKey("VatChoice")).contains(s4lData)

        val response = buildClient("/check-confirm-answers").post(Map("" -> Seq("")))
        whenReady(response){ res =>
         res.status mustBe 303
         res.header(HeaderNames.LOCATION) mustBe
           Some("/check-if-you-can-register-for-vat/register-voluntarily")
        }
      }
    }
  }
  "/register-voluntarily GET" should {
    "return 200" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[VoluntaryRegistration](S4LKey("VatChoice")).isEmpty

        val response = buildClient("/register-voluntarily").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/register-voluntarily POST" should {
    "return 303" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[VoluntaryRegistrationReason](S4LKey("VatChoice")).isEmpty
          .s4lContainer[VoluntaryRegistration](S4LKey("VatChoice")).isUpdatedWith(VoluntaryRegistration("REGISTER_YES"))(S4LKey("VatChoice"),VoluntaryRegistration.format)

        val response = buildClient("/register-voluntarily").post(Map("voluntaryRegistrationRadio" -> Seq("REGISTER_YES")))
        whenReady(response)(_.status) mustBe 303

      }
    }
  }
  "/applies-company GET" should {
    "return 200" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[VoluntaryRegistrationReason](S4LKey("VatChoice")).isEmpty

        val response = buildClient("/applies-company").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/applies-company POST" should {
    "return 303" when {
      "when the request is valid" in {
        val optVatChoice = Some(VatEligibilityChoice(necessity = "obligatory"))

        val eligibility = VatServiceEligibility(Some(true),Some(false),Some(false),Some(false),Some(false),optVatChoice)

        val s4lEligibility = Json.toJson(S4LVatEligibility(Some(eligibility)))


        val s4lData = Json.toJson(S4LVatEligibilityChoice(Some(
          TaxableTurnover("TAXABLE_YES"))
          ,Some(VoluntaryRegistration("REGISTER_NO"))
          ,None
          ,Some(OverThresholdView(false))))

        given()
          .user.isAuthorised
          .vatScheme.isBlank
          .currentProfile.withProfile
          .audit.writesAudit()
          .s4lContainer[VoluntaryRegistrationReason](S4LKey("VatChoice")).contains(s4lData)
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).contains(s4lEligibility)
          .s4lContainer[VoluntaryRegistrationReason](S4LKey("VatChoice")).isUpdatedWith(VoluntaryRegistrationReason("COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES"))(S4LKey("VatChoice"),VoluntaryRegistrationReason.format)
          .vatScheme.isUpdatedWith(eligibility)

        val response = buildClient("/applies-company").post(Map("voluntaryRegistrationReasonRadio" -> Seq("COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES")))
        whenReady(response)(_.status) mustBe 303
      }
    }
  }

}
