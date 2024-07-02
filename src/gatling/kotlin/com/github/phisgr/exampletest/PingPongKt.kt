package com.github.phisgr.exampletest

import authzed.api.v2.CheckpermissionService
import authzed.api.v2.CheckpermissionService.CheckPermissionRequest
import authzed.api.v2.CheckpermissionService.SubjectReference
import authzed.api.v2.PermissionsServiceGrpc
import com.github.phisgr.gatling.kt.grpc.grpc
import com.github.phisgr.gatling.kt.grpc.payload
import com.github.phisgr.gatling.kt.scenario
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Simulation
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata

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
                .setObjectId(session.getString("person")).setObjectType("person")
                .build()
            permission = "associated_with"
            subject = SubjectReference.newBuilder().setObject(
                CheckpermissionService.ObjectReference.newBuilder().setObjectType("partner")
                    .setObjectId(session.getString("partner"))
                    .build()

            ).build()
            build()
        }

    val scnSpice = scenario("SpiceDB") {
        +feed(csv("permission.csv").circular())
        +requestSpice("permission")
    }

    init {
        setUp(
            scnSpice.injectOpen(
                atOnceUsers(1)
//                incrementUsersPerSec(500.0)
//                    .times(5)
//                    .eachLevelLasting(10)
//                    .separatedByRampsLasting(10)
//                    .startingFrom(1000.0)
                    // Double
            )

                .protocols(grpcConfSpice)
        )
    }
}
