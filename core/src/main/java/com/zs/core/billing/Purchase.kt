package com.zs.core.billing

/**
 * Represents a completed or pending purchase transaction.
 *
 * This class encapsulates the essential metadata returned by the billing system
 * when a user buys a product. It provides identifiers, state information, and
 * acknowledgement status required for entitlement management.
 *
 * @property id Unique identifier of the purchase transaction. Used to correlate
 *              with backend records and entitlement checks.
 *
 * @property state Integer code representing the current state of the purchase.
 *                 Indicates whether the transaction is pending, completed, or
 *                 cancelled.
 *
 * @property purchased Flag indicating whether the item has been successfully
 *                     purchased. True if the transaction is complete.
 *
 * @property isAcknowledged Internal flag indicating whether the purchase has
 *                          been acknowledged by the app. Required to comply
 *                          with billing system rules.
 *
 * @property quantity Number of units purchased in this transaction. Typically
 *                    1 for most products, but may vary for consumables.
 *
 * @property time Epoch timestamp (milliseconds) when the purchase was made.
 *                Useful for subscription validation and history display.
 */
class Purchase(
    val id: String,
    val state: Int,
    val quantity: Int = -1,
    val time: Long = -1,
) {
}

/**
 * @return `true` if this [Purchase] object is `non-null`, has been `acknowledged`, and is in the
 * [GP_Purchase.PurchaseState.PURCHASED] state. Returns `false` otherwise.
 */
val Purchase?.purchased get() = if (this == null) false else state == Paymaster.STATE_ACKNOWLEDGED
