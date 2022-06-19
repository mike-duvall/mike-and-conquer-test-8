package main

import client.MikeAndConquerSimulationClient
import domain.*
import domain.event.EventBlock
import domain.event.FindEventResult
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
    def "Assert #unitType travel time is #expectedTimeInMillis ms when gameSpeed is #gameSpeed"() {
        given:

        WorldCoordinatesLocation unitStartLocation = new WorldCoordinatesLocationBuilder()
                .worldMapTileCoordinatesX(10)
                .worldMapTileCoordinatesY(17)
                .build()

        WorldCoordinatesLocation unitDestinationLocation = new WorldCoordinatesLocationBuilder()
                .worldMapTileCoordinatesX(unitStartLocation.XInWorldMapTileCoordinates() + 14)
                .worldMapTileCoordinatesY(unitStartLocation.YInWorldMapTileCoordinates())
                .build()



        List<SimulationStateUpdateEvent> gameEventList = null
        List<EventBlock> expectedEventList = []

        def jsonSlurper = new JsonSlurper()
        long startingTick = -1
        long endingTick = -1

        SimulationOptions simulationOptions = new SimulationOptions()
        simulationOptions.gameSpeed = gameSpeed
        simulationClient.setGameOptions(simulationOptions)
        int allowedDelta = 250

        when:
        if(unitType == "Jeep") {
            simulationClient.addJeep(unitStartLocation)
        }
        else if (unitType == "MCV") {
            simulationClient.addMCV(unitStartLocation)
        }
        else {
            throw new Exception ("Unexpected unit type": + unitType)
        }

        then:
        assertNumberOfSimulationStateUpdateEvents(2)

        when:
        String expectedCreationEventType = unitType + "Created"
        gameEventList = simulationClient.getSimulationStateUpdateEvents();
        expectedEventList.add(new EventBlock("InitializeScenario", 1))
        expectedEventList.add(new EventBlock(expectedCreationEventType, 1))

        then:
        assertExpectedEventList(gameEventList, expectedEventList)

        when:
        SimulationStateUpdateEvent unitCreatedEvent = gameEventList.get(1)

        then:
        assert unitCreatedEvent.eventType == expectedCreationEventType

        when:
        def unitDataObject = jsonSlurper.parseText(unitCreatedEvent.eventData)
        Unit createdUnit = new Unit()
        createdUnit.unitId = unitDataObject.UnitId
        createdUnit.x = unitDataObject.X
        createdUnit.y = unitDataObject.Y
        int createdUnitId = createdUnit.unitId

        then:
        assert createdUnit.x == unitStartLocation.XInWorldCoordinates()
        assert createdUnit.y == unitStartLocation.YInWorldCoordinates()

        when:
        simulationClient.moveUnit(createdUnit.unitId, unitDestinationLocation)

        sleep (expectedTimeInMillis - 10000)

        then:
        assertNumberOfSimulationStateUpdateEvents(expectedTotalEvents)

        when:
        gameEventList = simulationClient.getSimulationStateUpdateEvents()
        expectedEventList.add(new EventBlock("UnitOrderedToMove",1 ))
        expectedEventList.add(new EventBlock("UnitPositionChanged", expectedTotalEvents - 4 ))
        expectedEventList.add(new EventBlock("UnitArrivedAtDestination",1 ))

        then:
        assertExpectedEventList(gameEventList, expectedEventList)

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)
        assert secondEventDataAsObject.DestinationXInWorldCoordinates == unitDestinationLocation.XInWorldCoordinates()
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == unitDestinationLocation.YInWorldCoordinates()

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
        unitType    | expectedTotalEvents   | expectedTimeInMillis  | gameSpeed
        "Jeep"      |  123                  |  30236                | "Slowest"
        "Jeep"      |  123                  |  15120                | "Slower"
        "Jeep"      |  123                  |  10082                | "Slow"
        "Jeep"      |  123                  |  7560                 | "Moderate"
        "Jeep"      |  123                  |  5040                 | "Normal"
        "Jeep"      |  123                  | 3697                  | "Fast"
        "Jeep"      |  123                  | 3024                  | "Faster"
        "Jeep"      |  123                  | 2855                  | "Fastest"
        "MCV"       |  302                  | 75597                 | "Slowest"
        "MCV"       |  302                  | 37801                 | "Slower"
        "MCV"       |  302                  | 25201                 | "Slow"
        "MCV"       |  302                  | 18900                 | "Moderate"
        "MCV"       |  302                  | 12601                 | "Normal"
        "MCV"       |  302                  | 9240                  | "Fast"
        "MCV"       |  302                  | 7560                  | "Faster"
        "MCV"       |  302                  | 7139                  | "Fastest"
    }



    def "Move a minigunner and assert correct path is followed"() {
        given:
        int minigunnerId = -1


        when:
        WorldCoordinatesLocation startLocation = new WorldCoordinatesLocationBuilder()
                .worldMapTileCoordinatesX(14)
                .worldMapTileCoordinatesY(13)
                .build()

        simulationClient.addMinigunner(startLocation)



        then:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient, 2)

        when:
        minigunnerId = TestUtil.assertMinigunnerCreatedEventReceived(simulationClient)

        then:
        assert minigunnerId != -1


        when:
        WorldCoordinatesLocation destinationLocation = new WorldCoordinatesLocationBuilder()
                .worldMapTileCoordinatesX(7)
                .worldMapTileCoordinatesY(15)
                .build()

        simulationClient.moveUnit(minigunnerId, destinationLocation )

        and:
        int expectedTotalEvents = 231

        and:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient,expectedTotalEvents)

        then:
        List<SimulationStateUpdateEvent> gameEventList = simulationClient.getSimulationStateUpdateEvents()
        SimulationStateUpdateEvent expectedUnitOrderedToMoveEvent = gameEventList.get(2)
        TestUtil.assertUnitOrderedToMoveEvent(
                expectedUnitOrderedToMoveEvent,
                minigunnerId,
                destinationLocation.XInWorldCoordinates(),
                destinationLocation.YInWorldCoordinates())

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

