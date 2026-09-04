package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.merchant.MerchantTradeOfferView;
import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;
import lisbam.pastoraleconomy.merchant.TradeCatalog;
import lisbam.pastoraleconomy.merchant.TradeCatalogEntry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded S2C merchant GUI snapshot. */
public final class SyncMerchantTradeMessage implements IMessage {
    private static final int MAX_KEY_BYTES = 128;
    private MerchantTradeSnapshot snapshot;

    public SyncMerchantTradeMessage() {
    }

    public SyncMerchantTradeMessage(MerchantTradeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Merchant snapshot is required.");
        }
        this.snapshot = snapshot;
    }

    public MerchantTradeSnapshot getSnapshot() { return snapshot; }

    @Override
    public void fromBytes(ByteBuf buffer) {
        snapshot = null;
        if (buffer.readableBytes() < 16 + 4 + 8 + 8 + 12) {
            return;
        }
        UUID merchantId = new UUID(buffer.readLong(), buffer.readLong());
        int windowId = buffer.readInt();
        long day = buffer.readLong();
        long balance = buffer.readLong();
        if (day < 0L || balance < 0L) {
            return;
        }
        List<MerchantTradeOfferView> sells = readViews(buffer, 6);
        List<MerchantTradeOfferView> buys = readViews(buffer, 10);
        if (sells == null || buys == null) {
            return;
        }
        snapshot = new MerchantTradeSnapshot(merchantId, windowId, day, balance, sells, buys);
    }

    private static List<MerchantTradeOfferView> readViews(ByteBuf buffer, int count) {
        List<MerchantTradeOfferView> views = new ArrayList<MerchantTradeOfferView>(count);
        for (int index = 0; index < count; index++) {
            if (buffer.readableBytes() < 1 + 2 + 4 + 8 + 1 + 4 + 4) {
                return null;
            }
            boolean enabled = buffer.readBoolean();
            int length = buffer.readUnsignedShort();
            if (length > MAX_KEY_BYTES || buffer.readableBytes() < length) {
                return null;
            }
            byte[] keyBytes = new byte[length];
            buffer.readBytes(keyBytes);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            int bundle = buffer.readInt();
            long price = buffer.readLong();
            boolean hasPrevious = buffer.readBoolean();
            Long previous = null;
            if (hasPrevious) {
                if (buffer.readableBytes() < 8) {
                    return null;
                }
                previous = Long.valueOf(buffer.readLong());
            }
            int remaining = buffer.readInt();
            int enchantmentLevel = buffer.readInt();
            if (!enabled) {
                key = "";
                bundle = 0;
                price = 0L;
                previous = null;
                remaining = -1;
                enchantmentLevel = 0;
            } else {
                TradeCatalogEntry entry = TradeCatalog.get(key);
                boolean validLevel = entry != null && (entry.isEnchantment()
                        ? enchantmentLevel >= 1 && enchantmentLevel <= entry.getEnchantmentDefinition().getMaxLevel()
                        : enchantmentLevel == 0);
                if (key.length() == 0 || entry == null || bundle <= 0 || price <= 0L || remaining < -1
                        || !validLevel || (previous != null && previous.longValue() <= 0L)) {
                    return null;
                }
            }
            views.add(new MerchantTradeOfferView(enabled, key, bundle, price, previous, remaining, enchantmentLevel));
        }
        return views;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (snapshot == null) {
            throw new IllegalStateException("Cannot encode absent merchant snapshot.");
        }
        buffer.writeLong(snapshot.getMerchantId().getMostSignificantBits());
        buffer.writeLong(snapshot.getMerchantId().getLeastSignificantBits());
        buffer.writeInt(snapshot.getWindowId());
        buffer.writeLong(snapshot.getWorldDay());
        buffer.writeLong(snapshot.getBalance());
        writeViews(buffer, snapshot.getSellOffers());
        writeViews(buffer, snapshot.getBuyOffers());
    }

    private static void writeViews(ByteBuf buffer, List<MerchantTradeOfferView> views) {
        for (MerchantTradeOfferView view : views) {
            buffer.writeBoolean(view.isEnabled());
            byte[] key = view.getCatalogKey().getBytes(StandardCharsets.UTF_8);
            if (key.length > MAX_KEY_BYTES) {
                throw new IllegalArgumentException("Merchant catalog key exceeds packet limit.");
            }
            buffer.writeShort(key.length);
            buffer.writeBytes(key);
            buffer.writeInt(view.getBundleSize());
            buffer.writeLong(view.getCurrentPrice());
            buffer.writeBoolean(view.hasPreviousPrice());
            if (view.hasPreviousPrice()) {
                buffer.writeLong(view.getPreviousPrice());
            }
            buffer.writeInt(view.getRemainingBundles());
            buffer.writeInt(view.getEnchantmentLevel());
        }
    }
}
