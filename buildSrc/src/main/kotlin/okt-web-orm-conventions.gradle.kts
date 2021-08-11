/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("okt-web-common-conventions")
}
dependencies {
    // mysql
    implementation("mysql:mysql-connector-java:$mysqlVersion")

    // hikari
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    // exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    //reflections
    implementation("org.reflections:reflections:$reflectionsVersion")
    //JColor
    implementation("com.diogonunes:JColor:$jColorVersion")
}