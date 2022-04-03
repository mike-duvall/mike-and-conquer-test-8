package main

import client.MikeAndConquerSimulationClient
import domain.*
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions


class MiscTests extends Specification {


    MikeAndConquerSimulationClient simulationClient


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
        simulationClient.setGameOptions(gameOptions)
        assertGameOptionsAreSetTo(gameOptions)
    }

    def assertGameOptionsAreSetTo(SimulationOptions desiredGameOptions) {
        def conditions = new PollingConditions(timeout: 70, initialDelay: 1.5, factor: 1.25)
        conditions.eventually {
            SimulationOptions resetOptions1 = simulationClient.getGameOptions()
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
        simulationClient = new MikeAndConquerSimulationClient(host, port, useTimeouts )

//        simulationClient.resetScenario()
        simulationClient.startScenario()
        sleep(1000)


    }




    @Unroll
    def "Assert Jeep travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
        given:

        int startXInWorldCoordinates = 12
        int startYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 123
        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        simulationClient.setGameOptions(simulationOptions)
        int allowedDelta = 250

        when:
        simulationClient.addJeepAtWorldCoordinates(startXInWorldCoordinates, startYInWorldCoordinates)

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents();
        SimulationStateUpdateEvent unitCreatedEvent = gameEventList.get(1)

        then:
        assert unitCreatedEvent.eventType == "JeepCreated"

        when:
        def unitDataObject = jsonSlurper.parseText(unitCreatedEvent.eventData)
        Unit createdUnit = new Unit()
        createdUnit.id = unitDataObject.ID
        createdUnit.x = unitDataObject.X
        createdUnit.y = unitDataObject.Y
        int createdUnitId = createdUnit.id

        then:
//        assert createdUnit.id == 1
        assert createdUnit.x == startXInWorldCoordinates
        assert createdUnit.x == startYInWorldCoordinates

        when:
        int destinationXInWorldCoordinates = 360 - 12
        int destinationYInWorldCoordinates = 12

        simulationClient.moveUnit(createdUnit.id, destinationXInWorldCoordinates, destinationYInWorldCoordinates )

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationYInWorldCoordinates
        assert secondEventDataAsObject.ID == createdUnitId

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
        assert thirdEventDataAsObject.ID == createdUnitId

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)

        then:
        assert totalTime < expectedTimeInMillis + allowedDelta
        assert totalTime > expectedTimeInMillis - allowedDelta

        where:
        expectedTimeInMillis   | gameSpeed
        30236                   | "Slowest"
        15120                   | "Slower"
        10082                   | "Slow"
        7560                   | "Moderate"
        5040                   | "Normal"
        3697                    | "Fast"
        3024                    | "Faster"
        2855                    | "Fastest"
    }

    @Unroll
    def "Assert MCV travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {

        given:
        int startXInWorldCoordinates = 12
        int startYInWorldCoordinates = 12
        List<SimulationStateUpdateEvent>  gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 302
        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        simulationClient.setGameOptions(simulationOptions)
        int allowedDelta = 250

        when:
        simulationClient.addMCVAtWorldCoordinates(startXInWorldCoordinates, startYInWorldCoordinates)


        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents();
        SimulationStateUpdateEvent unitCreatedEvent = gameEventList.get(1)

        then:
        assert unitCreatedEvent.eventType == "MCVCreated"

        when:
        def UnitDataObject = jsonSlurper.parseText(unitCreatedEvent.eventData)
        Unit createdUnit = new Unit()
        createdUnit.id = UnitDataObject.ID
        createdUnit.x = UnitDataObject.X
        createdUnit.y = UnitDataObject.Y
        int createdUnitId = createdUnit.id

        then:
//        assert createdUnit.id == 1
        assert createdUnit.x == startXInWorldCoordinates
        assert createdUnit.x == startYInWorldCoordinates

        when:
        int destinationXInWorldCoordinates = 360 - 12
        int destinationYInWorldCoordinates = 12

        simulationClient.moveUnit(createdUnit.id, destinationXInWorldCoordinates, destinationYInWorldCoordinates )

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationYInWorldCoordinates
        assert secondEventDataAsObject.ID == createdUnitId

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
        assert thirdEventDataAsObject.ID == createdUnitId

        when:
        endingTick = thirdEventDataAsObject.Timestamp
        int startingMilliseconds = startingTick / 10000
        int endingMilliseconds = endingTick / 10000
        int totalTime = endingMilliseconds - startingMilliseconds
        println("totalTime was:" + totalTime)

        then:
        assert totalTime < expectedTimeInMillis + allowedDelta
        assert totalTime > expectedTimeInMillis - allowedDelta

        where:
        expectedTimeInMillis    | gameSpeed
        75597                   | "Slowest"
        37801                   | "Slower"
        25201                   | "Slow"
        18900                   | "Moderate"
        12601                   | "Normal"
        9240                    | "Fast"
        7560                    | "Faster"
        7139                    | "Fastest"
    }



    def assertNumberOfSimulationStateUpdateEvents(int numEventsToAssert) {
        int timeoutInSeconds = 30
        List<SimulationStateUpdateEvent>  gameEventList
        def conditions = new PollingConditions(timeout: timeoutInSeconds, initialDelay: 1.5, factor: 1.25)
        conditions.eventually {
            gameEventList = simulationClient.getSimulationStateUpdateEvents()
            assert gameEventList.size() == numEventsToAssert
        }
        return true

    }


}