package www

import java.time.LocalDate

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers._
import models._
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class ApplicantUKNinoISpec extends IntegrationSpecBase with AuthHelper with SessionStub {
  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())
  val internalId = "testInternalId"
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

  s"POST ${controllers.routes.ApplicantUKNinoController.onSubmit().url}" should {
    "submit eligibility answers" when {
      "if the user has reached the nino page on a voluntary flow" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        stubPatch("/vatreg/testRegId/eligibility-data", 200, """{}""", """
         |{"sections":[
         | {"title":"VAT-taxable sales","data":[
         |   {"questionId":"thresholdNextThirtyDays","question":"In the next 30 days alone, does the business expect to make more than £85,000 in VAT-taxable sales?","answer":"No","answerValue":false},
         |   {"questionId":"thresholdPreviousThirtyDays-value","question":"Has the business ever expected to go over the VAT-registration threshold in a single 30-day period?","answer":"No","answerValue":false},
         |   {"questionId":"thresholdInTwelveMonths-value","question":"In any 12-month period has the business gone over the VAT-registration threshold?","answer":"No","answerValue":false},
         |   {"questionId":"voluntaryRegistration","question":"Do you want to register the business for VAT voluntarily?","answer":"Yes","answerValue":true},
         |      {"questionId":"turnoverEstimate-value","question":"What do you think the business's VAT-taxable turnover will be for the next 12 months?","answer":"More than £10,000","answerValue":"tenthousand"},
         |   {"questionId":"turnoverEstimate-optionalData","question":"Give an estimate rounded to the nearest £10,000","answer":"£11,000","answerValue":11000}
         | ]},
         | {"title":"Who is doing the application?","data":[
         |  {"questionId":"completionCapacity","question":"Are you test1 testa?","answer":"Yes","answerValue": {
         |    "name":{"forename":"test1","otherForenames":"test11","surname":"testa","title":"Mr"},"role":"director"}
         |  }
         | ]},
         | {"title":"Special situations","data":[
         |   {"questionId":"internationalActivities","question":"Will the business be doing any of the following international activities over the next 12 months?","answer":"No","answerValue":false},
         |   {"questionId":"involvedInOtherBusiness","question":"Have you been involved with another business or taken over a VAT-registered business?","answer":"No","answerValue":false},
         |   {"questionId":"annualAccountingScheme","question":"Is the business applying for the Annual Accounting Scheme?","answer":"No","answerValue":false},
         |   {"questionId":"zeroRatedSales","question":"Does the business sell mainly zero-rated goods or services?","answer":"No","answerValue":false},
         |   {"questionId":"vatRegistrationException","question":"Is the business applying for a VAT registration exception?","answer":"No","answerValue":false},
         |   {"questionId":"agriculturalFlatRateScheme","question":"Is the business applying for the Agricultural Flat Rate Scheme?","answer":"No","answerValue":false},
         |   {"questionId":"racehorses","question":"Will the business be doing any of the following?","answer":"No","answerValue":false},
         |   {"questionId":"applicantUKNino-value","question":"Do you have a UK National Insurance number?","answer":"Yes","answerValue":true},
         |   {"questionId":"applicantUKNino-optionalData","question":"Enter the National Insurance number","answer":"AB123456A","answerValue":"AB123456A"}
         | ]}
         | ]
         |}
       """.stripMargin)

        val officerName = Name(Some("test1"),Some("test11"), "testa", Some("Mr"))
        val officer = Officer(officerName, "director", None, None)

        cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString, false)
        cacheSessionData(internalId, ThresholdPreviousThirtyDaysId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData(internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(false, None))
        cacheSessionData(internalId, VoluntaryRegistrationId.toString, true)
        cacheSessionData(internalId, TurnoverEstimateId.toString, TurnoverEstimateFormElement("tenthousand", Some("11000")))
        cacheSessionData(internalId, CompletionCapacityId.toString, officer.generateId)
        cacheSessionData(internalId, InternationalActivitiesId.toString, false)
        cacheSessionData(internalId, InvolvedInOtherBusinessId.toString, false)
        cacheSessionData(internalId, AnnualAccountingSchemeId.toString, false)
        cacheSessionData(internalId, ZeroRatedSalesId.toString, false)
        cacheSessionData(internalId, VATRegistrationExceptionId.toString, false)
        cacheSessionData(internalId, AgriculturalFlatRateSchemeId.toString, false)
        cacheSessionData(internalId, RacehorsesId.toString, false)

        val request = buildClient("/national-insurance-number").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            "applicantUKNinoSelection" -> Seq("true"),
            "applicantUKNinoEntry" -> Seq("AB123456A")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some("/register-for-vat/pass-security")
      }

      "if the user has reached the nino page on a mandatory flow, with multiple officers" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val officerName = Name(Some("test1"),Some("test11"), "testa", Some("Mr"))
        val officer = Officer(officerName, "director", None, None)

        stubPatch("/vatreg/testRegId/eligibility-data", 200, """{}""", s"""
           |{"sections":[
           | {"title":"VAT-taxable sales","data":[
           |   {"questionId":"thresholdNextThirtyDays","question":"In the next 30 days alone, does the business expect to make more than £85,000 in VAT-taxable sales?","answer":"No","answerValue":false},
           |   {"questionId":"thresholdPreviousThirtyDays-value","question":"Has the business ever expected to go over the VAT-registration threshold in a single 30-day period?","answer":"Yes","answerValue":true},
           |   {"questionId":"thresholdPreviousThirtyDays-optionalData","question":"When did the business first expect to go over the threshold?","answer":"15 October 2015","answerValue":"2015-10-15"},
           |   {"questionId":"thresholdInTwelveMonths-value","question":"In any 12-month period has the business gone over the VAT-registration threshold?","answer":"Yes","answerValue":true},
           |   {"questionId":"thresholdInTwelveMonths-optionalData","question":"When did the business first go over the threshold?","answer":"10 October 2010","answerValue":"2010-10-10"},
           |   {"questionId":"turnoverEstimate-value","question":"What do you think the business's VAT-taxable turnover will be for the next 12 months?","answer":"More than £10,000","answerValue":"tenthousand"},
           |   {"questionId":"turnoverEstimate-optionalData","question":"Give an estimate rounded to the nearest £10,000","answer":"£999,999,999,999,999","answerValue":999999999999999}
           | ]},
           | {"title":"Who is doing the application?","data":[
           |  {"questionId":"completionCapacity","question":"Which of the following people are you?","answer":"None of these","answerValue": "noneOfThese"},
           |  {"questionId":"completionCapacityFillingInFor","question":"Which company officer are you doing this application for?","answer":"test1 testa","answerValue": {
           |    "name":{"forename":"test1","otherForenames":"test11","surname":"testa","title":"Mr"},"role":"director"}
           |  }
           | ]},
           | {"title":"Special situations","data":[
           |   {"questionId":"internationalActivities","question":"Will the business be doing any of the following international activities over the next 12 months?","answer":"No","answerValue":false},
           |   {"questionId":"involvedInOtherBusiness","question":"Have you been involved with another business or taken over a VAT-registered business?","answer":"No","answerValue":false},
           |   {"questionId":"annualAccountingScheme","question":"Is the business applying for the Annual Accounting Scheme?","answer":"No","answerValue":false},
           |   {"questionId":"zeroRatedSales","question":"Does the business sell mainly zero-rated goods or services?","answer":"Yes","answerValue":true},
           |   {"questionId":"vatExemption","question":"Do you want to apply for a VAT exemption?","answer":"No","answerValue":false},
           |   {"questionId":"vatRegistrationException","question":"Is the business applying for a VAT registration exception?","answer":"No","answerValue":false},
           |   {"questionId":"agriculturalFlatRateScheme","question":"Is the business applying for the Agricultural Flat Rate Scheme?","answer":"No","answerValue":false},
           |   {"questionId":"racehorses","question":"Will the business be doing any of the following?","answer":"No","answerValue":false},
           |   {"questionId":"applicantUKNino-value","question":"Does test1 testa have a UK National Insurance number?","answer":"Yes","answerValue":true},
           |   {"questionId":"applicantUKNino-optionalData","question":"Enter the National Insurance number","answer":"AB123456A","answerValue":"AB123456A"}
           | ]}
           | ]
           |}
         """.stripMargin)

        cacheSessionData(internalId, ThresholdNextThirtyDaysId.toString, false)
        cacheSessionData(internalId, ThresholdPreviousThirtyDaysId.toString, ConditionalDateFormElement(true, Some(LocalDate.of(2015, 10, 15))))
        cacheSessionData(internalId, ThresholdInTwelveMonthsId.toString, ConditionalDateFormElement(true, Some(LocalDate.of(2010, 10, 10))))
        cacheSessionData(internalId, TurnoverEstimateId.toString, TurnoverEstimateFormElement("tenthousand", Some("999999999999999")))
        cacheSessionData(internalId, CompletionCapacityId.toString, "noneOfThese")
        cacheSessionData(internalId, CompletionCapacityFillingInForId.toString, officer.generateId)
        cacheSessionData(internalId, InternationalActivitiesId.toString, false)
        cacheSessionData(internalId, InvolvedInOtherBusinessId.toString, false)
        cacheSessionData(internalId, AnnualAccountingSchemeId.toString, false)
        cacheSessionData(internalId, ZeroRatedSalesId.toString, true)
        cacheSessionData(internalId, VATExemptionId.toString, false)
        cacheSessionData(internalId, VATRegistrationExceptionId.toString, false)
        cacheSessionData(internalId, AgriculturalFlatRateSchemeId.toString, false)
        cacheSessionData(internalId, RacehorsesId.toString, false)

        val request = buildClient("/national-insurance-number").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map(
            "applicantUKNinoSelection" -> Seq("true"),
            "applicantUKNinoEntry" -> Seq("AB123456A")
          ))

        val response = await(request)
        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some(s"/register-for-vat/pass-security")
      }
    }
  }
}
