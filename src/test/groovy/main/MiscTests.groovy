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
//        String localhost = "192.168.0.155"
        String remoteHost = "192.168.0.147"
        String host = localhost
//        String host = remoteHost

        int port = 5000
        boolean useTimeouts = true
        MikeAndConquerGameClient gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )

        when:
        gameClient.addGDIMinigunnerAtMapSquare(5,5)

        then:
        true

    }



}