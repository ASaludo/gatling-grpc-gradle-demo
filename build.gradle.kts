import com.google.protobuf.gradle.*

plugins {
    java
    idea

    kotlin("jvm") version "1.8.10"

    application

    id("com.google.protobuf") version "0.9.2"
    id("io.gatling.gradle") version "3.9.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    fun add(s: String) {
        implementation(s)
        gatling(s)
    }

    add("com.google.protobuf:protobuf-java:3.22.2")
    add("io.grpc:grpc-netty-shaded:1.53.0")
    add("io.grpc:grpc-protobuf:1.53.0")
    add("io.grpc:grpc-stub:1.53.0")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    // https://mvnrepository.com/artifact/build.buf.protoc-gen-validate/pgv-java-stub
    implementation("build.buf.protoc-gen-validate:pgv-java-stub:1.0.2")
    gatling("com.github.phisgr:gatling-grpc:0.16.0")
    // for Scala Gatling tests
    gatling("com.github.phisgr:gatling-javapb:1.3.0")
    // for Kotlin/Java Gatling tests
    gatling("com.github.phisgr:gatling-grpc-kt:0.15.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.22.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.53.0"
        }
        id("javapgv") {
            artifact = "build.buf.protoc-gen-validate:protoc-gen-validate:1.0.4"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("javapgv") { option("lang=java")}
            }
        }
    }
}

application {
    mainClass.set("com.github.phisgr.example.DemoServer")
}
