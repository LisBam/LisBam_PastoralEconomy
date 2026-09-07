package lisbam.pastoraleconomy.merchant;

/** Integer rounding and overflow guards for emerald spot-market settlement. */
public final class EmeraldTradeRulesSelfTest {
    private EmeraldTradeRulesSelfTest() {
    }

    public static void main(String[] args) {
        check(EmeraldTradeRules.calculateBuyCost(1000L, 10) == 10400L, "1000 x 10 buy fee");
        check(EmeraldTradeRules.calculateSellIncome(1000L, 10) == 9600L, "1000 x 10 sell fee");
        check(EmeraldTradeRules.calculateBuyCost(1247L, 10) == 12969L, "buy cost rounds up");
        check(EmeraldTradeRules.calculateSellIncome(1247L, 10) == 11971L, "sell income rounds down");
        check(EmeraldTradeRules.calculateFee(12470L) == 499L, "displayed settlement fee is exact");
        check(EmeraldTradeRules.getMaximumAffordableAmount(10400L, 1000L, 4096) == 10,
                "maximum buy includes the 4 percent fee");
        check(EmeraldTradeRules.getMaximumAffordableAmount(10399L, 1000L, 4096) == 9,
                "insufficient balance cannot buy the next emerald");
        check(EmeraldTradeRules.calculateBuyCost(Long.MAX_VALUE, 2) < 0L,
                "overflowing request is rejected before settlement");
        System.out.println("emeraldTradeRulesSelfTest PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
