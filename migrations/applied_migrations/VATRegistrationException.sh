#!/bin/bash

echo "Applying migration VATRegistrationException"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /registration-exception                                   controllers.VATRegistrationExceptionController.onPageLoad()" >> ../conf/app.routes
echo "POST       /registration-exception                                   controllers.VATRegistrationExceptionController.onSubmit()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatRegistrationException.title = Is the business applying for a VAT registration ‘exception’?" >> ../conf/messages.en
echo "vatRegistrationException.heading = Is the business applying for a VAT registration ‘exception’?" >> ../conf/messages.en
echo "vatRegistrationException.checkYourAnswersLabel = vatRegistrationException" >> ../conf/messages.en
echo "vatRegistrationException.error.required = Please give an answer for vatRegistrationException" >> ../conf/messages.en
echo "vatRegistrationException.text = You can get a registration exception if both of the following apply:" >> ../conf/messages.en

echo "Adding helper line into UserAnswers"
awk '/class/ {\
     print;\
     print "  def vatRegistrationException: Option[Boolean] = cacheMap.getEntry[Boolean](VATRegistrationExceptionId.toString)";\
     print "";\
     next }1' ../app/utils/UserAnswers.scala > tmp && mv tmp ../app/utils/UserAnswers.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class/ {\
     print;\
     print "";\
     print "  def vatRegistrationException: Option[AnswerRow] = userAnswers.vatRegistrationException map {";\
     print "    x => AnswerRow(\"vatRegistrationException.checkYourAnswersLabel\", if(x) \"site.yes\" else \"site.no\", true, routes.VATRegistrationExceptionController.onPageLoad().url)"; print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration VATRegistrationException completed"
