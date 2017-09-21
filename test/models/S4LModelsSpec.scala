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

package models

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import models.api.VatChoice.{NECESSITY_OBLIGATORY, NECESSITY_VOLUNTARY}
import models.api._

import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.VoluntaryRegistrationReason._
import org.scalatest.Inspectors
import uk.gov.hmrc.play.test.UnitSpec

class S4LModelsSpec  extends UnitSpec with Inspectors with VatRegistrationFixture {

  "S4LTradingDetails.S4LModelTransformer.toS4LModel" should {
    val specificDate = LocalDate.of(2017, 11, 12)
    val tradingName = "name"

    "transform VatScheme to S4L container" in {
      val vs = emptyVatScheme.copy(
        tradingDetails = Some(VatTradingDetails(
          vatChoice = VatChoice(
            necessity = NECESSITY_VOLUNTARY,
            reason = Some(INTENDS_TO_SELL),
            vatThresholdPostIncorp = Some(validVatThresholdPostIncorp))
        ))
      )

      val expected = S4LTradingDetails(
        taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
        tradingName = Some(TradingNameView(yesNo = TRADING_NAME_YES, tradingName = Some(tradingName))),
        startDate = Some(StartDateView(
          dateType = SPECIFIC_DATE,
          date = Some(specificDate),
          ctActiveDate = None)),
        voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(INTENDS_TO_SELL)),
        euGoods = Some(EuGoods(EU_GOODS_YES)),
        applyEori = Some(ApplyEori(APPLY_EORI_YES)),
        overThreshold = Some(OverThresholdView(false, None))
      )

      S4LTradingDetails.modelT.toS4LModel(vs) shouldBe expected
    }
  }

  "S4LTradingDetails.S4LApiTransformer.toApi" should {
    val specificDate = LocalDate.of(2017, 11, 12)
    val tradingName = "name"

    val s4l = S4LTradingDetails(
      taxableTurnover = Some(TaxableTurnover(TAXABLE_NO)),
      tradingName = Some(TradingNameView(yesNo = TRADING_NAME_YES, tradingName = Some(tradingName))),
      startDate = Some(StartDateView(
        dateType = SPECIFIC_DATE,
        date = Some(specificDate),
        ctActiveDate = None)),
      voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_YES)),
      voluntaryRegistrationReason = Some(VoluntaryRegistrationReason(INTENDS_TO_SELL)),
      euGoods = Some(EuGoods(EU_GOODS_YES)),
      applyEori = Some(ApplyEori(APPLY_EORI_YES)),
      overThreshold = Some(OverThresholdView(false, None))
    )

    "transform complete S4L with voluntary registration model to API" in {
      val expected = VatTradingDetails(
        vatChoice = VatChoice(
          necessity = NECESSITY_VOLUNTARY,
          vatStartDate = VatStartDate(selection = SPECIFIC_DATE, startDate = Some(specificDate)),
          reason = Some(INTENDS_TO_SELL),
          vatThresholdPostIncorp = Some(validVatThresholdPostIncorp)),
        tradingName = TradingName(selection = true, tradingName = Some(tradingName)),
        euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
      )

      S4LTradingDetails.apiT.toApi(s4l) shouldBe expected
    }

    "transform complete S4L with mandatory registration model to API" in {

      val expected = VatTradingDetails(
        vatChoice = VatChoice(
          necessity = NECESSITY_OBLIGATORY,
          vatStartDate = VatStartDate(selection = SPECIFIC_DATE, startDate = Some(specificDate)),
          reason = None,
          vatThresholdPostIncorp = Some(validVatThresholdPostIncorp)),
        tradingName = TradingName(selection = true, tradingName = Some(tradingName)),
        euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
      )

      val s4lMandatoryBydefault = s4l.copy(voluntaryRegistration = None, voluntaryRegistrationReason = None)
      S4LTradingDetails.apiT.toApi(s4lMandatoryBydefault) shouldBe expected

      val s4lMandatoryExplicit = s4l.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)), voluntaryRegistrationReason = None)
      S4LTradingDetails.apiT.toApi(s4lMandatoryExplicit) shouldBe expected

    }

    "transform S4L model with incomplete data error" in {
      val s4lNoStartDate = s4l.copy(startDate = None)
      an[IllegalStateException] should be thrownBy S4LTradingDetails.apiT.toApi(s4lNoStartDate)

      val s4lNoTradingName = s4l.copy(tradingName = None)
      an[IllegalStateException] should be thrownBy S4LTradingDetails.apiT.toApi(s4lNoTradingName)

      val s4lNoEuGoods = s4l.copy(euGoods = None)
      an[IllegalStateException] should be thrownBy S4LTradingDetails.apiT.toApi(s4lNoEuGoods)

    }
  }

  "S4LFlatRateScheme.S4LApiTransformer.toApi" should {
    val specificDate = LocalDate.of(2017, 11, 12)
    val category = "category"
    val percent = 16.5

    "transform complete s4l container to API" in {

      val s4l = S4LFlatRateScheme(
        joinFrs = Some(JoinFrsView(true)),
        annualCostsInclusive = Some(AnnualCostsInclusiveView(AnnualCostsInclusiveView.NO)),
        annualCostsLimited = Some(AnnualCostsLimitedView(AnnualCostsLimitedView.NO)),
        registerForFrs = Some(RegisterForFrsView(true)),
        frsStartDate = Some(FrsStartDateView(DIFFERENT_DATE, Some(specificDate))),
        categoryOfBusiness = Some(BusinessSectorView(category, percent))
      )

      val expected = VatFlatRateScheme(
        joinFrs = true,
        annualCostsInclusive = Some(AnnualCostsInclusiveView.NO),
        annualCostsLimited = Some(AnnualCostsLimitedView.NO),
        doYouWantToUseThisRate = Some(true),
        whenDoYouWantToJoinFrs = Some(DIFFERENT_DATE),
        startDate = Some(specificDate),
        categoryOfBusiness = Some(category),
        percentage = Some(percent)
      )

      S4LFlatRateScheme.apiT.toApi(s4l) shouldBe expected
    }

    "transform s4l container with defaults to API" in {
      val s4l = S4LFlatRateScheme(joinFrs = None)
      val expected = VatFlatRateScheme(joinFrs = false)

      S4LFlatRateScheme.apiT.toApi(s4l) shouldBe expected
    }

  }

  "S4LVatEligibility.S4LModelTransformer.toApi" should {
    "transform complete s4l container to API" in {
      val s4l = S4LVatEligibility(Some(validServiceEligibility))
      S4LVatEligibility.apiT.toApi(s4l) shouldBe validServiceEligibility
    }

    "transform s4l container with incomplete data error" in {
      val s4l = S4LVatEligibility()
      an[IllegalStateException] should be thrownBy S4LVatEligibility.apiT.toApi(s4l)
    }
  }

  "S4LVatContact.S4LModelTransformer.toApi" should {

    val s4l = S4LVatContact(
      businessContactDetails = Some(BusinessContactDetails(
        email = "email",
        daytimePhone = Some("tel"),
        mobile = Some("mobile"),
        website = Some("website"))),
      ppob = Some(PpobView(scrsAddress.id, Some(scrsAddress)))
    )

    "transform complete s4l container to API" in {

      val expected = VatContact(
        digitalContact = VatDigitalContact(
          email = "email",
          tel = Some("tel"),
          mobile = Some("mobile")),
        website = Some("website"),
        ppob = scrsAddress)

      S4LVatContact.apiT.toApi(s4l) shouldBe expected

    }

    "transform s4l container with incomplete data error" in {
      val s4lNoContactDetails = s4l.copy(businessContactDetails = None)
      an[IllegalStateException] should be thrownBy S4LVatContact.apiT.toApi(s4lNoContactDetails)

      val s4lPpob = s4l.copy(ppob = None)
      an[IllegalStateException] should be thrownBy S4LVatContact.apiT.toApi(s4lPpob)
    }
  }


}