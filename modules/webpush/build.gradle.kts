plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
}

repositories {
    mavenCentral()

    // TODO: remove after dev.whyoleg.cryptography release 0.4.0
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

kotlin {
    jvmToolchain(11)
    explicitApi()

    jvm()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.random)
            implementation(libs.cryptography.serialization.asn1.modules)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.http)
        }

        jvmTest.dependencies {
            implementation(libs.cryptography.provider.jdk)
            implementation(libs.kotlin.test.junit)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.html.builder)
            implementation(libs.playwright)
            implementation(libs.slf4j.simple)
        }
    }
}
