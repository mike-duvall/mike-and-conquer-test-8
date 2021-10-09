package main

import client.MikeAndConquerGameClient
import domain.*
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
        boolean useTimeouts = true
        MikeAndConquerGameClient gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )
        int minigunnerXInWorldCoordinates = 60
        int minigunnerYInWorldCoordinates = 40


        when:
//        gameClient.addGDIMinigunnerAtMapSquare(2,3)
        gameClient.addGDIMinigunnerAtWorldCoordinates(minigunnerXInWorldCoordinates, minigunnerYInWorldCoordinates)

//        then:
//        true
        and:
        List<SimulationStateUpdateEvent> gameEventList = gameClient.getSimulationStateUpdateEvents()

        then:
        assert gameEventList.size() == 1

        and:
        SimulationStateUpdateEvent simulationStateUpdateEvent = gameEventList.get(0)
        assert simulationStateUpdateEvent.X == minigunnerXInWorldCoordinates
        assert simulationStateUpdateEvent.Y == minigunnerYInWorldCoordinates
        assert simulationStateUpdateEvent.ID == 1

    }



}