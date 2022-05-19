package main


import client.MikeAndConquerSimulationClient
import client.MikeAndConquerUIClient
import domain.Unit
import groovy.json.JsonSlurper
import domain.SimulationStateUpdateEvent
import org.junit.Test
import spock.lang.Specification
import util.TestUtil
import util.Util

import java.awt.Point


class UITests extends Specification {

    MikeAndConquerSimulationClient simulationClient
    MikeAndConquerUIClient uiClient

    def setup() {
        String localhost = "localhost"
        String remoteHost = "192.168.0.186"

//        String host = localhost
        String host = remoteHost

        int port = 5010
        boolean useTimeouts = true
//        boolean useTimeouts = false
        uiClient = new MikeAndConquerUIClient(host, port, useTimeouts )

        simulationClient = new MikeAndConquerSimulationClient(host, 5000, useTimeouts)
//        simulationClient.resetScenario()
//        sleep(1000)


    }

    def "Move a minigunner with mouse clicks"() {
        given:
        def jsonSlurper = new JsonSlurper()
        uiClient.startScenario()
        int minigunnerId = -1

        when:
        simulationClient.addGDIMinigunnerAtMapSquare(18,14)

        then:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient, 2)


        when:
        minigunnerId = TestUtil.assertMinigunnerCreatedEventReceived(simulationClient)

        then:
        assert minigunnerId != -1

        when:
        uiClient.selectUnit(minigunnerId)

        then:
        TestUtil.assertUnitIsSelected(uiClient, minigunnerId)

        when:
        uiClient.leftClickInMapSquareCoordinates(18,12)
        Point destinationAsWorldCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(18,12)
        int destinationXInWorldCoordinates = destinationAsWorldCoordinates.x
        int destinationYInWorldCoordinates = destinationAsWorldCoordinates.y


        and:
        int expectedTotalEvents = 46

        and:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient,expectedTotalEvents)

        then:
        List<SimulationStateUpdateEvent> gameEventList = simulationClient.getSimulationStateUpdateEvents()
        SimulationStateUpdateEvent expectedUnitOrderedToMoveEvent = gameEventList.get(2)
        TestUtil.assertUnitOrderedToMoveEvent(expectedUnitOrderedToMoveEvent, minigunnerId, destinationXInWorldCoordinates, destinationYInWorldCoordinates)


        and:
        SimulationStateUpdateEvent expectedUnitArrivedAtDestinationEvent = gameEventList.get(expectedTotalEvents - 1)
        TestUtil.assertUnitArrivedAtDestinationEvent(expectedUnitArrivedAtDestinationEvent, minigunnerId)



    }

    def "Move a minigunner and validate path"() {
        given:
        def jsonSlurper = new JsonSlurper()
        uiClient.startScenario()
        int minigunnerId = -1

        when:
        simulationClient.addGDIMinigunnerAtMapSquare(18,14)

        then:
        TestUtil.assertNumberOfSimulationStateUpdateEvents(simulationClient, 2)


        when:
        List<SimulationStateUpdateEvent> events = simulationClient.getSimulationStateUpdateEvents()
        for(event in events) {
            if (event.eventType == "MinigunnerCreated") {
                def eventData = jsonSlurper.parseText(event.eventData)

                minigunnerId = eventData.UnitId
            }
        }

        then:
        assert minigunnerId != -1

        when:
        sleep(1000)
        uiClient.selectUnit(minigunnerId)

        and:
        sleep(4000)

        then:
        Unit retrievedUnit = uiClient.getUnit(minigunnerId)
        assert retrievedUnit.selected == true


        when:
        uiClient.leftClickInMapSquareCoordinates(18,12)
        Point destinationAsWorldCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(18,12)
        int destinationXInWorldCoordinates = destinationAsWorldCoordinates.x
        int destinationYInWorldCoordinates = destinationAsWorldCoordinates.y


        and:
        sleep(5000)
        int expectedTotalEvents = 47

        and:
        List<SimulationStateUpdateEvent> gameEventList = simulationClient.getSimulationStateUpdateEvents()

        then:
        SimulationStateUpdateEvent secondEvent = gameEventList.get(2)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationYInWorldCoordinates
        assert secondEventDataAsObject.UnitId == minigunnerId


//        and:
//        SimulationStateUpdateEvent thirdEvent = gameEventList.get(3)
//        Pickup here
        // assert "UnitPlansMovementPath" event (or something like that)
        // assert that actual path, in the for of a list of maptiles, is the correct path

        // Then assert that the unit starts moving and passes through every map tile in the path, in order


        and:
        SimulationStateUpdateEvent thirdEvent = gameEventList.get(expectedTotalEvents - 1)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"
        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)
        assert thirdEventDataAsObject.UnitId == minigunnerId


//        expect:
//        true
//
//        where:
//        i << (1..10)
//        // and:
//        // assert minigunner is location, in map square coordaintes


    }



}
