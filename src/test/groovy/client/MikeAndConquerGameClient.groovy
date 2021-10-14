package client

import domain.*
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.params.CoreConnectionPNames
import util.Util

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

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
//        inputMinigunner.aiIsOn = aiIsOn
//        def resp = restClient.post(
//                path: baseUrl,
//                body:   inputMinigunner ,
//                requestContentType: 'application/json' )

//        def resp = restClient.get(
//                path: 'WeatherForecast',
//                requestContentType: 'application/json' )

//        def resp = restClient.get(
//                path: '/minigunners',
//                requestContentType: 'application/json' )

        def resp = restClient.post(
                path: '/minigunners',
                body:   inputMinigunner ,
                requestContentType: 'application/json' )

        assert resp.status == 201

        Minigunner minigunner = new Minigunner()
        minigunner.id = resp.responseData.id
        minigunner.x = resp.responseData.x
        minigunner.y = resp.responseData.y
        return minigunner

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
            simulationStateUpdateEvent.X = resp.responseData[i]['x']
            simulationStateUpdateEvent.Y = resp.responseData[i]['y']
            simulationStateUpdateEvent.ID = resp.responseData[i]['id']

            allSimulationStateUpdateEvents.add(simulationStateUpdateEvent)
        }
        return allSimulationStateUpdateEvents

        //int x = 3

    }

    void moveUnit(int unitId, int destinationXInWorldCoordinates, int destinationYInWorldCoordinate) {

        MoveUnitEvent moveUnitEvent = new MoveUnitEvent()
        moveUnitEvent.id = unitId
        moveUnitEvent.destinationXInWorldCoordinates = destinationXInWorldCoordinates
        moveUnitEvent.destinationYInWorldCoordinates = destinationYInWorldCoordinate

        def resp = restClient.post(
                path: '/inputEvent',
                body:   moveUnitEvent ,
                requestContentType: 'application/json' )

        assert resp.status == 201

    }
}
