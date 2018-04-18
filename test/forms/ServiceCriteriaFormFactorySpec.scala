/*
 * Copyright 2018 HM Revenue & Customs
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

package forms

import models.view.YesOrNoQuestion
import uk.gov.hmrc.play.test.UnitSpec

class ServiceCriteriaFormFactorySpec extends UnitSpec {
  val testForm = ServiceCriteriaFormFactory.form("testQuestion")

  "Binding ServiceCriteriaFormFactory to a model" should {
    "Bind successfully with full data" in {
      val data = Map("testQuestionRadio" -> "true")
      val model = YesOrNoQuestion("testQuestion", true)

      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundModel shouldBe model
    }

    "Have the correct error if no data are completed" in {
      val data: Map[String,String] = Map()
      val boundForm = testForm.bind(data)

      boundForm.errors map { formErrors =>
        (formErrors.key, formErrors.message)
      } shouldBe Seq("testQuestionRadio" -> "validation.eligibility.testQuestion.missing")
    }
  }

  "Unbinding ServiceCriteriaFormFactory to a model" should {
    "Unbind successfully with full data" in {
      val data = Map("testQuestionRadio" -> "true")
      val model = YesOrNoQuestion("testQuestion", true)

      testForm.fill(model).data shouldBe data
    }
  }
}
