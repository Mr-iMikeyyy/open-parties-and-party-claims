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

package com.madmike.opapc.trade.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class Offer {

    private final UUID offerId;
    private final UUID sellerId;
    private final ItemStack item;
    private final long price;

    public Offer(UUID offerId, UUID sellerId, ItemStack item, long price) {
        this.offerId = offerId;
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("OfferID", this.offerId);
        nbt.putUUID("Seller", this.sellerId);
        nbt.put("Item", this.item.save(new CompoundTag()));
        nbt.putLong("Price", this.price);

        return nbt;
    }

    public static Offer fromNbt(CompoundTag nbt) {
        UUID offerId = nbt.getUUID("OfferID");
        UUID seller = nbt.getUUID("Seller");
        ItemStack item = ItemStack.of(nbt.getCompound("Item"));
        long price = nbt.getLong("Price");

        return new Offer(offerId, seller, item, price);
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUUID(offerId);
        buf.writeUUID(sellerId);
        buf.writeItem(item);
        buf.writeLong(price);
    }

    public static Offer readFromBuf(FriendlyByteBuf buf) {
        UUID offerId = buf.readUUID();
        UUID seller = buf.readUUID();
        ItemStack item = buf.readItem();
        long price = buf.readLong();
        return new Offer(offerId, seller, item, price);
    }

    public UUID getOfferId() {
        return this.offerId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ItemStack getItem() {
        return item;
    }

    public long getPrice() {
        return price;
    }
}
