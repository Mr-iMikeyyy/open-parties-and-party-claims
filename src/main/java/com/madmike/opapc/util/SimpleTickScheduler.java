/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SimpleTickScheduler {
    private static final class Task implements Comparable<Task> {
        final long dueTick;
        final Runnable run;
        Task(long dueTick, Runnable run) { this.dueTick = dueTick; this.run = run; }
        public int compareTo(Task o) { return Long.compare(this.dueTick, o.dueTick); }
    }

    private static final PriorityQueue<Task> QUEUE = new PriorityQueue<>();
    private static final AtomicBoolean INIT = new AtomicBoolean(false);
    private static long currentTick = 0;

    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                currentTick++;
                while (!QUEUE.isEmpty() && QUEUE.peek().dueTick <= currentTick) {
                    QUEUE.poll().run.run();
                }
            });
        }
    }

    /** Schedule to run after `delayTicks` on server thread. */
    public static void runLater(long delayTicks, Runnable r) {
        QUEUE.add(new Task(currentTick + Math.max(0, delayTicks), r));
    }
}
