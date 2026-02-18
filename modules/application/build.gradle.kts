plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

dependencies {
    implementation(project(":modules:domain"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    implementation("io.micrometer:micrometer-core")
    implementation("io.github.resilience4j:resilience4j-retry:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${property("resilience4jVersion")}")

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
