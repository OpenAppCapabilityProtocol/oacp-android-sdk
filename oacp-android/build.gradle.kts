plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "org.oacp.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        explicitApi()
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.json:json:20231013")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "org.oacp"
                artifactId = "oacp-android"
                version = project.findProperty("VERSION_NAME") as String? ?: "0.3.0"

                pom {
                    name.set("OACP Android SDK")
                    description.set("Make any Android app voice-controllable with the Open App Capability Protocol")
                    url.set("https://github.com/OpenAppCapabilityProtocol/oacp-android-sdk")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("0xharkirat")
                            name.set("Harkirat Singh")
                            url.set("https://github.com/0xharkirat")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/OpenAppCapabilityProtocol/oacp-android-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/OpenAppCapabilityProtocol/oacp-android-sdk.git")
                        url.set("https://github.com/OpenAppCapabilityProtocol/oacp-android-sdk")
                    }
                }
            }
        }
    }
}
