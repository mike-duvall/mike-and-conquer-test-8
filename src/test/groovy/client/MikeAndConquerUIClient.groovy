package client

import domain.*
import groovy.json.JsonOutput
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
        unit.unitId = resp.responseData.unitId
        unit.selected = resp.responseData.selected

        return unit
    }



}