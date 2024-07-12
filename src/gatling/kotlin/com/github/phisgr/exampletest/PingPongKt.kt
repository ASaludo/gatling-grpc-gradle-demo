package com.github.phisgr.exampletest

import authzed.api.v1.CheckpermissionService
import authzed.api.v1.CheckpermissionService.ObjectReference
import authzed.api.v1.CheckpermissionService.RelationshipUpdate
import authzed.api.v1.CheckpermissionService.Relationship
import authzed.api.v1.CheckpermissionService.CheckPermissionRequest
import authzed.api.v1.CheckpermissionService.Consistency
import authzed.api.v1.CheckpermissionService.SubjectReference
import authzed.api.v1.CheckpermissionService.WriteRelationshipsRequest
import authzed.api.v1.PermissionsServiceGrpc
import com.github.phisgr.gatling.kt.grpc.grpc
import com.github.phisgr.gatling.kt.grpc.payload
import com.github.phisgr.gatling.kt.scenario
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import java.time.Duration
import java.util.Random
import java.util.UUID

class PingPongKt : Simulation() {

    private val grpcConfSpice = grpc(ManagedChannelBuilder.forAddress("SRV-DEV-SPICEDB", 50051).usePlaintext())
        .header(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer altima_grpc_key2")
        .shareChannel()

    private fun requestSpice(name: String) = grpc(name)

        .rpc(PermissionsServiceGrpc.getCheckPermissionMethod())

        .payload(CheckPermissionRequest::newBuilder) { session ->
            consistency = Consistency.newBuilder()
                .setMinimizeLatency(true)
                .build()
            // dynamic payload!
            val rand = Random().nextLong(0,9935280)
            val randgest = Random().nextInt(0,101)
            val randomType = listOf("document","unitetraitement").get(randgest%2)
            resource = ObjectReference.newBuilder()
                .setObjectType(randomType)
                .setObjectId(randomType+rand.toString())
                .build()
            permission = "read"
            subject = SubjectReference.newBuilder().setObject(
                ObjectReference.newBuilder()
                    .setObjectType("gestionnaire")
                    .setObjectId("gestionnaire$randgest")
                    .build()

            ).build()
            build()
        }
        .check({ extract { it.permissionship }.`is`(CheckpermissionService.Permissionship.PERMISSIONSHIP_HAS_PERMISSION)})


    private val createRelationshipRequest = { session:Session ->
        WriteRelationshipsRequest.newBuilder()
            .addUpdates(
                RelationshipUpdate.newBuilder()
                    .setOperation(RelationshipUpdate.Operation.OPERATION_TOUCH)
                    .setRelationship(
                        Relationship.newBuilder()
                            .setResource(
                                ObjectReference.newBuilder()
                                    .setObjectType(session.getString("ressourceType"))
                                    .setObjectId(session.getString("ressourceId"))
                            )
                            .setRelation(session.getString("relation"))
                            .setSubject(
                                SubjectReference.newBuilder()
                                    .setObject(
                                        ObjectReference.newBuilder()
                                            .setObjectType(session.getString("sujbectType"))
                                            .setObjectId(session.getString("subjectID"))
                                    )
                            )

                    )
            ).build()
    }

val filename = "relationutclaim.csv"
    private fun createRelationShip(name: String) = grpc(name)
        .rpc(PermissionsServiceGrpc.getWriteRelationshipsMethod())
        .payload(createRelationshipRequest)

    val scnSpice = scenario("SpiceDB") {

//        +feed(csv(filename).batch(20000))
//        +createRelationShip("create")
        +requestSpice("permission")
    }

    init {
//        val records = csv(filename).recordsCount()
        setUp(
            scnSpice.injectOpen(
//                rampUsers(records).during(Duration.ofMinutes(records / 60000L ))
//                atOnceUsers(1)
                incrementUsersPerSec(500.0)
                    .times(3)
                    .eachLevelLasting(10)
                    .separatedByRampsLasting(10)
                    .startingFrom(500.0)
                    // Double
            )

                .protocols(grpcConfSpice)
        )
    }
}
