package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import org.jsoup.Jsoup
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class CompletionCapacityFillingInForISpec extends IntegrationSpecBase with AuthHelper with SessionStub{

  val singleOfficer = """
                        |{
                        |  "officers": [
                        |    {
                        |      "name_elements" : {
                        |        "forename" : "test1",
                        |        "other_forenames" : "test11",
                        |        "surname" : "testa",
                        |        "title" : "Mr"
                        |      },
                        |      "officer_role" : "director"
                        |    }, {
                        |      "name_elements" : {
                        |        "forename" : "test2",
                        |        "other_forenames" : "test22",
                        |        "surname" : "testb",
                        |        "title" : "Mr"
                        |      },
                        |      "officer_role" : "corporate-director"
                        |    }
                        |  ]
                        |}""".stripMargin

  val multipleOfficers = """
                           |{
                           |  "officers": [
                           |    {
                           |      "name_elements" : {
                           |        "forename" : "test1",
                           |        "other_forenames" : "test11",
                           |        "surname" : "testa",
                           |        "title" : "Mr"
                           |      },
                           |      "officer_role" : "director"
                           |    }, {
                           |      "name_elements" : {
                           |        "forename" : "test2",
                           |        "other_forenames" : "test22",
                           |        "surname" : "testb",
                           |        "title" : "Mr"
                           |      },
                           |      "officer_role" : "secretary"
                           |    }
                           |  ]
                           |}""".stripMargin

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s"GET ${controllers.routes.CompletionCapacityFillingInForController.onPageLoad().url}" should {
    "render the 'confirm on behalf of' view" when {
      "only one officer is present in II" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        val request = buildClient("/filling-in-for").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val result = await(request)
        result.status mustBe 200

        val document = Jsoup.parse(result.body)

        document.getElementById("main-heading").text() mustBe "Confirm you are doing this application for test1 testa"
      }
    }
    "render the 'which officer are you doing on behalf of' view" when {
      "multiple officers are present in II" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/filling-in-for").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val result = await(request)
        result.status mustBe 200

        val document = Jsoup.parse(result.body)

        document.getElementById("main-heading").text() mustBe "Which company officer are you doing this application for?"
        document.getElementsByAttributeValue("for", "completionCapacity-Mrtest1test11testadirector").text() mustBe "test1 testa"
        document.getElementsByAttributeValue("for", "completionCapacity-Mrtest2test22testbsecretary").text() mustBe "test2 testb"
      }
    }
  }

  s"POST ${controllers.routes.CompletionCapacityFillingInForController.onPageLoad().url}" should {
    "return a 400 with the correct error message" when {
      "nothing is selection for multiple directors" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/filling-in-for").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("")))

        val result = await(request)

        result.status mustBe 400

        val document = Jsoup.parse(result.body)

        document.getElementById("error-summary-heading").text() mustBe "This page has errors"
        document.getElementsByAttributeValue("href","#value").text() mustBe "Tell us which company officer you are doing this application for"
      }
    }
    "redirect to first eligibility page" when {
      "the user confirms they are doing on behalf of for one director" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        val request = buildClient("/filling-in-for").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("" -> Seq()))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.InvolvedInOtherBusinessController.onPageLoad().url)
      }
      "an officer is selected from multiple officers" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/filling-in-for").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("Mrtest2test22testbsecretary")))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.InvolvedInOtherBusinessController.onPageLoad().url)
      }

    }
  }

}

