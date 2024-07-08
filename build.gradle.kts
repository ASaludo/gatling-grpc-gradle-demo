import com.google.protobuf.gradle.*

plugins {
    idea
    kotlin("jvm")               version "1.9.24"
    kotlin("plugin.allopen")    version "1.9.24"



    id("com.google.protobuf")   version "0.9.4"
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
    add("com.google.protobuf:protobuf-kotlin:3.25.3")
    add("io.grpc:grpc-netty-shaded:1.53.0")
    add("io.grpc:grpc-protobuf:1.53.0")
    add("io.grpc:grpc-stub:1.53.0")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    // https://mvnrepository.com/artifact/build.buf.protoc-gen-validate/pgv-java-stub
    implementation("build.buf.protoc-gen-validate:pgv-java-stub:1.0.2")
    gatling("com.github.phisgr:gatling-grpc:0.17.0")
    // for Scala Gatling tests
//    gatling("com.github.phisgr:gatling-javapb:1.3.0")
    // for Kotlin/Java Gatling tests
    gatling("com.github.phisgr:gatling-grpc-kt:0.15.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.0"
        }
        id("javapgv") {
            artifact = "build.buf.protoc-gen-validate:protoc-gen-validate:1.0.4"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            tasks.getByName("compileGatlingKotlin").dependsOn(it)
            it.builtins {
                maybeCreate("java") // Used by kotlin and already defined by default
                create("kotlin")
            }
            it.plugins {
                id("grpc")
                id("javapgv") { option("lang=java")}
            }
        }
    }
}

