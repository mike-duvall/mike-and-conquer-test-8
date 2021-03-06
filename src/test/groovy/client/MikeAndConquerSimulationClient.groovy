package client

import domain.*
import domain.event.SimulationStateUpdateEvent
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.params.CoreConnectionPNames

class MikeAndConquerSimulationClient {


    String hostUrl
    RESTClient  restClient

    private static final String GDI_MINIGUNNERS_BASE_URL = '/mac/gdiMinigunners'
    private static final String NOD_MINIGUNNERS_BASE_URL = '/mac/nodMinigunners'
    private static final String MCV_BASE_URL = '/mac/MCV'
    private static final String GDI_CONSTRUCTION_YARD = '/mac/GDIConstructionYard'
    private static final String SIDEBAR_BASE_URL = '/mac/Sidebar'
    private static final String NOD_TURRET_BASE_URL = '/mac/NodTurret'
    private static final String GAME_OPTIONS_URL = '/mac/gameOptions'
    private static final String GAME_HISTORY_EVENTS_URL = '/mac/gameHistoryEvents'


    MikeAndConquerSimulationClient(String host, int port, boolean useTimeouts = true) {
        hostUrl = "http://$host:$port"
        restClient = new RESTClient(hostUrl)

        if(useTimeouts) {
            restClient.client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, new Integer(5000))
            restClient.client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(5000))
        }
    }



    void setGameOptions(SimulationOptions simulationOptions) {
//        def resp = restClient.post(
//                path: GAME_OPTIONS_URL,
//                body: resetOptions,
//                requestContentType: 'application/json' )
//
//        assert resp.status == 204


        SetOptionsUserCommand command = new SetOptionsUserCommand()
        command.commandType = "SetOptions"
//        command.unitId = unitId

        def commandParams =
                [
                    gameSpeed: simulationOptions.gameSpeed
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        def resp = restClient.post(
                path: '/simulation/command',
                body: command,
                requestContentType: 'application/json')

        assert resp.status == 200


    }


    SimulationOptions getGameOptions() {

        def resp
        try {
            resp = restClient.get(path: GAME_OPTIONS_URL)
        }
        catch(HttpResponseException e) {
            if(e.statusCode == 404) {
                return null
            }
            else {
                throw e
            }
        }
        if( resp.status == 404) {
            return null
        }
        assert resp.status == 200  // HTTP response code; 404 means not found, etc.

        SimulationOptions resetOptions = new SimulationOptions()
        resetOptions.drawShroud = resp.responseData.drawShroud
        resetOptions.initialMapZoom = resp.responseData.initialMapZoom
        resetOptions.gameSpeed = resp.responseData.gameSpeed
        return resetOptions

    }




    void addMinigunner( WorldCoordinatesLocation location) {

        CreateMinigunnerCommand createUnitCommand = new CreateMinigunnerCommand()
        createUnitCommand.commandType = "CreateMinigunner"

        def commandParams =
            [
                startLocationXInWorldCoordinates: location.XInWorldCoordinates(),
                startLocationYInWorldCoordinates: location.YInWorldCoordinates()
            ]

        createUnitCommand.commandData =  JsonOutput.toJson(commandParams)

        def resp = restClient.post(
                path: '/simulation/command',
                body: createUnitCommand,
                requestContentType: 'application/json')

        assert resp.status == 200

    }

    void addJeep(WorldCoordinatesLocation location) {

        CreateJeepCommand command = new CreateJeepCommand()
        command.commandType = "CreateJeep"

        def commandParams =
                [
                        startLocationXInWorldCoordinates: location.XInWorldCoordinates(),
                        startLocationYInWorldCoordinates: location.YInWorldCoordinates()
                ]

        command.commandData =  JsonOutput.toJson(commandParams)

        try {
            def resp = restClient.post(
                    path: '/simulation/command',
                    body: command,
                    requestContentType: 'application/json')

            assert resp.status == 200
        }
        catch(HttpResponseException e) {
            ByteArrayInputStream byteArrayInputStream = e.response.responseData
            int n = byteArrayInputStream.available()
            byte[] bytes = new byte[n]
            byteArrayInputStream.read(bytes, 0, n)
            String s = new String(bytes )
            println("exception details:" + s)
            Map json = new JsonSlurper().parseText(s)
        }


    }

    void addMCV( WorldCoordinatesLocation location) {

        CreateMCVCommand command = new CreateMCVCommand()
        command.commandType = "CreateMCV"

        def commandParams =
                [
                        startLocationXInWorldCoordinates: location.XInWorldCoordinates(),
                        startLocationYInWorldCoordinates: location.YInWorldCoordinates()
                ]

        command.commandData =  JsonOutput.toJson(commandParams)

        try {
            def resp = restClient.post(
                    path: '/simulation/command',
                    body: command,
                    requestContentType: 'application/json')

            assert resp.status == 200
        }
        catch(HttpResponseException e) {
//            int x = 3
            ByteArrayInputStream byteArrayInputStream = e.response.responseData
            int n = byteArrayInputStream.available()
            byte[] bytes = new byte[n]
            byteArrayInputStream.read(bytes, 0, n)
            String s = new String(bytes )
            println("exception details:" + s)
            Map json = new JsonSlurper().parseText(s)
        }


    }


    void startScenario() {

        StartScenarioCommand command = new StartScenarioCommand()
        command.commandType = "StartScenario"

        try {
            def resp = restClient.post(
                    path: '/simulation/command',
                    body: command,
                    requestContentType: 'application/json')


            assert resp.status == 200
        }
        catch(HttpResponseException e) {
            int x = 3
            throw e
        }

        int y = 4

    }


    def List<SimulationStateUpdateEvent> getSimulationStateUpdateEvents() {
        def resp = restClient.get(
                path: '/simulation/query/events',
                requestContentType: 'application/json' )

        assert resp.status == 200

        int numItems = resp.responseData.size

        List<SimulationStateUpdateEvent> allSimulationStateUpdateEvents = []
        for (int i = 0; i < numItems; i++) {
            SimulationStateUpdateEvent simulationStateUpdateEvent = new SimulationStateUpdateEvent()
            simulationStateUpdateEvent.eventType = resp.responseData[i].eventType
            simulationStateUpdateEvent.eventData = resp.responseData[i].eventData
            allSimulationStateUpdateEvents.add(simulationStateUpdateEvent)
        }
        return allSimulationStateUpdateEvents

        //int x = 3

    }

    void moveUnit(int unitId, WorldCoordinatesLocation location) {

        MoveUnitCommand command = new MoveUnitCommand()
        command.commandType = "OrderUnitMove"

        def commandParams =
                [
                        unitId: unitId,
                        destinationLocationXInWorldCoordinates: location.XInWorldCoordinates(),
                        destinationLocationYInWorldCoordinates: location.YInWorldCoordinates()
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        def resp = restClient.post(
                path: '/simulation/command',
                body: command,
                requestContentType: 'application/json')

        assert resp.status == 200

    }


}
