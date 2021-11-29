package main

import client.MikeAndConquerGameClient
import domain.*
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions
//import util.Util

class MiscTests extends Specification {


    def "Add minigunner and move across screen"() {
        given:
        String localhost = "localhost"
        String remoteHost = "192.168.0.147"
        String host = localhost
//        String host = remoteHost

        int port = 5000
//        boolean useTimeouts = true
        boolean useTimeouts = false
        MikeAndConquerGameClient gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )
        int minigunnerXInWorldCoordinates = 60
        int minigunnerYInWorldCoordinates = 40


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        Minigunner createdMinigunner = gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

//        then:
//        true
        and:
        List<SimulationStateUpdateEvent> gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        assert gameEventList.size() == 1

        and:
        SimulationStateUpdateEvent firstEvent = gameEventList.get(0)
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


        and:
        sleep(5000)

        and:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        assert gameEventList.size() == 2

        SimulationStateUpdateEvent secondEvent = gameEventList.get(1)
        assert secondEvent.eventType == "UnitOrderedToMove"

        def secondEventDataAsObject = jsonSlurper.parseText(secondEvent.eventData)

        assert secondEventDataAsObject.DestinationXInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert secondEventDataAsObject.DestinationYInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert secondEventDataAsObject.ID == 1


        when:
        sleep(50000)

        and:
        gameEventList = gameClient.getSimulationStateUpdateEvents()


        then:
        assert gameEventList.size() == 3

        SimulationStateUpdateEvent thirdEvent = gameEventList.get(2)
        assert thirdEvent.eventType == "UnitArrivedAtDestination"

        def thirdEventDataAsObject = jsonSlurper.parseText(thirdEvent.eventData)

        assert thirdEventDataAsObject.XInWorldCoordinates == destinationMinigunnerXInWorldCoordinates
        assert thirdEventDataAsObject.YInWorldCoordinates == destinationMinigunnerYInWorldCoordinates
        assert thirdEventDataAsObject.ID == 1


//        Add validation here that minigunner arrived at destination
//        By checking events
//        but in polling loop to wait for proper events

    }



}