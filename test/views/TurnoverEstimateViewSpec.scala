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
import models.{NormalMode, TurnoverEstimate}
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.turnoverEstimate

class TurnoverEstimateViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "turnoverEstimate"

  val form = new TurnoverEstimateFormProvider()()

  def createView = () => turnoverEstimate(frontendAppConfig, form, NormalMode)(fakeDataRequestIncorped, messages)

  def createViewUsingForm = (form: Form[_]) => turnoverEstimate(frontendAppConfig, form, NormalMode)(fakeDataRequestIncorped, messages)

  "TurnoverEstimate view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "TurnoverEstimate view" when {
    "rendered" must {
      "contain radio buttons for the value" in {
        val doc = asDocument(createViewUsingForm(form))
        for (option <- TurnoverEstimate.options) {
          assertContainsRadioButton(doc, option.id, "turnoverEstimateSelection", option.value, false)
        }
      }
    }

    for(option <- TurnoverEstimate.options) {
      s"rendered with a value of '${option.value}'" must {
        s"have the '${option.value}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("turnoverEstimateSelection" -> s"${option.value}"))))
          assertContainsRadioButton(doc, option.id, "turnoverEstimateSelection", option.value, true)

          for(unselectedOption <- TurnoverEstimate.options.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, unselectedOption.id, "turnoverEstimateSelection", unselectedOption.value, false)
          }
        }
      }
    }
  }
}
