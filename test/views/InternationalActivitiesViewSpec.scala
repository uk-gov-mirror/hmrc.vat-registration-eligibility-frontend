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
import forms.InternationalActivitiesFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.internationalActivities
import views.newbehaviours.YesNoViewBehaviours

class InternationalActivitiesViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "internationalActivities"
  val form = new InternationalActivitiesFormProvider()()
  implicit val msgs = messages

  val continueButton = "Continue"
  val h1 = "Will the business do any of the following international activities over the next 12 months?"
  val paragraph = "Tell us if the business will:"
  val bullet1 = "export goods or services to EU countries"
  val bullet2 = "import goods or services from EU countries"
  val bullet3 = "sell assets that it bought from a country outside the EU and claimed a VAT refund on"
  val bullet4 = "do all of its business outside the UK"
  val bullet5 = "have its head office outside the UK"

  object Selectors extends BaseSelectors

  def createView: () => HtmlFormat.Appendable =
    () => internationalActivities(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => internationalActivities(form, NormalMode)(fakeDataRequest, messages, frontendAppConfig)

  "InternationalActivities view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like pageWithBackLink(createViewUsingForm(form))
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.InternationalActivitiesController.onSubmit().url)
    behave like pageWithSubmitButton(createViewUsingForm(form), continueButton)

    "have the correct heading " in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.h1).text() mustBe h1
    }

    "have the correct paragraph" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.p(1)).text() mustBe paragraph
    }

    "display the bullet text correctly" in {
      val doc = asDocument(createViewUsingForm(form))
      doc.select(Selectors.bullet(1)).first().text() mustBe bullet1
      doc.select(Selectors.bullet(2)).first().text() mustBe bullet2
      doc.select(Selectors.bullet(3)).first().text() mustBe bullet3
      doc.select(Selectors.bullet(4)).first().text() mustBe bullet4
      doc.select(Selectors.bullet(5)).first().text() mustBe bullet5
    }

  }

}
