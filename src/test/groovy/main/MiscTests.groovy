package main

import client.MikeAndConquerGameClient
import domain.*
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions


class MiscTests extends Specification {


    MikeAndConquerGameClient gameClient


    enum GameSpeed
    {
        Slowest,
        Slower,
        Slow,
        Moderate,
        Normal,
        Fast,
        Faster,
        Fastest

    }


    protected void setAndAssertGameOptions(GameSpeed gameSpeed) {
        SimulationOptions gameOptions = new SimulationOptions( gameSpeed.name())
        gameClient.setGameOptions(gameOptions)
        assertGameOptionsAreSetTo(gameOptions)
    }

    def assertGameOptionsAreSetTo(SimulationOptions desiredGameOptions) {
        def conditions = new PollingConditions(timeout: 70, initialDelay: 1.5, factor: 1.25)
        conditions.eventually {
            SimulationOptions resetOptions1 = gameClient.getGameOptions()
            assert resetOptions1.gameSpeed == desiredGameOptions.gameSpeed
            assert resetOptions1.initialMapZoom == desiredGameOptions.initialMapZoom
            assert resetOptions1.drawShroud == desiredGameOptions.drawShroud
        }
        return true
    }




    def setup() {
        String localhost = "localhost"
        String remoteHost = "192.168.0.186"

//        String host = localhost
        String host = remoteHost

        int port = 5000
        boolean useTimeouts = true
//        boolean useTimeouts = false
        gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )

        gameClient.resetScenario()
        sleep(1000)


    }




    @Unroll
    def "Assert Jeep travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
        // This test presumes a minigunner is set to run at Jeep speed,
        // which is MPH_MEDIUM_FAST=30, which is 30 leptons per time loop

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 122
        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        gameClient.setGameOptions(simulationOptions)

        when:
        gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(1)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents();
        SimulationStateUpdateEvent minigunnerCreatedEvent = gameEventList.get(0)

        then:
        assert minigunnerCreatedEvent.eventType == "MinigunnerCreated"

        when:
        def minigunnerDataObject = jsonSlurper.parseText(minigunnerCreatedEvent.eventData)
        Minigunner createdMinigunner = new Minigunner()
        createdMinigunner.id = minigunnerDataObject.ID
        createdMinigunner.x = minigunnerDataObject.X
        createdMinigunner.y = minigunnerDataObject.Y

        then:
        assert createdMinigunner.id == 1
        assert createdMinigunner.x == minigunnerXInWorldCoordinates
        assert createdMinigunner.x == minigunnerYInWorldCoordinates

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(1)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
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

        where:
        expectedTimeInMillis   | gameSpeed
        2855                   | "Fastest"
        30236                  | "Slowest"
        5040                   | "Normal"
    }

    @Unroll
    def "Assert MCV travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
        // This test presumes a minigunner is set to run at Jeep speed,
        // which is MPH_MEDIUM_SLOW=12, which is 30 leptons per time loop

        given:

        int minigunnerXInWorldCoordinates = 12
        int minigunnerYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 301
        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        gameClient.setGameOptions(simulationOptions)

        when:
        gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(1)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents();
        SimulationStateUpdateEvent minigunnerCreatedEvent = gameEventList.get(0)

        then:
        assert minigunnerCreatedEvent.eventType == "MinigunnerCreated"

        when:
        def minigunnerDataObject = jsonSlurper.parseText(minigunnerCreatedEvent.eventData)
        Minigunner createdMinigunner = new Minigunner()
        createdMinigunner.id = minigunnerDataObject.ID
        createdMinigunner.x = minigunnerDataObject.X
        createdMinigunner.y = minigunnerDataObject.Y

        then:
        assert createdMinigunner.id == 1
        assert createdMinigunner.x == minigunnerXInWorldCoordinates
        assert createdMinigunner.x == minigunnerYInWorldCoordinates

        when:
        int destinationMinigunnerXInWorldCoordinates = 360 - 12
        int destinationMinigunnerYInWorldCoordinates = 12

        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(1)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
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

        where:
        expectedTimeInMillis   | gameSpeed
        7139                   | "Fastest"
        75597                  | "Slowest"
        12601                   | "Normal"
    }


//    @Unroll
//    def "Assert MCV travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
//        // This test presumes a minigunner is set to run at Jeep speed,
//        // which is MPH_MEDIUM_FAST=30, which is 30 leptons per time loop
//
//        given:
//
//        int minigunnerXInWorldCoordinates = 12
//        int minigunnerYInWorldCoordinates = 12
//        List<SimulationStateUpdateEvent>  gameEventList = null
//        def jsonSlurper = new JsonSlurper()
//        long startingTick = -1
//        long endingTick = -1
//        int expectedTotalEvents = 301
////        int expectedTimeInMillis = 2855
//        SimulationOptions simulationOptions = new SimulationOptions()
//        simulationOptions.gameSpeed = gameSpeed
//        gameClient.setGameOptions(simulationOptions)
//
//
//
//        when:
////        gameClient.addGDIMinigunnerAtMapSquare(2,3)
//        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)
//
//        then:
//        assertNumberOfSimulationStateUpdateEvents(1)
//
//        when:
//
//        when:
//        int destinationMinigunnerXInWorldCoordinates = 360 - 12
//        int destinationMinigunnerYInWorldCoordinates = 12
//
//        gameClient.moveUnit(createdMinigunner.id, destinationMinigunnerXInWorldCoordinates, destinationMinigunnerYInWorldCoordinates )
//
//        sleep (expectedTimeInMillis - 10000)
//
//        then:
//        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)
//
//        when:
//        gameEventList = gameClient.getSimulationStateUpdateEvents()
//
//
//        then:
//        SimulationStateUpdateEvent secondEvent = gameEventList.get(1)
//        assert secondEvent.eventType == "UnitOrderedToMove"
//
//        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)
//
//        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
//        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
//        assert secondEventDataAsObject.ID == 1
//
//
//        when:
//        startingTick = secondEventDataAsObject.Timestamp
//
//        then:
////        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
//        assert thirdEvent.eventType == "UnitArrivedAtDestination"
//
//        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
//
////        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
////        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
//        assert thirdEventDataAsObject.ID == 1
//
//        when:
//        endingTick = thirdEventDataAsObject.Timestamp
//        int startingMilliseconds = startingTick / 10000
//        int endingMilliseconds = endingTick / 10000
//        int totalTime = endingMilliseconds - startingMilliseconds
//        println("totalTime was:" + totalTime)
//
//
//        then:
//        assert totalTime < expectedTimeInMillis + 200
//        assert totalTime > expectedTimeInMillis - 200
//
//
//        where:
//        expectedTimeInMillis   | gameSpeed
//        7139                   | "Fastest"
//        75597                  | "Slowest"
//        12601                   | "Normal"
//    }









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