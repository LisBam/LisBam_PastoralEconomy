package lisbam.pastoraleconomy.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
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

    private static ClassNode transform(ShoulderEquipmentTransformer transformer, String className) throws IOException {
        byte[] transformed = transformer.transform(className, className, readClass(className));
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        return node;
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
