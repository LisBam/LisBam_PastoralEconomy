package lisbam.pastoraleconomy.enchantment;

import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.core.EnchantingCompatibilityHooks;
import lisbam.pastoraleconomy.core.EnchantingTableTransformer;
import lisbam.pastoraleconomy.event.ToolEnchantmentEventHandler;
import lisbam.pastoraleconomy.event.AnvilEnchantmentEventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.Item;
import net.minecraftforge.event.AnvilUpdateEvent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Random;

/** Standalone deterministic checks for all registered pastoral enchantments. */
public final class EnchantmentSelfTest {
    private EnchantmentSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyDefinitions();
        verifyEnchantingTableCompatibilityPatch();
        verifySrgRuntimeNameCompatibility();
        verifyLegacyItemLayoutCompatibility();
        verifyReforgedAnvilRule();
        verifyHoeAnvilRepairRule();
        verifyHarvestFormulas();
        verifyProbabilityThresholds();
    }

    private static void verifyReforgedAnvilRule() {
        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        left.setRepairCost(31);
        ItemStack right = new ItemStack(Items.ENCHANTED_BOOK);
        right.setRepairCost(15);
        ItemEnchantedBook.addEnchantment(right, new EnchantmentData(Enchantments.UNBREAKING, 1));

        AnvilFirstUseRules.Result result = AnvilFirstUseRules.createResult(left, right, null);
        require(result != null, "Reforged must produce an ordinary first-use result despite prior-work costs");
        require(result.getCost() == 1, "Reforged must charge the first-use book cost");
        require(result.getOutput().getRepairCost() == 1, "Reforged output must restart at first-use repair cost");
        require(left.getRepairCost() == 31 && right.getRepairCost() == 15,
                "Reforged must not mutate either anvil input's persistent NBT");
    }

    private static void verifyHoeAnvilRepairRule() {
        ItemStack left = new ItemStack(Items.IRON_HOE);
        left.setItemDamage(200);
        ItemStack iron = new ItemStack(Items.IRON_INGOT, 2);
        require(HoeAnvilRepairRules.isHoeRepairMaterial(left, iron),
                "an iron hoe must accept iron ingots as its anvil repair material");
        require(!HoeAnvilRepairRules.isHoeRepairMaterial(left, new ItemStack(Items.DIAMOND)),
                "a hoe must reject the wrong-tier repair material");

        AnvilUpdateEvent event = new AnvilUpdateEvent(left, iron, null, 0);
        AnvilEnchantmentEventHandler.repairVanillaHoesWithTheirMaterials(event);
        require(!event.getOutput().isEmpty() && event.getOutput().getItemDamage() == 76
                        && event.getMaterialCost() == 2 && event.getCost() == 2,
                "the custom hoe repair path must preserve vanilla quarter-durability material repair");
        require(event.getOutput().getRepairCost() == 1,
                "a first hoe repair must receive the normal next prior-work cost");

        AnvilFirstUseRules.Result reforged = AnvilFirstUseRules.createResult(left, iron, null,
                HoeAnvilRepairRules.isHoeRepairMaterial(left, iron));
        require(reforged != null && reforged.getOutput().getItemDamage() == 76
                        && reforged.getOutput().getRepairCost() == 1,
                "Reforged hoes must retain their first-use repair behavior instead of losing material repair");
    }

    private static void verifyDefinitions() {
        Enchantment[] all = ModEnchantments.getAll();
        require(all.length == 13, "exactly thirteen active enchantments must be registered");
        for (Enchantment enchantment : all) {
            require(enchantment.isAllowedOnBooks(), "all batch enchantments must allow enchanted books");
        }
        require(ModEnchantments.HARVEST.getRarity() == Rarity.RARE, "Harvest rarity");
        require(ModEnchantments.FARMLAND_WALKER.getRarity() == Rarity.UNCOMMON, "Farmland Walker rarity");
        require(ModEnchantments.PASTORAL_FAVOR.getRarity() == Rarity.UNCOMMON, "Pastoral Favor rarity");
        require(ModEnchantments.FINE_CULTIVATION.getRarity() == Rarity.RARE, "Fine Cultivation rarity");
        require(ModEnchantments.FELLING.getRarity() == Rarity.VERY_RARE, "Felling rarity");
        require(ModEnchantments.SLAUGHTER.getRarity() == Rarity.RARE, "Slaughter rarity");
        require(ModEnchantments.FLEETFOOT.getRarity() == Rarity.UNCOMMON, "Fleetfoot rarity");
        require(ModEnchantments.NIGHT_VISION.getRarity() == Rarity.RARE, "Night Vision rarity");
        require(ModEnchantments.ATTACK_SPEED.getRarity() == Rarity.COMMON, "Attack Speed rarity");
        require(ModEnchantments.RANGE.getRarity() == Rarity.COMMON, "Range rarity");
        require(ModEnchantments.DROP_ATTRACTION.getRarity() == Rarity.VERY_RARE, "Drop Attraction rarity");
        require(ModEnchantments.REFORGED.getRarity() == Rarity.VERY_RARE, "Reforged rarity");
        require(ModEnchantments.BLUNTNESS_CURSE.getRarity() == Rarity.VERY_RARE
                        && ModEnchantments.BLUNTNESS_CURSE.isTreasureEnchantment()
                        && ModEnchantments.BLUNTNESS_CURSE.isCurse(),
                "Bluntness Curse must be a treasure curse");
        require(ModEnchantments.HARVEST.getMaxLevel() == 3, "Harvest maximum level");
        require(ModEnchantments.FARMLAND_WALKER.getMaxLevel() == 3, "Farmland Walker maximum level");
        require(ModEnchantments.PASTORAL_FAVOR.getMaxLevel() == 4, "Pastoral Favor maximum level");
        require(ModEnchantments.FINE_CULTIVATION.getMaxLevel() == 4, "Fine Cultivation maximum level");
        require(ModEnchantments.FELLING.getMaxLevel() == 1, "Felling maximum level");
        require(ModEnchantments.SLAUGHTER.getMaxLevel() == 3, "Slaughter maximum level");
        require(ModEnchantments.FLEETFOOT.getMaxLevel() == 4, "Fleetfoot maximum level");
        require(ModEnchantments.NIGHT_VISION.getMaxLevel() == 1, "Night Vision maximum level");
        require(ModEnchantments.ATTACK_SPEED.getMaxLevel() == 5, "Attack Speed maximum level");
        require(ModEnchantments.RANGE.getMaxLevel() == 5, "Range maximum level");
        require(ModEnchantments.DROP_ATTRACTION.getMaxLevel() == 1, "Drop Attraction maximum level");
        require(ModEnchantments.REFORGED.getMaxLevel() == 1, "Reforged maximum level");
        require(ModEnchantments.BLUNTNESS_CURSE.getMaxLevel() == 1, "Bluntness Curse maximum level");
        require(ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_HOE)),
                "Harvest must apply to a vanilla hoe");
        require(ModEnchantments.HARVEST.canApply(new ItemStack(Items.DIAMOND_AXE))
                        && !ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Harvest must support axes through books/anvils but stay out of axe table rolls");
        require(ModEnchantments.isAxe(new ItemStack(Items.DIAMOND_AXE)),
                "Harvest crop effect must recognize an axe enchanted through a book or anvil");
        require(ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Harvest must apply to shears");
        require(Enchantments.MENDING.canApply(new ItemStack(Items.SHEARS))
                        && Enchantments.UNBREAKING.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS))
                        && Enchantments.VANISHING_CURSE.canApply(new ItemStack(Items.SHEARS)),
                "vanilla books and Breakable table enchantments must apply to shears");
        require(ModEnchantments.SLAUGHTER.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Slaughter must apply to a sword");
        require(ModEnchantments.SLAUGHTER.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Slaughter must apply to an axe");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.SHARPNESS),
                "Slaughter must reject Sharpness");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.SMITE),
                "Slaughter must reject Smite");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.BANE_OF_ARTHROPODS),
                "Slaughter must reject Bane of Arthropods");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.LOOTING),
                "Slaughter must reject Looting");
        require(!ModEnchantments.HARVEST.isCompatibleWith(Enchantments.FORTUNE),
                "Harvest must reject Fortune");
        require(ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Attack Speed must apply to weapons");
        require(ModEnchantments.FLEETFOOT.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_LEGGINGS))
                        && !ModEnchantments.FLEETFOOT.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_BOOTS)),
                "Fleetfoot must apply to leggings, not boots");
        require(ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_PICKAXE)),
                "Attack Speed must apply to tools");
        require(ModEnchantments.RANGE.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Range must apply to shears");
        require(ModEnchantments.DROP_ATTRACTION.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_HOE))
                        && ModEnchantments.DROP_ATTRACTION.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE))
                        && ModEnchantments.DROP_ATTRACTION.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_PICKAXE))
                        && ModEnchantments.DROP_ATTRACTION.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS))
                        && ModEnchantments.DROP_ATTRACTION.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Drop Attraction must cover swords, every vanilla tool class and shears");
        require(ModEnchantments.isDropAttractionItem(new ItemStack(Items.DIAMOND_SWORD)),
                "Drop Attraction combat handling must recognize an enchanted sword");
        require(!ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Attack Speed must not extend beyond the stated shears enchantment list");
        require(ModEnchantments.REFORGED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Reforged must apply to tools");
        require(!ModEnchantments.REFORGED.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Reforged must not extend beyond the stated shears enchantment list");
        require(ModEnchantments.BLUNTNESS_CURSE.canApply(new ItemStack(Items.DIAMOND_SWORD)),
                "Bluntness Curse item scope must be swords");
        require(!ModEnchantments.BLUNTNESS_CURSE.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Bluntness Curse must stay out of enchanting-table rolls");
        require(!ModEnchantments.BLUNTNESS_CURSE.isCompatibleWith(Enchantments.SWEEPING),
                "Bluntness Curse must reject Sweeping Edge");
        require(ToolEnchantmentEventHandler.getRangeBonus(1) == 1.5D
                        && ToolEnchantmentEventHandler.getRangeBonus(5) == 7.5D,
                "Range must add exactly 1.5 blocks per enchantment level");
    }

    private static void verifyEnchantingTableCompatibilityPatch() {
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.SHEARS, 0)
                        == Item.ToolMaterial.IRON.getEnchantability(),
                "shears must use iron-tier enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.WOODEN_HOE, 0)
                        == Item.ToolMaterial.WOOD.getEnchantability(),
                "wood hoe enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.STONE_HOE, 0)
                        == Item.ToolMaterial.STONE.getEnchantability(),
                "stone hoe enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.IRON_HOE, 0)
                        == Item.ToolMaterial.IRON.getEnchantability(),
                "iron hoe enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.GOLDEN_HOE, 0)
                        == Item.ToolMaterial.GOLD.getEnchantability(),
                "gold hoe enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.DIAMOND_HOE, 0)
                        == Item.ToolMaterial.DIAMOND.getEnchantability(),
                "diamond hoe enchanting power");
        require(EnchantingCompatibilityHooks.getItemEnchantability(Items.DIAMOND_SWORD, 10)
                        == 10, "unrelated items must retain vanilla enchanting power");

        byte[] transformed = new EnchantingTableTransformer().transform("net.minecraft.item.Item",
                "net.minecraft.item.Item", readItemClassBytes());
        verifyBytecode(transformed);
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        boolean enchantabilityHook = false;
        boolean enchantabilityUsesItemReceiver = false;
        boolean efficiencyGate = false;
        boolean efficiencyGateHasJump = false;
        for (MethodNode method : node.methods) {
            if ("getItemEnchantability".equals(method.name)
                    && "(Lnet/minecraft/item/ItemStack;)I".equals(method.desc)) {
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                     instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        if ("lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks".equals(call.owner)
                                && "getItemEnchantability".equals(call.name)) {
                            enchantabilityHook = true;
                            AbstractInsnNode vanillaCall = previousOpcode(instruction);
                            AbstractInsnNode vanillaReceiver = previousOpcode(vanillaCall);
                            AbstractInsnNode helperItem = previousOpcode(vanillaReceiver);
                            enchantabilityUsesItemReceiver = vanillaCall instanceof MethodInsnNode
                                    && vanillaReceiver instanceof VarInsnNode
                                    && helperItem instanceof VarInsnNode
                                    && vanillaReceiver.getOpcode() == org.objectweb.asm.Opcodes.ALOAD
                                    && helperItem.getOpcode() == org.objectweb.asm.Opcodes.ALOAD
                                    && ((VarInsnNode) vanillaReceiver).var == 0
                                    && ((VarInsnNode) helperItem).var == 0;
                        }
                    }
                }
            }
            if ("canApplyAtEnchantingTable".equals(method.name)
                    && "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/enchantment/Enchantment;)Z".equals(method.desc)) {
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                     instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        if ("lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks".equals(call.owner)
                                && "canApplyAtEnchantingTable".equals(call.name)
                                && "(Ljava/lang/Object;Ljava/lang/Object;Z)Z".equals(call.desc)) {
                            efficiencyGate = true;
                        }
                    }
                    if (instruction instanceof JumpInsnNode) {
                        efficiencyGateHasJump = true;
                    }
                }
            }
        }
        require(enchantabilityHook && enchantabilityUsesItemReceiver
                        && efficiencyGate && !efficiencyGateHasJump,
                "core patch must add table power and a stack-map-safe native Efficiency gate");
    }

    private static AbstractInsnNode previousOpcode(AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static void verifyBytecode(byte[] transformed) {
        StringWriter diagnostics = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(transformed), false, new PrintWriter(diagnostics));
        require(diagnostics.getBuffer().length() == 0,
                "core patch must produce verifier-clean Item bytecode: " + diagnostics);
    }

    private static void verifySrgRuntimeNameCompatibility() {
        ClassNode node = new ClassNode();
        new ClassReader(readItemClassBytes()).accept(node, 0);
        node.name = "vg";
        for (MethodNode method : node.methods) {
            if ("getItemEnchantability".equals(method.name)
                    && Type.getArgumentTypes(method.desc).length == 1) {
                method.desc = "(Ladd;)I";
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                     instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode && "()I".equals(((MethodInsnNode) instruction).desc)) {
                        ((MethodInsnNode) instruction).owner = "vg";
                        ((MethodInsnNode) instruction).name = "func_77619_b";
                    }
                }
            } else if ("canApplyAtEnchantingTable".equals(method.name)) {
                method.desc = "(Ladd;Lamz;)Z";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] transformed = new EnchantingTableTransformer().transform("vg", "net.minecraft.item.Item",
                writer.toByteArray());
        ClassNode transformedNode = new ClassNode();
        new ClassReader(transformed).accept(transformedNode, 0);
        boolean enchantabilityHook = false;
        boolean efficiencyHook = false;
        for (MethodNode method : transformedNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!"lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks".equals(call.owner)) {
                    continue;
                }
                enchantabilityHook |= "getItemEnchantability".equals(call.name)
                        && "(Ljava/lang/Object;I)I".equals(call.desc);
                efficiencyHook |= "canApplyAtEnchantingTable".equals(call.name)
                        && "(Ljava/lang/Object;Ljava/lang/Object;Z)Z".equals(call.desc);
            }
        }
        require(enchantabilityHook && efficiencyHook,
                "core patch must use mapping-neutral helper descriptors");
    }

    private static void verifyLegacyItemLayoutCompatibility() {
        ClassNode node = new ClassNode();
        new ClassReader(readItemClassBytes()).accept(node, 0);
        node.name = "vg";
        for (Iterator<MethodNode> iterator = node.methods.iterator(); iterator.hasNext();) {
            MethodNode method = iterator.next();
            if ("getItemEnchantability".equals(method.name)
                    && Type.getArgumentTypes(method.desc).length == 1
                    || "canApplyAtEnchantingTable".equals(method.name)) {
                iterator.remove();
            } else if ("getItemEnchantability".equals(method.name)
                    && Type.getArgumentTypes(method.desc).length == 0) {
                method.name = "func_77619_b";
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] transformed = new EnchantingTableTransformer().transform("vg", "net.minecraft.item.Item",
                writer.toByteArray());
        ClassNode transformedNode = new ClassNode();
        new ClassReader(transformed).accept(transformedNode, 0);
        boolean legacyHook = false;
        for (MethodNode method : transformedNode.methods) {
            if (!"func_77619_b".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    legacyHook |= "lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks".equals(call.owner)
                            && "getItemEnchantability".equals(call.name)
                            && "(Ljava/lang/Object;I)I".equals(call.desc);
                }
            }
        }
        require(legacyHook,
                "legacy Item layouts must receive the legacy enchantability hook");
    }

    private static byte[] readItemClassBytes() {
        InputStream input = Item.class.getResourceAsStream("Item.class");
        if (input == null) {
            throw new AssertionError("Item class bytes are unavailable for core patch verification");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            input.close();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError("Unable to read Item class bytes", exception);
        }
    }

    private static void verifyHarvestFormulas() {
        ItemStack carrots = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.CARROT, 1,
                new FixedRandom(0, 4));
        require(carrots.getItem() == Items.CARROT && carrots.getCount() == 1,
                "carrot Harvest must use two independent 4/7 trials per level");

        ItemStack wheat = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.WHEAT, 2,
                new FixedRandom(0, 1, 0, 1));
        require(wheat.getItem() == Items.WHEAT && wheat.getCount() == 2,
                "wheat Harvest must use two 1/2 trials per level");

        ItemStack wart = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.NETHER_WART, 3,
                new FixedRandom(6));
        require(wart.getItem() == Items.NETHER_WART && wart.getCount() == 6,
                "nether wart Harvest upper bound must be 2L");

        ItemStack melon = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.MELON, 3,
                new FixedRandom(3));
        require(melon.getItem() == Items.MELON && melon.getCount() == 6,
                "melon Harvest must be UniformInt(L, 2L)");

        ItemStack cocoa = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.COCOA, 3,
                new FixedRandom());
        require(cocoa.getItem() == Items.DYE && cocoa.getCount() == 3 && cocoa.getMetadata() == 3,
                "cocoa Harvest must add fixed cocoa-bean dye metadata 3");

        require(AgricultureRules.createHarvestBonus(AgricultureRules.Crop.PUMPKIN, 3,
                new FixedRandom()).isEmpty(), "pumpkins must receive no Harvest bonus");
        require(AgricultureRules.rollShearingHarvestBonus(2, new FixedRandom(0, 4, 1, 6)) == 2,
                "shearing Harvest must use two 4/7 trials per level");
    }

    private static void verifyProbabilityThresholds() {
        require(AgricultureRules.rollFineCultivation(1, new FixedRandom(24)), "Fine Cultivation I succeeds below 25");
        require(!AgricultureRules.rollFineCultivation(1, new FixedRandom(25)), "Fine Cultivation I fails at 25");
        require(AgricultureRules.rollFineCultivation(4, new FixedRandom(99)), "Fine Cultivation IV is 100 percent");
        require(AgricultureRules.getPastoralFavorChancePercent(1) == 20
                        && AgricultureRules.getPastoralFavorChancePercent(2) == 40
                        && AgricultureRules.getPastoralFavorChancePercent(3) == 60
                        && AgricultureRules.getPastoralFavorChancePercent(4) == 80,
                "Pastoral Favor must use the frozen 20/40/60/80 chances");
        require(AgricultureRules.rollPastoralFavor(4, new FixedRandom(79)), "Pastoral Favor IV succeeds below 80");
        require(!AgricultureRules.rollPastoralFavor(4, new FixedRandom(80)), "Pastoral Favor IV fails at 80");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                throw new AssertionError("test random exhausted for bound " + bound);
            }
            int value = values[index++];
            if (value < 0 || value >= bound) {
                throw new AssertionError("test random value " + value + " outside bound " + bound);
            }
            return value;
        }
    }
}
