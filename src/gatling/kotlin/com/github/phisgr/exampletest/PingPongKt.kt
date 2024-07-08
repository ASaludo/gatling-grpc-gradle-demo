package com.github.phisgr.exampletest

import authzed.api.v1.*
import authzed.api.v1.CheckpermissionService.CheckPermissionRequest
import authzed.api.v1.CheckpermissionService.ObjectReference
import authzed.api.v1.CheckpermissionService.SubjectReference
import authzed.api.v1.CheckpermissionService.RelationshipUpdate
import authzed.api.v1.CheckpermissionService.Relationship

import com.github.phisgr.gatling.kt.grpc.grpc
import com.github.phisgr.gatling.kt.grpc.payload
import com.github.phisgr.gatling.kt.scenario
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import java.util.*

class PingPongKt : Simulation() {

    private val grpcConfSpice = grpc(ManagedChannelBuilder.forAddress("SRV-DEV-SPICEDB", 50051).usePlaintext())
        .header(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer altima_grpc_key2")

    private fun requestSpice(name: String) = grpc(name)

        .rpc(PermissionsServiceGrpc.getCheckPermissionMethod())

        .payload(CheckPermissionRequest::newBuilder) { session ->
            consistency = CheckPermissionRequest.Consistency.newBuilder()
                .setFullyConsistent(true)
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

    private val createRelationshipRequest = { session: Session ->
        val person = session.getString("person" + session.userId())
        val partner = session.getString("partner" + session.userId())
        writeRelationshipsRequest {
            updates.add(
                relationshipUpdate {
                    operation = RelationshipUpdate.Operation.OPERATION_CREATE
                    relationship = relationship {
                        resource = objectReference {
                            objectType = "person"
                            objectId = person!!
                        }
                        relation = "associated_with"
                        subject = subjectReference {
                            object_ = objectReference {
                                objectType = "partner"
                                objectId = partner!!
                            }
                        }
                    }
                }
            )
        }
    }


    private fun createRelationShip(name: String) = grpc(name)
        .rpc(PermissionsServiceGrpc.getWriteRelationshipsMethod())
        .payload(createRelationshipRequest)

    private val setSession = exec { session: Session ->
        val person = UUID.randomUUID().toString()
        val partner = UUID.randomUUID().toString()
        session.setAll(mapOf("person" + session.userId() to person, "partner" + session.userId() to partner))
    }

    val scnSpice = scenario("SpiceDB") {
       // +exitHereIfFailed()
        +feed(csv("permission.csv"))
        +requestSpice("permission")

//        +setSession
//        +createRelationShip("create")
    }

    init {
        setUp(
            scnSpice.injectOpen(
//                atOnceUsers(10)
                incrementUsersPerSec(500.0)
                    .times(5)
                    .eachLevelLasting(30)
                    .separatedByRampsLasting(10)
                    .startingFrom(500.0)

                    // Double
            )

                .protocols(grpcConfSpice)
        )
    }
}
