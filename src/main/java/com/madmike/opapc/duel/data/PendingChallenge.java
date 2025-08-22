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

package com.madmike.opapc.duel.data;

import com.madmike.opapc.OPAPCConfig;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PendingChallenge {
    public final UUID id;
    public final UUID challengerId;
    public final UUID opponentId;
    public final String challengerName;
    public final String opponentName;
    public final String desiredMapName; // "Random" or specific name
    public final long wager;
    public final long createdAtMs;
    public final long expiresAtMs;

    public PendingChallenge(UUID id,
                            UUID challengerId, UUID opponentId,
                            String challengerName, String opponentName,
                            @Nullable String desiredMapName,
                            long wager,
                            long createdAtMs, long expiresAtMs) {
        this.id = id;
        this.challengerId = challengerId;
        this.opponentId = opponentId;
        this.challengerName = challengerName;
        this.opponentName = opponentName;
        this.desiredMapName = desiredMapName;
        this.wager = wager;
        this.createdAtMs = createdAtMs;
        this.expiresAtMs = createdAtMs + (OPAPCConfig.duelChallengeMaxTime * 1000L);
    }

    public long remainingMs() { return Math.max(0L, expiresAtMs - System.currentTimeMillis()); }
}
