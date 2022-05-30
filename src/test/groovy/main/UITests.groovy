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

    def "Select and move a minigunner with mouse clicks"() {
        given:
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
        int expectedTotalEvents = 48

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




}
