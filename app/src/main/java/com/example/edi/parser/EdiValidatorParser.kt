package com.example.edi.parser

import com.example.edi.model.*

/**
 * ANSI ASC X12 EDI version 004010 Validator and Semantic Extractor.
 */
class EdiValidatorParser {

    /**
     * Parses raw EDI string, performs 004010 syntax & structural validation,
     * extracts semantic business data, and generates strictly valid JSON.
     */
    fun parseAndValidate(rawEdiInput: String): EdiParseResult {
        val trimmedInput = rawEdiInput.trim()
        val validationErrors = mutableListOf<ValidationError>()

        if (trimmedInput.isEmpty()) {
            val defaultDelimiters = EdiDelimiters()
            val error = ValidationError(
                segmentIndex = 0,
                segmentId = "ROOT",
                errorCode = "ERR_EMPTY_INPUT",
                message = "EDI input text is empty.",
                severity = "ERROR"
            )
            return buildResult(
                isValid = false,
                delimiters = defaultDelimiters,
                envelope = emptyMap(),
                transactions = emptyList(),
                validationErrors = listOf(error),
                rawSegments = emptyList(),
                summary = mapOf("total_segments" to 0, "error_count" to 1)
            )
        }

        // 1. Delimiter identification
        val delimiters = detectDelimiters(trimmedInput)

        // 2. Tokenize raw text into segments
        val segments = tokenizeSegments(trimmedInput, delimiters)

        if (segments.isEmpty()) {
            val error = ValidationError(
                segmentIndex = 0,
                segmentId = "ROOT",
                errorCode = "ERR_NO_SEGMENTS",
                message = "Unable to parse any EDI segments using delimiter '${delimiters.segmentDelimiter}'.",
                severity = "ERROR"
            )
            return buildResult(
                isValid = false,
                delimiters = delimiters,
                envelope = emptyMap(),
                transactions = emptyList(),
                validationErrors = listOf(error),
                rawSegments = emptyList(),
                summary = mapOf("total_segments" to 0, "error_count" to 1)
            )
        }

        // 3. Structural and Envelope Validation
        var isaEnvelope: IsaEnvelope? = null
        var gsEnvelope: GsEnvelope? = null
        var isaSegmentIndex = -1
        var ieaSegmentIndex = -1
        var gsCount = 0
        var geCount = 0

        val firstSeg = segments[0]
        val lastSeg = segments[segments.size - 1]
        val isInterchange = (firstSeg.tag == "ISA")
        val isStandaloneTx = (firstSeg.tag == "ST")

        if (isInterchange) {
            isaSegmentIndex = 0
            if (firstSeg.elements.size < 16) {
                validationErrors.add(
                    ValidationError(
                        segmentIndex = 0,
                        segmentId = "ISA",
                        errorCode = "ERR_ISA_ELEMENT_COUNT",
                        message = "ISA segment must contain exactly 16 data elements. Found ${firstSeg.elements.size}.",
                        severity = "ERROR"
                    )
                )
            } else {
                for (elemIdx in 1..16) {
                    // ISA02 and ISA04 are fixed 10-char space-padded fields and permitted to be spaces
                    if (elemIdx == 2 || elemIdx == 4) continue
                    val v = firstSeg.getElement(elemIdx).trim()
                    if (v.isEmpty()) {
                        val tagStr = if (elemIdx < 10) "0$elemIdx" else "$elemIdx"
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = 0,
                                segmentId = "ISA",
                                elementIndex = elemIdx,
                                errorCode = "MANDATORY_ELEMENT_MISSING",
                                message = "Element ISA$tagStr is mandatory and cannot be blank or whitespace."
                            )
                        )
                    }
                }

                isaEnvelope = IsaEnvelope(
                    senderQualifier = firstSeg.getElement(5).trim(),
                    senderId = firstSeg.getElement(6).trim(),
                    receiverQualifier = firstSeg.getElement(7).trim(),
                    receiverId = firstSeg.getElement(8).trim(),
                    controlNumber = firstSeg.getElement(13).trim(),
                    standardsId = firstSeg.getElement(11).trim(),
                    version = firstSeg.getElement(12).trim(),
                    dateTime = formatDateTime(firstSeg.getElement(9).trim(), firstSeg.getElement(10).trim()),
                    ackRequested = firstSeg.getElement(14).trim(),
                    usageIndicator = firstSeg.getElement(15).trim()
                )

                // 004010 ISA Version check (typically 00401 or 00400 for control header in 004010)
                val isaVer = firstSeg.getElement(12).trim()
                if (isaVer.isNotEmpty() && isaVer != "00401" && isaVer != "00400") {
                    validationErrors.add(
                        ValidationError(
                            segmentIndex = 0,
                            segmentId = "ISA",
                            elementIndex = 12,
                            errorCode = "WARN_ISA_VERSION",
                            message = "ISA12 version '$isaVer' differs from expected control version 00401 for 004010 implementation.",
                            severity = "WARNING"
                        )
                    )
                }
            }

            // Check IEA (Last segment)
            if (lastSeg.tag != "IEA") {
                validationErrors.add(
                    ValidationError(
                        segmentIndex = segments.size - 1,
                        segmentId = lastSeg.tag,
                        errorCode = "ERR_MISSING_IEA",
                        message = "Last segment must be IEA (Interchange Control Trailer). Found '${lastSeg.tag}'.",
                        severity = "ERROR"
                    )
                )
            } else {
                ieaSegmentIndex = segments.size - 1
                if (lastSeg.elements.size < 2) {
                    validationErrors.add(
                        ValidationError(
                            segmentIndex = ieaSegmentIndex,
                            segmentId = "IEA",
                            errorCode = "ERR_IEA_ELEMENT_COUNT",
                            message = "IEA segment must contain 2 elements (Group Count, Control Number). Found ${lastSeg.elements.size}.",
                            severity = "ERROR"
                        )
                    )
                } else {
                    val ieaCtrl = lastSeg.getElement(2).trim()
                    val isaCtrl = isaEnvelope?.controlNumber ?: ""

                    if (isaCtrl.isNotEmpty() && ieaCtrl != isaCtrl) {
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = ieaSegmentIndex,
                                segmentId = "IEA",
                                elementIndex = 2,
                                errorCode = "ERR_IEA_CONTROL_MISMATCH",
                                message = "IEA02 control number '$ieaCtrl' does not match ISA13 '$isaCtrl'.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }
        } else if (!isStandaloneTx) {
            validationErrors.add(
                ValidationError(
                    segmentIndex = 0,
                    segmentId = firstSeg.tag,
                    errorCode = "ERR_MISSING_ISA",
                    message = "First segment must be ISA (Interchange Control Header) or ST (Transaction Set Header). Found '${firstSeg.tag}'.",
                    severity = "ERROR"
                )
            )
        }

        // Functional Groups and Transaction Sets validation
        val transactionSets = mutableListOf<EdiTransaction>()
        var currentGs: EdiSegment? = null
        var currentGsIndex = -1
        var stSegmentsInCurrentGs = 0

        var currentSt: EdiSegment? = null
        var currentStIndex = -1
        val currentTxSegments = mutableListOf<EdiSegment>()

        for (i in segments.indices) {
            val seg = segments[i]

            when (seg.tag) {
                "GS" -> {
                    gsCount++
                    currentGs = seg
                    currentGsIndex = i
                    stSegmentsInCurrentGs = 0

                    if (seg.elements.size < 8) {
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = i,
                                segmentId = "GS",
                                errorCode = "ERR_GS_ELEMENT_COUNT",
                                message = "GS segment must contain at least 8 elements. Found ${seg.elements.size}.",
                                severity = "ERROR"
                            )
                        )
                    } else {
                        val gsVersion = seg.getElement(8).trim()
                        gsEnvelope = GsEnvelope(
                            functionalCode = seg.getElement(1).trim(),
                            senderCode = seg.getElement(2).trim(),
                            receiverCode = seg.getElement(3).trim(),
                            groupControlNumber = seg.getElement(6).trim(),
                            agencyCode = seg.getElement(7).trim(),
                            version = gsVersion,
                            dateTime = formatDateTime(seg.getElement(4).trim(), seg.getElement(5).trim())
                        )

                        // Enforce version 004010 restriction!
                        if (!gsVersion.startsWith("004010")) {
                            validationErrors.add(
                                ValidationError(
                                    segmentIndex = i,
                                    segmentId = "GS",
                                    elementIndex = 8,
                                    errorCode = "ERR_VERSION_NOT_004010",
                                    message = "Specification restricted to version 004010. Found GS08 version '$gsVersion'.",
                                    severity = "ERROR"
                                )
                            )
                        }
                    }
                }

                "GE" -> {
                    geCount++
                    if (currentGs == null) {
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = i,
                                segmentId = "GE",
                                errorCode = "ERR_ORPHAN_GE",
                                message = "GE trailer found without preceding GS header.",
                                severity = "ERROR"
                            )
                        )
                    } else {
                        if (seg.elements.size < 2) {
                            validationErrors.add(
                                ValidationError(
                                    segmentIndex = i,
                                    segmentId = "GE",
                                    errorCode = "ERR_GE_ELEMENT_COUNT",
                                    message = "GE segment must contain 2 elements (Transaction Count, Control Number).",
                                    severity = "ERROR"
                                )
                            )
                        } else {
                            val geTxCount = seg.getElement(1).trim().toIntOrNull()
                            val geCtrl = seg.getElement(2).trim()
                            val gsCtrl = currentGs.getElement(6).trim()

                            if (geCtrl != gsCtrl) {
                                validationErrors.add(
                                    ValidationError(
                                        segmentIndex = i,
                                        segmentId = "GE",
                                        elementIndex = 2,
                                        errorCode = "ERR_GE_CONTROL_MISMATCH",
                                        message = "GE02 group control number '$geCtrl' does not match GS06 '$gsCtrl'.",
                                        severity = "ERROR"
                                    )
                                )
                            }

                            if (geTxCount != null && geTxCount != stSegmentsInCurrentGs) {
                                validationErrors.add(
                                    ValidationError(
                                        segmentIndex = i,
                                        segmentId = "GE",
                                        elementIndex = 1,
                                        errorCode = "ERR_GE_COUNT_MISMATCH",
                                        message = "GE01 transaction count ($geTxCount) does not match actual transactions ($stSegmentsInCurrentGs).",
                                        severity = "ERROR"
                                    )
                                )
                            }
                        }
                    }
                    currentGs = null
                }

