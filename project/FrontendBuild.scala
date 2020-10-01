import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "vat-registration-eligibility-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val playHealthVersion = "3.9.0-play-26"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val govukTemplateVersion = "5.54.0-play-26"
  private val playUiVersion = "8.11.0-play-26"
  private val scalaTestVersion = "3.0.8"
  private val scalaTestPlusPlayVersion = "3.1.3"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "8.0.0"
  private val playSimpleMongoVersion = "7.30.0-play-26"
  private val playConditionalFormMappingVersion = "0.2.0"
  private val playLanguageVersion = "4.3.0-play-26"
  private val bootstrapVersion = "1.14.0"
  private val scalacheckVersion = "1.13.4"
  private val jsoupVersion = "1.11.2"
  private val scoverageVersion = "1.3.1"
  private val wireMockVersion = "2.26.3"
  private val reactivemongoTestVersion = "4.21.0-play-26"


  private val playGovukFrontendVersion = "0.50.0-play-26"
  private val playHmrcFrontendVersion = "0.18.0-play-26"
  private val govukFrontendVersion = "3.7.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "simple-reactivemongo" % playSimpleMongoVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "play-frontend-govuk" % playGovukFrontendVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc" % playHmrcFrontendVersion,
    "org.webjars.npm" % "govuk-frontend" % govukFrontendVersion
  )

  trait TestDependencies {
    val scope: Configuration
    val test: Seq[ModuleID]
  }

  private object UnitTestDependencies extends TestDependencies {
    override val scope: Configuration = Test
    override val test: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
      "org.pegdown" % "pegdown" % pegdownVersion % scope,
      "org.jsoup" % "jsoup" % jsoupVersion % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
      "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
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
      "org.scoverage" % "scalac-scoverage-runtime_2.11" % scoverageVersion % scope,
      "uk.gov.hmrc" %% "reactivemongo-test" % reactivemongoTestVersion % scope
    )

    def apply(): Seq[ModuleID] = test
  }

  def apply() = compile ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}
