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
    private UUID partyId;

    public Offer(UUID offerId, UUID sellerId, ItemStack item, long price, @Nullable UUID partyId) {
        this.offerId = offerId;
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
        this.partyId = partyId;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("OfferID", this.offerId);
        nbt.putUUID("Seller", this.sellerId);
        nbt.put("Item", this.item.save(new CompoundTag()));
        nbt.putLong("Price", this.price);

        if (this.partyId != null) {
            nbt.putUUID("PartyID", this.partyId);
            nbt.putBoolean("HasParty", true);
        } else {
            nbt.putBoolean("HasParty", false);
        }

        return nbt;
    }

    public static Offer fromNbt(CompoundTag nbt) {
        UUID offerId = nbt.getUUID("OfferID");
        UUID seller = nbt.getUUID("Seller");
        ItemStack item = ItemStack.of(nbt.getCompound("Item"));
        long price = nbt.getLong("Price");

        UUID party = nbt.getBoolean("HasParty") ? nbt.getUUID("PartyID") : null;

        return new Offer(offerId, seller, item, price, party);
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUUID(offerId);
        buf.writeUUID(sellerId);
        buf.writeItem(item);
        buf.writeLong(price);
        buf.writeBoolean(partyId != null);
        if (partyId != null) buf.writeUUID(partyId);
    }

    public static Offer readFromBuf(FriendlyByteBuf buf) {
        UUID offerId = buf.readUUID();
        UUID seller = buf.readUUID();
        ItemStack item = buf.readItem();
        long price = buf.readLong();
        UUID party = buf.readBoolean() ? buf.readUUID() : null;
        return new Offer(offerId, seller, item, price, party);
    }

    public UUID getOfferId() {
        return this.offerId;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
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
