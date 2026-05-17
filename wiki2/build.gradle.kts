plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}

tasks.named<JavaExec>("run") {
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
}
