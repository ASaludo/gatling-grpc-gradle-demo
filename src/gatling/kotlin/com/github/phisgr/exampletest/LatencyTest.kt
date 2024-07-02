package com.github.phisgr.exampletest


import com.authzed.api.v1.*
import com.github.phisgr.gatling.kt.grpc.grpc
import com.github.phisgr.gatling.kt.grpc.payload
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.Simulation
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import java.util.*

class LatencyTest : Simulation() {

    private val grpcConfSpice = grpc(ManagedChannelBuilder.forAddress("SRV-DEV-SPICEDB", 50051).usePlaintext())
        .header(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer altima_grpc_key2")
        .shareChannel()

    private fun requestCheckPermission(name: String) = grpc(name)

        .rpc(PermissionsServiceGrpc.getCheckPermissionMethod())

        .payload(CheckPermissionRequest::newBuilder) { session ->
            consistency = Consistency.newBuilder()
                .setMinimizeLatency(true)
                .build()
            // dynamic payload!
            resource = ObjectReference.newBuilder()
                .setObjectId(session.getString("person")).setObjectType("person")
                .build()
            permission = "associated_with"
            subject = SubjectReference.newBuilder().setObject(
                ObjectReference.newBuilder().setObjectType("partner")
                    .setObjectId(session.getString("partner"))
                    .build()

            ).build()
            build()
        }
//        .check({ extract { it.permissionship }.`is`(Permissionship.PERMISSIONSHIP_HAS_PERMISSION) })

    private fun createRelationShip(name: String) = grpc(name)
        .rpc(PermissionsServiceGrpc.getWriteRelationshipsMethod())
        .payload(WriteRelationshipsRequest::newBuilder) {
            val person = UUID.randomUUID().toString()
            it.set("person", person)
            val partner = UUID.randomUUID().toString()
            it.set("partner", partner)
            updatesList.add(
                RelationshipUpdate.newBuilder()
                    .setOperation(RelationshipUpdate.Operation.OPERATION_CREATE)
                    .setRelationship(
                        Relationship.newBuilder()
                            .setResource(
                                ObjectReference.newBuilder()
                                    .setObjectType("person")
                                    .setObjectId(person)
                                    .build()
                            )
                            .setRelation("associated_with")
                            .setSubject(
                                SubjectReference.newBuilder()
                                    .setObject(
                                        ObjectReference.newBuilder()
                                            .setObjectType("partner")
                                            .setObjectId(partner)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            build()
        }

    val scnSpice = scenario("SpiceDB").exec(createRelationShip("test"))
//    {
////        +group("Injection").on {
//        +createRelationShip("create")
////            +requestCheckPermission("permission")
////        +doWhile{ it.isFailed }.on {  }
////        }
//    }

    init {
        setUp(
            scnSpice.injectOpen(
                atOnceUsers(10)
            )
                .protocols(grpcConfSpice)
        )
    }
}
