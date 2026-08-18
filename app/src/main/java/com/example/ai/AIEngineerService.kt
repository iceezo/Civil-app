package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AIEngineerService {

    enum class AIAgentRole(val title: String, val badge: String, val systemPrompt: String) {
        CIVIL_GENERAL(
            "CiviAI Principal Engineer",
            "General Lead",
            "You are the CiviAI Lead Civil & Structural Engineer. Provide rigorous, code-compliant civil engineering advice. Reference Eurocode 2 (EN 1992), ACI 318, BS 8110, and Nigerian/African regional standards where applicable. Always state design assumptions, formulas, and safety factors clearly."
        ),
        STRUCTURAL_EXPERT(
            "Structural Design Specialist",
            "Structural Analysis",
            "You are a Senior Structural Engineer. You specialize in reinforced concrete and structural steel design, bending moment and shear calculations, deflection checks, column buckling, pad/raft foundations, and bar bending schedules."
        ),
        ARCHITECT_AGENT(
            "Architectural Planning AI",
            "Architect AI",
            "You are an Architectural Space Planner. You optimize residential and commercial floor plans, room zoning (living, private, service areas), ventilation, daylighting, circulation corridors, and building setbacks."
        ),
        QUANTITY_SURVEYOR(
            "Senior Quantity Surveyor AI",
            "QS & Costing",
            "You are a Senior Quantity Surveyor (QS) and Cost Estimator. You break down civil engineering quantities (takeoffs), material schedules, cement bags, rebar tonnage, block counts, labor rates, and professional BOQ line items."
        ),
        SITE_SAFETY_CHECKER(
            "Design & Safety Compliance AI",
            "Safety & Audit",
            "You are an Engineering Quality & Safety Auditor. You review beam spans, load paths, punching shear risks, low bearing soil conditions, and flag any code violations or under-designed structural members."
        )
    }

    data class AIResponse(
        val role: AIAgentRole,
        val text: String,
        val suggestedActions: List<String> = emptyList(),
        val isOfflineFallback: Boolean = false
    )

    private const val DISCLAIMER = "\n\n⚠️ *Mandatory Engineering Disclaimer: AI-generated outputs are preliminary decision-support calculations and must be independently reviewed and stamped by a registered Professional Engineer (PE/COREN/ICE) before construction.*"

    suspend fun consultEngineer(
        userQuery: String,
        agentRole: AIAgentRole = AIAgentRole.CIVIL_GENERAL,
        contextProjectSummary: String = ""
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isNullOrBlank() || apiKey.contains("TODO") || apiKey.contains("your_api_key")) {
            return@withContext getOfflineDeterministicEngineeringAdvice(userQuery, agentRole)
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 12000
            conn.readTimeout = 15000

            val prompt = """
                ${agentRole.systemPrompt}
                
                Project Context:
                $contextProjectSummary
                
                User Request:
                $userQuery
                
                Please provide structured engineering feedback with:
                1. Concise technical analysis
                2. Key mathematical equations or code clauses (e.g. Eurocode 2 / ACI 318)
                3. Concrete actionable recommendations
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseStr)
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val suggestions = extractSuggestionsFromText(text)
                    return@withContext AIResponse(
                        role = agentRole,
                        text = text + DISCLAIMER,
                        suggestedActions = suggestions,
                        isOfflineFallback = false
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to rich offline engineering logic
        }

        return@withContext getOfflineDeterministicEngineeringAdvice(userQuery, agentRole)
    }

    private fun extractSuggestionsFromText(text: String): List<String> {
        val list = mutableListOf<String>()
        if (text.contains("beam", ignoreCase = true)) list.add("Calculate Beam Reinforcement")
        if (text.contains("column", ignoreCase = true)) list.add("Check Column Slenderness")
        if (text.contains("soil", ignoreCase = true) || text.contains("foundation", ignoreCase = true)) list.add("Size Pad Footing")
        if (text.contains("cost", ignoreCase = true) || text.contains("BOQ", ignoreCase = true)) list.add("View Full Project BOQ")
        if (text.contains("plan", ignoreCase = true) || text.contains("room", ignoreCase = true)) list.add("Open 2D CAD Floor Plan")
        if (list.isEmpty()) {
            list.addAll(listOf("Run Beam Calculator", "Generate BOQ", "Open CAD Editor"))
        }
        return list.take(3)
    }

    fun getOfflineDeterministicEngineeringAdvice(
        userQuery: String,
        agentRole: AIAgentRole
    ): AIResponse {
        val q = userQuery.lowercase()

        val text = when {
            q.contains("beam") || q.contains("span") || q.contains("reinforce") -> """
                ### 📐 Structural Beam Engineering Analysis (Eurocode 2 / BS 8110)
                
                **1. Key Design Principles:**
                - **Singly Reinforced Check:** Compute K = M_Ed / (b * d^2 * f_ck). Ensure K <= K' = 0.167. If K > 0.167, compression steel is required or section depth h must be increased.
                - **Lever Arm:** z = d * [ 0.5 + sqrt(0.25 - K/1.134) ] <= 0.95d.
                - **Tension Steel Area:** As,req = M_Ed / (0.87 * f_yk * z).
                
                **2. Practical Site Recommendations:**
                - For a typical 4.5m–6.0m residential beam, use minimum 230mm × 450mm section with 3T16 or 2T20 bottom bars.
                - Shear links: T8 @ 175mm c/c near supports, relaxed to 250mm at midspan.
                - Minimum concrete cover: 25mm (internal), 35mm (external/coastal).
            """.trimIndent()

            q.contains("column") || q.contains("axial") || q.contains("buckl") -> """
                ### 🏛️ Column Sizing & Reinforcement Guidelines
                
                **1. Axial Load Capacity:**
                - N_Rd = 0.56 * f_ck * A_c + 0.87 * f_yk * A_sc (short braced column under axial compression).
                - Minimum steel: A_sc,min = 0.002 * A_c (0.2% of gross cross-sectional area).
                - Maximum steel: A_sc,max = 0.04 * A_c (4.0% at regular height).
                
                **2. Geometry & Ties:**
                - Minimum residential column: 230mm × 230mm with 4T16 longitudinal bars.
                - For 2-storey duplexes: ground floor columns recommended at 230mm × 300mm with 6T16 bars.
                - Links/Ties: T8 @ 200mm c/c, spacing reduced to 100mm within 450mm of beam-column joints.
            """.trimIndent()

            q.contains("foundation") || q.contains("footing") || q.contains("soil") -> """
                ### 🏗️ Foundation & Geotechnical Design Guidelines
                
                **1. Soil Bearing Capacity & Sizing:**
                - Required Pad Area: A_req = (1.10 * P_service) / q_allowable.
                - For medium firm clay/sand (q_a ~ 150 kPa) and P ~ 500 kN: Base dimension ~ 1.8m x 1.8m x 400mm.
                
                **2. Critical Checks:**
                - Check punching shear at 2.0d perimeter from column face (V_Ed,punch <= V_Rd,c).
                - Provide 50mm lean concrete blinding (1:3:6) and minimum 50mm cover to prevent rebar corrosion from soil moisture.
            """.trimIndent()

            q.contains("boq") || q.contains("cost") || q.contains("estimate") || q.contains("price") -> """
                ### 📊 Quantity Surveying & Cost Optimization Insights
                
                **1. Key Cost Distribution in Residential Construction:**
                - Substructure & Foundation: ~18–22% of total cost.
                - Superstructure Frame & Blockwork: ~28–32%.
                - Roofing & Ceiling: ~14–18%.
                - Finishes & Tiling: ~18–22%.
                - Mechanical & Electrical: ~10–14%.
                
                **2. Material Efficiency Tips:**
                - Order commercial 12m steel rebar in standardized cutting multiples to reduce scrap cut-off below 4%.
                - Use 9-inch blocks for external walls and 6-inch blocks for non-loadbearing partitions to save up to 25% on masonry mortar and block costs.
            """.trimIndent()

            q.contains("mix") || q.contains("concrete") || q.contains("cement") || q.contains("sand") -> """
                ### 🧪 Concrete Batching & Material Volume Formula
                
                **1. Universal Dry Volume Constant:**
                - Wet Volume * 1.54 accounts for shrinkage and interstitial void filling between coarse and fine aggregates.
                - 1 Bag of 50kg Portland Cement occupies 0.0347 m3 dry volume (density ~ 1440 kg/m3).
                
                **2. 1:2:4 Mix per 1.0 m³ Wet Concrete:**
                - Dry volume needed = 1.54 m3.
                - Cement: ~ 6.4 bags (50kg each).
                - Sand: ~ 0.44 m3 (0.70 tonnes).
                - Granite Aggregate: ~ 0.88 m3 (1.28 tonnes).
                - Water: ~ 160 Litres (w/c ~ 0.50).
            """.trimIndent()

            else -> """
                ### 👷 CiviAI Engineering Advisory
                
                I have analyzed your query based on standard civil and structural engineering guidelines (Eurocode 2 / ACI 318 / BS 8110).
                
                - **Interactive Calculators:** You can run precise deterministic calculations for Beams, Columns, Slabs, Foundations, Blockwork, and Concrete mixes from the calculators tab.
                - **2D CAD Floor Plans:** Interactive floor plan layouts and DXF exports are available in the CAD tab.
                - **Automated BOQ:** Complete multi-section Bills of Quantities and itemized cost summaries are automatically generated for your active project.
            """.trimIndent()
        }

        return AIResponse(
            role = agentRole,
            text = text + DISCLAIMER,
            suggestedActions = listOf("Beam Design Calculator", "Footing Calculator", "View Project BOQ"),
            isOfflineFallback = true
        )
    }
}
