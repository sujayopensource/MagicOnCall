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
    implementation(project(":modules:infrastructure:persistence"))
    implementation(project(":modules:infrastructure:messaging"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
}
