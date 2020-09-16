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

import controllers.routes
import forms.AgriculturalFlatRateSchemeFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.agriculturalFlatRateScheme
import views.newbehaviours.YesNoViewBehaviours

class AgriculturalFlatRateSchemeViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "agriculturalFlatRateScheme"
  val form = new AgriculturalFlatRateSchemeFormProvider()()
  implicit val msgs = messages

  val continueButton = "Continue"
  val h1 = "Is the business applying for the Agricultural Flat Rate Scheme?"
  val p1 = "The Agricultural Flat Rate Scheme is an alternative to VAT registration for farmers."
  val afrsLinkText1 = "Find out more about the"
  val afrsLinkText2 = "Agricultural Flat Rate Scheme (opens in a new window)."
  val afrsLink = "https://www.gov.uk/government/publications/vat-notice-70046-agricultural-flat-rate-scheme/vat-notice-70046-agricultural-flat-rate-scheme"
  val p2 = s"$afrsLinkText1 $afrsLinkText2"

  object Selectors extends BaseSelectors

  def createView: () => HtmlFormat.Appendable =
    () => agriculturalFlatRateScheme(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => agriculturalFlatRateScheme(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "AgriculturalFlatRateScheme view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.AgriculturalFlatRateSchemeController.onSubmit().url)
    behave like pageWithSubmitButton(createViewUsingForm(form), continueButton)
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
    doc.select(Selectors.p(2)).select("a").attr("href") mustBe afrsLink
  }
}
