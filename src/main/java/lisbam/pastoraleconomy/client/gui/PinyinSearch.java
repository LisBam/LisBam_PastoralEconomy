package lisbam.pastoraleconomy.client.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Client-side Chinese display-name matcher. The compact local table covers
 * the Chinese characters present in vanilla item/block labels and this mod's
 * localisation, so searching does not require a dependency or a server call.
 */
final class PinyinSearch {
    private static final String PINYIN_CHARACTERS =
            "一丁三上下不与丑世丛丝严个中丰丹为主久义之乐书买了云交人今仙令以价伐休传位余作你使侦信僵充光免兔入公兰内农冰准凋凝出刀制刷前剑剩剪副力务动助势包化十半单南卜占历压原发取变只可台史右叶合名后向告周命品售唱商喷器囊园围图圆土在地场坏坐块型基塞境墙墨壳备复外夜大太失头奇套奶始子字安定宝宰容射小尸屏展屠山岗岩工左已币市帜带帽干平庄床底度开弓张弹当录形影往径待循微徽态性怪总恢息恶情感成我或戴手才打找把投护拉拴拼持指按挡捕捞损掉掌排探接掷搜支收放效数斗料斜斧新方旗无日时易星昨晚晶暂暗曜曲更最有服木未末朵机杆杉村条板构林果枯架染查栅标栏树根格框桦桶梗梯棍棒棕植楼横橙橡欢正步死毒比毛毯气水汤沙河法波泥泪活派浆测济浮海消涨液淡深混渐游湿溅滑滞滨漏潜激灌火灯灰灵炉炭点炼烁烈烛烟烤焰煤煲熔熟燧爆爬片版牌牛牡物状狱猪玩玫环现玻珀珍珠球理瑰璃瓜瓦瓶甘甜生用田甲画界留疣疾白的皮盆盒盔盾眷眼睡知石矿砂砖砧破砾础确碎碗磨示禁离种称移稳空穿竖站端竹竿笔符笼等签算管箭箱粉粒粘精糕糖索紫红纸纹线终绊经绒结络继续绳维绵绿网置羊美羽翅者耐耕聆肉胡胸能脚腐腾腿膏自至船色节花苔苗英苹茜草荡荧药莲获菇菊菌菜菱萝落葱葵蒲蓝蔓蔗蕨薯藤蘑虞蛋蛛蜘蟹行被裂裤西视觉角计认记设识读豆豚质购费趋足跌距踪身车轨轮轻较辅输边运近这远连迹送通造遗郁酵酿釉重量金錾针钓钟钩钮钻铁铃铠链锁锄锅锭锹镐长门闪闲间阱阳阶阻附限陶陷障雪需霜青面革靴鞍鞘音顶顾颂预颅颜飞首香马骨骷髅高魂魔鱼鲑鸡麦黄黑龙";
    private static final String PINYIN_READINGS =
            "yi ding san shang xia bu yu chou shi cong si yan ge zhong feng dan wei zhu jiu yi zhi le shu mai le yun jiao ren jin xian ling yi jia fa xiu chuan wei yu zuo ni shi zhen xin jiang chong guang mian tu ru gong lan nei nong bing zhun diao ning chu dao zhi shua qian jian sheng jian fu li wu dong zhu shi bao hua shi ban dan nan bu zhan li ya yuan fa qu bian zhi ke tai shi you ye he ming hou xiang gao zhou ming pin shou chang shang pen qi nang yuan wei tu yuan tu zai di chang huai zuo kuai xing ji sai jing qiang mo qiao bei fu wai ye da tai shi tou qi tao nai shi zi zi an ding bao zai rong she xiao shi ping zhan tu shan gang yan gong zuo yi bi shi zhi dai mao gan ping zhuang chuang di du kai gong zhang dan dang lu xing ying wang jing dai xun wei hui tai xing guai zong hui xi e qing gan cheng wo huo dai shou cai da zhao ba tou hu la shuan pin chi zhi an dang bu lao sun diao zhang pai tan jie zhi sou zhi shou fang xiao shu dou liao xie fu xin fang qi wu ri shi yi xing zuo wan jing zan an yao qu geng zui you fu mu wei mo duo ji gan shan cun tiao ban gou lin guo ku jia ran cha zha biao lan shu gen ge kuang hua tong geng ti gun bang zong zhi lou heng cheng xiang huan zheng bu si du bi mao tan qi shui tang sha he fa bo ni lei huo pai jiang ce ji fu hai xiao zhang ye dan shen hun jian you shi jian hua zhi bin lou qian ji guan huo deng hui ling lu tan dian lian shuo lie zhu yan kao yan mei bao rong shu sui bao pa pian ban pai niu mu wu zhuang yu zhu wan mei huan xian bo po zhen zhu qiu li gui li gua wa ping gan tian sheng yong tian jia hua jie liu you ji bai de pi pen he kui dun juan yan shui zhi shi kuang sha zhuan zhen po li chu que sui wan mo shi jin li zhong cheng yi wen kong chuan shu zhan duan zhu gan bi fu long deng qian suan guan jian xiang fen li zhan jing gao tang suo zi hong zhi wen xian zhong ban jing rong jie luo ji xu sheng wei mian lv wang zhi yang mei yu chi zhe nai geng ling rou hu xiong neng jiao fu teng tui gao zi zhi chuan se jie hua tai miao ying ping qian cao dang ying yao lian huo gu ju jun cai ling luo luo cong kui pu lan man zhe jue shu teng mo yu dan zhu zhi xie xing bei lie ku xi shi jue jiao ji ren ji she shi du dou tun zhi gou fei qu zu die ju zong shen che gui lun qing jiao fu shu bian yun jin zhe yuan lian ji song tong zao yi yu jiao niang you zhong liang jin zan zhen diao zhong gou niu zuan tie ling kai lian suo chu guo ding qiao hao zhang men shan xian jian jing yang jie zu fu xian tao xian zhang xue xu shuang qing mian ge xue an qiao yin ding gu song yu lu yan fei shou xiang ma gu ku lou gao hun mo yu gui ji mai huang hei long";
    private static final Map<Character, String> PINYIN = createPinyinMap();
    private static final Map<String, SearchTokens> TOKEN_CACHE = new HashMap<String, SearchTokens>();

