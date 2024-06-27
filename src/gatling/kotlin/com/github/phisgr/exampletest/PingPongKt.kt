package com.github.phisgr.exampletest

import authzed.api.v1.CheckpermissionService
import authzed.api.v1.CheckpermissionService.CheckPermissionRequest
import authzed.api.v1.CheckpermissionService.SubjectReference
import authzed.api.v1.PermissionsServiceGrpc
import com.github.phisgr.example.DemoServiceGrpc
import com.github.phisgr.example.Ping
import com.github.phisgr.gatling.kt.grpc.grpc
import com.github.phisgr.gatling.kt.grpc.payload
import com.github.phisgr.gatling.kt.hook
import com.github.phisgr.gatling.kt.on
import com.github.phisgr.gatling.kt.scenario
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class PingPongKt : Simulation() {

    private val grpcConfSpice = grpc(ManagedChannelBuilder.forAddress("SRV-DEV-SPICEDB", 50051).usePlaintext())
        .header(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer altima_grpc_key2")
        .shareChannel()

    private fun requestSpice(name: String) = grpc(name)

        .rpc(PermissionsServiceGrpc.getCheckPermissionMethod())
        .payload(CheckPermissionRequest::newBuilder) { session ->
            consistency = CheckPermissionRequest.Consistency.newBuilder()
                .setMinimizeLatency(true)
                .build()
            // dynamic payload!
            resource = CheckpermissionService.ObjectReference.newBuilder()
                .setObjectId("ALT0001210003").setObjectType("person")
                .build()
            permission = "associated_with"
            subject = SubjectReference.newBuilder().setObject(
                CheckpermissionService.ObjectReference.newBuilder().setObjectType("partner")
                    .setObjectId("primav8")
                    .build()

            ).build()
            withTracing = true
            build()
        }

    val scnSpice = scenario("SpiceDB") {
        +during(10.seconds.toJavaDuration()).on {
            +pause(500.milliseconds.toJavaDuration())
            +requestSpice("permission")

        }
    }

    private val grpcConf = grpc(ManagedChannelBuilder.forAddress("localhost", 9999).usePlaintext())
        .shareChannel()

    private fun request(name: String) = grpc(name)
        .rpc(DemoServiceGrpc.getPingPongMethod())
        .payload(Ping::newBuilder) { session ->
            // dynamic payload!
            data = session.getInt("data")
            build()
        }
        .check({ extract { it.data }.isEL("#{data}") })

    private val scn = scenario("Play Ping Pong") {
        +hook { it.set("data", 0) }
        +during(10.seconds.toJavaDuration()).on {
            +pause(500.milliseconds.toJavaDuration())
            +request("Send message")
            +hook { session -> session.set("data", 1 + session.getInt("data")) }
        }
    }

    init {
        setUp(scnSpice.injectOpen(atOnceUsers(1)).protocols(grpcConfSpice))
    }
}
