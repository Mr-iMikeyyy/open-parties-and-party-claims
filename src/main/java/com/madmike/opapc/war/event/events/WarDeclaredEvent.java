package com.madmike.opapc.war.event.events;

import com.madmike.opapc.war.War;
import com.madmike.opapc.war.event.events.abs.WarEvent;

public class WarDeclaredEvent extends WarEvent {
    public WarDeclaredEvent(War war) {
        super(war);
    }
}
