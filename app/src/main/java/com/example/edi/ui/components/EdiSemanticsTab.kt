package com.example.edi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edi.model.EdiParseResult
import com.example.edi.model.EdiTransaction
import com.example.ui.theme.*

@Composable
fun EdiSemanticsTab(
    parseResult: EdiParseResult?,
    onSwitchToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (parseResult == null || parseResult.transactions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "No extracted semantic data available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onSwitchToEditor) {
                    Text("Load Sample or Parse EDI")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(parseResult.transactions) { tx ->
            TransactionSemanticsCard(tx = tx)
        }
    }
}

@Composable
private fun TransactionSemanticsCard(tx: EdiTransaction) {
    val data = tx.extractedData

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_semantics_card_${tx.transactionSet}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Transaction Set & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tx.transactionSet,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        Text(
                            text = tx.transactionName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Control #: ${tx.controlNumber} | Version: ${tx.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = if (tx.isValid) EdiSuccess.copy(alpha = 0.15f) else EdiError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (tx.isValid) "VALID" else "ERRORS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.isValid) EdiSuccess else EdiError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Control Numbers & Identification
            val controlNumbers = data["control_numbers"] as? Map<*, *>
            if (controlNumbers != null) {
                InfoSection(title = "Control Identifiers") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoBadge(
                            label = "Interchange Ctrl",
                            value = controlNumbers["interchange_control_number"]?.toString() ?: "",
                            modifier = Modifier.weight(1f)
                        )
                        InfoBadge(
                            label = "Group Ctrl",
                            value = controlNumbers["group_control_number"]?.toString() ?: "",
                            modifier = Modifier.weight(1f)
                        )
                        InfoBadge(
                            label = "Transaction Ctrl",
                            value = controlNumbers["transaction_control_number"]?.toString() ?: "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Sender / Receiver
            val senderReceiver = data["sender_receiver"] as? Map<*, *>
            if (senderReceiver != null) {
                InfoSection(title = "Trading Partners (Sender & Receiver)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sender = senderReceiver["interchange_sender"] as? Map<*, *>
                        val receiver = senderReceiver["interchange_receiver"] as? Map<*, *>

                        PartnerBadge(
                            label = "Sender (ISA06)",
                            qualifier = sender?.get("qualifier")?.toString() ?: "",
                            id = sender?.get("id")?.toString() ?: "",
                            appCode = senderReceiver["application_sender_code"]?.toString() ?: "",
                            modifier = Modifier.weight(1f)
                        )

                        PartnerBadge(
                            label = "Receiver (ISA08)",
                            qualifier = receiver?.get("qualifier")?.toString() ?: "",
                            id = receiver?.get("id")?.toString() ?: "",
                            appCode = senderReceiver["application_receiver_code"]?.toString() ?: "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Document Dates
            val dates = data["dates"] as? Map<*, *>
            if (dates != null && dates.isNotEmpty()) {
                InfoSection(title = "Document & Milestone Dates") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        dates.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key.toString().replace("_", " ").capitalizeWords(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Transaction-specific semantic breakdown
            when (tx.transactionSet) {
                "850" -> Order850Semantics(data = data)
                "214" -> Carrier214Semantics(data = data)
                "315" -> Ocean315Semantics(data = data)
                "856" -> Asn856Semantics(data = data)
                "210", "110", "310" -> FreightInvoiceSemantics(txId = tx.transactionSet, data = data)
            }

            // 5. Extracted Parties (N1/N2/N3/N4)
            val parties = data["parties"] as? List<*>
            if (!parties.isNullOrEmpty()) {
                InfoSection(title = "Entities & Locations (${parties.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parties.filterIsInstance<Map<*, *>>().forEach { p ->
                            PartyCard(party = p)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun InfoBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value.ifEmpty { "N/A" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PartnerBadge(
    label: String,
    qualifier: String,
    id: String,
    appCode: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = "$id ($qualifier)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            if (appCode.isNotEmpty()) {
                Text(
                    text = "App: $appCode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun Order850Semantics(data: Map<String, Any?>) {
    val poNumber = data["po_number"]?.toString() ?: ""
    val orderType = data["order_type_description"]?.toString() ?: ""
    val lineItems = (data["line_items"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
    val summary = data["summary"] as? Map<*, *>

    InfoSection(title = "Purchase Order Details") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(label = "PO Number", value = poNumber, modifier = Modifier.weight(1f))
                InfoBadge(label = "Order Type", value = orderType, modifier = Modifier.weight(1f))
            }

            if (lineItems.isNotEmpty()) {
                Text(
                    text = "Line Items (${lineItems.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                lineItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Line #${item["line_number"]} - ${item["quantity"]} ${item["unit_of_measure"]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${item["unit_price"]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val part = item["buyer_part_number"]?.toString()
                                ?: item["vendor_part_number"]?.toString() ?: ""
                            if (part.isNotEmpty()) {
                                Text(
                                    text = "Part: $part",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            val desc = item["description"]?.toString() ?: ""
                            if (desc.isNotEmpty()) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Lines: ${summary["total_line_items"]}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    val amt = summary["monetary_amount"]?.toString() ?: ""
                    if (amt.isNotEmpty()) {
                        Text(
                            text = "Total Amount: $$amt",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Carrier214Semantics(data: Map<String, Any?>) {
    val shipmentId = data["shipment_id"]?.toString() ?: ""
    val scac = data["carrier_scac"]?.toString() ?: ""
    val events = (data["shipment_events"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

    InfoSection(title = "Carrier Shipment & Tracking") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(label = "Shipment ID / PRO", value = shipmentId, modifier = Modifier.weight(1f))
                InfoBadge(label = "Carrier SCAC", value = scac, modifier = Modifier.weight(1f))
            }

            if (events.isNotEmpty()) {
                Text(
                    text = "Carrier Milestone Events (${events.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                events.forEach { ev ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ev["status_code"]?.toString() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev["status_description"]?.toString() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val location = listOfNotNull(ev["city"], ev["state"]).joinToString(", ")
                                if (location.isNotEmpty()) {
                                    Text(
                                        text = "Location: $location",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Timestamp: ${ev["event_date_time"] ?: ev["event_date"]}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Ocean315Semantics(data: Map<String, Any?>) {
    val container = data["container_number"]?.toString() ?: ""
    val statusDesc = data["event_status_description"]?.toString() ?: ""
    val vesselInfo = data["vessel_info"] as? Map<*, *>

    InfoSection(title = "Ocean Container Milestone") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(label = "Container #", value = container, modifier = Modifier.weight(1f))
                InfoBadge(label = "Status Event", value = statusDesc, modifier = Modifier.weight(1f))
            }

            if (vesselInfo != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Vessel: ${vesselInfo["vessel_name"] ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Voyage: ${vesselInfo["voyage_number"] ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Asn856Semantics(data: Map<String, Any?>) {
    val shipmentId = data["shipment_id"]?.toString() ?: ""
    val levels = (data["hierarchical_structure"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()

    InfoSection(title = "Advance Ship Notice (ASN) Structure") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBadge(label = "Shipment ID", value = shipmentId, modifier = Modifier.fillMaxWidth())

            if (levels.isNotEmpty()) {
                Text(
                    text = "Hierarchical Levels (HL):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                levels.forEach { hl ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Level ${hl["level_code"]}: ${hl["level_description"]}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "HL #${hl["id"]}" + if ((hl["parent_id"]?.toString() ?: "").isNotEmpty()) " (Parent #${hl["parent_id"]})" else " (Root)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreightInvoiceSemantics(txId: String, data: Map<String, Any?>) {
    val invoice = data["invoice_number"]?.toString() ?: ""
    val bol = data["bol_number"]?.toString() ?: ""
    val netAmount = data["net_amount_due"]?.toString() ?: ""
    val charges = data["charges_summary"] as? Map<*, *>

    InfoSection(title = "Freight Invoice ($txId)") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(label = "Invoice #", value = invoice, modifier = Modifier.weight(1f))
                InfoBadge(label = "BOL / Waybill", value = bol, modifier = Modifier.weight(1f))
            }

            if (charges != null || netAmount.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Net Amount Due:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (charges != null) {
                                Text(
                                    text = "Weight: ${charges["weight"]} ${charges["weight_qualifier"] ?: "LB"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = if (netAmount.isNotEmpty()) "$${netAmount.toDoubleOrNull()?.let { "%.2f".format(it / 100.0) } ?: netAmount}" else "Calculated",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyCard(party: Map<*, *>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = party["role"]?.toString() ?: "Party",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                val id = party["identification_code"]?.toString() ?: ""
                if (id.isNotEmpty()) {
                    Text(
                        text = "ID: $id",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = party["name"]?.toString() ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            val address = party["address"]?.toString() ?: ""
            val city = party["city"]?.toString() ?: ""
            val state = party["state"]?.toString() ?: ""
            val postal = party["postal_code"]?.toString() ?: ""

            if (address.isNotEmpty()) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val loc = listOf(city, state, postal).filter { it.isNotEmpty() }.joinToString(", ")
            if (loc.isNotEmpty()) {
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
