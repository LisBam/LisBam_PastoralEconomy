package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persisted shuffle-bag cursor for one merchant offer pool. */
public final class OfferRotationState {
    private static final String KEY_CURSOR = "cursor";
    private static final String KEY_CYCLE = "cycle";
    private static final String KEY_BAG = "bag";

    private final List<String> bag = new ArrayList<String>();
    private int cursor;
    private int cycle;

    public List<String> getBag() {
        return Collections.unmodifiableList(bag);
    }

    public int getCursor() {
        return cursor;
    }

    public int getCycle() {
        return cycle;
    }

    void replaceBag(List<String> orderedKeys) {
        bag.clear();
        bag.addAll(orderedKeys);
        cursor = 0;
        if (cycle < Integer.MAX_VALUE) {
            cycle++;
        }
    }

    String takeNext() {
        if (bag.isEmpty() || cursor < 0 || cursor >= bag.size()) {
            return null;
        }
        String key = bag.get(cursor);
        cursor++;
        return key;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(KEY_CURSOR, cursor);
        tag.setInteger(KEY_CYCLE, cycle);
        NBTTagList values = new NBTTagList();
        for (String key : bag) {
            values.appendTag(new NBTTagString(key));
        }
        tag.setTag(KEY_BAG, values);
        return tag;
    }

    public static OfferRotationState readFromNBT(NBTTagCompound tag) {
        OfferRotationState state = new OfferRotationState();
        state.cursor = Math.max(0, tag.getInteger(KEY_CURSOR));
        state.cycle = Math.max(0, tag.getInteger(KEY_CYCLE));
        NBTTagList values = tag.getTagList(KEY_BAG, 8);
        for (int index = 0; index < values.tagCount(); index++) {
            String key = values.getStringTagAt(index);
            if (!key.isEmpty()) {
                state.bag.add(key);
            }
        }
        if (state.cursor > state.bag.size()) {
            state.cursor = state.bag.size();
        }
        return state;
    }
}
