package com.zs.core.billing

/**
 * Represents a product available for purchase in the application.
 *
 * This class encapsulates the essential metadata required to identify,
 * display, and transact a product. Instances are constructed internally
 * from billing system data and should not be created directly by clients.
 *
 * @property id Unique identifier of the product, defined in the Billing API. Used to distinguish
 *              products during purchase and entitlement checks.
 *
 * @property title Localized title of the product, suitable for display
 *                 in the user interface. Typically includes the product
 *                 name as shown to the user.
 *
 * @property formattedPrice Localized string representation of the product’s
 *                          price, including currency symbol. May be `null`
 *                          if the product is free or pricing information
 *                          is not available.
 *
 * @property description Localized description of the product, providing
 *                       additional detail about its features, benefits,
 *                       or usage. Intended for display alongside the title
 *                       and price in the storefront UI.
 */
class Product internal constructor(
    internal val value: Any? = null,
    val id: String,
    val title: String,
    val formattedPrice: String?,
    val description: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (id != other.id) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (formattedPrice != other.formattedPrice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (formattedPrice?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Product(id='$id', title='$title', description='$description', formattedPrice=$formattedPrice)"
    }
}