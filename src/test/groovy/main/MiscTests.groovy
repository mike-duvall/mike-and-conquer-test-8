package main

import client.MikeAndConquerGameClient
import domain.*
import groovyx.net.http.HttpResponseException
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
//        boolean useTimeouts = false
        MikeAndConquerGameClient gameClient = new MikeAndConquerGameClient(host, port, useTimeouts )

        when:
        gameClient.addGDIMinigunnerAtMapSquare(5,5)

        then:
        true

    }



}