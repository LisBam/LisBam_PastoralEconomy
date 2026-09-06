package lisbam.pastoraleconomy.data.player;

import lisbam.pastoraleconomy.client.ClientHudEventHandler;
import lisbam.pastoraleconomy.client.ClientPlayerState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Items;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;

/**
 * Standalone verification only; it is not registered as a Minecraft command
 * or shipped as a gameplay entry point.
 */
public final class PlayerDataSelfTest {
    private PlayerDataSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        PlayerData data = new PlayerData();
        require(data.getCoins() == 0L, "new player data must start at zero");
        require(data.canAfford(0L), "zero cost must be affordable");
        require(!data.canAfford(-1L), "negative cost must be rejected");
        require(!data.addCoins(-1L), "negative additions must be rejected");
        require(data.addCoins(0L), "zero addition is a valid no-op");
        require(data.getCoins() == 0L, "zero addition must not mutate balance");
        require(data.addCoins(100L), "positive addition must succeed");
        require(!data.trySpend(101L), "overspend must fail");
        require(data.getCoins() == 100L, "overspend must not mutate balance");
        require(data.trySpend(100L), "exact spend must succeed");
        require(data.getCoins() == 0L, "exact spend must reach zero");
        require(data.trySpend(0L), "zero spend is a valid no-op");

        require(data.addCoins(Long.MAX_VALUE), "Long.MAX_VALUE must remain representable");
        require(!data.addCoins(1L), "overflowing addition must fail");
        require(data.getCoins() == Long.MAX_VALUE, "overflow must not mutate balance");

        NBTTagCompound saved = data.writeToNBT();
        PlayerData restored = new PlayerData();
        restored.readFromNBT(saved);
        require(restored.getCoins() == Long.MAX_VALUE, "saved balance must round-trip");

        data.setShoulderStack(new ItemStack(Items.ELYTRA));
        NBTTagCompound shoulderSaved = data.writeToNBT();
        restored.readFromNBT(shoulderSaved);
        require(restored.getShoulderStack().getItem() == Items.ELYTRA
                        && restored.getShoulderStack().getCount() == 1,
                "shoulder Elytra must persist as one item");
        PlayerData clone = new PlayerData();
        clone.copyFrom(restored);
        require(clone.getCoins() == Long.MAX_VALUE && clone.getShoulderStack().getItem() == Items.ELYTRA,
                "clone data must preserve the balance and shoulder Elytra");
        restored.setShoulderStack(new ItemStack(Items.DIAMOND_CHESTPLATE));
        require(restored.getShoulderStack().isEmpty(), "shoulder slot rejects unsupported stacks");
        restored.setShoulderStack(new ItemStack(lisbam.pastoraleconomy.item.ModItems.SUPER_BACKPACK));
        require(restored.getShoulderStack().getItem() == lisbam.pastoraleconomy.item.ModItems.SUPER_BACKPACK,
                "shoulder slot accepts backpacks");

        NBTTagCompound corrupt = new NBTTagCompound();
        corrupt.setLong("coins", -1L);
        restored.readFromNBT(corrupt);
        require(restored.getCoins() == 0L, "negative persisted balance must be repaired");

        require("0".equals(ClientHudEventHandler.formatCoins(0L)), "zero HUD format");
        require("999".equals(ClientHudEventHandler.formatCoins(999L)), "hundreds HUD format");
        require("1,000".equals(ClientHudEventHandler.formatCoins(1000L)), "thousands HUD format");
        require("12,580".equals(ClientHudEventHandler.formatCoins(12580L)), "comma HUD format");

        ClientPlayerState.clear();
        ClientPlayerState.acceptCoins(12580L);
        require(ClientPlayerState.hasValidCoinSync(), "client cache must mark a received sync as valid");
        ClientPlayerState.clear();
        require(!ClientPlayerState.hasValidCoinSync() && ClientPlayerState.getCoins() == 0L,
                "client cache must clear between connections");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
