#!/bin/bash
set -e

B=app/src/main/java/com/sherif/ledger

echo "Applying Sprint 2A — Transactions domain & presentation state..."

mkdir -p "$B/feature/transactions/presentation"
mkdir -p "$B/feature/transactions/presentation/preview"

cat > "$B/feature/transactions/presentation/TransactionsUiState.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation

import java.math.BigDecimal

data class TransactionsUiState(val groups: List<TransactionGroupUi>)

data class TransactionGroupUi(val id:String,val title:String,val summary:DaySummaryUi,val transactions:List<TransactionUi>)

data class DaySummaryUi(val spent:BigDecimal,val income:BigDecimal,val transactionCount:Int,val dominantCategory:MerchantCategory)

data class TransactionUi(val id:String,val merchant:String,val category:MerchantCategory,val amount:BigDecimal,val subtitle:String,val state:TransactionState=TransactionState.Posted)

enum class TransactionState{Posted,Pending,Recurring}
enum class MerchantCategory{Grocery,Shopping,Coffee,Fuel,Salary,Bills,Transport,Entertainment,Electronics,Food,Healthcare,Travel,Education}
EOF

cat > "$B/feature/transactions/presentation/preview/TransactionsPreviewData.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.preview

import com.sherif.ledger.feature.transactions.presentation.*
import java.math.BigDecimal

object TransactionsPreviewData{
val state=TransactionsUiState(listOf(
TransactionGroupUi("today","Today",DaySummaryUi(BigDecimal("197"),BigDecimal.ZERO,Currency.AED,3,MerchantCategory.Grocery),listOf(
TransactionUi("amazon","Amazon",MerchantCategory.Shopping,BigDecimal("52"),Currency.AED,"2:14 PM"),
TransactionUi("carrefour","Carrefour",MerchantCategory.Grocery,BigDecimal("126"),Currency.AED,"11:02 AM"),
TransactionUi("costa","Costa Coffee",MerchantCategory.Coffee,BigDecimal("19"),Currency.AED,"9:30 AM")
)),
TransactionGroupUi("yesterday","Yesterday",DaySummaryUi(BigDecimal("397"),BigDecimal.ZERO,Currency.AED,3,MerchantCategory.Fuel),listOf(
TransactionUi("adnoc","ADNOC",MerchantCategory.Fuel,BigDecimal("120"),Currency.AED,"3:15 PM"),
TransactionUi("noon","Noon",MerchantCategory.Shopping,BigDecimal("245"),Currency.AED,"1:20 PM",TransactionState.Pending),
TransactionUi("uber","Uber",MerchantCategory.Transport,BigDecimal("32"),Currency.AED,"8:45 AM")
)),
TransactionGroupUi("2026-06-23","23 Jun",DaySummaryUi(BigDecimal("454"),BigDecimal("9500"),Currency.AED,3,MerchantCategory.Salary),listOf(
TransactionUi("salary","Salary",MerchantCategory.Salary,BigDecimal("9500"),Currency.AED,"Bank transfer"),
TransactionUi("netflix","Netflix",MerchantCategory.Entertainment,BigDecimal("55"),Currency.AED,"Monthly",TransactionState.Recurring),
TransactionUi("du","du",MerchantCategory.Bills,BigDecimal("399"),Currency.AED,"Monthly",TransactionState.Recurring)
))
))}
EOF

echo "Sprint 2A applied."
echo "Run: ./gradlew assembleDebug"
