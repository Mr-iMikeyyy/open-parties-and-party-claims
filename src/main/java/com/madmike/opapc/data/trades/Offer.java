package com.madmike.opapc.data.trades;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("OfferID", this.offerId);
        nbt.putUuid("Seller", this.sellerId);
        nbt.put("Item", this.item.writeNbt(new NbtCompound()));
        nbt.putLong("Price", this.price);

        if (this.partyId != null) {
            nbt.putUuid("PartyID", this.partyId);
            nbt.putBoolean("HasParty", true);
        } else {
            nbt.putBoolean("HasParty", false);
        }

        return nbt;
    }

    public static Offer fromNbt(NbtCompound nbt) {
        UUID offerId = nbt.getUuid("OfferID"); // ✅ match case
        UUID seller = nbt.getUuid("Seller");
        ItemStack item = ItemStack.fromNbt(nbt.getCompound("Item"));
        long price = nbt.getLong("Price");

        UUID party = nbt.getBoolean("HasParty") ? nbt.getUuid("PartyID") : null; // ✅ match case

        return new Offer(offerId, seller, item, price, party);
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(offerId);
        buf.writeUuid(sellerId);
        buf.writeItemStack(item);
        buf.writeLong(price);
        buf.writeBoolean(partyId != null);
        if (partyId != null) buf.writeUuid(partyId);
    }

    public static Offer readFromBuf(PacketByteBuf buf) {
        UUID offerId = buf.readUuid();
        UUID seller = buf.readUuid();
        ItemStack item = buf.readItemStack();
        long price = buf.readLong();
        UUID party = buf.readBoolean() ? buf.readUuid() : null;
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
