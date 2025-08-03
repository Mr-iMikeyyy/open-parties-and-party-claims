package com.madmike.opapc.war.event.bus;

import com.madmike.opapc.war.event.events.abs.WarEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WarEventBus {
    private static final List<Consumer<WarEvent>> listeners = new ArrayList<>();

    public static void register(Consumer<WarEvent> listener) {
        listeners.add(listener);
    }

    public static void post(WarEvent event) {
        for (Consumer<WarEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
