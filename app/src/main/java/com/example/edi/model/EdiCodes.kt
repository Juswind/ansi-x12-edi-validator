package com.example.edi.model

object EdiCodes {
    val SUPPORTED_TRANSACTIONS = mapOf(
        "110" to "Air Freight Details and Invoice",
        "210" to "Motor Carrier Freight Details and Invoice",
        "214" to "Transportation Carrier Shipment Status Message",
        "310" to "Ocean Freight Details and Invoice",
        "315" to "Ocean Status Details (Ocean Container Event)",
        "850" to "Purchase Order",
        "856" to "Ship Notice/Manifest (ASN)"
    )

    val SEGMENT_NAMES = mapOf(
        "ISA" to "Interchange Control Header",
        "IEA" to "Interchange Control Trailer",
        "GS" to "Functional Group Header",
        "GE" to "Functional Group Trailer",
        "ST" to "Transaction Set Header",
        "SE" to "Transaction Set Trailer",
        "BEG" to "Beginning Segment for Purchase Order",
        "CUR" to "Currency",
        "REF" to "Reference Identification",
        "PER" to "Administrative Communications Contact",
        "FOB" to "F.O.B. Related Instructions",
        "SAC" to "Service, Promotion, Allowance, or Charge Information",
        "ITD" to "Terms of Sale/Deferred Terms of Sale",
        "DTM" to "Date/Time Reference",
        "TD1" to "Carrier Details (Quantity and Weight)",
        "TD5" to "Carrier Details (Routing Sequence/Transit Time)",
        "N1" to "Name / Party Identification",
        "N2" to "Additional Name Information",
        "N3" to "Address Information",
        "N4" to "Geographic Location",
        "PO1" to "Baseline Item Data (Purchase Order)",
        "PID" to "Product/Item Description",
        "MEA" to "Measurements",
        "PO4" to "Item Physical Details",
        "CTT" to "Transaction Totals",
        "AMT" to "Monetary Amount",
        "BSN" to "Beginning Segment for Ship Notice",
        "HL" to "Hierarchical Level",
        "LIN" to "Item Identification",
        "SN1" to "Item Detail (Shipment)",
        "PRF" to "Purchase Order Reference",
        "B10" to "Beginning Segment for Transportation Carrier Shipment Status",
        "L11" to "Business Instructions and Reference Number",
        "MS3" to "Intermodal Chassis Equipment",
        "LX" to "Assigned Number",
        "AT7" to "Shipment Status Details",
        "MS1" to "Equipment, Shipment, or Real Property Location",
        "MS2" to "Sales Representative",
        "AT8" to "Shipment Weight, Packaging and Quantity Data",
        "B3" to "Beginning Segment for Carrier's Invoice",
        "C3" to "Currency",
        "N9" to "Reference Identification",
        "V1" to "Vessel Identification",
        "B4" to "Beginning Segment for Inquiry or Arrival Notification",
        "Q2" to "Status Details (Ocean)",
        "R4" to "Port or Terminal",
        "L0" to "Line Item - Quantity and Weight",
        "L1" to "Rate and Charges",
        "L3" to "Total Weight and Charges",
        "L5" to "Description, Marks and Numbers"
    )

    val PARTY_ROLES = mapOf(
        "BY" to "Buyer",
        "VN" to "Vendor / Seller",
        "ST" to "Ship To",
        "SF" to "Ship From",
        "BT" to "Bill To",
        "SH" to "Shipper",
        "CN" to "Consignee",
        "CA" to "Carrier",
        "PR" to "Payer",
        "SU" to "Supplier",
        "WH" to "Warehouse"
    )

    val STATUS_214_CODES = mapOf(
        "AF" to "Departed Terminal Location",
        "AG" to "Estimated Delivery",
        "AV" to "Available for Delivery",
        "CD" to "Carrier Departed Delivery Location",
        "D1" to "Completed Unloading / Delivered",
        "X1" to "Arrived at Exception Location",
        "X3" to "Arrived at Terminal",
        "X6" to "En Route to Delivery Location",
        "SD" to "Shipment Delayed",
        "P1" to "Departed Pick-up Location"
    )

    val STATUS_315_CODES = mapOf(
        "OA" to "Ocean Arrival",
        "VD" to "Vessel Departure",
        "UV" to "Unloaded From Vessel",
        "VA" to "Vessel Arrival",
        "AL" to "Loaded On Vessel",
        "RD" to "Empty Equipment Returned",
        "CO" to "Cargo Available For Pickup",
        "CT" to "Customs Released"
    )

    val PO_TYPE_CODES = mapOf(
        "SA" to "Stand-alone Order",
        "BK" to "Blanket Order",
        "DS" to "Drop Ship",
        "NE" to "New Order",
        "RL" to "Release or Delivery Order",
        "RO" to "Rush Order"
    )

    val PURPOSE_CODES = mapOf(
        "00" to "Original",
        "01" to "Cancellation",
        "04" to "Change",
        "05" to "Replace",
        "07" to "Duplicate"
    )

    fun getSegmentName(tag: String): String = SEGMENT_NAMES[tag] ?: "Segment $tag"

    fun getPartyRole(code: String): String = PARTY_ROLES[code] ?: "Role $code"

    fun get214StatusDescription(code: String): String = STATUS_214_CODES[code] ?: "Status $code"

    fun get315StatusDescription(code: String): String = STATUS_315_CODES[code] ?: "Status $code"

    fun getPoTypeDescription(code: String): String = PO_TYPE_CODES[code] ?: "Type $code"

    fun getPurposeDescription(code: String): String = PURPOSE_CODES[code] ?: "Purpose $code"
}
