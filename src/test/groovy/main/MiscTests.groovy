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


    def "test1"() {
        given:
        int x = 3


        when:
        x=4
        RESTClient client = new RESTClient("http://localhost:5000")
        def path = "/minigunners"
        def response
        try {
            response = client.get(path: path)
            assert response.statusCode == 200
            assert response.json?.headers?.host == "postman-echo.com"
        } catch (Exception e) {
            assert e?.response?.statusCode != 200
        }


        then:
        true
    }

    def "Add minigunner and move across screen"() {
        given:
        String localhost = "localhost"
//        String localhost = "192.168.0.155"
        String remoteHost = "192.168.0.147"
        String host = localhost
//        String host = remoteHost

        int port = 5000
//        int port = 5555
        boolean useTimeouts = true
//        boolean useTimeouts = false
        MikeAndConquerGameClient gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )

        when:
        gameClient.addGDIMinigunnerAtMapSquare(5,5)

        then:
        true

    }



}