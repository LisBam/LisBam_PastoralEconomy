package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.entity.MerchantSkinCatalog;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/** Deterministic validation for merchant display-name generation and UTF-8 source data. */
public final class MerchantNameGeneratorSelfTest {
    private MerchantNameGeneratorSelfTest() {
    }

    public static void main(String[] args) {
        Random random = new Random(0x4C495342414D4CL);
        boolean sawOneCharacterGivenName = false;
        boolean sawTwoCharacterGivenName = false;
        for (int index = 0; index < 4096; index++) {
            String name = MerchantNameGenerator.generate(random);
            check(MerchantNameGenerator.isGeneratedName(name), "generated Chinese name " + index);
            check(name.equals(new String(name.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)),
                    "UTF-8 round trip " + index);
            if (name.length() == 2) {
                sawOneCharacterGivenName = true;
            } else if (name.length() == 3) {
                sawTwoCharacterGivenName = true;
            }
        }
        check(sawOneCharacterGivenName, "one-character given names");
        check(sawTwoCharacterGivenName, "two-character given names");
        check(MerchantNameGenerator.surnameWeight('王') == 794, "Wang population weight");
        check(MerchantNameGenerator.surnameWeight('李') == 741, "Li population weight");
        check(MerchantNameGenerator.surnameWeight('王') > MerchantNameGenerator.surnameWeight('赵'),
                "top surname order");
        check(MerchantNameGenerator.surnameWeight('赵') > MerchantNameGenerator.surnameWeight('钱'),
                "common surname exceeds traditional tail");
        check(MerchantNameGenerator.surnameWeight('不') == 0, "unknown surname rejected");
        check(MerchantSkinCatalog.isValid(MerchantSkinCatalog.FIRST_FARMER_SKIN), "first farmer skin index");
        check(MerchantSkinCatalog.isValid(MerchantSkinCatalog.LAST_FARMER_SKIN), "last farmer skin index");
        check(!MerchantSkinCatalog.isValid(0) && !MerchantSkinCatalog.isValid(-1)
                        && !MerchantSkinCatalog.isValid(MerchantSkinCatalog.LAST_FARMER_SKIN + 1),
                "skin index bounds");
        System.out.println("merchantNameSelfTest PASS");
    }

    private static void check(boolean value, String description) {
        if (!value) {
            throw new AssertionError("Failed: " + description);
        }
    }
}
