package lisbam.pastoraleconomy.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.apache.logging.log4j.Level;

/**
 * Patches only the two Forge hooks that otherwise reject vanilla shears and
 * hoes before enchanting-table candidates are generated.
 */
public final class EnchantingTableTransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "lisbam/pastoraleconomy/core/EnchantingCompatibilityHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !"net.minecraft.item.Item".equals(transformedName)) {
            return basicClass;
        }
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        boolean hasModernEnchantability = hasModernEnchantabilityMethod(node);
        boolean hasModernEfficiencyGate = hasModernEfficiencyGate(node);
        boolean patchedEnchantability;
        boolean patchedEfficiency;
        if (hasModernEnchantability || hasModernEfficiencyGate) {
            patchedEnchantability = hasModernEnchantability && patchEnchantability(node);
            patchedEfficiency = hasModernEfficiencyGate && patchNativeEfficiencyGate(node);
        } else {
            // Forge 14.23.5.2847 has neither ItemStack overload. Its table
            // rolls use Item#getItemEnchantability() plus
            // Enchantment#canApply(ItemStack), where vanilla Efficiency
            // already explicitly accepts shears.
            patchedEnchantability = patchLegacyEnchantability(node);
            patchedEfficiency = patchedEnchantability;
        }
        if (!patchedEnchantability || !patchedEfficiency) {
            // Forge 1.12.2 runs this Coremod before all later gameplay code. A
            // mapping variation must leave vanilla Item intact rather than make
            // the whole client/server unable to launch.
            FMLLog.log(Level.ERROR, "LisBam skipped the enchanting-table compatibility patch for %s "
                            + "(enchantability=%s, efficiency=%s).",
                    node.name, Boolean.valueOf(patchedEnchantability), Boolean.valueOf(patchedEfficiency));
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean patchEnchantability(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!isModernEnchantabilityMethod(method)) {
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
            method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, vanillaMethod, "()I", false));
            method.instructions.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "getItemEnchantability",
                    "(Ljava/lang/Object;I)I", false));
            method.instructions.add(new InsnNode(IRETURN));
            method.maxStack = 0;
            method.maxLocals = 0;
            return true;
        }
        return false;
    }

    private static boolean patchLegacyEnchantability(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!isLegacyEnchantabilityMethod(method)) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(ALOAD, 0));
            // The 1.12.2 Item base implementation is the zero-enchantability
            // default. Item-specific overrides remain untouched.
            hook.add(new InsnNode(ICONST_0));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "getItemEnchantability",
                    "(Ljava/lang/Object;I)I", false));
            hook.add(new InsnNode(IRETURN));
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.instructions.add(hook);
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
                if (call.getOpcode() == INVOKEVIRTUAL && "()I".equals(call.desc)) {
                    return call.name;
                }
            }
        }
        return null;
    }

    private static boolean patchNativeEfficiencyGate(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!isModernEfficiencyGate(method)) {
                continue;
            }
            LabelNode continueVanilla = new LabelNode();
            InsnList gate = new InsnList();
            gate.add(new VarInsnNode(ALOAD, 0));
            gate.add(new VarInsnNode(ALOAD, 2));
            gate.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "isShearsEfficiency",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
            gate.add(new JumpInsnNode(IFEQ, continueVanilla));
            gate.add(new InsnNode(ICONST_1));
            gate.add(new InsnNode(IRETURN));
            gate.add(continueVanilla);
            method.instructions.insert(gate);
            return true;
        }
        return false;
    }

    private static boolean hasModernEnchantabilityMethod(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (isModernEnchantabilityMethod(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasModernEfficiencyGate(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (isModernEfficiencyGate(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isModernEnchantabilityMethod(MethodNode method) {
        return "getItemEnchantability".equals(method.name)
                && Type.getArgumentTypes(method.desc).length == 1
                && Type.getReturnType(method.desc).getSort() == Type.INT;
    }

    private static boolean isModernEfficiencyGate(MethodNode method) {
        return "canApplyAtEnchantingTable".equals(method.name)
                && Type.getArgumentTypes(method.desc).length == 2
                && Type.getReturnType(method.desc).getSort() == Type.BOOLEAN;
    }

    private static boolean isLegacyEnchantabilityMethod(MethodNode method) {
        return ("getItemEnchantability".equals(method.name) || "func_77619_b".equals(method.name))
                && Type.getArgumentTypes(method.desc).length == 0
                && Type.getReturnType(method.desc).getSort() == Type.INT;
    }
}
