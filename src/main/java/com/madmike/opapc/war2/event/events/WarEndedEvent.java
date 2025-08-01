package com.madmike.opapc.war2.event.events;

import com.madmike.opapc.war2.EndOfWarType;
import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.event.events.abs.WarEvent;

public class WarEndedEvent extends WarEvent {
    private final EndOfWarType endType;
    public WarEndedEvent(War war, EndOfWarType type) {
        super(war);
        this.endType = type;
    }
    public EndOfWarType getEndType() { return endType; }
}
