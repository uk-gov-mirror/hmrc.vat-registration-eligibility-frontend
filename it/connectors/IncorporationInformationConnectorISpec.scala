
package connectors

import common.enums.VatRegStatus
import play.api.libs.json.Json
import support.AppAndStubs
import uk.gov.hmrc.play.test.UnitSpec

class IncorporationInformationConnectorISpec extends UnitSpec with AppAndStubs {

  val iiConnector               = app.injector.instanceOf[IncorporationInformationConnector]

  val companyNameRaw            =
    """
      |{"company_name": "Normal User LTD"}
    """.stripMargin
  val nonWhitelistedRegId         = "normalUser"
  val transactionID               = "000-434-1"


  override lazy val additionalConfig: Map[String, String] =
    Map(
      "default-company-name" -> "eyJjb21wYW55X25hbWUiOiAiTm9ybWFsIFVzZXIgTFREIn0=",
      "regIdPostIncorpWhitelist" -> "OTgsOTk=",
      "regIdPreIncorpWhitelist" -> "MTAyLDEwMw=="
    )

  "getCompanyName" should {
    "return default data from config for a whitelisted regId" in {
      val res = await(iiConnector.getCompanyName("99",transactionID)(hc))
      res shouldBe Json.parse(companyNameRaw)

    }

    "return data from II when the regId is not whitelisted" in {
      given()
        .company.nameIs("Foo Bar Wizz Bang")

      val res = await(iiConnector.getCompanyName(nonWhitelistedRegId, transactionID)(hc))
      res shouldBe Json.parse("""{"company_name": "Foo Bar Wizz Bang"}""")
    }
  }
}
