package main

import client.MikeAndConquerGameClient
import domain.*
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions


class MiscTests extends Specification {


    MikeAndConquerGameClient gameClient


    def setup() {
        String localhost = "localhost"
        String remoteHost = "192.168.0.186"

//        String host = localhost
        String host = remoteHost

        int port = 5000
        boolean useTimeouts = true
//        boolean useTimeouts = false
        gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )


    }




    def "Add minigunner and move across screen"() {
        given:

        int minigunnerXInWorldCoordinates = 60
        int minigunnerYInWorldCoordinates = 40
        List<SimulationStateUpdateEvent>  gameEventList = null


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)


        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent firstEvent = gameEventList.get(1)
        assert firstEvent.eventType == "MinigunnerCreated"

        def jsonSlurper = new JsonSlurper()
        def eventDataAsObject = jsonSlurper.parseText(firstEvent.eventData)

        assert eventDataAsObject.X == minigunnerXInWorldCoordinates
        assert eventDataAsObject.Y == minigunnerYInWorldCoordinates
        assert eventDataAsObject.ID == 1

        when:
        int destinationMinigunnerXInWorldCoordinates = 100
        int destinationMinigunnerYInWorldCoordinates = 110

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        then:
//        assertNumberOfSimulationStateUpdateEvents(2)
        assertNumberOfSimulationStateUpdateEvents(470)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1

        then:
//        assertNumberOfSimulationStateUpdateEvents(3)
        assertNumberOfSimulationStateUpdateEvents(470)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(469)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1


    }

    def "Assert mcv speed is correct for Fastest"() {
        // This test presumes a minigunner is set to run at MCV speed,
        // which is MPH_MEDIUM_SLOW=12, which is 12 leptons per time loop
        // and is set to verify move when game is running at Fastest
        // which is currently being realized with: int sleepTime = 23; // 7025

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        int startingTick = -1
        int endingTick = -1
        int expectedTotalEvents = 303
        int expectedTimeInMillis = 7139


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

//        sleep (30000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200
    }


    def "Assert mcv speed is correct for Slowest"() {
        // This test presumes a minigunner is set to run at MCV speed,
        // which is MPH_MEDIUM_SLOW=12, which is 12 leptons per time loop
        // and is set to verify move when game is running at Slowest
        // which is currently being realized with: int sleepTime = 252; // 75700

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 303
        long expectedTimeInMillis = 75597


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (expectedTimeInMillis - 20000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        long startingMilliseconds = startingTick / 10000
        long endingMilliseconds = endingTick / 10000
        long totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200


    }


    def "Assert mcv speed is correct for Normal"() {
        // This test presumes a minigunner is set to run at MCV speed,
        // which is MPH_MEDIUM_SLOW=12, which is 12 leptons per time loop
        // and is set to verify move when game is running at Normal
        // which is currently being realized with: int sleepTime = 42; //12733

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 303
        long expectedTimeInMillis = 12601


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (expectedTimeInMillis - 20000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        long startingMilliseconds = startingTick / 10000
        long endingMilliseconds = endingTick / 10000
        long totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200
    }

    def "Assert Jeep speed is correct for Fastest"() {
        // This test presumes a minigunner is set to run at MCV speed,
        // which is MPH_MEDIUM_FAST=30, which is 30 leptons per time loop
        // and is set to verify move when game is running at Fastest
        // which is currently being realized with: int sleepTime = 23; // 7025

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        int startingTick = -1
        int endingTick = -1
        int expectedTotalEvents = 123
        int expectedTimeInMillis = 2855


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

//        sleep (30000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

//        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
//        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200
    }


    def "Assert Jeep speed is correct for Slowest"() {
        // This test presumes a minigunner is set to run at MCV speed,
        // which is MPH_MEDIUM_FAST=30, which is 30 leptons per time loop
        // and is set to verify move when game is running at Fastest
        // which is currently being realized with: int sleepTime = 23; // 7025

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        int startingTick = -1
        int endingTick = -1
        int expectedTotalEvents = 123
        int expectedTimeInMillis = 30236


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (5000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

//        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
//        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200
    }


    def "Assert Jeep speed is correct for Normal"() {
        // This test presumes a minigunner is set to run at Jeep speed,
        // which is MPH_MEDIUM_FAST=30, which is 30 leptons per time loop
        // and is set to verify move when game is running at Fastest
        // which is currently being realized with: int sleepTime = 23; // 7025

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        int startingTick = -1
        int endingTick = -1
        int expectedTotalEvents = 123
        int expectedTimeInMillis = 5040


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (5000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

//        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
//        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)


        then:
        assert totalTime < expectedTimeInMillis + 200
        assert totalTime > expectedTimeInMillis - 200
    }




    def assertNumberOfSimulationStateUpdateEvents(int numEventsToAssert) {
        int timeoutInSeconds = 30
        List<SimulationStateUpdateEvent>  gameEventList
        def conditions = new PollingConditions(timeout: timeoutInSeconds, initialDelay: 1.5, factor: 1.25)
        conditions.eventually {
            gameEventList = gameClient.getSimulationStateUpdateEvents()
            assert gameEventList.size() == numEventsToAssert
        }
        return true

    }


}