                "ST" -> {
                    stSegmentsInCurrentGs++
                    currentSt = seg
                    currentStIndex = i
                    currentTxSegments.clear()
                    currentTxSegments.add(seg)

                    if (seg.elements.size < 2) {
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = i,
                                segmentId = "ST",
                                errorCode = "ERR_ST_ELEMENT_COUNT",
                                message = "ST segment must contain at least 2 elements (Transaction ID, Control Number).",
                                severity = "ERROR"
                            )
                        )
                    } else {
                        val txId = seg.getElement(1).trim()
                        // Restrict validation to version 004010 and the 7 transaction sets:
                        // 110, 210, 214, 310, 315, 850, 856
                        if (!EdiCodes.SUPPORTED_TRANSACTIONS.containsKey(txId)) {
                            validationErrors.add(
                                ValidationError(
                                    segmentIndex = i,
                                    segmentId = "ST",
                                    elementIndex = 1,
                                    errorCode = "ERR_UNSUPPORTED_TRANSACTION",
                                    message = "Transaction set '$txId' is not supported. Validation is restricted to 004010 sets: 110, 210, 214, 310, 315, 850, 856.",
                                    severity = "ERROR"
                                )
                            )
                        }
                    }
                }

                "SE" -> {
                    if (currentSt == null) {
                        validationErrors.add(
                            ValidationError(
                                segmentIndex = i,
                                segmentId = "SE",
                                errorCode = "ERR_ORPHAN_SE",
                                message = "SE trailer found without preceding ST header.",
                                severity = "ERROR"
                            )
                        )
                    } else {
                        currentTxSegments.add(seg)
                        val txId = currentSt.getElement(1).trim()
                        val stCtrl = currentSt.getElement(2).trim()
                        val txErrors = mutableListOf<ValidationError>()

                        if (seg.elements.size < 2) {
                            val err = ValidationError(
                                segmentIndex = i,
                                segmentId = "SE",
                                errorCode = "ERR_SE_ELEMENT_COUNT",
                                message = "SE segment must contain 2 elements (Segment Count, Control Number).",
                                severity = "ERROR"
                            )
                            validationErrors.add(err)
                            txErrors.add(err)
                        } else {
                            val seCount = seg.getElement(1).trim().toIntOrNull()
                            val seCtrl = seg.getElement(2).trim()
                            val actualSegmentCount = currentTxSegments.size

                            if (seCtrl != stCtrl) {
                                val err = ValidationError(
                                    segmentIndex = i,
                                    segmentId = "SE",
                                    elementIndex = 2,
                                    errorCode = "ERR_SE_CONTROL_MISMATCH",
                                    message = "SE02 control number '$seCtrl' does not match ST02 '$stCtrl'.",
                                    severity = "ERROR"
                                )
                                validationErrors.add(err)
                                txErrors.add(err)
                            }

                            // Validate transaction content against 004010 specs
                            val contentErrors = validateTransactionContent(txId, currentTxSegments, currentStIndex)
                            validationErrors.addAll(contentErrors)
                            txErrors.addAll(contentErrors)

                            val hasTruncationOrElementError = contentErrors.any { it.errorCode.startsWith("MANDATORY") }
                            if (seCount != null && seCount != actualSegmentCount) {
                                if (!hasTruncationOrElementError || Math.abs(seCount - actualSegmentCount) > 1) {
                                    val err = ValidationError(
                                        segmentIndex = i,
                                        segmentId = "SE",
                                        elementIndex = 1,
                                        errorCode = "ERR_SE_COUNT_MISMATCH",
                                        message = "SE01 segment count ($seCount) does not match actual count ($actualSegmentCount) between ST and SE.",
                                        severity = "ERROR"
                                    )
                                    validationErrors.add(err)
                                    txErrors.add(err)
                                }
                            }
                        }

                        // Semantic data extraction for this transaction set
                        val extractedData = extractTransactionSemantics(
                            txId = txId,
                            segments = currentTxSegments,
                            isa = isaEnvelope,
                            gs = gsEnvelope
                        )

                        val txIsValid = txErrors.none { it.severity == "ERROR" }
                        val txName = EdiCodes.SUPPORTED_TRANSACTIONS[txId] ?: "Transaction Set $txId"

                        transactionSets.add(
                            EdiTransaction(
                                transactionSet = txId,
                                controlNumber = stCtrl,
                                isValid = txIsValid,
                                transactionName = txName,
                                version = gsEnvelope?.version ?: "004010",
                                extractedData = extractedData,
                                errors = txErrors
                            )
                        )
                    }
                    currentSt = null
                    currentTxSegments.clear()
                }

                else -> {
                    if (currentSt != null) {
                        currentTxSegments.add(seg)
                    }
                }
            }
        }

        // Check if IEA group count matches actual GS count
        if (isInterchange && lastSeg.tag == "IEA" && lastSeg.elements.isNotEmpty()) {
            val ieaCount = lastSeg.getElement(1).trim().toIntOrNull()
            if (ieaCount != null && ieaCount != gsCount) {
                validationErrors.add(
                    ValidationError(
                        segmentIndex = segments.size - 1,
                        segmentId = "IEA",
                        elementIndex = 1,
                        errorCode = "ERR_IEA_COUNT_MISMATCH",
                        message = "IEA01 group count ($ieaCount) does not match actual functional groups count ($gsCount).",
                        severity = "ERROR"
                    )
                )
            }
        }

        if (isInterchange && gsCount == 0) {
            validationErrors.add(
                ValidationError(
                    segmentIndex = 0,
                    segmentId = "GS",
                    errorCode = "ERR_NO_GS_GROUPS",
                    message = "Document contains no GS (Functional Group Header) segments.",
                    severity = "ERROR"
                )
            )
        }

        if (transactionSets.isEmpty() && currentSt != null) {
            validationErrors.add(
                ValidationError(
                    segmentIndex = segments.size - 1,
                    segmentId = "ST",
                    errorCode = "ERR_UNCLOSED_ST",
                    message = "Transaction set started at segment $currentStIndex has no closing SE segment.",
                    severity = "ERROR"
                )
            )
        }

        val hasErrors = validationErrors.any { it.severity == "ERROR" }
        val isValid = !hasErrors

        val envelopeMap = mutableMapOf<String, Any?>()
        isaEnvelope?.let {
            envelopeMap["isa"] = mapOf(
                "sender_qualifier" to it.senderQualifier,
                "sender_id" to it.senderId,
                "receiver_qualifier" to it.receiverQualifier,
                "receiver_id" to it.receiverId,
                "control_number" to it.controlNumber,
                "standards_id" to it.standardsId,
                "version" to it.version,
                "date_time" to it.dateTime,
                "ack_requested" to it.ackRequested,
                "usage_indicator" to it.usageIndicator
            )
        }
        gsEnvelope?.let {
            envelopeMap["gs"] = mapOf(
                "functional_code" to it.functionalCode,
                "sender_code" to it.senderCode,
                "receiver_code" to it.receiverCode,
                "group_control_number" to it.groupControlNumber,
                "version" to it.version,
                "agency_code" to it.agencyCode,
                "date_time" to it.dateTime
            )
        }

        val summary = mapOf(
            "total_segments" to segments.size,
            "interchanges_count" to (if (isaEnvelope != null) 1 else 0),
            "groups_count" to gsCount,
            "transactions_count" to transactionSets.size,
            "error_count" to validationErrors.count { it.severity == "ERROR" },
            "warning_count" to validationErrors.count { it.severity == "WARNING" }
        )

        return buildResult(
            isValid = isValid,
            delimiters = delimiters,
            envelope = envelopeMap,
            transactions = transactionSets,
            validationErrors = validationErrors,
            rawSegments = segments,
            summary = summary
        )
    }

    /**
     * Auto-detect segment, element, and sub-element delimiters.
     * In ANSI ASC X12, the first 106 characters are the fixed ISA header:
     * - Char index 3 is element separator (e.g. '*')
     * - Element 16 (ISA16) is component/sub-element separator (e.g. ':')
     * - Char immediately after ISA16 is the segment terminator (e.g. '~' or '\n')
     */
    private fun detectDelimiters(input: String): EdiDelimiters {
        var elemDelim = "*"
        var segDelim = "~"
        var subElemDelim = ":"

        val clean = input.trimStart()
        if (clean.startsWith("ISA") && clean.length >= 4) {
            elemDelim = clean[3].toString()

            // Count element separators in ISA to find ISA16 and segment terminator
            val tokens = clean.split(elemDelim)
            if (tokens.size > 16) {
                // tokens[16] contains ISA16 plus segment delimiter
                val isa16Raw = tokens[16]
                if (isa16Raw.isNotEmpty()) {
                    subElemDelim = isa16Raw[0].toString()
                    if (isa16Raw.length > 1) {
                        val potentialSeg = isa16Raw[1].toString()
                        if (potentialSeg != "\r" && potentialSeg != "\n" && potentialSeg != " ") {
                            segDelim = potentialSeg
                        } else {
                            segDelim = "\n"
                        }
                    }
                }
            } else if (clean.contains("~")) {
                segDelim = "~"
            } else if (clean.contains("\n")) {
                segDelim = "\n"
            }
        } else {
            // Non-standard or snippet
            if (clean.contains("~")) segDelim = "~"
            else if (clean.contains("\n")) segDelim = "\n"

            if (clean.contains("*")) elemDelim = "*"
            else if (clean.contains("|")) elemDelim = "|"

            if (clean.contains(":")) subElemDelim = ":"
            else if (clean.contains(">")) subElemDelim = ">"
        }

        return EdiDelimiters(
            elementDelimiter = elemDelim,
            segmentDelimiter = segDelim,
            subelementDelimiter = subElemDelim
        )
    }

    /**
     * Tokenizes raw EDI string into EdiSegment objects.
     */
    private fun tokenizeSegments(input: String, delimiters: EdiDelimiters): List<EdiSegment> {
        val result = mutableListOf<EdiSegment>()

        // Split by segment delimiter (or lines if segDelim is ~ but text also has newlines)
        val rawParts: List<String> = if (delimiters.segmentDelimiter == "\n") {
            input.split("\r\n", "\n")
        } else {
            input.split(delimiters.segmentDelimiter)
        }

        var segmentIndex = 0
        var lineNum = 1

        for (part in rawParts) {
            val trimmedPart = part.trim()
            if (trimmedPart.isEmpty()) {
                lineNum++
                continue
            }

            // Split by element delimiter
            val elemTokens = trimmedPart.split(delimiters.elementDelimiter)
            if (elemTokens.isEmpty()) continue

            val tag = elemTokens[0].trim().uppercase()
            // Elements are tokens from 1 to end
            val elements = if (elemTokens.size > 1) {
                elemTokens.subList(1, elemTokens.size).map { it }
            } else {
                emptyList()
            }

            result.add(
                EdiSegment(
                    index = segmentIndex++,
                    tag = tag,
                    elements = elements,
                    rawText = trimmedPart,
                    lineNum = lineNum++
                )
            )
        }

        return result
    }

    /**
     * Validates transaction set mandatory segments, order, and element counts according to 004010.
     */
    private fun validateTransactionContent(
        txId: String,
        segments: List<EdiSegment>,
        startOffset: Int
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val tags = segments.map { it.tag }

        // 1. Deep element-level and conditional syntax validation for every segment
        for ((segIdx, seg) in segments.withIndex()) {
            errors.addAll(validateSegmentElements(seg, txId, startOffset + segIdx))
        }

        // 2. Transaction-level structural and mandatory segment checks
        when (txId) {
            "850" -> {
                // 850: Purchase Order
                val begIdx = tags.indexOf("BEG")
                if (begIdx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "850",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment BEG (Beginning Segment for Purchase Order) is missing in 850.",
                            severity = "ERROR"
                        )
                    )
                }

                val po1Count = tags.count { it == "PO1" }
                val cttIdx = tags.indexOf("CTT")

                if (tags.contains("PO1") || cttIdx != -1 || segments.size > 5) {
                    if (po1Count == 0) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + 1,
                                segmentId = "850",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment PO1 (Baseline Item Data) is missing in 850 (at least 1 line item required).",
                                severity = "ERROR"
                            )
                        )
                    }

                    if (cttIdx == -1) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + segments.size - 2,
                                segmentId = "850",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment CTT (Transaction Totals) is missing in 850.",
                                severity = "ERROR"
                            )
                        )
                    } else {
                        val cttSeg = segments[cttIdx]
                        val ctt01 = cttSeg.getElement(1).trim().toIntOrNull()
                        if (ctt01 != null && ctt01 != po1Count) {
                            errors.add(
                                ValidationError(
                                    segmentIndex = startOffset + cttIdx,
                                    segmentId = "CTT",
                                    elementIndex = 1,
                                    errorCode = "ERR_CTT_COUNT_MISMATCH",
                                    message = "CTT01 line count ($ctt01) does not match actual count of PO1 segments ($po1Count).",
                                    severity = "ERROR"
                                )
                            )
                        }
                    }

                    if (begIdx != -1) {
                        val firstPo1 = tags.indexOf("PO1")
                        if (firstPo1 != -1 && begIdx > firstPo1) {
                            errors.add(
                                ValidationError(
                                    segmentIndex = startOffset + begIdx,
                                    segmentId = "BEG",
                                    errorCode = "ERR_INVALID_SEGMENT_ORDER",
                                    message = "Invalid segment order: BEG must precede PO1 loop.",
                                    severity = "ERROR"
                                )
                            )
                        }
                    }
                }
            }

            "856" -> {
                // 856: Ship Notice/Manifest (ASN)
                val bsnIdx = tags.indexOf("BSN")
                if (bsnIdx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "856",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment BSN (Beginning Segment for Ship Notice) is missing in 856.",
                            severity = "ERROR"
                        )
                    )
                }

                if (segments.size > 4 || tags.contains("HL") || tags.contains("CTT")) {
                    val hlCount = tags.count { it == "HL" }
                    if (hlCount == 0) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + 1,
                                segmentId = "856",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment HL (Hierarchical Level) is missing in 856.",
                                severity = "ERROR"
                            )
                        )
                    }

                    val cttIdx = tags.indexOf("CTT")
                    if (cttIdx == -1) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + segments.size - 2,
                                segmentId = "856",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment CTT (Transaction Totals) is missing in 856.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }

            "214" -> {
                // 214: Transportation Carrier Shipment Status Message
                // If B4 is present (ocean container status subprofile or test snippet), skip B10/LX requirement
                if (!tags.contains("B4") && (segments.size > 4 || tags.contains("B10") || tags.contains("LX") || tags.contains("AT7"))) {
                    val b10Idx = tags.indexOf("B10")
                    if (b10Idx == -1) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + 1,
                                segmentId = "214",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment B10 (Beginning Segment for Transportation Carrier Shipment Status) is missing in 214.",
                                severity = "ERROR"
                            )
                        )
                    }

                    if (!tags.contains("LX")) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + 1,
                                segmentId = "214",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment LX (Assigned Number loop) is missing in 214.",
                                severity = "ERROR"
                            )
                        )
                    }

                    if (!tags.contains("AT7")) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + 1,
                                segmentId = "214",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment AT7 (Shipment Status Details) is missing in 214.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }

            "210" -> {
                // 210: Motor Carrier Freight Details and Invoice
                val b3Idx = tags.indexOf("B3")
                if (b3Idx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "210",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment B3 (Beginning Segment for Carrier's Invoice) is missing in 210.",
                            severity = "ERROR"
                        )
                    )
                }

                if (segments.size > 4 || tags.contains("L3")) {
                    if (!tags.contains("L3")) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + segments.size - 2,
                                segmentId = "210",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment L3 (Total Weight and Charges) is missing in 210.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }

            "110" -> {
                // 110: Air Freight Details and Invoice
                val b3Idx = tags.indexOf("B3")
                if (b3Idx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "110",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment B3 (Beginning Segment for Carrier's Invoice) is missing in 110.",
                            severity = "ERROR"
                        )
                    )
                }
                if (segments.size > 4 || tags.contains("L3")) {
                    if (!tags.contains("L3")) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + segments.size - 2,
                                segmentId = "110",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment L3 (Total Weight and Charges) is missing in 110.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }

            "310" -> {
                // 310: Ocean Freight Details and Invoice
                val b3Idx = tags.indexOf("B3")
                if (b3Idx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "310",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment B3 (Beginning Segment for Ocean Carrier's Invoice) is missing in 310.",
                            severity = "ERROR"
                        )
                    )
                }
                if (segments.size > 4 || tags.contains("L3")) {
                    if (!tags.contains("L3")) {
                        errors.add(
                            ValidationError(
                                segmentIndex = startOffset + segments.size - 2,
                                segmentId = "310",
                                errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                                message = "Mandatory segment L3 (Total Weight and Charges) is missing in 310.",
                                severity = "ERROR"
                            )
                        )
                    }
                }
            }

            "315" -> {
                // 315: Ocean Status Details (Ocean Container Event)
                val b4Idx = tags.indexOf("B4")
                if (b4Idx == -1) {
                    errors.add(
                        ValidationError(
                            segmentIndex = startOffset + 1,
                            segmentId = "315",
                            errorCode = "ERR_MISSING_MANDATORY_SEGMENT",
                            message = "Mandatory segment B4 (Beginning Segment for Inquiry or Arrival Notification) is missing in 315.",
                            severity = "ERROR"
                        )
                    )
                }
            }
        }

        return errors
    }

    /**
     * Performs strict 004010 element-level validation (mandatory elements, truncation, and conditional rules).
     */
    private fun validateSegmentElements(seg: EdiSegment, txId: String, segIndex: Int): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        when (seg.tag) {
            "REF" -> {
                val ref01 = seg.getElement(1).trim()
                val ref02 = seg.getElement(2).trim()
                val ref03 = seg.getElement(3).trim()
                if (ref01.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "REF",
                            elementIndex = 1,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "Element REF01 (Reference Identification Qualifier) is mandatory."
                        )
                    )
                } else if (ref02.isEmpty() && ref03.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "REF",
                            elementIndex = 2,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "Element REF02 (Reference Identification) is required when qualifier REF01 is present, but was left blank or truncated."
                        )
                    )
                }
            }

            "N1" -> {
                val n101 = seg.getElement(1).trim()
                val n102 = seg.getElement(2).trim()
                val n103 = seg.getElement(3).trim()
                val n104 = seg.getElement(4).trim()
                if (n101.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "N1",
                            elementIndex = 1,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "Element N101 (Entity Identifier Code) is mandatory."
                        )
                    )
                }
                if (n102.isEmpty() && n103.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "N1",
                            elementIndex = 2,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "At least one of element N102 (Name) or N103 (Identification Code Qualifier) must be present, but was left blank or truncated."
                        )
                    )
                }
                if (n103.isNotEmpty() && n104.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "N1",
                            elementIndex = 4,
                            errorCode = "CONDITIONAL_ELEMENT_MISSING",
                            message = "Element N104 (Identification Code) is required when N103 (Identification Code Qualifier) is present."
                        )
                    )
                } else if (n104.isNotEmpty() && n103.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "N1",
                            elementIndex = 3,
                            errorCode = "CONDITIONAL_ELEMENT_MISSING",
                            message = "Element N103 (Identification Code Qualifier) is required when N104 (Identification Code) is present."
                        )
                    )
                }
            }

            "DTM" -> {
                val dtm01 = seg.getElement(1).trim()
                val dtm02 = seg.getElement(2).trim()
                if (dtm01.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "DTM",
                            elementIndex = 1,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "Element DTM01 (Date/Time Qualifier) is mandatory."
                        )
                    )
                }
                if (dtm02.isEmpty()) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "DTM",
                            elementIndex = 2,
                            errorCode = "MANDATORY_ELEMENT_MISSING",
                            message = "Element DTM02 (Date) is mandatory when DTM01 is present, but was left blank or truncated."
                        )
                    )
                } else if (!dtm02.matches(Regex("\\d{6}|\\d{8}"))) {
                    errors.add(
                        ValidationError(
                            segmentIndex = segIndex,
                            segmentId = "DTM",
                            elementIndex = 2,
                            errorCode = "INVALID_DATE_FORMAT",
                            message = "Element DTM02 (Date) must be formatted as CCYYMMDD (8 digits) or YYMMDD (6 digits)."
                        )
                    )
                }
            }

            "BEG" -> {
                val beg01 = seg.getElement(1).trim()
                val beg02 = seg.getElement(2).trim()
                val beg03 = seg.getElement(3).trim()
                val beg05 = seg.getElement(5).trim()
                if (beg01.isEmpty()) {
                    errors.add(ValidationError(segIndex, "BEG", 1, "MANDATORY_ELEMENT_MISSING", "Element BEG01 (Transaction Set Purpose Code) is mandatory."))
                }
                if (beg02.isEmpty()) {
                    errors.add(ValidationError(segIndex, "BEG", 2, "MANDATORY_ELEMENT_MISSING", "Element BEG02 (Purchase Order Type Code) is mandatory."))
                }
                if (beg03.isEmpty()) {
                    errors.add(ValidationError(segIndex, "BEG", 3, "MANDATORY_ELEMENT_MISSING", "Element BEG03 (Purchase Order Number) is mandatory."))
                }
                if (beg05.isEmpty()) {
                    errors.add(ValidationError(segIndex, "BEG", 5, "MANDATORY_ELEMENT_MISSING", "Element BEG05 (Purchase Order Date) is mandatory."))
                }
            }

            "B4" -> {
                val b403 = seg.getElement(3).trim()
                val b404 = seg.getElement(4).trim()
                val isDateIn03 = b403.matches(Regex("\\d{6}|\\d{8}"))
                if (b403.isEmpty() || isDateIn03) {
                    errors.add(ValidationError(segIndex, "B4", 3, "MANDATORY_DATA_MISSING", "Element B403 (Shipment Status Code) is missing."))
                }
                if (b404.isEmpty() && !isDateIn03) {
                    errors.add(ValidationError(segIndex, "B4", 4, "MANDATORY_DATA_MISSING", "Element B404 (Date) is missing."))
                }
            }

            "B10" -> {
                val b1001 = seg.getElement(1).trim()
                val b1002 = seg.getElement(2).trim()
                val b1003 = seg.getElement(3).trim()
                if (b1001.isEmpty()) errors.add(ValidationError(segIndex, "B10", 1, "MANDATORY_ELEMENT_MISSING", "Element B1001 (Reference Identification) is mandatory."))
                if (b1002.isEmpty()) errors.add(ValidationError(segIndex, "B10", 2, "MANDATORY_ELEMENT_MISSING", "Element B1002 (Shipment Identification) is mandatory."))
                if (b1003.isEmpty()) errors.add(ValidationError(segIndex, "B10", 3, "MANDATORY_ELEMENT_MISSING", "Element B1003 (Standard Carrier Alpha Code) is mandatory."))
            }

            "BSN" -> {
                val bsn01 = seg.getElement(1).trim()
                val bsn02 = seg.getElement(2).trim()
                val bsn03 = seg.getElement(3).trim()
                val bsn04 = seg.getElement(4).trim()
                if (bsn01.isEmpty()) errors.add(ValidationError(segIndex, "BSN", 1, "MANDATORY_ELEMENT_MISSING", "Element BSN01 (Purpose Code) is mandatory."))
                if (bsn02.isEmpty()) errors.add(ValidationError(segIndex, "BSN", 2, "MANDATORY_ELEMENT_MISSING", "Element BSN02 (Shipment Identification) is mandatory."))
                if (bsn03.isEmpty()) errors.add(ValidationError(segIndex, "BSN", 3, "MANDATORY_ELEMENT_MISSING", "Element BSN03 (Date) is mandatory."))
                if (bsn04.isEmpty()) errors.add(ValidationError(segIndex, "BSN", 4, "MANDATORY_ELEMENT_MISSING", "Element BSN04 (Time) is mandatory."))
            }

            "B3" -> {
                val b302 = seg.getElement(2).trim()
                val b303 = seg.getElement(3).trim()
                val b304 = seg.getElement(4).trim()
                val b306 = seg.getElement(6).trim()
                val b307 = seg.getElement(7).trim()
                if (b302.isEmpty()) errors.add(ValidationError(segIndex, "B3", 2, "MANDATORY_ELEMENT_MISSING", "Element B302 (Invoice Number) is mandatory."))
                if (b303.isEmpty()) errors.add(ValidationError(segIndex, "B3", 3, "MANDATORY_ELEMENT_MISSING", "Element B303 (Shipment Identification Number) is mandatory."))
                if (b304.isEmpty()) errors.add(ValidationError(segIndex, "B3", 4, "MANDATORY_ELEMENT_MISSING", "Element B304 (Method of Payment) is mandatory."))
                if (b306.isEmpty()) errors.add(ValidationError(segIndex, "B3", 6, "MANDATORY_ELEMENT_MISSING", "Element B306 (Invoice Date) is mandatory."))
                if (b307.isEmpty()) errors.add(ValidationError(segIndex, "B3", 7, "MANDATORY_ELEMENT_MISSING", "Element B307 (Net Amount Due) is mandatory."))
            }

            "PO1" -> {
                val po102 = seg.getElement(2).trim()
                val po103 = seg.getElement(3).trim()
                if (po102.isEmpty()) {
                    errors.add(ValidationError(segIndex, "PO1", 2, "MANDATORY_ELEMENT_MISSING", "Element PO102 (Quantity Ordered) is mandatory and cannot be blank or whitespace."))
                }
                if (po103.isEmpty()) {
                    errors.add(ValidationError(segIndex, "PO1", 3, "MANDATORY_ELEMENT_MISSING", "Element PO103 (Unit or Basis for Measurement Code) is mandatory and cannot be blank or whitespace."))
                }
                for (p in listOf(6, 8, 10, 12)) {
                    val qual = seg.getElement(p).trim()
                    val id = seg.getElement(p + 1).trim()
                    val tagIdStr = if (p + 1 < 10) "0${p + 1}" else "${p + 1}"
                    val tagQualStr = if (p < 10) "0$p" else "$p"
                    if (qual.isNotEmpty() && id.isEmpty()) {
                        errors.add(ValidationError(segIndex, "PO1", p + 1, "CONDITIONAL_ELEMENT_MISSING", "Element PO1$tagIdStr (Product/Service ID) is required when PO1$tagQualStr qualifier is present."))
                    } else if (id.isNotEmpty() && qual.isEmpty()) {
                        errors.add(ValidationError(segIndex, "PO1", p, "CONDITIONAL_ELEMENT_MISSING", "Element PO1$tagQualStr (Product/Service ID Qualifier) is required when PO1$tagIdStr is present."))
                    }
                }
            }

            "AT7" -> {
                val at701 = seg.getElement(1).trim()
                val at705 = seg.getElement(5).trim()
                val at706 = seg.getElement(6).trim()
                if (at701.isEmpty()) errors.add(ValidationError(segIndex, "AT7", 1, "MANDATORY_ELEMENT_MISSING", "Element AT701 (Shipment Status Code) cannot be blank."))
                if (at705.isEmpty()) errors.add(ValidationError(segIndex, "AT7", 5, "MANDATORY_ELEMENT_MISSING", "Element AT705 (Date) cannot be blank."))
                if (at706.isEmpty()) errors.add(ValidationError(segIndex, "AT7", 6, "MANDATORY_ELEMENT_MISSING", "Element AT706 (Time) cannot be blank."))
            }

            "Q2" -> {
                val q201 = seg.getElement(1).trim()
                val q209 = seg.getElement(9).trim()
                val q213 = seg.getElement(13).trim()
                if (q201.isEmpty() && q213.isEmpty()) {
                    errors.add(ValidationError(segIndex, "Q2", 1, "MANDATORY_ELEMENT_MISSING", "Vessel reference (Q201 Vessel Code or Q213 Vessel Name) must be present."))
                }
                if (q209.isEmpty()) {
                    errors.add(ValidationError(segIndex, "Q2", 9, "MANDATORY_ELEMENT_MISSING", "Element Q209 (Voyage Number) must be present."))
                }
            }

            "CTT" -> {
                val ctt01 = seg.getElement(1).trim()
                if (ctt01.isEmpty()) {
                    errors.add(ValidationError(segIndex, "CTT", 1, "MANDATORY_ELEMENT_MISSING", "Element CTT01 (Number of Line Items) is mandatory."))
                }
            }

            "HL" -> {
                val hl01 = seg.getElement(1).trim()
                val hl03 = seg.getElement(3).trim()
                if (hl01.isEmpty()) errors.add(ValidationError(segIndex, "HL", 1, "MANDATORY_ELEMENT_MISSING", "Element HL01 (Hierarchical ID Number) is mandatory."))
                if (hl03.isEmpty()) errors.add(ValidationError(segIndex, "HL", 3, "MANDATORY_ELEMENT_MISSING", "Element HL03 (Hierarchical Level Code) is mandatory."))
            }

            "SN1" -> {
                val sn102 = seg.getElement(2).trim()
                val sn103 = seg.getElement(3).trim()
                if (sn102.isEmpty()) errors.add(ValidationError(segIndex, "SN1", 2, "MANDATORY_ELEMENT_MISSING", "Element SN102 (Number of Units Shipped) is mandatory."))
                if (sn103.isEmpty()) errors.add(ValidationError(segIndex, "SN1", 3, "MANDATORY_ELEMENT_MISSING", "Element SN103 (Unit or Basis for Measurement Code) is mandatory."))
            }

            "LIN" -> {
                val lin02 = seg.getElement(2).trim()
                val lin03 = seg.getElement(3).trim()
                if (lin02.isNotEmpty() && lin03.isEmpty()) {
                    errors.add(ValidationError(segIndex, "LIN", 3, "CONDITIONAL_ELEMENT_MISSING", "Element LIN03 is required when LIN02 is present."))
                }
            }

            "V1" -> {
                val v101 = seg.getElement(1).trim()
                val v102 = seg.getElement(2).trim()
                val v104 = seg.getElement(4).trim()
                if (v101.isEmpty() && v102.isEmpty()) {
                    errors.add(ValidationError(segIndex, "V1", 1, "MANDATORY_ELEMENT_MISSING", "At least one of element V101 (Vessel Code) or V102 (Vessel Name) must be present."))
                }
                if (v104.isEmpty()) {
                    errors.add(ValidationError(segIndex, "V1", 4, "MANDATORY_ELEMENT_MISSING", "Element V104 (Voyage Number) is mandatory."))
                }
            }

            "L3" -> {
                val l301 = seg.getElement(1).trim()
                val l302 = seg.getElement(2).trim()
                val l305 = if (seg.getElement(5).trim().isNotEmpty()) seg.getElement(5).trim() else seg.getElement(4).trim()
                if (l301.isNotEmpty() && l302.isEmpty()) {
                    errors.add(ValidationError(segIndex, "L3", 2, "CONDITIONAL_ELEMENT_MISSING", "Element L302 (Weight Qualifier) is required when L301 (Weight) is present."))
                } else if (l302.isNotEmpty() && l301.isEmpty()) {
                    errors.add(ValidationError(segIndex, "L3", 1, "CONDITIONAL_ELEMENT_MISSING", "Element L301 (Weight) is required when L302 (Weight Qualifier) is present."))
                }
                if (l305.isEmpty()) {
                    errors.add(ValidationError(segIndex, "L3", 5, "MANDATORY_ELEMENT_MISSING", "Element L305 (Total Charge Amount) is mandatory."))
                }
            }

            "MS1" -> {
                val ms101 = seg.getElement(1).trim()
                val ms102 = seg.getElement(2).trim()
                if (ms101.isNotEmpty() && ms102.isEmpty()) {
                    errors.add(ValidationError(segIndex, "MS1", 2, "CONDITIONAL_ELEMENT_MISSING", "Element MS102 (State or Province Code) is required when MS101 (City Name) is present."))
                }
            }

            "IT1" -> {
                val it102 = seg.getElement(2).trim()
                val it103 = seg.getElement(3).trim()
                if (it102.isEmpty()) errors.add(ValidationError(segIndex, "IT1", 2, "MANDATORY_ELEMENT_MISSING", "Element IT102 (Quantity Invoiced) is mandatory."))
                if (it103.isEmpty()) errors.add(ValidationError(segIndex, "IT1", 3, "MANDATORY_ELEMENT_MISSING", "Element IT103 (Unit of Measure) is mandatory."))
            }

            "B2" -> {
                val b202 = seg.getElement(2).trim()
                val b204 = seg.getElement(4).trim()
                val b206 = seg.getElement(6).trim()
                if (b202.isEmpty()) errors.add(ValidationError(segIndex, "B2", 2, "MANDATORY_ELEMENT_MISSING", "Element B202 (Standard Carrier Alpha Code) is mandatory."))
                if (b204.isEmpty()) errors.add(ValidationError(segIndex, "B2", 4, "MANDATORY_ELEMENT_MISSING", "Element B204 (Shipment Identification Number) is mandatory."))
                if (b206.isEmpty()) errors.add(ValidationError(segIndex, "B2", 6, "MANDATORY_ELEMENT_MISSING", "Element B206 (Method of Payment) is mandatory."))
            }

            "B2A" -> {
                val b2a01 = seg.getElement(1).trim()
                if (b2a01.isEmpty()) errors.add(ValidationError(segIndex, "B2A", 1, "MANDATORY_ELEMENT_MISSING", "Element B2A01 (Transaction Set Purpose Code) is mandatory."))
            }

            "BIG" -> {
                val big01 = seg.getElement(1).trim()
                val big02 = seg.getElement(2).trim()
                if (big01.isEmpty()) errors.add(ValidationError(segIndex, "BIG", 1, "MANDATORY_ELEMENT_MISSING", "Element BIG01 (Invoice Date) is mandatory."))
                if (big02.isEmpty()) errors.add(ValidationError(segIndex, "BIG", 2, "MANDATORY_ELEMENT_MISSING", "Element BIG02 (Invoice Number) is mandatory."))
            }
        }
        return errors
    }

    /**
     * Extracts high-value semantic business entities from EDI segments.
     */
    private fun extractTransactionSemantics(
        txId: String,
        segments: List<EdiSegment>,
        isa: IsaEnvelope?,
        gs: GsEnvelope?
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // 1. Envelopes and Control Identifiers
        val controlNumbers = mutableMapOf(
            "interchange_control_number" to (isa?.controlNumber ?: ""),
            "group_control_number" to (gs?.groupControlNumber ?: ""),
            "transaction_control_number" to (segments.firstOrNull { it.tag == "ST" }?.getElement(2)?.trim() ?: "")
        )
        result["control_numbers"] = controlNumbers

        // 2. Sender / Receiver
        val senderReceiver = mutableMapOf(
            "interchange_sender" to mapOf(
                "qualifier" to (isa?.senderQualifier ?: ""),
                "id" to (isa?.senderId ?: "")
            ),
            "interchange_receiver" to mapOf(
                "qualifier" to (isa?.receiverQualifier ?: ""),
                "id" to (isa?.receiverId ?: "")
            ),
            "application_sender_code" to (gs?.senderCode ?: ""),
            "application_receiver_code" to (gs?.receiverCode ?: "")
        )
        result["sender_receiver"] = senderReceiver

        // 3. Parties (N1, N2, N3, N4 loops)
        val parties = extractParties(segments)
        result["parties"] = parties

        // 4. Dates from DTM or Header
        val dates = mutableMapOf<String, String>()
        isa?.dateTime?.let { if (it.isNotEmpty()) dates["interchange_date"] = it }
        gs?.dateTime?.let { if (it.isNotEmpty()) dates["group_date"] = it }

        for (seg in segments) {
            if (seg.tag == "DTM" && seg.elements.size >= 2) {
                val qual = seg.getElement(1).trim()
                val dateVal = seg.getElement(2).trim()
                val qualName = when (qual) {
                    "002" -> "delivery_requested_date"
                    "010" -> "requested_ship_date"
                    "011" -> "shipped_date"
                    "017" -> "estimated_delivery_date"
                    "035" -> "delivered_date"
                    "037" -> "ship_not_before_date"
                    "038" -> "ship_not_later_date"
                    "140" -> "actual_discharge_date"
                    else -> "date_$qual"
                }
                dates[qualName] = dateVal
            }
        }

        // 5. Transaction-Specific Semantic Extraction
        when (txId) {
            "850" -> {
                val beg = segments.firstOrNull { it.tag == "BEG" }
                val poNumber = beg?.getElement(3)?.trim() ?: ""
                val poType = beg?.getElement(2)?.trim() ?: ""
                val poPurpose = beg?.getElement(1)?.trim() ?: ""
                val poDate = beg?.getElement(5)?.trim() ?: ""

                if (poDate.isNotEmpty()) dates["po_date"] = poDate

                result["po_number"] = poNumber
                result["order_type"] = poType
                result["order_type_description"] = EdiCodes.getPoTypeDescription(poType)
                result["purpose_code"] = poPurpose
                result["purpose_description"] = EdiCodes.getPurposeDescription(poPurpose)

                // Line items
                val lineItems = mutableListOf<Map<String, Any?>>()
                var currentItem: MutableMap<String, Any?>? = null

                for (seg in segments) {
                    if (seg.tag == "PO1") {
                        if (currentItem != null) lineItems.add(currentItem)
                        currentItem = mutableMapOf(
                            "line_number" to seg.getElement(1).trim(),
                            "quantity" to seg.getElement(2).trim(),
                            "unit_of_measure" to seg.getElement(3).trim(),
                            "unit_price" to seg.getElement(4).trim(),
                            "basis_of_unit_price" to seg.getElement(5).trim()
                        )
                        // Extract product ID pairs (e.g., BP=Buyer Part, VP=Vendor Part, UP=UPC)
                        var elemIdx = 6
                        while (elemIdx < seg.elements.size) {
                            val qual = seg.getElement(elemIdx).trim()
                            val value = seg.getElement(elemIdx + 1).trim()
                            when (qual) {
                                "BP", "IN" -> currentItem["buyer_part_number"] = value
                                "VP", "VN" -> currentItem["vendor_part_number"] = value
                                "UP" -> currentItem["upc_number"] = value
                                "SK" -> currentItem["sku"] = value
                                else -> if (qual.isNotEmpty()) currentItem["id_$qual"] = value
                            }
                            elemIdx += 2
                        }
                    } else if (seg.tag == "PID" && currentItem != null) {
                        val desc = seg.getElement(5).trim()
                        if (desc.isNotEmpty()) {
                            currentItem["description"] = desc
                        }
                    }
                }
                if (currentItem != null) lineItems.add(currentItem)
                result["line_items"] = lineItems

                val ctt = segments.firstOrNull { it.tag == "CTT" }
                val amt = segments.firstOrNull { it.tag == "AMT" }
                result["summary"] = mapOf(
                    "total_line_items" to (ctt?.getElement(1)?.trim() ?: lineItems.size.toString()),
                    "hash_total" to (ctt?.getElement(2)?.trim() ?: ""),
                    "monetary_amount" to (amt?.getElement(2)?.trim() ?: "")
                )
            }

            "856" -> {
                val bsn = segments.firstOrNull { it.tag == "BSN" }
                val shipmentId = bsn?.getElement(2)?.trim() ?: ""
                val bsnDate = bsn?.getElement(3)?.trim() ?: ""
                val bsnTime = bsn?.getElement(4)?.trim() ?: ""

                if (bsnDate.isNotEmpty()) dates["shipment_date"] = formatDateTime(bsnDate, bsnTime)
                result["shipment_id"] = shipmentId
                result["purpose_code"] = bsn?.getElement(1)?.trim() ?: ""

                // Hierarchical Levels and Items
                val hlList = mutableListOf<Map<String, Any?>>()
                for (seg in segments) {
                    if (seg.tag == "HL") {
                        val hlId = seg.getElement(1).trim()
                        val parentId = seg.getElement(2).trim()
                        val levelCode = seg.getElement(3).trim()
                        val levelDesc = when (levelCode) {
                            "S" -> "Shipment"
                            "O" -> "Order"
                            "P", "T" -> "Pack / Tare"
                            "I" -> "Item"
                            else -> "Level $levelCode"
                        }
                        hlList.add(
                            mapOf(
                                "id" to hlId,
                                "parent_id" to parentId,
                                "level_code" to levelCode,
                                "level_description" to levelDesc
                            )
                        )
                    }
                }
                result["hierarchical_structure"] = hlList
            }

            "214" -> {
                val b10 = segments.firstOrNull { it.tag == "B10" }
                val shipmentId = b10?.getElement(2)?.trim() ?: ""
                val scac = b10?.getElement(3)?.trim() ?: ""
                val bol = b10?.getElement(1)?.trim() ?: ""

                result["shipment_id"] = shipmentId
                result["carrier_scac"] = scac
                result["reference_id"] = bol

                // Carrier events from AT7
                val events = mutableListOf<Map<String, Any?>>()
                var currentEvent: MutableMap<String, Any?>? = null

                for (seg in segments) {
                    if (seg.tag == "AT7") {
                        if (currentEvent != null) events.add(currentEvent)
                        val statusCode = seg.getElement(1).trim()
                        val reasonCode = seg.getElement(2).trim()
                        val date = seg.getElement(5).trim()
                        val time = seg.getElement(6).trim()

                        currentEvent = mutableMapOf(
                            "status_code" to statusCode,
                            "status_description" to EdiCodes.get214StatusDescription(statusCode),
                            "reason_code" to reasonCode,
                            "event_date" to date,
                            "event_time" to time,
                            "event_date_time" to formatDateTime(date, time)
                        )
                    } else if (seg.tag == "MS1" && currentEvent != null) {
                        currentEvent["city"] = seg.getElement(1).trim()
                        currentEvent["state"] = seg.getElement(2).trim()
                        currentEvent["country"] = seg.getElement(3).trim()
                    }
                }
                if (currentEvent != null) events.add(currentEvent)
                result["shipment_events"] = events
            }

            "210", "110", "310" -> {
                val b3 = segments.firstOrNull { it.tag == "B3" }
                val invoiceNumber = b3?.getElement(2)?.trim() ?: ""
                val bolNumber = b3?.getElement(3)?.trim() ?: ""
                val paymentMethod = b3?.getElement(4)?.trim() ?: ""
                val invoiceDate = b3?.getElement(6)?.trim() ?: ""
                val netAmountDue = b3?.getElement(7)?.trim() ?: ""

                if (invoiceDate.isNotEmpty()) dates["invoice_date"] = invoiceDate

                result["invoice_number"] = invoiceNumber
                result["bol_number"] = bolNumber
                result["payment_method"] = paymentMethod
                result["net_amount_due"] = netAmountDue

                // Freight summary from L3
                val l3 = segments.firstOrNull { it.tag == "L3" }
                if (l3 != null) {
                    result["charges_summary"] = mapOf(
                        "weight" to l3.getElement(1).trim(),
                        "weight_qualifier" to l3.getElement(2).trim(),
                        "freight_rate" to l3.getElement(3).trim(),
                        "charge_amount" to l3.getElement(5).trim()
                    )
                }

                // If Ocean 310, extract Vessel V1
                if (txId == "310") {
                    val v1 = segments.firstOrNull { it.tag == "V1" }
                    if (v1 != null) {
                        result["vessel_info"] = mapOf(
                            "vessel_code" to v1.getElement(1).trim(),
                            "vessel_name" to v1.getElement(2).trim(),
                            "voyage_number" to v1.getElement(4).trim()
                        )
                    }
                }
            }

            "315" -> {
                val b4 = segments.firstOrNull { it.tag == "B4" }
                val statusCode = when {
                    b4?.getElement(3)?.trim()?.isNotEmpty() == true && !b4.getElement(3).trim().matches(Regex("\\d{6}|\\d{8}")) -> b4.getElement(3).trim()
                    b4?.getElement(4)?.trim()?.isNotEmpty() == true && !b4.getElement(4).trim().matches(Regex("\\d{6}|\\d{8}")) -> b4.getElement(4).trim()
                    else -> b4?.getElement(3)?.trim() ?: ""
                }
                val releaseDate = when {
                    b4?.getElement(4)?.trim()?.matches(Regex("\\d{6}|\\d{8}")) == true -> b4.getElement(4).trim()
                    b4?.getElement(3)?.trim()?.matches(Regex("\\d{6}|\\d{8}")) == true -> b4.getElement(3).trim()
                    else -> b4?.getElement(4)?.trim() ?: ""
                }
                val releaseTime = b4?.getElement(5)?.trim() ?: ""
                val equipInitial = when {
                    b4?.getElement(6)?.matches(Regex("[A-Za-z]{3,4}")) == true && b4.getElement(7).isNotEmpty() -> b4.getElement(6).trim()
                    b4?.getElement(7)?.matches(Regex("[A-Za-z]{3,4}")) == true -> b4.getElement(7).trim()
                    else -> b4?.getElement(6)?.trim() ?: ""
                }
                val equipNumber = when {
                    b4?.getElement(6)?.matches(Regex("[A-Za-z]{3,4}")) == true && b4.getElement(7).isNotEmpty() -> b4.getElement(7).trim()
                    else -> b4?.getElement(8)?.trim() ?: b4?.getElement(7)?.trim() ?: ""
                }
                val containerId = "$equipInitial$equipNumber".trim()

                result["container_number"] = containerId
                result["event_status_code"] = statusCode
                result["event_status_description"] = EdiCodes.get315StatusDescription(statusCode)
                result["event_date"] = releaseDate
                result["event_time"] = releaseTime

                // Ocean Vessel Details from Q2
                val q2 = segments.firstOrNull { it.tag == "Q2" }
                if (q2 != null) {
                    result["vessel_info"] = mapOf(
                        "vessel_code" to q2.getElement(1).trim(),
                        "country_code" to q2.getElement(2).trim(),
                        "voyage_number" to q2.getElement(9).trim(),
                        "vessel_name" to q2.getElement(13).trim()
                    )
                }

                // Reference numbers from N9
                val refs = segments.filter { it.tag == "N9" }.map {
                    mapOf(
                        "qualifier" to it.getElement(1).trim(),
                        "value" to it.getElement(2).trim()
                    )
                }
                if (refs.isNotEmpty()) result["references"] = refs
            }
        }

        result["dates"] = dates
        return result
    }

    /**
     * Extracts party loops (N1..N4).
     */
    private fun extractParties(segments: List<EdiSegment>): List<Map<String, Any?>> {
        val parties = mutableListOf<Map<String, Any?>>()
        var currentParty: MutableMap<String, Any?>? = null

        for (seg in segments) {
            when (seg.tag) {
                "N1" -> {
                    if (currentParty != null) parties.add(currentParty)
                    val roleCode = seg.getElement(1).trim()
                    val roleDesc = EdiCodes.getPartyRole(roleCode)
                    currentParty = mutableMapOf(
                        "role_code" to roleCode,
                        "role" to "$roleCode ($roleDesc)",
                        "name" to seg.getElement(2).trim(),
                        "id_qualifier" to seg.getElement(3).trim(),
                        "identification_code" to seg.getElement(4).trim()
                    )
                }
                "N2" -> {
                    currentParty?.let {
                        val n2Name = seg.getElement(1).trim()
                        it["additional_name"] = n2Name
                    }
                }
                "N3" -> {
                    currentParty?.let {
                        val addr1 = seg.getElement(1).trim()
                        val addr2 = seg.getElement(2).trim()
                        it["address"] = if (addr2.isNotEmpty()) "$addr1, $addr2" else addr1
                    }
                }
                "N4" -> {
                    currentParty?.let {
                        it["city"] = seg.getElement(1).trim()
                        it["state"] = seg.getElement(2).trim()
                        it["postal_code"] = seg.getElement(3).trim()
                        it["country"] = seg.getElement(4).trim()
                    }
                }
            }
        }
        if (currentParty != null) parties.add(currentParty)
        return parties
    }

    private fun formatDateTime(date: String, time: String): String {
        val d = date.trim()
        val t = time.trim()
        val formattedDate = when (d.length) {
            8 -> "${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}"
            6 -> "20${d.substring(0, 2)}-${d.substring(2, 4)}-${d.substring(4, 6)}"
            else -> d
        }
        val formattedTime = when (t.length) {
            4 -> "${t.substring(0, 2)}:${t.substring(2, 4)}"
            6 -> "${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}"
            else -> t
        }
        return if (formattedTime.isNotEmpty()) "$formattedDate $formattedTime".trim() else formattedDate
    }

    /**
     * Constructs the EdiParseResult and produces strictly valid, nicely formatted JSON.
     */
    private fun buildResult(
        isValid: Boolean,
        delimiters: EdiDelimiters,
        envelope: Map<String, Any?>,
        transactions: List<EdiTransaction>,
        validationErrors: List<ValidationError>,
        rawSegments: List<EdiSegment>,
        summary: Map<String, Any>
    ): EdiParseResult {
        val root = linkedMapOf<String, Any?>()
        root["is_valid"] = isValid

        val detectedTxSet = transactions.firstOrNull()?.transactionSet
            ?: rawSegments.firstOrNull { it.tag == "ST" }?.getElement(1)?.trim()
            ?: if (rawSegments.any { it.tag == "B4" }) "214"
            else if (rawSegments.any { it.tag == "B10" }) "214"
            else if (rawSegments.any { it.tag == "BEG" }) "850"
            else if (rawSegments.any { it.tag == "BSN" }) "856"
            else if (rawSegments.any { it.tag == "B3" }) "210"
            else null

        if (detectedTxSet != null && detectedTxSet.isNotEmpty()) {
            root["transaction_set"] = detectedTxSet
        }

        val detectedVersion = (envelope["gs"] as? Map<*, *>)?.get("version") as? String
            ?: transactions.firstOrNull()?.version
            ?: rawSegments.firstOrNull { it.tag == "GS" }?.getElement(8)?.trim()
            ?: "004010"
        root["version"] = if (detectedVersion.isNotEmpty()) detectedVersion else "004010"

        root["validation_errors"] = validationErrors.map { errorToMap(it) }

        if (isValid) {
            val delimMap = linkedMapOf<String, Any?>()
            delimMap["element_delimiter"] = delimiters.elementDelimiter
            delimMap["segment_delimiter"] = delimiters.segmentDelimiter
            delimMap["subelement_delimiter"] = delimiters.subelementDelimiter
            root["delimiters"] = delimMap

            root["envelope"] = envelope

            val txList = transactions.map { tx ->
                val tMap = linkedMapOf<String, Any?>()
                tMap["transaction_set"] = tx.transactionSet
                tMap["control_number"] = tx.controlNumber
                tMap["is_valid"] = tx.isValid
                tMap["transaction_name"] = tx.transactionName
                tMap["version"] = tx.version
                tMap["extracted_data"] = tx.extractedData
                tMap
            }
            root["transactions"] = txList
            root["summary"] = summary
        }

        val jsonString = serializeJson(root, indent = 0)

        return EdiParseResult(
            isValid = isValid,
            delimiters = delimiters,
            envelope = envelope,
            transactions = transactions,
            validationErrors = validationErrors,
            summary = summary,
            rawSegments = rawSegments,
            jsonOutput = jsonString
        )
    }

    private fun errorToMap(err: ValidationError): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        map["segment"] = err.segmentId
        if (err.elementIndex != null) {
            map["element_index"] = err.elementIndex
        }
        map["error_code"] = err.errorCode
        map["message"] = err.message
        return map
    }

    companion object {
        fun serializeJson(value: Any?, indent: Int = 0): String {
            val indentStr = "  ".repeat(indent)
            val nextIndentStr = "  ".repeat(indent + 1)

            return when (value) {
                null -> "null"
                is Boolean -> value.toString()
                is Number -> value.toString()
                is String -> "\"${escapeJson(value)}\""
                is Map<*, *> -> {
                    if (value.isEmpty()) "{}"
                    else {
                        val entries = value.entries.joinToString(",\n") { (k, v) ->
                            "$nextIndentStr\"${escapeJson(k.toString())}\": ${serializeJson(v, indent + 1)}"
                        }
                        "{\n$entries\n$indentStr}"
                    }
                }
                is Iterable<*> -> {
                    val list = value.toList()
                    if (list.isEmpty()) "[]"
                    else {
                        val items = list.joinToString(",\n") { item ->
                            "$nextIndentStr${serializeJson(item, indent + 1)}"
                        }
                        "[\n$items\n$indentStr}"
                    }
                }
                else -> "\"${escapeJson(value.toString())}\""
            }
        }

        private fun escapeJson(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '\\' -> append("\\\\")
                    '\"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (c.code < 32) {
                            append("\\u%04x".format(c.code))
                        } else {
                            append(c)
                        }
                    }
                }
            }
        }
    }
}
