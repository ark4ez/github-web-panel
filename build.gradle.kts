import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "io.github.ark4ez"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        val localRider = providers.gradleProperty("localRider")
        if (localRider.isPresent) local(localRider.get())
        else rider(providers.gradleProperty("platformVersion"))
        bundledPlugin("Git4Idea")
        bundledPlugin("com.intellij.modules.jcef")
        bundledModule("intellij.platform.vcs.dvcs")
        bundledModule("intellij.platform.vcs.dvcs.impl")
        pluginVerifier("1.410")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8"; options.release = 21 }
val regression by sourceSets.creating {
    java.setSrcDirs(listOf("tests"))
    java.exclude("**/SmokeStartup.java")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val regressionTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Run URL, preferences and responsive toolbar regressions without a live GitHub account."
    dependsOn(tasks.named(regression.classesTaskName))
    classpath = regression.runtimeClasspath
    mainClass = "local.githubpanel.RegressionSuite"
    jvmArgs("-Djava.awt.headless=true")
}
tasks.check { dependsOn(regressionTest) }
tasks.named("buildPlugin") { dependsOn(regressionTest) }
tasks.processResources {
    from("LICENSE") { into("META-INF") }
    from("PRIVACY.md") { into("META-INF") }
}
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
    pluginConfiguration {
        version = project.version.toString()
        ideaVersion { sinceBuild = "262.9437.287"; untilBuild = "262.*" }
    }
    pluginVerification {
        ides {
            current()
        }
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
            VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
        )
    }
}
// Publishing is intentionally manual: no Marketplace token or automated upload task is configured.
