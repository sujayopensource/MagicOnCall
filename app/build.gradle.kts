plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // ── Module dependencies (composition root wires everything) ──
    implementation(project(":modules:domain"))
    implementation(project(":modules:application"))
    implementation(project(":modules:api"))
    implementation(project(":modules:infrastructure:persistence"))
    implementation(project(":modules:infrastructure:messaging"))
    implementation(project(":modules:infrastructure:connectors"))
    implementation(project(":modules:infrastructure:observability"))
    implementation(project(":modules:workers"))
    implementation(project(":modules:eval"))
    implementation(project(":modules:ui-vaadin"))

    // ── Spring Boot starters ──
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── Database ──
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // ── Messaging ──
    implementation("org.springframework.kafka:spring-kafka")

    // ── Cache ──
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // ── Observability ──
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── Test ──
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("io.rest-assured:rest-assured")
}
