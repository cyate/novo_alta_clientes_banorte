plugins {
    java
//    id("org.gretty") version "4.1.0"
    `maven-publish`
}

group = "com.novo"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    flatDir {
        dirs("lib")
    }
}

dependencies {
    // https://mvnrepository.com/artifact/javax.xml.bind/jaxb-api
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

//tasks.named<Jar>("war") {
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    from("web")
//    webInf {
//        from("web/WEB-INF")
//    }
//}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

//gretty {
//    httpPort = 8080
//    contextPath = "/"
//    servletContainer = "tomcat10"
//}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                developers {
                    developer {
                        id.set("cyate")
                        email.set("cyate@novopayment.com")
                        name.set("Camilo Yate Yara")
                        roles.set(listOf("Líder Técnico", "Novopayment Colombia"))
                        properties.set(mapOf("timezone" to "Colombia"))
                    }
                }
            }
        }
    }
}
