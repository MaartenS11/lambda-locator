plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.guardsquare:proguard-base:7.3.2")
    implementation("com.guardsquare:proguard-core-android:9.0.10")
    implementation("com.guardsquare:proguard-assembler:1.0.0")
    implementation("com.fifesoft:rsyntaxtextarea:3.3.3")
    implementation("com.fifesoft:rstaui:3.3.1")
    implementation("com.fifesoft:autocomplete:3.3.1")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation(testFixtures("com.guardsquare:proguard-core:9.0.8"))
    implementation("com.formdev:flatlaf:3.1.1")
    implementation("com.formdev:flatlaf-extras:3.1.1")
}

tasks.test {
    useJUnitPlatform()
}