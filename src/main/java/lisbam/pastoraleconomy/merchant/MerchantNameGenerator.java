package lisbam.pastoraleconomy.merchant;

import java.util.Random;

/**
 * Generates display-only Chinese names for merchants. Merchant UUIDs remain
 * the sole logical identity and are never derived from these names.
 */
public final class MerchantNameGenerator {
    /** The ten highest-frequency Chinese surnames, with population-share weights in basis points. */
    private static final String COMMON_SURNAMES = "王李张刘陈杨黄赵吴周";
    private static final int[] COMMON_SURNAME_WEIGHTS = {794, 741, 707, 538, 453, 308, 267, 229, 205, 202};
    /** Remaining traditional Hundred Family Surnames form the lower-frequency tail. */
    private static final String TRADITIONAL_SURNAMES =
            "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧计伏成戴谈宋茅庞熊纪舒屈项祝董梁杜阮蓝闵席季麻强贾路娄危江童颜郭梅盛林刁钟徐邱骆高夏蔡田樊胡凌霍虞万支柯管卢莫房裘缪解应宗丁邓郁单杭洪包左石崔吉程嵇邢裴陆荣翁荀羊惠甄魏";
    private static final String TAIL_SURNAMES = buildTailSurnames();
    private static final int COMMON_SURNAME_WEIGHT_TOTAL = sumCommonSurnameWeights();
    private static final int TAIL_SURNAME_WEIGHT = Math.max(1,
            (10000 - COMMON_SURNAME_WEIGHT_TOTAL) / TAIL_SURNAMES.length());
    private static final int TOTAL_SURNAME_WEIGHT = COMMON_SURNAME_WEIGHT_TOTAL
            + TAIL_SURNAME_WEIGHT * TAIL_SURNAMES.length();
    /** Familiar personal-name characters; every code point is one BMP Han character. */
    private static final String GIVEN_NAME_CHARACTERS =
            "子安清河若云文远思雨晨曦明月星辰雅宁嘉禾春华秋实景行书言知远怀瑾修竹听澜沐阳佳禾亦凡可心雨桐诗涵宇轩梓涵浩然景天明轩乐天新知嘉树怀远玉兰蓁蓁婉清依依青禾向荣丰年晴川临风南山北辰安然清欢昭华承恩令仪知夏宁远景云清晏天佑宏达瑞丰宜年静姝素心望舒昭明";

    private MerchantNameGenerator() {
    }

    /** Creates one surname plus one or two personal-name characters. */
    public static String generate(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random source is required.");
        }
        int givenLength = 1 + random.nextInt(2);
        StringBuilder result = new StringBuilder(1 + givenLength);
        result.append(selectSurname(random));
        for (int index = 0; index < givenLength; index++) {
            result.append(GIVEN_NAME_CHARACTERS.charAt(random.nextInt(GIVEN_NAME_CHARACTERS.length())));
        }
        return result.toString();
    }

    /** Package-visible validation used by the deterministic regression check. */
    static boolean isGeneratedName(String name) {
        if (name == null || name.length() < 2 || name.length() > 3 || surnameWeight(name.charAt(0)) <= 0) {
            return false;
        }
        for (int index = 1; index < name.length(); index++) {
            if (GIVEN_NAME_CHARACTERS.indexOf(name.charAt(index)) < 0) {
                return false;
            }
        }
        return true;
    }

    static int surnameWeight(char surname) {
        int commonIndex = COMMON_SURNAMES.indexOf(surname);
        if (commonIndex >= 0) {
            return COMMON_SURNAME_WEIGHTS[commonIndex];
        }
        return TAIL_SURNAMES.indexOf(surname) >= 0 ? TAIL_SURNAME_WEIGHT : 0;
    }

    private static char selectSurname(Random random) {
        int ticket = random.nextInt(TOTAL_SURNAME_WEIGHT);
        for (int index = 0; index < COMMON_SURNAMES.length(); index++) {
            if (ticket < COMMON_SURNAME_WEIGHTS[index]) {
                return COMMON_SURNAMES.charAt(index);
            }
            ticket -= COMMON_SURNAME_WEIGHTS[index];
        }
        return TAIL_SURNAMES.charAt(ticket / TAIL_SURNAME_WEIGHT);
    }

    private static int sumCommonSurnameWeights() {
        int total = 0;
        for (int weight : COMMON_SURNAME_WEIGHTS) {
            total += weight;
        }
        return total;
    }

    private static String buildTailSurnames() {
        StringBuilder tail = new StringBuilder(TRADITIONAL_SURNAMES.length());
        for (int index = 0; index < TRADITIONAL_SURNAMES.length(); index++) {
            char surname = TRADITIONAL_SURNAMES.charAt(index);
            if (COMMON_SURNAMES.indexOf(surname) < 0 && tail.indexOf(String.valueOf(surname)) < 0) {
                tail.append(surname);
            }
        }
        return tail.toString();
    }
}
