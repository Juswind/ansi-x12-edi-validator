package com.example

import com.example.edi.model.EdiSamples
import com.example.edi.parser.EdiValidatorParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EdiValidatorParserTest {

    private lateinit var parser: EdiValidatorParser

    @Before
    fun setUp() {
        parser = EdiValidatorParser()
    }

    @Test
    fun testValid850PurchaseOrder() {
        val sample = EdiSamples.getSample("850_sample")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertTrue("850 sample should be valid", result.isValid)
        assertEquals(0, result.validationErrors.count { it.severity == "ERROR" })
        assertEquals("*", result.delimiters.elementDelimiter)
        assertEquals("~", result.delimiters.segmentDelimiter)
        assertEquals(":", result.delimiters.subelementDelimiter)

        assertEquals(1, result.transactions.size)
        val tx = result.transactions[0]
        assertEquals("850", tx.transactionSet)
        assertEquals("0001", tx.controlNumber)

        val poNum = tx.extractedData["po_number"]
        assertEquals("PO-2024-99882", poNum)

        val lineItems = tx.extractedData["line_items"] as List<*>
        assertEquals(2, lineItems.size)

        // Verify JSON string is strictly valid and formatted
        assertTrue(result.jsonOutput.contains("\"is_valid\": true"))
        assertTrue(result.jsonOutput.contains("\"element_delimiter\": \"*\""))
        assertTrue(result.jsonOutput.contains("\"segment_delimiter\": \"~\""))
        assertTrue(result.jsonOutput.contains("\"transaction_set\": \"850\""))
    }

    @Test
    fun testValid214CarrierShipmentStatus() {
        val sample = EdiSamples.getSample("214_sample")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertTrue("214 sample should be valid", result.isValid)
        assertEquals(1, result.transactions.size)
        val tx = result.transactions[0]
        assertEquals("214", tx.transactionSet)

        val scac = tx.extractedData["carrier_scac"]
        assertEquals("SWFT", scac)

        val events = tx.extractedData["shipment_events"] as List<*>
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun testValid315OceanContainerStatus() {
        val sample = EdiSamples.getSample("315_sample")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertTrue("315 sample should be valid", result.isValid)
        val tx = result.transactions[0]
        assertEquals("315", tx.transactionSet)
        assertEquals("MSCU7823419", tx.extractedData["container_number"])
        assertEquals("UV", tx.extractedData["event_status_code"])
    }

    @Test
    fun testValid856ShipNoticeASN() {
        val sample = EdiSamples.getSample("856_sample")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertTrue("856 sample should be valid", result.isValid)
        val tx = result.transactions[0]
        assertEquals("856", tx.transactionSet)
        assertEquals("ASN-20240316-01", tx.extractedData["shipment_id"])
    }

    @Test
    fun testValidFreightInvoices110_210_310() {
        listOf("110_sample", "210_sample", "310_sample").forEach { sampleId ->
            val sample = EdiSamples.getSample(sampleId)!!
            val result = parser.parseAndValidate(sample.rawContent)
            assertTrue("Sample $sampleId should be valid", result.isValid)
        }
    }

    @Test
    fun testEnvelopeControlNumberMismatch() {
        val sample = EdiSamples.getSample("invalid_envelope")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertFalse("Invalid envelope sample should fail validation", result.isValid)
        val errorCodes = result.validationErrors.map { it.errorCode }
        assertTrue("Should detect IEA control mismatch", errorCodes.contains("ERR_IEA_CONTROL_MISMATCH"))
        assertTrue("Should detect GE count mismatch", errorCodes.contains("ERR_GE_COUNT_MISMATCH"))
    }

    @Test
    fun testVersionMismatchFlagged() {
        val sample = EdiSamples.getSample("invalid_version")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertFalse("Version not 004010 should fail validation", result.isValid)
        val errorCodes = result.validationErrors.map { it.errorCode }
        assertTrue("Should flag version mismatch error", errorCodes.contains("ERR_VERSION_NOT_004010"))
    }

    @Test
    fun testMissingMandatoryBEGSegment() {
        val sample = EdiSamples.getSample("invalid_mandatory")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertFalse("Missing BEG should fail validation", result.isValid)
        val errorCodes = result.validationErrors.map { it.errorCode }
        assertTrue("Should flag missing mandatory segment", errorCodes.contains("ERR_MISSING_MANDATORY_SEGMENT"))
        assertTrue("Should flag CTT count mismatch", errorCodes.contains("ERR_CTT_COUNT_MISMATCH"))
    }

    @Test
    fun testExample1TruncatedRefElement() {
        val sample = EdiSamples.getSample("truncated_ref_850")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertFalse("Truncated REF segment should fail validation", result.isValid)
        assertEquals(1, result.validationErrors.size)
        val err = result.validationErrors[0]
        assertEquals("REF", err.segmentId)
        assertEquals(2, err.elementIndex)
        assertEquals("MANDATORY_ELEMENT_MISSING", err.errorCode)
        assertTrue(err.message.contains("Element REF02"))

        // Verify JSON string format matches requested schema
        assertTrue(result.jsonOutput.contains("\"is_valid\": false"))
        assertTrue(result.jsonOutput.contains("\"transaction_set\": \"850\""))
        assertTrue(result.jsonOutput.contains("\"version\": \"004010\""))
        assertTrue(result.jsonOutput.contains("\"segment\": \"REF\""))
        assertTrue(result.jsonOutput.contains("\"element_index\": 2"))
        assertTrue(result.jsonOutput.contains("\"error_code\": \"MANDATORY_ELEMENT_MISSING\""))
    }

    @Test
    fun testExample2MissingQualifierValueB403() {
        val sample = EdiSamples.getSample("missing_qualifier_b4_214")!!
        val result = parser.parseAndValidate(sample.rawContent)

        assertFalse("Missing B403 should fail validation", result.isValid)
        assertEquals(1, result.validationErrors.size)
        val err = result.validationErrors[0]
        assertEquals("B4", err.segmentId)
        assertEquals(3, err.elementIndex)
        assertEquals("MANDATORY_DATA_MISSING", err.errorCode)
        assertEquals("Element B403 (Shipment Status Code) is missing.", err.message)

        // Verify JSON string format matches requested schema
        assertTrue(result.jsonOutput.contains("\"is_valid\": false"))
        assertTrue(result.jsonOutput.contains("\"transaction_set\": \"214\""))
        assertTrue(result.jsonOutput.contains("\"version\": \"004010\""))
        assertTrue(result.jsonOutput.contains("\"segment\": \"B4\""))
        assertTrue(result.jsonOutput.contains("\"element_index\": 3"))
        assertTrue(result.jsonOutput.contains("\"error_code\": \"MANDATORY_DATA_MISSING\""))
    }

    @Test
    fun testConditionalAndElementRules() {
        // Test N1 missing both name and qualifier
        val n1Test = "ST*850*0001~BEG*00*SA*PO1**20260412~N1*BY*~SE*4*0001~"
        val n1Result = parser.parseAndValidate(n1Test)
        assertFalse(n1Result.isValid)
        assertTrue(n1Result.validationErrors.any { it.segmentId == "N1" && it.elementIndex == 2 })

        // Test DTM empty date
        val dtmTest = "ST*850*0001~BEG*00*SA*PO1**20260412~DTM*002~SE*4*0001~"
        val dtmResult = parser.parseAndValidate(dtmTest)
        assertFalse(dtmResult.isValid)
        assertTrue(dtmResult.validationErrors.any { it.segmentId == "DTM" && it.elementIndex == 2 })

        // Test AT7 missing status code in 214
        val at7Test = "ST*214*0001~B10*REF1*SHIP1*SCAC~LX*1~AT7***20260412*1200~SE*5*0001~"
        val at7Result = parser.parseAndValidate(at7Test)
        assertFalse(at7Result.isValid)
        assertTrue(at7Result.validationErrors.any { it.segmentId == "AT7" && it.elementIndex == 1 })
    }
}
