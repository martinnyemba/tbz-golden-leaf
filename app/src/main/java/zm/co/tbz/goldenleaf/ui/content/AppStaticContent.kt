package zm.co.tbz.goldenleaf.ui.content

object AppStaticContent {
    val ABOUT = """
TBZ Golden Leaf is the official field application for Tobacco Board of Zambia (TBZ) officers.

Use it to register growers, schedule inspections, capture sales, manage transport and group permits, and sync work offline when connectivity is limited.
    """.trimIndent()

    val TERMS = """
These terms govern use of the TBZ Golden Leaf mobile application by authorized TBZ staff and partners.

You must keep login credentials confidential, follow TBZ data protection policies, and use the app only for official TBZ business. Unauthorized access or data export is prohibited.

TBZ may suspend access when policies are violated or when maintenance is required.
    """.trimIndent()

    val PRIVACY = """
TBZ Golden Leaf processes grower personal data, location coordinates, inspection records, and marketing transactions to support regulatory compliance.

Data is transmitted to TRMCS servers over encrypted connections. Offline copies are stored on your device until sync completes. Do not share device access with unauthorized persons.

Contact TBZ IT for data subject requests or breach reporting.
    """.trimIndent()

    val GUIDELINES = """
TBZ Regulatory Guidelines (summary)

Registration
- Verify NRC and grower identity before submission.
- Capture accurate GPS for farm location.
- Crop hectarage must be between 0.5 and 300 ha.

Movement & permits
- Transport permits require an approved grower validation.
- Group permits need at least two growers on one vehicle movement.
- QR tokens must be validated at the sales floor before bale capture.

Marketing
- Scan or enter permit tokens before recording bales.
- Rejected bales require a documented reason.

Inspection & arbitration
- Schedule inspections before field visits when possible.
- Arbitration requires a valid bale ticket and rejection reason when applicable.
    """.trimIndent()
}
