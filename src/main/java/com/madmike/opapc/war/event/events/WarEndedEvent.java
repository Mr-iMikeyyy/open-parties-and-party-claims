package com.madmike.opapc.war.event.events;

import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.event.events.abs.WarEvent;

public class WarEndedEvent extends WarEvent {
    private final EndOfWarType endType;
    public WarEndedEvent(War war, EndOfWarType type) {
        super(war);
        this.endType = type;
    }
    public EndOfWarType getEndType() { return endType; }
}
