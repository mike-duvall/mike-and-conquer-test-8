package main

import client.MikeAndConquerSimulationClient
import domain.*
import domain.event.EventBlock
import domain.event.SimulationStateUpdateEvent
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions
import util.TestUtil
import util.Util




class MiscTests extends Specification {


    MikeAndConquerSimulationClient simulationClient

    void assertExpectedEventList(List<SimulationStateUpdateEvent> simulationStateUpdateEvents, ArrayList<EventBlock> expectedEventList) {
        boolean done = false
        int actualEventIndex = 0

        for(EventBlock eventBlock in expectedEventList) {
            for(int i = 0; i < eventBlock.numberOfEvents; i++) {
                SimulationStateUpdateEvent simulationStateUpdateEvent = simulationStateUpdateEvents.get(actualEventIndex)
                assert simulationStateUpdateEvent.eventType == eventBlock.eventType
                actualEventIndex++
            }
        }

        assert actualEventIndex == simulationStateUpdateEvents.size()


    }


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

//        gameClient.resetScenario()
        simulationClient.startScenario()
        sleep(1000)


    }




    @Unroll
    def "Assert Jeep travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
        given:

        Point startPointInMapSquareCoordinates = new Point(10,17)
        Point destinationPointInMapSquareCoordinates = new Point(
                startPointInMapSquareCoordinates.x + 14,
                startPointInMapSquareCoordinates.y)

        Point startPointInWorldCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(
                startPointInMapSquareCoordinates.x,
                startPointInMapSquareCoordinates.y)

        int startXInWorldCoordinates = startPointInWorldCoordinates.x
        int startYInWorldCoordinates = startPointInWorldCoordinates.y


        Point destinationPointInWorldCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(
                destinationPointInMapSquareCoordinates.x,
                destinationPointInMapSquareCoordinates.y)

        List<SimulationStateUpdateEvent> gameEventList = null
        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1
        int expectedTotalEvents = 123
        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        simulationClient.setGameOptions(simulationOptions)
        int allowedDelta = 250

        when:
//        simulationClient.addJeepAtWorldCoordinates(startXInWorldCoordinates, startYInWorldCoordinates)
        simulationClient.addJeepAtMapSquareCoordinates(
                startPointInMapSquareCoordinates.x,
                startPointInMapSquareCoordinates.y)
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
        createdUnit.unitId = unitDataObject.UnitId
        createdUnit.x = unitDataObject.X
        createdUnit.y = unitDataObject.Y
        int createdUnitId = createdUnit.unitId

        then:
        assert createdUnit.x == startXInWorldCoordinates
        assert createdUnit.y == startYInWorldCoordinates

        when:
//        int destinationXInWorldCoordinates = 360 - 12
//        int destinationYInWorldCoordinates = 12
        int destinationXInWorldCoordinates = destinationPointInWorldCoordinates.x
        int destinationYInWorldCoordinates = destinationPointInWorldCoordinates.y


