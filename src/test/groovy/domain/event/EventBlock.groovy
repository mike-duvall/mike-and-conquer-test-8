package domain.event

class EventBlock {


    String eventType
    int numberOfEvents

    public EventBlock(String anEventType, int aNumberOfEvents) {
        this.eventType = anEventType
        this.numberOfEvents = aNumberOfEvents
    }

}
