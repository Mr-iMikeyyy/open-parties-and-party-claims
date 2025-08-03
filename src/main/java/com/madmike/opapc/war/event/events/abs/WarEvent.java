package com.madmike.opapc.war.event.events.abs;

import com.madmike.opapc.war.War;

public abstract class WarEvent {
    private final War war;
    public WarEvent(War war) { this.war = war; }
    public War getWar() { return war; }
}
