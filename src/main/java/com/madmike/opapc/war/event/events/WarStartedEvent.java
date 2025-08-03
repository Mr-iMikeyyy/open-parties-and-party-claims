package com.madmike.opapc.war.event.events;

import com.madmike.opapc.war.War;
import com.madmike.opapc.war.event.events.abs.WarEvent;

public class WarStartedEvent extends WarEvent {
    public WarStartedEvent(War war) { super(war); }
}
