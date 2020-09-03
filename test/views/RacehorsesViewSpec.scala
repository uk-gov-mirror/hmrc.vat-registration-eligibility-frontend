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
import forms.RacehorsesFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.html.racehorses
import views.newbehaviours.YesNoViewBehaviours

class RacehorsesViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "racehorses"
  val form = new RacehorsesFormProvider()()
  implicit val msgs = messages

  object Selectors extends BaseSelectors

  val paragraph = "Tell us if the business will:"
  val bullet1 = "buy, sell or rent out land or property as a business activity (not just to have its own premises)"
  val bullet2 = "own one or more racehorses"
  val button = "Save and continue"

  def createView: () => HtmlFormat.Appendable =
    () => racehorses(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => racehorses(form, NormalMode)(fakeDataRequestIncorped, messages, frontendAppConfig)

  "Racehorses view" must {
    behave like normalPage(createView(), messageKeyPrefix)
    behave like yesNoPage(form, createViewUsingForm, messageKeyPrefix, routes.RacehorsesController.onSubmit().url)

    "have a paragraph" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.p(1)).text() mustBe paragraph
    }

    "have bullets" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.bullet(1)).text() mustBe bullet1
      doc.select(Selectors.bullet(2)).text() mustBe bullet2
    }

    "have a save and continue button" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.button).text() mustBe button
    }
  }
}
