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

import java.time.LocalDate

import controllers.routes
import forms.ThresholdPreviousThirtyDaysFormProvider
import models.NormalMode
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import services.ThresholdService
import utils.TimeMachine
import views.html.thresholdPreviousThirtyDays
import views.newbehaviours.ViewBehaviours

class ThresholdPreviousThirtyDaysViewSpec extends ViewBehaviours {

  object TestTimeMachine extends TimeMachine {
    override def today: LocalDate = LocalDate.parse("2020-01-01")
  }

  val messageKeyPrefix = "thresholdPreviousThirtyDays"
  val form = new ThresholdPreviousThirtyDaysFormProvider(TestTimeMachine)()
  implicit val msgs: Messages = messages

  val thresholdService: ThresholdService = app.injector.instanceOf[ThresholdService]

  object Selectors extends BaseSelectors

  val button = "Continue"

  def createView: () => HtmlFormat.Appendable = () =>
      thresholdPreviousThirtyDays(form, NormalMode, thresholdService)(fakeDataRequestIncorped, messages, frontendAppConfig)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
      thresholdPreviousThirtyDays(form, NormalMode, thresholdService)(fakeDataRequestIncorped, messages, frontendAppConfig)

  "ThresholdPreviousThirtyDays view" must {
    behave like normalPage(createView(), messageKeyPrefix)

    "have a continue button" in {
      val doc = asDocument(createViewUsingForm(form))

      doc.select(Selectors.button).text() mustBe button
    }
  }
}