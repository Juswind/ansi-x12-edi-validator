package com.example.edi.model

/**
 * Delimiters detected or configured for ANSI ASC X12 EDI text.
 */
data class EdiDelimiters(
    val elementDelimiter: String = "*",
    val segmentDelimiter: String = "~",
    val subelementDelimiter: String = ":"
)

/**
 * Represents a single parsed EDI segment.
 */
data class EdiSegment(
    val index: Int,           // 0-based position in segment stream
    val tag: String,          // Segment identifier (e.g., "ISA", "ST", "BEG")
    val elements: List<String>, // Elements following the tag (1-indexed conceptually: elements[0] is element 1)
    val rawText: String,      // Original text of this segment
    val lineNum: Int          // 1-based line or sequence number
) {
    /**
     * Gets 1-based element safely.
     * E.g., getElement(1) returns elements[0] if exists, otherwise "".
     */
    fun getElement(index: Int): String {
        return if (index in 1..elements.size) elements[index - 1] else ""
    }

    val elementCount: Int
        get() = elements.size
}

/**
 * Detailed validation error/discrepancy adhering to X12 004010 specification.
 */
data class ValidationError(
    val segmentIndex: Int,
    val segmentId: String,
    val elementIndex: Int? = null,
    val errorCode: String,
    val message: String,
    val severity: String = "ERROR" // "ERROR" or "WARNING"
)

/**
 * Extracted ISA Header details.
 */
data class IsaEnvelope(
    val senderQualifier: String = "",
    val senderId: String = "",
    val receiverQualifier: String = "",
    val receiverId: String = "",
    val controlNumber: String = "",
    val standardsId: String = "",
    val version: String = "",
    val dateTime: String = "",
    val ackRequested: String = "",
    val usageIndicator: String = ""
)

/**
 * Extracted GS Functional Group details.
 */
data class GsEnvelope(
    val functionalCode: String = "",
    val senderCode: String = "",
    val receiverCode: String = "",
    val groupControlNumber: String = "",
    val agencyCode: String = "",
    val version: String = "",
    val dateTime: String = ""
)

/**
 * Trading Partner party (from N1, N2, N3, N4).
 */
data class EdiParty(
    val roleCode: String,
    val roleDescription: String,
    val name: String,
    val idQualifier: String = "",
    val identificationCode: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = ""
)

/**
 * Line item in 850 (PO1) or 856 (LIN/SN1) or freight charge line.
 */
data class EdiLineItem(
    val lineNumber: String = "",
    val quantity: String = "",
    val unitOfMeasure: String = "",
    val unitPrice: String = "",
    val buyerPartNumber: String = "",
    val vendorPartNumber: String = "",
    val upcNumber: String = "",
    val description: String = "",
    val totalAmount: String = ""
)

/**
 * Logistics / Carrier status event (214 AT7, 315 B4).
 */
data class EdiStatusEvent(
    val statusCode: String = "",
    val statusDescription: String = "",
    val statusReasonCode: String = "",
    val statusReasonDescription: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val city: String = "",
    val stateOrCountry: String = "",
    val equipmentInitial: String = "",
    val equipmentNumber: String = "",
    val vesselName: String = "",
    val voyageNumber: String = ""
)

/**
 * Hierarchical Level entry for 856 ASN.
 */
data class EdiHierarchicalLevel(
    val id: String,
    val parentId: String = "",
    val levelCode: String,
    val levelDescription: String,
    val details: String = ""
)

/**
 * Extracted semantic data and validation results for a single transaction set.
 */
data class EdiTransaction(
    val transactionSet: String,          // e.g. "850", "214"
    val controlNumber: String,           // ST02
    val isValid: Boolean,
    val transactionName: String,         // e.g. "Purchase Order"
    val version: String,                 // e.g. "004010"
    val extractedData: Map<String, Any?>, // Semantic business data
    val errors: List<ValidationError> = emptyList()
)

/**
 * Complete parsing and validation result for the EDI document.
 */
data class EdiParseResult(
    val isValid: Boolean,
    val delimiters: EdiDelimiters,
    val envelope: Map<String, Any?>,
    val transactions: List<EdiTransaction>,
    val validationErrors: List<ValidationError>,
    val summary: Map<String, Any>,
    val rawSegments: List<EdiSegment>,
    val jsonOutput: String
)
