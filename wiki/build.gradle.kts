plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("vintagebadger.trainingplanner.wiki.MainKt")
}

val standaloneLogging by configurations.creating

tasks.named<JavaExec>("run") {
    classpath += standaloneLogging
    systemProperty(
        "org.slf4j.simpleLogger.defaultLogLevel",
        project.findProperty("wikiLogLevel")?.toString() ?: "debug"
    )

    project.findProperty("items")
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?.let { args("--items", it) }

    project.findProperty("item")
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?.let { args("--item", it) }

    (project.findProperty("output") ?: project.findProperty("out"))
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?.let { args("--output", it) }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    standaloneLogging("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("junit:junit:4.13.2")
}
