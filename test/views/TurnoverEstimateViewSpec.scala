/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import forms.TurnoverEstimateFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.newbehaviours.ViewBehaviours
import views.html.turnoverEstimate

class TurnoverEstimateViewSpec extends ViewBehaviours {

  object Selectors extends BaseSelectors

  implicit val msgs = messages
  val messageKeyPrefix = "turnoverEstimate"
  val button = "Continue"
  val h1 = "What do you think the business’ VAT-taxable turnover will be for the next 12 months?"
  val p1 = "Include the sale of all goods and services that are not exempt from VAT. You must include goods and services that have a 0% VAT rate."
  val estimateLinkText1 = "Find out more about"
  val estimateLinkText2 = "which goods and services are exempt from VAT (opens in a new tab)."
  val estimateLink = "https://www.gov.uk/guidance/rates-of-vat-on-different-goods-and-services"
  val p2 = s"$estimateLinkText1 $estimateLinkText2"

  val form = new TurnoverEstimateFormProvider()()

  def createView: () => HtmlFormat.Appendable = () => turnoverEstimate(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => turnoverEstimate(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  "TurnoverEstimate view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like pageWithSubmitButton(createViewUsingForm(form), button)

    "have a Continue button" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.button).text() mustBe button
    }

    "have the correct heading" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h1).text() mustBe h1
    }

    "have the first paragraph" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(1)).text() mustBe p1
    }

    "have the second paragraph with the correct url link" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(2)).select("a").attr("href") mustBe estimateLink
    }
  }
}