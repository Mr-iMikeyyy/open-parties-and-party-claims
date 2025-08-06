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

package com.madmike.opapc.duel.state;

import com.madmike.opapc.duel.Duel;
import com.madmike.opapc.duel.EndOfDuelType;
import net.minecraft.server.level.ServerPlayer;

public interface IDuelState {
    void tick(Duel duel);
    void onChallengerDeath(ServerPlayer ch, Duel duel);
    void onOpponentDeath(ServerPlayer opp, Duel duel);
    void end(Duel duel, EndOfDuelType type);
}
