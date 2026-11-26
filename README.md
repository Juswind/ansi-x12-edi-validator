# ANSI X12 (004010) EDI Validator App

A mobile utility designed for supply chain analysts, EDI specialists, and logistics operations teams to audit **ANSI ASC X12 (Version 004010)** payloads for structural envelope integrity and mandatory segment rules before partner UAT or production transmission.

---

## Business Problem & Functional Context

In global freight forwarding and B2B supply chain integrations, malformed EDI files—such as mismatched control numbers, truncated segments, or missing mandatory qualifiers—frequently stall partner testing and cause shipment milestone failures. 

As a **Senior Business Analyst**, I conceptualized and built this application to shift EDI validation left: enabling functional analysts and onboarding teams to catch structural and business-rule defects immediately on mobile before engaging middleware developers or external trading partners.

---

## Key Capabilities

* **Control Envelope Auditing:** Validates structural pairing and control number consistency across `ISA/IEA` (Interchange), `GS/GE` (Functional Group), and `ST/SE` (Transaction Set) envelopes, including segment counts (`SE01`).
* **Transaction-Specific Rule Validation:** Enforces mandatory segment presence and qualifier rules tailored to logistics and retail supply chain transaction sets.
* **Human-Readable Segment Decoding:** Translates raw X12 segment tags (e.g., `B10`, `AT7`, `N1`, `TD5`, `BSN`, `HL`, `PO1`) into clear functional descriptions for faster root-cause analysis.
* **Actionable Defect Reporting:** Highlights exact line/segment failures with clear remediation guidance so analysts can resolve payload errors without manual line-by-line inspection.

---

## Supported ANSI X12 Transaction Sets (Version 004010)

| Transaction Set | Standard Name | Primary Supply Chain Use Case |
| :--- | :--- | :--- |
| **110** | Air Freight Details and Invoice | Air carrier freight billing and charge breakdowns |
| **210** | Motor Carrier Freight Details and Invoice | Road/trucking freight invoicing and rate validation |
| **214** | Transportation Carrier Shipment Status | Real-time transit milestones, pickup, and delivery updates |
| **310** | Ocean Freight Details and Invoice | Ocean carrier bill of lading and freight charge invoicing |
| **315** | Status Details (Ocean) | Container milestone events (gate-in, vessel departure, arrival, customs release) |
| **850** | Purchase Order | Retail/shipper order initiation and SKU/quantity specifications |
| **856** | Ship Notice / Manifest (ASN) | Advance shipment notifications and hierarchical packaging details |

---

## Tech Stack

* **Language & UI:** Kotlin, Jetpack Compose (Android)
* **AI & Validation Engine:** Google AI Studio / Gemini API (Structured Outputs)
* **Domain Standards:** ANSI ASC X12 Version 004010

---

## Local Setup & Configuration

1. Clone this repository:
   ```bash
   git clone [https://github.com/Juswind/ansi-x12-edi-validator.git](https://github.com/Juswind/ansi-x12-edi-validator.git)


  (Update the repository URL above if you name your GitHub repo differently.)
  
2. Open the project in Android Studio.

3. Copy .env.example to .env (or add your key to local.properties) and configure your own Gemini API key:GEMINI_API_KEY=your_api_key_here

4. Build and run on an Android device or emulator.

## About the Author
Andrews Rajkumar — Senior Business Analyst | Supply Chain, ERP & TMS Workflows | CargoWise Certified Professional (CCP)

Portfolio: (https://juswind.github.io/)

LinkedIn: (https://www.linkedin.com/in/andrews-rajkumar/)

YouTube: (https://www.youtube.com/@AndrewsRajkumarEDI)
