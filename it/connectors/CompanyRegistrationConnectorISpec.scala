
package connectors

import models.external.CompanyRegistrationProfile
import support.AppAndStubs
import uk.gov.hmrc.play.test.UnitSpec
import utils.VREFEFeatureSwitches

class CompanyRegistrationConnectorISpec extends UnitSpec with AppAndStubs {

  val companyRegConnector                 = app.injector.instanceOf[CompanyRegistrationConnector]
  val featureSwitch: VREFEFeatureSwitches = app.injector.instanceOf[VREFEFeatureSwitches]

  val ctStatusRaw = "draft"

  override lazy val additionalConfig: Map[String, String] =
    Map(
      "default-ct-status" -> "ZHJhZnQ=",
      "regIdPostIncorpWhitelist" -> "OTgsOTk=",
      "regIdPreIncorpWhitelist" -> "MTAyLDEwMw=="
    )

  "getCompanyProfile" should {
    featureSwitch.manager.disable(featureSwitch.companyReg)

    "return the default CompanyRegProfile for a whitelisted regId" in {
      given()
        .audit.writesAudit()

      val res = companyRegConnector.getCompanyRegistrationDetails("99")(hc)
      await(res) shouldBe CompanyRegistrationProfile("accepted", "fakeTxId-99")
    }
    "return a CompanyRegProfile for a non-whitelisted regId" in {
      given()
        .corporationTaxRegistration.existsWithStatus("held")
        .audit.writesAudit()

      val res = companyRegConnector.getCompanyRegistrationDetails("1")(hc)
      await(res) shouldBe CompanyRegistrationProfile("held", "000-434-1")
    }
  }
}
