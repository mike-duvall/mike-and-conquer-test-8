package client

import domain.*
import groovy.json.JsonOutput
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.params.CoreConnectionPNames
import util.Util

class MikeAndConquerGameClient {


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


    MikeAndConquerGameClient(String host, int port, boolean useTimeouts = true) {
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
                path: '/simulation/command/user',
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



//    Minigunner addMinigunnerAtWorldCoordinates(String baseUrl, int minigunnerX, int minigunnerY, boolean aiIsOn) {
//        Minigunner inputMinigunner = new Minigunner()
//        inputMinigunner.x = minigunnerX
//        inputMinigunner.y = minigunnerY
//        inputMinigunner.aiIsOn = aiIsOn
//        def resp = restClient.post(
//                path: baseUrl,
//                body:   inputMinigunner ,
//                requestContentType: 'application/json' )
//
//        assert resp.status == 200
//
//        Minigunner minigunner = new Minigunner()
//        minigunner.id = resp.responseData.id
//        minigunner.x = resp.responseData.x
//        minigunner.y = resp.responseData.y
//        minigunner.health = resp.responseData.health
//        return minigunner
//    }

    Minigunner addMinigunnerAtWorldCoordinates(String baseUrl, int minigunnerX, int minigunnerY, boolean aiIsOn) {
        Minigunner inputMinigunner = new Minigunner()
        inputMinigunner.x = minigunnerX
        inputMinigunner.y = minigunnerY

        CreateUnitCommand createUnitCommand = new CreateUnitCommand()
        createUnitCommand.commandType = "CreateMinigunner"

        def commandParams =
            [
                startLocationXInWorldCoordinates: minigunnerX,
                startLocationYInWorldCoordinates: minigunnerY
            ]

        createUnitCommand.commandData =  JsonOutput.toJson(commandParams)

        def resp = restClient.post(
                path: '/simulation/command/admin',
                body: createUnitCommand,
                requestContentType: 'application/json')


        assert resp.status == 201

        Minigunner minigunner = new Minigunner()
        minigunner.id = resp.responseData.id
        minigunner.x = resp.responseData.x
        minigunner.y = resp.responseData.y
        return minigunner

    }

    void resetScenario() {

        ResetScenarioCommand command = new ResetScenarioCommand()
        command.commandType = "ResetScenario"

//        def commandParams =
//                [
//                        startLocationXInWorldCoordinates: minigunnerX,
//                        startLocationYInWorldCoordinates: minigunnerY
//                ]
//
//        command.commandData =  JsonOutput.toJson(commandParams)

        def resp = restClient.post(
                path: '/simulation/command/admin',
                body: command,
                requestContentType: 'application/json')


        assert resp.status == 200

//        Minigunner minigunner = new Minigunner()
//        minigunner.id = resp.responseData.id
//        minigunner.x = resp.responseData.x
//        minigunner.y = resp.responseData.y
//        return minigunner

    }




    Minigunner addGDIMinigunnerAtWorldCoordinates(int minigunnerX, int minigunnerY) {
        boolean aiIsOn = false
        return addMinigunnerAtWorldCoordinates(GDI_MINIGUNNERS_BASE_URL, minigunnerX, minigunnerY, aiIsOn)
    }


    Minigunner addGDIMinigunnerAtMapSquare(int x, int y) {
        int halfMapSquareWidth = Util.mapSquareWidth / 2
        int worldX = (x * Util.mapSquareWidth) + halfMapSquareWidth
        int worldY = (y * Util.mapSquareWidth) + halfMapSquareWidth

        return addGDIMinigunnerAtWorldCoordinates(worldX, worldY)
    }

//    Minigunner addNodMinigunnerAtMapSquare(int x, int y, boolean aiIsOn) {
//        int halfMapSquareWidth = Util.mapSquareWidth / 2
//        int worldX = (x * Util.mapSquareWidth) + halfMapSquareWidth
//        int worldY = (y * Util.mapSquareWidth) + halfMapSquareWidth
//
//        return addNodMinigunnerAtWorldCoordinates(worldX, worldY, aiIsOn)
//    }


    def List<SimulationStateUpdateEvent> getSimulationStateUpdateEvents() {
        def resp = restClient.get(
                path: '/simulationStateUpdateEvents',
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

    void moveUnit(int unitId, int destinationXInWorldCoordinates, int destinationYInWorldCoordinate) {

        MoveUnitCommand command = new MoveUnitCommand()
        command.commandType = "OrderUnitMove"
//        command.unitId = unitId

        def commandParams =
                [
                        unitId: unitId,
                        destinationLocationXInWorldCoordinates: destinationXInWorldCoordinates,
                        destinationLocationYInWorldCoordinates: destinationYInWorldCoordinate
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        def resp = restClient.post(
                path: '/simulation/command/user',
                body: command,
                requestContentType: 'application/json')

        assert resp.status == 200

    }


}