    private PinyinSearch() {
    }

    static boolean matches(String displayName, String rawQuery) {
        if (displayName == null || rawQuery == null) {
            return false;
        }
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return false;
        }
        if (displayName.toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        String pinyinQuery = normalizePinyinQuery(query);
        return !pinyinQuery.isEmpty() && matchesPinyin(getTokens(displayName), pinyinQuery);
    }

    static String fullPinyin(String displayName) {
        return getTokens(displayName).fullPinyin;
    }

    private static Map<Character, String> createPinyinMap() {
        String[] readings = PINYIN_READINGS.split(" ");
        if (PINYIN_CHARACTERS.length() != readings.length) {
            throw new IllegalStateException("Pinyin dictionary entries are incomplete.");
        }
        Map<Character, String> result = new HashMap<Character, String>();
        for (int index = 0; index < PINYIN_CHARACTERS.length(); index++) {
            result.put(PINYIN_CHARACTERS.charAt(index), readings[index]);
        }
        return Collections.unmodifiableMap(result);
    }

    private static SearchTokens getTokens(String displayName) {
        SearchTokens cached = TOKEN_CACHE.get(displayName);
        if (cached != null) {
            return cached;
        }
        StringBuilder full = new StringBuilder(displayName.length() * 3);
        StringBuilder initials = new StringBuilder(displayName.length());
        String[] readings = new String[displayName.length()];
        int count = 0;
        for (int index = 0; index < displayName.length(); index++) {
            char character = displayName.charAt(index);
            String pinyin = PINYIN.get(character);
            if (pinyin == null && Character.isLetterOrDigit(character)) {
                pinyin = String.valueOf(Character.toLowerCase(character));
            }
            if (pinyin != null) {
                full.append(pinyin);
                initials.append(pinyin.charAt(0));
                readings[count++] = pinyin;
            }
        }
        String[] compactReadings = new String[count];
        System.arraycopy(readings, 0, compactReadings, 0, count);
        SearchTokens tokens = new SearchTokens(full.toString(), initials.toString(), compactReadings);
        TOKEN_CACHE.put(displayName, tokens);
        return tokens;
    }

    private static boolean matchesPinyin(SearchTokens tokens, String query) {
        if (tokens.fullPinyin.contains(query) || tokens.initials.contains(query)) {
            return true;
        }
        for (int start = 0; start < tokens.readings.length; start++) {
            int queryOffset = 0;
            for (int index = start; index < tokens.readings.length && queryOffset < query.length(); index++) {
                String reading = tokens.readings[index];
                if (query.startsWith(reading, queryOffset)) {
                    queryOffset += reading.length();
                } else if (query.charAt(queryOffset) == reading.charAt(0)) {
                    queryOffset++;
                } else {
                    break;
                }
            }
            if (queryOffset == query.length()) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePinyinQuery(String query) {
        StringBuilder normalized = new StringBuilder(query.length());
        for (int index = 0; index < query.length(); index++) {
            char character = query.charAt(index);
            if ((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9')) {
                normalized.append(character);
            } else if (character == 'ü' || character == 'ǖ' || character == 'ǘ'
                    || character == 'ǚ' || character == 'ǜ') {
                normalized.append('v');
            }
        }
        return normalized.toString();
    }

    private static final class SearchTokens {
        private final String fullPinyin;
        private final String initials;
        private final String[] readings;

        private SearchTokens(String fullPinyin, String initials, String[] readings) {
            this.fullPinyin = fullPinyin;
            this.initials = initials;
            this.readings = readings;
        }
    }
}