//        simulationClient.moveUnitToWorldCoordinates(createdUnit.unitId, destinationXInWorldCoordinates, destinationYInWorldCoordinates )
        simulationClient.moveUnitToMapSquareCoordinates(createdUnit.unitId,
                destinationPointInMapSquareCoordinates.x,
                destinationPointInMapSquareCoordinates.y)

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
        assert secondEventDataAsObject.UnitId == createdUnitId

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
        assert thirdEventDataAsObject.UnitId == createdUnitId

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
//        30236                   | "Slowest"
//        15120                   | "Slower"
//        10082                   | "Slow"
//        7560                    | "Moderate"
//        5040                    | "Normal"
//        3697                    | "Fast"
//        3024                    | "Faster"
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
        List<EventBlock> expectedEventList = []

        and:
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
        expectedEventList.add(new EventBlock("InitializeScenario", 1))
        expectedEventList.add(new EventBlock("MCVCreated", 1))

        then:
        assertExpectedEventList(gameEventList, expectedEventList)

        SimulationStateUpdateEvent unitCreatedEvent = gameEventList.get(1)

        then:
        assert unitCreatedEvent.eventType == "MCVCreated"

        when:
        def UnitDataObject = jsonSlurper.parseText(unitCreatedEvent.eventData)
        Unit createdUnit = new Unit()
        createdUnit.unitId = UnitDataObject.UnitId
        createdUnit.x = UnitDataObject.X
        createdUnit.y = UnitDataObject.Y
        int createdUnitId = createdUnit.unitId

        then:
        assert createdUnit.x == startXInWorldCoordinates
        assert createdUnit.x == startYInWorldCoordinates

        when:
        int destinationXInWorldCoordinates = 360 - 12
        int destinationYInWorldCoordinates = 12

        simulationClient.moveUnitToWorldCoordinates(createdUnit.unitId, destinationXInWorldCoordinates, destinationYInWorldCoordinates )

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)


        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents()
        expectedEventList.add(new EventBlock("UnitOrderedToMove",1 ))
        expectedEventList.add(new EventBlock("UnitPositionChanged",298 ))
        expectedEventList.add(new EventBlock("UnitArrivedAtDestination",1 ))

        then:
        assertExpectedEventList(gameEventList, expectedEventList)

        and:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationYInWorldCoordinates
        assert secondEventDataAsObject.UnitId == createdUnitId

        when:
        startingTick = secondEventDataAsObject.Timestamp

        then:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
        assert thirdEventDataAsObject.UnitId == createdUnitId

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
//        75597                   | "Slowest"
//        37801                   | "Slower"
//        25201                   | "Slow"
//        18900                   | "Moderate"
//        12601                   | "Normal"
//        9240                    | "Fast"
//        7560                    | "Faster"
        7139                    | "Fastest"


    }


    def "Move a minigunner and assert correct path is followed"() {
        given:
        int minigunnerId = -1

        when:
        simulationClient.addGDIMinigunnerAtMapSquare(14,13)


        then:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient, 2)

        when:
        minigunnerId = TestUtil.assertMinigunnerCreatedEventReceived(simulationClient)

        then:
        assert minigunnerId != -1


        when:
//        uiClient.selectUnit(minigunnerId)
        Point destinationAsWorldCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(7,15)

        int destinationXInWorldCoordinates = destinationAsWorldCoordinates.x
        int destinationYInWorldCoordinates = destinationAsWorldCoordinates.y


        simulationClient.moveUnitToWorldCoordinates(minigunnerId, destinationXInWorldCoordinates, destinationYInWorldCoordinates)

        and:
        int expectedTotalEvents = 220

        and:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient,expectedTotalEvents)

        then:
        List<SimulationStateUpdateEvent> gameEventList = simulationClient.getSimulationStateUpdateEvents()
        SimulationStateUpdateEvent expectedUnitOrderedToMoveEvent = gameEventList.get(2)
        TestUtil.assertUnitOrderedToMoveEvent(expectedUnitOrderedToMoveEvent, minigunnerId, destinationXInWorldCoordinates, destinationYInWorldCoordinates)

        and:

        SimulationStateUpdateEvent expectedUnitMovementPlanCreatedEvent = gameEventList.get(3)
        assert expectedUnitMovementPlanCreatedEvent.eventType == "UnitMovementPlanCreated"

        def jsonSlurper = new JsonSlurper()
        def expectedUnitMovementPlanCreatedEventDataAsObject = jsonSlurper.parseText(expectedUnitMovementPlanCreatedEvent.eventData)

        assert expectedUnitMovementPlanCreatedEventDataAsObject.UnitId == minigunnerId
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps.size() == 11

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[0].X == 14
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[0].Y == 13

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[1].X == 14
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[1].Y == 14

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[2].X == 14
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[2].Y == 15

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[3].X == 13
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[3].Y == 16

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[4].X == 12
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[4].Y == 17

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[5].X == 11
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[5].Y == 17

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[6].X == 10
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[6].Y == 17

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[7].X == 9
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[7].Y == 17

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[8].X == 8
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[8].Y == 17

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[9].X == 7
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[9].Y == 16

        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[10].X == 7
        assert expectedUnitMovementPlanCreatedEventDataAsObject.PathSteps[10].Y == 15



//        assert expectedUnitMovementPlanCreatedEventDataAsObject.DestinationYInWorldCoordinates == destinationYInWorldCoordinates
//        assert expectedUnitMovementPlanCreatedEventDataAsObject.UnitId == minigunnerId

//         assert "UnitPlansMovementPath" event (or something like that)
//         assert that actual path, in the for of a list of maptiles, is the correct path
//
//         Then assert that the unit starts moving and passes through every map tile in the path, in order


        and:
        SimulationStateUpdateEvent expectedUnitArrivedAtDestinationEvent = gameEventList.get(expectedTotalEvents - 1)
        TestUtil.assertUnitArrivedAtDestinationEvent(expectedUnitArrivedAtDestinationEvent, minigunnerId)

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