# IOHK Scala test - A Proof of Product

This scala project implements a protocol where three parties are involved: Alice and Bob, who are `Users` and Carrol who is the `Broker`.

* The protocol starts when Carrol receives an `InitProtocol` message, upon which she asks the users to supply their numbers `A` and `B` encrypted using the Damgard-Jurik cryptosystem.
 Should the users not reply within `get-numbers-max-wait-seconds` Carrol ends the protocol by returning an 
`AbortProtocol` message.
* Alice and Bob encrypt their numbers into `c_A` and `c_B` using Carrol's `public key`.
* Carrol decrypts the numbers with her `private key` and computes `C = A * B` 
* Carrol encrypts `C` into `c_C` with her `public key` and shares the result of computation as `<c_A, c_B, c_C>` with Alice and Bob
* Alice and Bob then ask Carrol to start the Damgard-Jurik proof of product protocol to verify that `<c_A, c_B, c_C>` are so that `C = A * B`.
* The 3-move protocol is repeated a total of `verification-attempts` times
* Subsequently both users send Carrol a `Done` message
* After receiving the `Done` message from both users Carrol sends a `Done` message back to the protocol initiator


## Build, test and run
The project can be build and tested by running
 
        sbt test it:test
        
The application can be launched by running

        sbt run

the app will exit after the protocol finishes.
Some parameters are configured in `application.conf` and these can also be tuned when running the app.

        sbt -Dverification-attempts=100 run
