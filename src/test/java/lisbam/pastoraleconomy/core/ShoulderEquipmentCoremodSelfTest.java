package lisbam.pastoraleconomy.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Exercises the release Coremod against the actual mapped 1.12.2 bytecode. */
public final class ShoulderEquipmentCoremodSelfTest {
    private static final String EQUIPMENT_HOOKS = "lisbam/pastoraleconomy/equipment/ShoulderEquipmentService";
    private static final String CONTAINER_HOOKS = "lisbam/pastoraleconomy/equipment/ShoulderEquipmentContainerHooks";
    private static final String CHEST_SLOT = "lisbam/pastoraleconomy/equipment/SlotChestArmorWithoutElytra";

    private ShoulderEquipmentCoremodSelfTest() {
    }

    public static void main(String[] args) throws IOException {
        ShoulderEquipmentTransformer transformer = new ShoulderEquipmentTransformer();
        verifyElytraLookup(transformer, "net.minecraft.entity.EntityLivingBase");
        verifyElytraLookup(transformer, "net.minecraft.client.entity.EntityPlayerSP");
        verifyElytraLookup(transformer, "net.minecraft.network.NetHandlerPlayServer");
        verifyContainer(transformer);
        verifyObfuscatedReleaseNames(transformer);
    }

    private static void verifyElytraLookup(ShoulderEquipmentTransformer transformer, String className)
            throws IOException {
        ClassNode node = transform(transformer, className);
        require(containsMethodCall(node, EQUIPMENT_HOOKS, "getShoulderElytraOrEmpty"),
                className + " must use the shoulder Elytra lookup hook");
    }

    private static void verifyContainer(ShoulderEquipmentTransformer transformer) throws IOException {
        ClassNode node = transform(transformer, "net.minecraft.inventory.ContainerPlayer");
        require(containsMethodCall(node, CONTAINER_HOOKS, "addShoulderSlot"),
                "ContainerPlayer must add the persistent shoulder slot");
        require(containsMethodCall(node, CONTAINER_HOOKS, "tryMoveElytraToShoulder"),
                "ContainerPlayer shift-click must route Elytra into the shoulder slot");
        boolean foundChestReplacement = false;
        for (org.objectweb.asm.tree.MethodNode method : node.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof TypeInsnNode && CHEST_SLOT.equals(((TypeInsnNode) instruction).desc)) {
                    foundChestReplacement = true;
                }
            }
        }
        require(foundChestReplacement, "ContainerPlayer chest armor slot must reject Elytra");
    }

    /**
     * LaunchWrapper invokes Coremods before its final name remap in a release
     * client.  Simulate the relevant 1.12.2 obfuscated member spellings so a
     * development-only MCP match can never silently remove the whole feature.
     */
    private static void verifyObfuscatedReleaseNames(ShoulderEquipmentTransformer transformer) throws IOException {
        verifyObfuscatedElytraLookup(transformer, "net.minecraft.entity.EntityLivingBase", "updateElytra", "r");
        verifyObfuscatedElytraLookup(transformer, "net.minecraft.client.entity.EntityPlayerSP", "onLivingUpdate", "n");
        verifyObfuscatedElytraLookup(transformer, "net.minecraft.network.NetHandlerPlayServer",
                "processEntityAction", "a");
        ClassNode container = transform(transformer, "net.minecraft.inventory.ContainerPlayer",
                "transferStackInSlot", "b", true);
        require(containsMethodCall(container, CONTAINER_HOOKS, "addShoulderSlot"),
                "obfuscated ContainerPlayer must add the persistent shoulder slot");
        require(containsMethodCall(container, CONTAINER_HOOKS, "tryMoveElytraToShoulder"),
                "obfuscated ContainerPlayer shift-click must route Elytra into the shoulder slot");
    }

    private static void verifyObfuscatedElytraLookup(ShoulderEquipmentTransformer transformer, String className,
                                                      String mappedMethodName, String obfuscatedMethodName)
            throws IOException {
        ClassNode node = transform(transformer, className, mappedMethodName, obfuscatedMethodName, false);
        require(containsMethodCall(node, EQUIPMENT_HOOKS, "getShoulderElytraOrEmpty"),
                className + " must patch the release-obfuscated Elytra lookup");
    }

    private static ClassNode transform(ShoulderEquipmentTransformer transformer, String className) throws IOException {
        byte[] transformed = transformer.transform(className, className, readClass(className));
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        return node;
    }

    private static ClassNode transform(ShoulderEquipmentTransformer transformer, String className,
                                       String mappedMethodName, String obfuscatedMethodName,
                                       boolean container) throws IOException {
        ClassNode input = new ClassNode();
        new ClassReader(readClass(className)).accept(input, 0);
        for (MethodNode method : input.methods) {
            if (!mappedMethodName.equals(method.name)) {
                continue;
            }
            method.name = obfuscatedMethodName;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof FieldInsnNode && "CHEST".equals(((FieldInsnNode) instruction).name)) {
                    ((FieldInsnNode) instruction).name = "e";
                }
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (container && "getSlotForItemStack".equals(call.name)) {
                        call.name = "d";
                    } else if (!container && "getItemStackFromSlot".equals(call.name)) {
                        call.name = "b";
                    }
                }
            }
            break;
        }
        ClassWriter writer = new ClassWriter(0);
        input.accept(writer);
        byte[] transformed = transformer.transform(className, className, writer.toByteArray());
        ClassNode output = new ClassNode();
        new ClassReader(transformed).accept(output, 0);
        return output;
    }

    private static boolean containsMethodCall(ClassNode node, String owner, String name) {
        for (org.objectweb.asm.tree.MethodNode method : node.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (owner.equals(call.owner) && name.equals(call.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static byte[] readClass(String className) throws IOException {
        InputStream input = ShoulderEquipmentCoremodSelfTest.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class");
        if (input == null) {
            throw new IOException("Missing mapped Minecraft class resource: " + className);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
