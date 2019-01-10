/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.CompletionCapacityFormProvider
import identifiers.CompletionCapacityFillingInForId
import models._
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.completionCapacityFillingInFor

class CompletionCapacityFillingInForViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "completionCapacityFillingInFor"

  val officersList: Seq[Officer] = Seq(
    Officer(Name(Some("First"), Some("Middle"), "Last",Some("Mrs")),"director", None, Some("some-url")),
    Officer(Name(Some("Second"), None, "VeryLast",Some("Mr")), "secretary", None, Some("some-url"))
  )

  val form = new CompletionCapacityFormProvider()(CompletionCapacityFillingInForId)(officersList)

  def createView = () => completionCapacityFillingInFor(frontendAppConfig, form, NormalMode, CompletionCapacity.multipleOfficers(officersList, false))(fakeRequest, messages)

  def createViewUsingForm = (form: Form[_]) => completionCapacityFillingInFor(frontendAppConfig, form, NormalMode, CompletionCapacity.multipleOfficers(officersList, false))(fakeRequest, messages)

  "CompletionCapacityFillingInFor view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "CompletionCapacityFillingInFor view" when {
    "rendered" must {
      "contain radio buttons for the value" in {
        val doc = asDocument(createViewUsingForm(form))
        for (option <- officersList) {
          assertContainsRadioButton(doc, s"completionCapacity-${option.generateId}", "value", option.generateId, false)
        }
      }
    }

    for(option <- officersList) {
      s"rendered with a value of '${option.generateId}'" must {
        s"have the '${option.generateId}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"${option.generateId}"))))
          assertContainsRadioButton(doc, s"completionCapacity-${option.generateId}", "value", option.generateId, true)

          for(unselectedOption <- officersList.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, s"completionCapacity-${unselectedOption.generateId}", "value", unselectedOption.generateId, false)
          }
        }
      }
    }
  }
}
