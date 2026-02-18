plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

// Vaadin admin console — placeholder for future implementation
dependencies {
    implementation(project(":modules:domain"))
    implementation(project(":modules:application"))

    implementation("org.springframework.boot:spring-boot-starter-web")
}
