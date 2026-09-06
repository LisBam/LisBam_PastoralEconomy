package lisbam.pastoraleconomy.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;

import java.util.Random;

/** Exact animal whitelist and random rolls for the additional bone death drop. */
final class AnimalBoneDropRules {
    private static final int PROBABILITY_DENOMINATOR = 300;

    private AnimalBoneDropRules() {
    }

    static int rollDropCount(EntityLivingBase animal, int lootingLevel, Random random) {
        int baseCount = rollBaseCount(animal, random);
        if (baseCount <= 0) {
            return 0;
        }
        return applyLootingBonus(baseCount, lootingLevel, random);
    }

    static int rollLargeAnimalBaseCount(Random random) {
        int roll = random.nextInt(PROBABILITY_DENOMINATOR);
        if (roll < 40) {
            return 2;
        }
        return roll < 140 ? 1 : 0;
    }

    static int rollSmallAnimalBaseCount(Random random) {
        return random.nextInt(PROBABILITY_DENOMINATOR) < 50 ? 1 : 0;
    }

    static int applyLootingBonus(int baseCount, int lootingLevel, Random random) {
        if (baseCount <= 0 || lootingLevel <= 0) {
            return baseCount;
        }
        return baseCount + random.nextInt(lootingLevel + 1);
    }

    private static int rollBaseCount(EntityLivingBase animal, Random random) {
        if (isLargeAnimal(animal)) {
            return rollLargeAnimalBaseCount(random);
        }
        return isSmallAnimal(animal) ? rollSmallAnimalBaseCount(random) : 0;
    }

    private static boolean isLargeAnimal(EntityLivingBase animal) {
        Class<?> animalClass = animal.getClass();
        return animalClass == EntityCow.class
                || animalClass == EntityMooshroom.class
                || animalClass == EntityPig.class
                || animalClass == EntitySheep.class
                || animalClass == EntityHorse.class
                || animalClass == EntityDonkey.class
                || animalClass == EntityMule.class
                || animalClass == EntityLlama.class;
    }

    private static boolean isSmallAnimal(EntityLivingBase animal) {
        Class<?> animalClass = animal.getClass();
        return animalClass == EntityChicken.class
                || animalClass == EntityRabbit.class
                || animalClass == EntityWolf.class
                || animalClass == EntityOcelot.class
                || animalClass == EntityParrot.class;
    }
}
