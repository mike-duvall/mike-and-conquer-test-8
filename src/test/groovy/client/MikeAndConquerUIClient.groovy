package client

import domain.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.params.CoreConnectionPNames
import util.Util

import java.awt.Point

class MikeAndConquerUIClient {


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


    MikeAndConquerUIClient(String host, int port, boolean useTimeouts = true) {
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




    void addMinigunnerAtWorldCoordinates(String baseUrl, int minigunnerX, int minigunnerY, boolean aiIsOn) {
        Unit inputMinigunner = new Unit()
        inputMinigunner.x = minigunnerX
        inputMinigunner.y = minigunnerY

        CreateMinigunnerCommand createUnitCommand = new CreateMinigunnerCommand()
        createUnitCommand.commandType = "CreateMinigunner"

        def commandParams =
            [
                startLocationXInWorldCoordinates: minigunnerX,
                startLocationYInWorldCoordinates: minigunnerY
            ]

        createUnitCommand.commandData =  JsonOutput.toJson(commandParams)

        def resp = restClient.post(
                path: '/simulation/command',
                body: createUnitCommand,
                requestContentType: 'application/json')

        assert resp.status == 200

    }

    void addJeepAtWorldCoordinates( int x, int y) {

        CreateJeepCommand command = new CreateJeepCommand()
        command.commandType = "CreateJeep"

        def commandParams =
                [
                        startLocationXInWorldCoordinates: x,
                        startLocationYInWorldCoordinates: y
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

    void addMCVAtWorldCoordinates( int minigunnerX, int minigunnerY) {
        Unit inputMinigunner = new Unit()
        inputMinigunner.x = minigunnerX
        inputMinigunner.y = minigunnerY

        CreateMCVCommand command = new CreateMCVCommand()
        command.commandType = "CreateMCV"

        def commandParams =
                [
                        startLocationXInWorldCoordinates: minigunnerX,
                        startLocationYInWorldCoordinates: minigunnerY
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
                    path: '/ui/command',
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


    void selectUnit(int unitId) {
        SelectUnitCommand command = new SelectUnitCommand()
        command.commandType = "SelectUnit"

        def commandParams =
                [
                        UnitId: unitId
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        try {
            def resp = restClient.post(
                    path: '/ui/command',
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


    void leftClickInMapSquareCoordinates(int xInMapSquareCoordinates, int yInMapSquareCoordinates) {

        Point pointInWordCoordinates = Util.convertMapSquareCoordinatesToWorldCoordinates(xInMapSquareCoordinates, yInMapSquareCoordinates)
        int xInWorldCoordinates = pointInWordCoordinates.x
        int yInWorldCoordinates = pointInWordCoordinates.y

        // Todo, decided if commands have commandType hard coded, or if we just need Command instead of specific subclasses
        SelectUnitCommand command = new SelectUnitCommand()
        command.commandType = "LeftClick"

        def commandParams =
                [
                    XInWorldCoordinates: xInWorldCoordinates,
                    YInWorldCoordinates: yInWorldCoordinates
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        try {
            def resp = restClient.post(
                    path: '/ui/command',
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


    Unit getUnit(int unitId) {

        def resp
        try {
            resp = restClient.get(
                    path: '/ui/query/unit',
                    query:['unitId': unitId],
                    requestContentType: 'application/json')


            assert resp.status == 200
        }
        catch(HttpResponseException e) {
            int x = 3
            throw e
        }

        int y = 4

        Unit unit = new Unit()
        unit.id = resp.responseData.unitId
        unit.selected = resp.responseData.selected

        return unit
    }




    Unit addGDIMinigunnerAtWorldCoordinates(int minigunnerX, int minigunnerY) {
        boolean aiIsOn = false
        return addMinigunnerAtWorldCoordinates(GDI_MINIGUNNERS_BASE_URL, minigunnerX, minigunnerY, aiIsOn)
    }


    Unit addGDIMinigunnerAtMapSquare(int x, int y) {
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

        def commandParams =
                [
                        unitId: unitId,
                        destinationLocationXInWorldCoordinates: destinationXInWorldCoordinates,
                        destinationLocationYInWorldCoordinates: destinationYInWorldCoordinate
                ]

        command.commandData =  JsonOutput.toJson(commandParams)


        def resp = restClient.post(
                path: '/simulation/command',
                body: command,
                requestContentType: 'application/json')

        assert resp.status == 200

    }


}
