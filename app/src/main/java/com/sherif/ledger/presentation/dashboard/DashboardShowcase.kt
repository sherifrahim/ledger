package com.sherif.ledger.presentation.dashboard

/**
 * Realistic placeholder data for the Milestone 1.5 design sprint.
 *
 * These are **not** real figures and there is no business logic behind them —
 * they exist only to let the flagship Dashboard be designed and screenshotted
 * with lifelike content while the underlying engines (Safe-to-Spend, Story
 * generation, forecast, recommendation) remain out of scope. Consumption is
 * gated behind `BuildConfig.DEBUG` in [DashboardScreen], so release builds never
 * render this. Replaced by real engine output in Milestone 2+.
 */
data class ShowcaseUpcoming(val name: String, val due: String, val amount: String)
data class ShowcaseInsight(val title: String, val value: String, val positive: Boolean)
data class ShowcaseAccount(val name: String, val type: String, val balance: String)
data class ShowcaseTxn(val merchant: String, val amount: String, val category: String, val isExpense: Boolean, val time: String)

object DashboardShowcase {
    const val safeToSpend = "AED 1,420"
    const val safeToSpendPeriod = "Until 31 July"
    const val safeToSpendProgress = 0.62f

    const val story =
        "You spent 18% less than last week. Your salary covered all recurring expenses, " +
        "and dining fell while transport rose."

    const val reviewCount = 3

    val upcoming = listOf(
        ShowcaseUpcoming("Netflix", "Due tomorrow", "AED 39"),
        ShowcaseUpcoming("Electricity Bill", "Due in 2 days", "AED 210"),
        ShowcaseUpcoming("DU Mobile", "Due in 5 days", "AED 155"),
    )

    val insights = listOf(
        ShowcaseInsight("Salary received", "Today", true),
        ShowcaseInsight("Credit utilization improved", "+8%", true),
        ShowcaseInsight("Emergency fund grew", "+3%", true),
    )

    val accounts = listOf(
        ShowcaseAccount("Emirates NBD", "Checking •• 1234", "AED 12,420"),
        ShowcaseAccount("Liv. Savings", "Savings •• 5678", "AED 6,850"),
        ShowcaseAccount("ADCB Credit Card", "Credit •• 9012", "-AED 3,120"),
    )

    val recent = listOf(
        ShowcaseTxn("Carrefour", "85.60", "Groceries", true, "Today"),
        ShowcaseTxn("Salary", "8,950.00", "Income", false, "Today"),
        ShowcaseTxn("Careem", "28.00", "Transport", true, "Yesterday"),
        ShowcaseTxn("Starbucks", "23.50", "Dining", true, "Yesterday"),
    )
}
