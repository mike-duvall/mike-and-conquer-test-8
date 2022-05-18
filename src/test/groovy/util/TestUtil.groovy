package util

import client.MikeAndConquerSimulationClient
import domain.SimulationStateUpdateEvent
import spock.util.concurrent.PollingConditions

class TestUtil {


    def static assertNumberOfSimulationStateUpdateEvents(MikeAndConquerSimulationClient simulationClient, int numEventsToAssert) {
        int timeoutInSeconds = 30
        List<SimulationStateUpdateEvent> gameEventList
        def conditions = new PollingConditions(timeout: timeoutInSeconds, initialDelay: 1.5, factor: 1.25)
        conditions.eventually {
            gameEventList = simulationClient.getSimulationStateUpdateEvents()
            assert gameEventList.size() == numEventsToAssert
        }
        return true

    }

}
