package com.madmike.opapc.war2.event.events;

import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.event.events.abs.WarEvent;

public class WarStartedEvent extends WarEvent {
    public WarStartedEvent(War war) { super(war); }
}
