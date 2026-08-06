package com.sherif.ledger.feature.transactions.presentation

import java.math.BigDecimal

data class TransactionsUiState(val groups: List<TransactionGroupUi>)

data class TransactionGroupUi(val id:String,val title:String,val summary:DaySummaryUi,val transactions:List<TransactionUi>)

data class DaySummaryUi(val spent:String,val income:String,val transactionCount:Int,val dominantCategory:MerchantCategory)

data class TransactionUi(val id:String,val merchant:String,val category:MerchantCategory,val amount:String,val subtitle:String,val isExpense:Boolean=true,val state:TransactionState=TransactionState.Posted)

enum class TransactionState{Posted,Pending,Recurring}
enum class MerchantCategory{Grocery,Shopping,Coffee,Fuel,Salary,Bills,Transport,Entertainment,Electronics,Food,Healthcare,Travel,Education}
