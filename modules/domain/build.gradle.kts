plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

dependencies {
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
}
