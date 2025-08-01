package com.madmike.opapc.war2.event.events;

import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.event.events.abs.WarEvent;

public class WarDeclaredEvent extends WarEvent {
    public WarDeclaredEvent(War war) {
        super(war);
    }
}
