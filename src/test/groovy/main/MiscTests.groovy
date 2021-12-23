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