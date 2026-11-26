package com.example.edi.model

data class EdiSample(
    val id: String,
    val name: String,
    val transactionCode: String,
    val description: String,
    val isExpectedValid: Boolean,
    val rawContent: String
)

object EdiSamples {

    val SAMPLES = listOf(
        // 1. 850: Purchase Order
        EdiSample(
            id = "850_sample",
            name = "850 - Purchase Order",
            transactionCode = "850",
            description = "Retail Purchase Order with buyer/vendor details, 2 item line orders (PO1/PID), and totals (CTT)",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *ZZ*ACMEBUYER      *ZZ*SUPPLIERGLOBAL *240315*1030*U*00401*000000101*0*P*:~
GS*PO*ACMEBUYER*SUPPLIERGLOBAL*20240315*1030*101*X*004010~
ST*850*0001~
BEG*00*SA*PO-2024-99882**20240315~
CUR*BY*USD~
REF*DP*042~
DTM*002*20240325~
N1*BY*ACME RETAIL CORP*92*100234~
N3*1200 COMMERCE WAY*SUITE 400~
N4*CHICAGO*IL*60601*US~
N1*VN*GLOBAL SUPPLY PARTNERS*91*988712~
N3*450 INDUSTRIAL PARKWAY~
N4*DALLAS*TX*75201*US~
N1*ST*ACME DISTRIBUTION CENTER #4*92*DC04~
N3*8800 LOGISTICS BLVD~
N4*INDIANAPOLIS*IN*46241*US~
PO1*1*500*EA*18.50*PE*BP*PART-8890*VP*GL-5510*UP*012345678905~
PID*F****INDUSTRIAL GRADE STEEL FASTENERS 10MM~
PO1*2*250*EA*42.00*PE*BP*PART-9921*VP*GL-7720*UP*012345678912~
PID*F****HEAVY DUTY MOUNTING BRACKET TITANIUM~
CTT*2*750~
AMT*TT*19750.00~
SE*21*0001~
GE*1*101~
IEA*1*000000101~
""".trimIndent()
        ),

        // 2. 856: Ship Notice/Manifest (ASN)
        EdiSample(
            id = "856_sample",
            name = "856 - Ship Notice / ASN",
            transactionCode = "856",
            description = "Advance Ship Notice with hierarchical levels: Shipment -> Order -> Item with tracking and packaging",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *ZZ*LOGIXVENDOR    *ZZ*RETAILCENTRAL  *240316*0815*U*00401*000000202*0*P*:~
GS*SH*LOGIXVENDOR*RETAILCENTRAL*20240316*0815*202*X*004010~
ST*856*0001~
BSN*00*ASN-20240316-01*20240316*0815*0001~
DTM*011*20240316~
HL*1**S~
TD1*CTN*50****G*1250*LB~
TD5*B*2*FDEG*M*FEDEX FREIGHT ECONOMY~
REF*BM*BOL-99881122~
N1*SF*LOGIX SHIP CENTER*91*VEND88~
N3*55 FREIGHT WAY~
N4*MEMPHIS*TN*38118*US~
N1*ST*RETAIL DC EAST*92*DC01~
N3*100 COMMERCE PARK~
N4*ATLANTA*GA*30303*US~
HL*2*1*O~
PRF*PO-2024-99882~
HL*3*2*I~
LIN*1*BP*PART-8890*VP*GL-5510~
SN1*1*500*EA~
PID*F****INDUSTRIAL GRADE STEEL FASTENERS 10MM~
HL*4*2*I~
LIN*2*BP*PART-9921*VP*GL-7720~
SN1*2*250*EA~
PID*F****HEAVY DUTY MOUNTING BRACKET TITANIUM~
CTT*4~
SE*25*0001~
GE*1*202~
IEA*1*000000202~
""".trimIndent()
        ),

        // 3. 214: Transportation Carrier Shipment Status Message
        EdiSample(
            id = "214_sample",
            name = "214 - Carrier Shipment Status",
            transactionCode = "214",
            description = "Real-time carrier tracking with multiple shipment events (Departed Terminal, En Route, Delivered)",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *02*SWFT           *ZZ*CLIENTLOGIX    *240317*1420*U*00401*000000303*0*P*:~
GS*QM*SWFT*CLIENTLOGIX*20240317*1420*303*X*004010~
ST*214*0001~
B10*SWFT-BOL-54120*PRO-987654321*SWFT~
L11*PO-2024-99882*PO~
MS3*SWFT*B*SWFT88912~
N1*SH*GLOBAL SUPPLY PARTNERS~
N1*CN*ACME RETAIL DC #4~
LX*1~
AT7*AF*NS***20240316*1800*ET~
MS1*DALLAS*TX*USA~
MS2*SWFT*TRUCK-4421~
LX*2~
AT7*X6*NS***20240317*0630*CT~
MS1*LITTLE ROCK*AR*USA~
LX*3~
AT7*D1*NS***20240317*1415*ET~
MS1*INDIANAPOLIS*IN*USA~
AT8*G*L*1250*50*CTN~
SE*18*0001~
GE*1*303~
IEA*1*000000303~
""".trimIndent()
        ),

        // 4. 210: Motor Carrier Freight Details and Invoice
        EdiSample(
            id = "210_sample",
            name = "210 - Motor Freight Invoice",
            transactionCode = "210",
            description = "Motor carrier freight invoice with equipment, mileage, line haul, fuel surcharge and total amount due",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *02*ODFL           *ZZ*ACMEPAYABLE    *240318*0900*U*00401*000000404*0*P*:~
GS*IM*ODFL*ACMEPAYABLE*20240318*0900*404*X*004010~
ST*210*0001~
B3*B*INV-2024-8841*BOL-54120*PP**20240318*148550**20240325*ODFL~
C3*USD~
N9*PO*PO-2024-99882~
N1*SH*GLOBAL SUPPLY PARTNERS~
N3*450 INDUSTRIAL PARKWAY~
N4*DALLAS*TX*75201*US~
N1*CN*ACME RETAIL DC #4~
N3*8800 LOGISTICS BLVD~
N4*INDIANAPOLIS*IN*46241*US~
N1*BT*ACME HEADQUARTERS~
N3*1200 COMMERCE WAY*SUITE 400~
N4*CHICAGO*IL*60601*US~
LX*1~
L5*1*STEEL FASTENERS & BRACKETS~
L0*1*1250*LB*1250*G***50*CTN~
L1*1*110.00*FR*137500~
LX*2~
L5*2*FUEL SURCHARGE~
L1*2*0.08*FS*11050~
L3*1250*G**148550***50~
SE*22*0001~
GE*1*404~
IEA*1*000000404~
""".trimIndent()
        ),

        // 5. 110: Air Freight Details and Invoice
        EdiSample(
            id = "110_sample",
            name = "110 - Air Freight Invoice",
            transactionCode = "110",
            description = "Air freight cargo invoice with AWB tracking, airport codes, flight number and tariff breakdown",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *02*DAL            *ZZ*GLOBALSHIPPER  *240319*1130*U*00401*000000505*0*P*:~
GS*IA*DAL*GLOBALSHIPPER*20240319*1130*505*X*004010~
ST*110*0001~
B3*B*AIR-INV-33901*006-88992211*PP**20240319*325000**20240326*DAL~
C3*USD~
N9*AW*006-88992211~
N1*SH*AEROSPACE TECH INC~
N3*100 AIRPORT ROAD~
N4*SEATTLE*WA*98158*US~
N1*CN*EUROLOGIX SYSTEMS~
N3*AIRPORT CARGO BUILDING 4~
N4*FRANKFURT**60549*DE~
LX*1~
L0*1*320*KG*320*G***12*CTN~
L1*1*8.50*FR*272000~
LX*2~
L1*2*53000*SC*53000~
L3*320*G**325000***12~
SE*17*0001~
GE*1*505~
IEA*1*000000505~
""".trimIndent()
        ),

        // 6. 310: Ocean Freight Details and Invoice
        EdiSample(
            id = "310_sample",
            name = "310 - Ocean Freight Invoice",
            transactionCode = "310",
            description = "Ocean carrier bill of lading freight invoice with vessel, voyage, discharge port and ocean tariff charges",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *02*MAEU           *ZZ*GLOBALIMPORT   *240320*1545*U*00401*000000606*0*P*:~
GS*SO*MAEU*GLOBALIMPORT*20240320*1545*606*X*004010~
ST*310*0001~
B3*B*OCN-INV-99014*MAEU98765432*PP**20240320*485000**20240401*MAEU~
C3*USD~
V1*933215*MAERSK MC-KINNEY MOLLER*DK*2401W*MAEU~
N9*BM*MAEU98765432~
N1*SH*PACIFIC MANUFACTURING LTD~
N3*88 HARBOUR ROAD~
N4*SHANGHAI**200000*CN~
N1*CN*NORTH AMERICAN LOGISTICS~
N3*PORT TERMINAL 4~
N4*LONG BEACH*CA*90802*US~
LX*1~
L0*1*18500*KG*18500*G***1*40HC~
L1*1*4200.00*OC*420000~
LX*2~
L1*2*650.00*TH*65000~
L3*18500*G**485000***1~
SE*18*0001~
GE*1*606~
IEA*1*000000606~
""".trimIndent()
        ),

        // 7. 315: Ocean Status Details (Ocean Container Event)
        EdiSample(
            id = "315_sample",
            name = "315 - Ocean Container Event",
            transactionCode = "315",
            description = "Ocean container milestone event tracking container discharge (UV), vessel, voyage and port terminal",
            isExpectedValid = true,
            rawContent = """
ISA*00*          *00*          *02*MSCU           *ZZ*IMPORTRACKER   *240321*1210*U*00401*000000707*0*P*:~
GS*QO*MSCU*IMPORTRACKER*20240321*1210*707*X*004010~
ST*315*0001~
B4*N**UV*20240321*0830*USLGB*MSCU*7823419*L*22G1~
N9*BM*MSCU91827364~
N9*4F*SEAL-889912~
Q2*9450648*LR******2403*MSCU*MSC OSCAR~
R4*D*UN*USLGB*PIER 400 CONTAINER TERMINAL~
DTM*140*20240321*0830~
SE*8*0001~
GE*1*707~
IEA*1*000000707~
""".trimIndent()
        ),

        // 8. Invalid Test Case 1: Envelope Control Number Mismatch
        EdiSample(
            id = "invalid_envelope",
            name = "Invalid - Envelope Mismatch",
            transactionCode = "850",
            description = "Detects IEA02 control mismatch (ISA13=101, IEA02=999) and GE01 count mismatch",
            isExpectedValid = false,
            rawContent = """
ISA*00*          *00*          *ZZ*ACMEBUYER      *ZZ*SUPPLIERGLOBAL *240315*1030*U*00401*000000101*0*P*:~
GS*PO*ACMEBUYER*SUPPLIERGLOBAL*20240315*1030*101*X*004010~
ST*850*0001~
BEG*00*SA*PO-TEST-001**20240315~
PO1*1*100*EA*10.00~
CTT*1~
SE*6*0001~
GE*5*101~
IEA*1*000000999~
""".trimIndent()
        ),

        // 9. Invalid Test Case 2: Version Mismatch (GS08 != 004010)
        EdiSample(
            id = "invalid_version",
            name = "Invalid - Version Not 004010",
            transactionCode = "214",
            description = "Fails validation because GS08 version is 005010 instead of required 004010",
            isExpectedValid = false,
            rawContent = """
ISA*00*          *00*          *02*SWFT           *ZZ*CLIENTLOGIX    *240317*1420*U*00401*000000303*0*P*:~
GS*QM*SWFT*CLIENTLOGIX*20240317*1420*303*X*005010~
ST*214*0001~
B10*SWFT-BOL-101*PRO-101*SWFT~
LX*1~
AT7*AF*NS***20240316*1800*ET~
SE*6*0001~
GE*1*303~
IEA*1*000000303~
""".trimIndent()
        ),

        // 10. Invalid Test Case 3: Missing Mandatory BEG and CTT Count Discrepancy
        EdiSample(
            id = "invalid_mandatory",
            name = "Invalid - Missing Mandatory & CTT Discrepancy",
            transactionCode = "850",
            description = "Fails validation because BEG segment is omitted, and CTT01 line count does not match PO1 segments",
            isExpectedValid = false,
            rawContent = """
ISA*00*          *00*          *ZZ*ACMEBUYER      *ZZ*SUPPLIERGLOBAL *240315*1030*U*00401*000000101*0*P*:~
GS*PO*ACMEBUYER*SUPPLIERGLOBAL*20240315*1030*101*X*004010~
ST*850*0001~
PO1*1*50*EA*25.00~
PO1*2*30*EA*15.00~
CTT*99~
SE*6*0001~
GE*1*101~
IEA*1*000000101~
""".trimIndent()
        ),

        // 11. Invalid Test Case 4: Truncated REF Element (Example 1)
        EdiSample(
            id = "truncated_ref_850",
            name = "Invalid - Truncated REF Element (850)",
            transactionCode = "850",
            description = "Fails validation: REF01 qualifier is present without required REF02 or REF03",
            isExpectedValid = false,
            rawContent = """
ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260412*1200*U*00401*000000001*0*P*>~
GS*PO*SENDER*RECEIVER*20260412*1200*1*X*004010~
ST*850*0001~
BEG*00*SA*PO12345**20260412~
REF*DP*~
SE*5*0001~
GE*1*1~
IEA*1*000000001~
""".trimIndent()
        ),

        // 12. Invalid Test Case 5: Missing Qualifier Value B403 (Example 2)
        EdiSample(
            id = "missing_qualifier_b4_214",
            name = "Invalid - Missing B403 Code (214)",
            transactionCode = "214",
            description = "Fails validation: Element B403 (Shipment Status Code) is missing in standalone 214 snippet",
            isExpectedValid = false,
            rawContent = """
ST*214*0002~
B4***20260412~
SE*3*0002~
""".trimIndent()
        )
    )

    fun getSample(id: String): EdiSample? = SAMPLES.firstOrNull { it.id == id }
}
