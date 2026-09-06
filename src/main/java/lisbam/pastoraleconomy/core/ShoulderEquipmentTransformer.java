package lisbam.pastoraleconomy.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Keeps the shoulder feature within the exact vanilla paths which decide
 * whether an Elytra may start or continue gliding and whether its player layer
 * renders. All injected hook descriptors use Object so MCP and SRG class names
 * never leak into the patch.
 */
public final class ShoulderEquipmentTransformer implements IClassTransformer, Opcodes {
    private static final String CONTAINER_HOOKS = "lisbam/pastoraleconomy/equipment/ShoulderEquipmentContainerHooks";
    private static final String EQUIPMENT_HOOKS = "lisbam/pastoraleconomy/equipment/ShoulderEquipmentService";
    private static final String CHEST_SLOT = "lisbam/pastoraleconomy/equipment/SlotChestArmorWithoutElytra";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) {
            return basicClass;
        }
        if ("net.minecraft.inventory.ContainerPlayer".equals(transformedName)) {
            return transformContainer(basicClass, transformedName);
        }
        if ("net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
            return transformElytraLookup(basicClass, transformedName, "updateElytra", "func_184616_r", "r");
        }
        if ("net.minecraft.client.entity.EntityPlayerSP".equals(transformedName)) {
            return transformElytraLookup(basicClass, transformedName, "onLivingUpdate", "func_70636_d", "n");
        }
        if ("net.minecraft.network.NetHandlerPlayServer".equals(transformedName)) {
            return transformElytraLookup(basicClass, transformedName, "processEntityAction", "func_147357_a", "a");
        }
        if ("net.minecraft.client.renderer.entity.layers.LayerElytra".equals(transformedName)) {
            return transformElytraLookup(basicClass, transformedName, "doRenderLayer", "func_177141_a", "a");
        }
        return basicClass;
    }

    private static byte[] transformContainer(byte[] basicClass, String transformedName) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        boolean replacedArmorSlot = replaceArmorSlots(node);
        boolean addedShoulderSlot = addShoulderSlot(node);
        boolean patchedShiftClick = patchElytraShiftClick(node);
        if (!replacedArmorSlot || !addedShoulderSlot || !patchedShiftClick) {
            FMLLog.log(Level.ERROR, "LisBam skipped the shoulder ContainerPlayer patch for %s "
                            + "(armor=%s, shoulder=%s, shift=%s).", transformedName,
                    Boolean.valueOf(replacedArmorSlot), Boolean.valueOf(addedShoulderSlot),
                    Boolean.valueOf(patchedShiftClick));
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean replaceArmorSlots(ClassNode node) {
        boolean replaced = false;
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() != INVOKESPECIAL || !"<init>".equals(call.name)
                        || !isVanillaArmorSlotConstructor(call.desc, node.name)) {
                    continue;
                }
                String oldOwner = call.owner;
                call.owner = CHEST_SLOT;
                for (AbstractInsnNode prior = instruction.getPrevious(); prior != null; prior = prior.getPrevious()) {
                    if (prior instanceof TypeInsnNode && prior.getOpcode() == NEW
                            && oldOwner.equals(((TypeInsnNode) prior).desc)) {
                        ((TypeInsnNode) prior).desc = CHEST_SLOT;
                        replaced = true;
                        break;
                    }
                }
            }
        }
        return replaced;
    }

    private static boolean isVanillaArmorSlotConstructor(String descriptor, String containerName) {
        if (!descriptor.startsWith("(L" + containerName + ";")) {
            return false;
        }
        int objectParameters = 0;
        for (Type type : Type.getArgumentTypes(descriptor)) {
            if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                objectParameters++;
            }
        }
        // ContainerPlayer$1 has outer ContainerPlayer, IInventory and equipment-slot parameters.
        return objectParameters == 3;
    }

    private static boolean addShoulderSlot(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name) || Type.getArgumentTypes(method.desc).length != 3) {
                continue;
            }
            boolean added = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction.getOpcode() != RETURN) {
                    continue;
                }
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new VarInsnNode(ALOAD, 3));
                hook.add(new MethodInsnNode(INVOKESTATIC, CONTAINER_HOOKS, "addShoulderSlot",
                        "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(instruction, hook);
                added = true;
            }
            return added;
        }
        return false;
    }

    private static boolean patchElytraShiftClick(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!isTransferStackMethod(method)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!isItemStackEquipmentSlotLookup(call)) {
                    continue;
                }
                AbstractInsnNode stackLoad = previousRealInstruction(instruction);
                if (!(stackLoad instanceof VarInsnNode) || stackLoad.getOpcode() != ALOAD) {
                    continue;
                }
                int stackLocal = ((VarInsnNode) stackLoad).var;
                org.objectweb.asm.tree.LabelNode continueVanilla = new org.objectweb.asm.tree.LabelNode();
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new VarInsnNode(ALOAD, stackLocal));
                hook.add(new VarInsnNode(ILOAD, 2));
                hook.add(new MethodInsnNode(INVOKESTATIC, CONTAINER_HOOKS, "tryMoveElytraToShoulder",
                        "(Ljava/lang/Object;Ljava/lang/Object;I)Z", false));
                hook.add(new org.objectweb.asm.tree.JumpInsnNode(IFEQ, continueVanilla));
                hook.add(new VarInsnNode(ALOAD, 3));
                hook.add(new org.objectweb.asm.tree.InsnNode(ARETURN));
                hook.add(continueVanilla);
                method.instructions.insertBefore(stackLoad, hook);
                return true;
            }
        }
        return false;
    }

    /** The release JVM sees obfuscated `b`, so identify transfer by signature and its unique static slot lookup. */
    private static boolean isTransferStackMethod(MethodNode method) {
        Type[] arguments = Type.getArgumentTypes(method.desc);
        return arguments.length == 2 && arguments[0].getSort() == Type.OBJECT
                && arguments[1].getSort() == Type.INT && Type.getReturnType(method.desc).getSort() == Type.OBJECT;
    }

    private static boolean isItemStackEquipmentSlotLookup(MethodInsnNode call) {
        if (call.getOpcode() != INVOKESTATIC) {
            return false;
        }
        Type[] arguments = Type.getArgumentTypes(call.desc);
        return arguments.length == 1 && arguments[0].getSort() == Type.OBJECT
                && Type.getReturnType(call.desc).getSort() == Type.OBJECT;
    }

    private static byte[] transformElytraLookup(byte[] basicClass, String transformedName,
                                                  String mcpMethodName, String srgMethodName,
                                                  String obfuscatedMethodName) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        int replacements = 0;
        for (MethodNode method : node.methods) {
            if (!mcpMethodName.equals(method.name) && !srgMethodName.equals(method.name)
                    && !obfuscatedMethodName.equals(method.name)) {
                continue;
            }
            replacements += replaceChestLookup(method);
        }
        if (replacements != 1) {
            FMLLog.log(Level.ERROR, "LisBam skipped the shoulder Elytra patch for %s "
                            + "(expected 1 chest lookup, found %d).", transformedName, Integer.valueOf(replacements));
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static int replaceChestLookup(MethodNode method) {
        int replacements = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            AbstractInsnNode chest = previousRealInstruction(instruction);
            if (!isChestStackLookup(call, chest)) {
                continue;
            }
            AbstractInsnNode store = nextRealInstruction(instruction);
            if (!(store instanceof VarInsnNode) || store.getOpcode() != ASTORE) {
                continue;
            }
            InsnList hook = entityLoadBeforeChest(chest);
            if (hook == null) {
                continue;
            }
            int local = ((VarInsnNode) store).var;
            hook.add(new VarInsnNode(ALOAD, local));
            hook.add(new MethodInsnNode(INVOKESTATIC, EQUIPMENT_HOOKS, "getShoulderElytraOrEmpty",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
            hook.add(new TypeInsnNode(CHECKCAST, Type.getReturnType(call.desc).getInternalName()));
            hook.add(new VarInsnNode(ASTORE, local));
            method.instructions.insert(store, hook);
            replacements++;
        }
        return replacements;
    }

    private static boolean isChestStackLookup(MethodInsnNode call, AbstractInsnNode chest) {
        return chest instanceof FieldInsnNode && chest.getOpcode() == GETSTATIC
                && ("CHEST".equals(((FieldInsnNode) chest).name) || "e".equals(((FieldInsnNode) chest).name))
                && ("getItemStackFromSlot".equals(call.name) || "func_184582_a".equals(call.name)
                || "b".equals(call.name))
                && Type.getArgumentTypes(call.desc).length == 1
                && Type.getReturnType(call.desc).getSort() == Type.OBJECT;
    }

    private static InsnList entityLoadBeforeChest(AbstractInsnNode chest) {
        AbstractInsnNode entitySource = previousRealInstruction(chest);
        InsnList result = new InsnList();
        if (entitySource instanceof VarInsnNode && entitySource.getOpcode() == ALOAD) {
            result.add(new VarInsnNode(ALOAD, ((VarInsnNode) entitySource).var));
            return result;
        }
        if (entitySource instanceof FieldInsnNode && entitySource.getOpcode() == GETFIELD) {
            AbstractInsnNode owner = previousRealInstruction(entitySource);
            if (owner instanceof VarInsnNode && owner.getOpcode() == ALOAD) {
                FieldInsnNode field = (FieldInsnNode) entitySource;
                result.add(new VarInsnNode(ALOAD, ((VarInsnNode) owner).var));
                result.add(new FieldInsnNode(GETFIELD, field.owner, field.name, field.desc));
                return result;
            }
        }
        return null;
    }

    private static AbstractInsnNode previousRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode current = instruction == null ? null : instruction.getPrevious();
        while (current != null && current.getOpcode() < 0) {
            current = current.getPrevious();
        }
        return current;
    }

    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode current = instruction == null ? null : instruction.getNext();
        while (current != null && current.getOpcode() < 0) {
            current = current.getNext();
        }
        return current;
    }
}
