import sbt._

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val govukTemplateVersion = "5.60.0-play-26"
  private val playUiVersion = "8.11.0-play-26"
  private val scalaTestPlusPlayVersion = "3.1.3"
  private val pegdownVersion = "1.6.0"
  private val mockitoVersion = "2.13.0"
  private val httpCachingClientVersion = "9.1.0-play-26"
  private val playSimpleMongoVersion = "7.30.0-play-26"
  private val playConditionalFormMappingVersion = "1.4.0-play-26"
  private val playLanguageVersion = "4.5.0-play-26"
  private val bootstrapVersion = "2.1.0"
  private val scalacheckVersion = "1.13.4"
  private val jsoupVersion = "1.11.2"
  private val scoverageVersion = "1.3.1"
  private val wireMockVersion = "2.26.3"
  private val reactivemongoTestVersion = "4.21.0-play-26"

  private val playGovukFrontendVersion = "0.55.0-play-26"
  private val playHmrcFrontendVersion = "0.27.0-play-26"

  private val govukFrontendVersion = "3.7.0"
  private val hmrcFrontendVersion = "1.20.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "simple-reactivemongo" % playSimpleMongoVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "play-frontend-govuk" % playGovukFrontendVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc" % playHmrcFrontendVersion,
    "org.webjars.npm" % "govuk-frontend" % govukFrontendVersion,
    "org.webjars.npm" % "hmrc-frontend" % hmrcFrontendVersion
  )

  trait TestDependencies {
    val scope: Configuration
    val test: Seq[ModuleID]
  }

  private object UnitTestDependencies extends TestDependencies {
    override val scope: Configuration = Test
    override val test: Seq[ModuleID] = Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
      "org.pegdown" % "pegdown" % pegdownVersion % scope,
      "org.jsoup" % "jsoup" % jsoupVersion % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
      "org.mockito" % "mockito-core" % mockitoVersion % scope,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope
    )

    def apply(): Seq[ModuleID] = test
  }

  private object IntegrationTestDependencies extends TestDependencies {
    override val scope: Configuration = IntegrationTest
    override val test: Seq[ModuleID] = Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
      "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % scope,
      "org.jsoup" % "jsoup" % jsoupVersion % scope,
      "org.scoverage" % "scalac-scoverage-runtime_2.12" % scoverageVersion % scope,
      "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion % scope
    )

    def apply(): Seq[ModuleID] = test
  }

  def apply() = compile ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}
