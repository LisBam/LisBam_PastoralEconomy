package lisbam.pastoraleconomy.client.gui;

/** Deterministic client-only pinyin matching checks for the market-book search field. */
public final class PinyinSearchSelfTest {
    private PinyinSearchSelfTest() {
    }

    public static void main(String[] args) {
        check("xiaomai".equals(PinyinSearch.fullPinyin("小麦")), "small wheat full pinyin");
        check(PinyinSearch.matches("小麦", "xiaomai"), "full pinyin search");
        check(PinyinSearch.matches("小麦", "xm"), "initial pinyin search");
        check(PinyinSearch.matches("附魔书", "fumoshu"), "enchanted book full pinyin search");
        check(PinyinSearch.matches("耕地行者", "gdxz"), "farmland walker initial search");
        check(PinyinSearch.matches("百炼如新", "blrx"), "new enchantment initial search");
        check(PinyinSearch.matches("绿宝石", "lvbaoshi"), "umlaut pinyin normalization");
        check(PinyinSearch.matches("绿宝石", "lübs"), "umlaut initial pinyin normalization");
        check(!PinyinSearch.matches("小麦", "zuanshi"), "unrelated pinyin must not match");
        System.out.println("pinyinSearchSelfTest PASS");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }
}
