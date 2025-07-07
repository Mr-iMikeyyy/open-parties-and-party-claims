package com.madmike.opapc.data.war;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class WarStat {
    private final UUID partyId;
    private int attackingWins;
    private int attackingLosses;
    private int defendingWins;
    private int defendingLosses;
    private int claimsStolen;
    private int blocksBroken;

    public WarStat(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getPartyId() { return partyId; }
    public int getAttackingWins() { return attackingWins; }
    public int getAttackingLosses() { return attackingLosses; }
    public int getDefendingWins() { return defendingWins; }
    public int getDefendingLosses() { return defendingLosses; }
    public int getClaimsStolen() { return claimsStolen; }
    public int getBlocksBroken() { return blocksBroken; }

    public void addAttackingWin() { this.attackingWins++; }
    public void addAttackingLoss() { this.attackingLosses++; }
    public void addDefendingWin() { this.defendingWins++; }
    public void addDefendingLoss() { this.defendingLosses++; }
    public void addClaimsStolen(int amount) { this.claimsStolen += amount; }
    public void addBlocksBroken(int amount) { this.blocksBroken += amount; }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("partyId", partyId);
        tag.putInt("attackingWins", attackingWins);
        tag.putInt("attackingLosses", attackingLosses);
        tag.putInt("defendingWins", defendingWins);
        tag.putInt("defendingLosses", defendingLosses);
        tag.putInt("claimsStolen", claimsStolen);
        tag.putInt("blocksBroken", blocksBroken);
        return tag;
    }

    public static WarStat fromNbt(CompoundTag tag) {
        WarStat stat = new WarStat(tag.getUUID("partyId"));
        stat.attackingWins = tag.getInt("attackingWins");
        stat.attackingLosses = tag.getInt("attackingLosses");
        stat.defendingWins = tag.getInt("defendingWins");
        stat.defendingLosses = tag.getInt("defendingLosses");
        stat.claimsStolen = tag.getInt("claimsStolen");
        stat.blocksBroken = tag.getInt("blocksBroken");
        return stat;
    }
}
