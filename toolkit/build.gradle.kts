import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKotlinMultiplatformLibrary)
  alias(libs.plugins.androidLint)
  alias(libs.plugins.vanniktechMavenPublish)
  alias(libs.plugins.dokka)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }

  androidLibrary {
    namespace = "com.airsaid.toolkit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    androidResources {
      enable = true
    }

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }

    withHostTest {
    }

    withDeviceTestBuilder {
      sourceSetTreeName = "test"
    }.configure {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines.core)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.documentfile)
        implementation(libs.androidx.lifecycle.process)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    getByName("androidDeviceTest") {
      dependencies {
        implementation(libs.androidx.runner)
        implementation(libs.androidx.core)
        implementation(libs.androidx.testExt.junit)
      }
    }
  }
}

mavenPublishing {
  configure(
    KotlinMultiplatform(
      javadocJar = JavadocJar.Dokka("dokkaGenerateHtml")
    )
  )
}