//        Pickup here
//        Do below
//         Then assert that the unit starts moving and passes through every map tile in the path, in order
        when:

        int currentIndex = 3
//        FindEventResult findEventResult = findNextEventAfter(currentIndex, gameEventList, "UnitArrivedAtPathStep")
//        currentIndex = findEventResult.index
//
//        SimulationStateUpdateEvent unitArrivedAtPathStepEvent = findEventResult.event
//        assert unitArrivedAtPathStepEvent.eventType == "UnitArrivedAtPathStep"
//        def unitArrivedAtPathStepEventData = jsonSlurper.parseText(unitArrivedAtPathStepEvent.eventData)
//        assert unitArrivedAtPathStepEventData.PathStep.X == 14 * 24 + 12
//        assert unitArrivedAtPathStepEventData.PathStep.Y == 13 * 24 + 12

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        14,
                        13
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        14,
                        14
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        14,
                        15
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        13,
                        16
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        12,
                        17
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        11,
                        17
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        10,
                        17
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        9,
                        17
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        8,
                        17
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        7,
                        16
                )

        currentIndex =
                assertReceivedUnitArrivedAtPathStepEvent(
                        currentIndex,
                        gameEventList,
                        7,
                        15
                )


//        findEventResult = findNextEventAfter(currentIndex, gameEventList, "UnitArrivedAtPathStep")
//        currentIndex = findEventResult.index
//
//        unitArrivedAtPathStepEvent = findEventResult.event
//        assert unitArrivedAtPathStepEvent.eventType == "UnitArrivedAtPathStep"
//        unitArrivedAtPathStepEventData = jsonSlurper.parseText(unitArrivedAtPathStepEvent.eventData)
//        assert unitArrivedAtPathStepEventData.PathStep.X == 14 * 24 + 12
//        assert unitArrivedAtPathStepEventData.PathStep.Y == 14 * 24 + 12



//        def jsonSlurper = new JsonSlurper()
//        def expectedUnitMovementPlanCreatedEventDataAsObject = jsonSlurper.parseText(expectedUnitMovementPlanCreatedEvent.eventData)


        then:
        SimulationStateUpdateEvent expectedUnitArrivedAtDestinationEvent = gameEventList.get(expectedTotalEvents - 1)
        TestUtil.assertUnitArrivedAtDestinationEvent(expectedUnitArrivedAtDestinationEvent, minigunnerId)

    }



    FindEventResult findNextEventAfter(int index,List<SimulationStateUpdateEvent> gameEventList, String eventType) {
        int totalNumEvents = gameEventList.size()
        SimulationStateUpdateEvent foundEvent = null
        boolean  done = false
        while(!done) {

            SimulationStateUpdateEvent nextEvent = gameEventList.get(index)
            if(nextEvent.eventType == eventType) {
                done = true
                foundEvent = nextEvent
            }

            if(index >= totalNumEvents - 1) {
                done = true
            }
            index++
        }
        FindEventResult result = new FindEventResult()
        result.event = foundEvent
        result.index = index
        return result

    }

    def assertReceivedUnitArrivedAtPathStepEvent(int index, gameEventList, int expectedXInMapTileSquareCoordinates, int expectedYInMapTileSquareCoordinates) {
        FindEventResult findEventResult = findNextEventAfter(index, gameEventList, "UnitArrivedAtPathStep")

        SimulationStateUpdateEvent unitArrivedAtPathStepEvent = findEventResult.event
        assert unitArrivedAtPathStepEvent.eventType == "UnitArrivedAtPathStep"
        def jsonSlurper = new JsonSlurper()
        def unitArrivedAtPathStepEventData = jsonSlurper.parseText(unitArrivedAtPathStepEvent.eventData)
        assert unitArrivedAtPathStepEventData.PathStep.X == expectedXInMapTileSquareCoordinates * 24 + 12
        assert unitArrivedAtPathStepEventData.PathStep.Y == expectedYInMapTileSquareCoordinates * 24 + 12

        return findEventResult.index

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