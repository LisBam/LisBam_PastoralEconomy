package lisbam.pastoraleconomy.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patches only the two Forge hooks that otherwise reject vanilla shears and
 * hoes before enchanting-table candidates are generated.
 */
public final class EnchantingTableTransformer implements IClassTransformer, Opcodes {
    private static final String ITEM = "net/minecraft/item/Item";
    private static final String ITEM_STACK = "net/minecraft/item/ItemStack";
    private static final String ENCHANTMENT = "net/minecraft/enchantment/Enchantment";
    private static final String SHEARS = "net/minecraft/item/ItemShears";
    private static final String EFFICIENCY = "net/minecraft/enchantment/EnchantmentDigging";
    private static final String ENCHANTABILITY_DESC = "(L" + ITEM_STACK + ";)I";
    private static final String APPLY_DESC = "(L" + ITEM_STACK + ";L" + ENCHANTMENT + ";)Z";
    private static final String HOOKS = "lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !"net.minecraft.item.Item".equals(transformedName)) {
            return basicClass;
        }
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        boolean patchedEnchantability = patchEnchantability(node);
        boolean patchedEfficiency = patchNativeEfficiencyGate(node);
        if (!patchedEnchantability || !patchedEfficiency) {
            throw new IllegalStateException("Unable to apply LisBam 1.12.2 enchanting compatibility patch.");
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean patchEnchantability(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"getItemEnchantability".equals(method.name) || !ENCHANTABILITY_DESC.equals(method.desc)) {
                continue;
            }
            String vanillaMethod = findVanillaEnchantabilityMethod(method);
            if (vanillaMethod == null) {
                return false;
            }
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new VarInsnNode(ALOAD, 1));
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, ITEM, vanillaMethod, "()I", false));
            method.instructions.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "getItemEnchantability",
                    "(L" + ITEM + ";L" + ITEM_STACK + ";I)I", false));
            method.instructions.add(new InsnNode(IRETURN));
            method.maxStack = 0;
            method.maxLocals = 0;
            return true;
        }
        return false;
    }

    private static String findVanillaEnchantabilityMethod(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (ITEM.equals(call.owner) && "()I".equals(call.desc)) {
                    return call.name;
                }
            }
        }
        return null;
    }

    private static boolean patchNativeEfficiencyGate(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"canApplyAtEnchantingTable".equals(method.name) || !APPLY_DESC.equals(method.desc)) {
                continue;
            }
            LabelNode continueVanilla = new LabelNode();
            InsnList gate = new InsnList();
            gate.add(new VarInsnNode(ALOAD, 0));
            gate.add(new TypeInsnNode(INSTANCEOF, SHEARS));
            gate.add(new JumpInsnNode(IFEQ, continueVanilla));
            gate.add(new VarInsnNode(ALOAD, 2));
            gate.add(new TypeInsnNode(INSTANCEOF, EFFICIENCY));
            gate.add(new JumpInsnNode(IFEQ, continueVanilla));
            gate.add(new InsnNode(ICONST_1));
            gate.add(new InsnNode(IRETURN));
            gate.add(continueVanilla);
            method.instructions.insert(gate);
            return true;
        }
        return false;
    }
}
