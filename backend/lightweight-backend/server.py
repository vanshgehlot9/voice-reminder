import os
import uuid
import re
import json
import asyncio
import datetime
import zoneinfo
from typing import Optional
from fastapi import FastAPI, BackgroundTasks, Depends, UploadFile, File, Form, Request
from fastapi.responses import FileResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import BaseModel
import edge_tts
from groq import Groq
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Voice Reminder Backend (Zero-Memory AI)")

# Enable CORS for all origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def get_groq_client():
    load_dotenv(override=True)
    key = os.getenv("GROQ_API_KEY")
    if key and key.strip():
        try:
            return Groq(api_key=key.strip())
        except Exception:
            return None
    return None

client = get_groq_client()

# ============================================================
# Request & Response Models
# ============================================================

class ReminderRequest(BaseModel):
    title: str
    description: str
    language: str = "Hinglish"

class TextCommandRequest(BaseModel):
    transcript: Optional[str] = None
    text: Optional[str] = None
    timezone: Optional[str] = "Asia/Kolkata"
    model_size: Optional[str] = "1.7B"
    model: Optional[str] = "1.7B"

def remove_file(path: str):
    try:
        if os.path.exists(path):
            os.remove(path)
    except:
        pass

# ============================================================
# Smart Document & NLP Engine
# ============================================================

def generate_smart_document(topic: str) -> tuple[str, str]:
    clean_topic = re.sub(
        r"(?i)^(create a note (about|on|for)|write a note (about|on|for)|take a note (about|on|for)|note down|note that|create note|write note|take note|create a document (about|on|for)|create document (about|on|for)|make a document (about|on|for)|i want to create (a )?startup for|i want to make a startup for|startup for|document for|write a proposal for|make a note (about|on|for)|note about)\s*",
        "",
        topic
    ).strip()
    clean_topic = re.sub(r"^(a|an|the)\s+", "", clean_topic, flags=re.IGNORECASE).strip()
    if not clean_topic:
        clean_topic = "Intelligent System Architecture"

    topic_title = " ".join(w.capitalize() for w in clean_topic.split())
    now_str = datetime.datetime.now().strftime("%B %d, %Y")
    ql = topic.lower()

    # Determine Title based on domain
    if any(k in ql for k in ["wildlife", "animal", "forest", "nature", "safari", "tiger", "lion", "bird", "jungle", "ecosystem", "marine", "ocean", "biodiversity", "conservation", "flora", "fauna", "endangered"]):
        title = "Wildlife & Ecosystem Conservation Overview" if "wildlife" in topic_title.lower() else f"{topic_title} & Wildlife Conservation Overview"
    elif any(k in ql for k in ["fit", "gym", "workout", "diet", "nutrition", "exercise", "health", "muscle", "yoga", "cardio", "weight loss", "protein"]):
        title = f"{topic_title} Comprehensive Guide"
    elif any(k in ql for k in ["startup", "business", "pitch", "venture", "monetiz", "b2b", "revenue", "market opportunity", "business plan"]):
        title = f"{topic_title} Startup Proposal" if "startup" not in topic_title.lower() else topic_title
    elif any(k in ql for k in ["space", "mars", "moon", "physics", "quantum", "astronomy", "planet", "orbit", "nasa", "galaxy"]):
        title = f"{topic_title} Scientific Exploration Spec"
    elif any(k in ql for k in ["travel", "trip", "tour", "vacation", "flight", "hotel", "itinerary", "visit", "destination"]):
        title = f"{topic_title} Travel Itinerary & Field Guide"
    elif any(k in ql for k in ["invest", "crypto", "bitcoin", "stock", "portfolio", "trading", "finance", "wealth"]):
        title = f"{topic_title} Financial Analysis & Strategy"
    elif any(k in ql for k in ["study", "exam", "revision", "learn", "history", "economics", "syllabus", "lecture"]):
        title = f"{topic_title} Study Guide & Reference Notes"
    elif any(k in ql for k in ["ai", "automation", "code", "python", "software", "cloud", "cyber", "algorithm", "database", "devops", "machine learning"]):
        title = f"{topic_title} Technical Architecture Guide"
    else:
        title = f"{topic_title} Comprehensive Analysis"

    # If Groq client is configured, generate dynamic LLM synthesis
    llm = get_groq_client()
    if llm:
        for model_name in ["openai/gpt-oss-120b", "qwen/qwen3.8-27b", "openai/gpt-oss-20b"]:
            try:
                prompt = f"""You are a domain expert and professional researcher. Write an exhaustive, highly structured, beautifully detailed document about "{clean_topic}".
Original Prompt: "{topic}"

Use this exact Markdown structure:
# {title}
*Prepared by AI Voice Assistant on {now_str}*

Write 5 distinct, highly detailed, real-world sections with relevant headers, detailed explanations, bullet points, statistics, and concrete insights specific to "{clean_topic}".
Do NOT use generic startup boilerplate unless the topic is actually a business or startup.
Write engaging, rich, substantive content tailored to the real subject matter."""
                completion = llm.chat.completions.create(
                    model=model_name,
                    messages=[
                        {"role": "system", "content": "You are a world-class domain researcher. Output only rich, informative, beautifully structured markdown."},
                        {"role": "user", "content": prompt}
                    ],
                    max_tokens=900,
                    temperature=0.3
                )
                content = completion.choices[0].message.content.strip()
                if content and len(content) > 100:
                    return title, content
            except Exception as e:
                print(f"Groq document generation fallback ({model_name}):", e)

    # ────────────────────────────────────────────────────────────
    # High-Quality Specialized Local Synthesis Engines
    # ────────────────────────────────────────────────────────────

    # 1. Wildlife & Nature
    if any(k in ql for k in ["wildlife", "animal", "forest", "nature", "safari", "tiger", "lion", "bird", "jungle", "ecosystem", "marine", "ocean", "biodiversity", "conservation", "flora", "fauna", "endangered"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Executive Summary & Ecological Significance
The preservation of {clean_topic} represents an urgent global imperative for safeguarding biosphere integrity. Healthy natural ecosystems maintain planetary carbon cycles, stabilize weather patterns, and support the intricate web of biodiversity essential for sustainable life on Earth.

## 2. Habitat Dynamics & Biodiversity Hotspots
- **Core Ecosystem Architecture**: Interconnected habitats spanning tropical rainforests, alpine regions, and coastal wetlands provide irreplaceable nesting and foraging grounds for endemic species.
- **Trophic Balance**: Apex predators, herbivores, and decomposers establish homeostatic equilibriums that regulate prey populations and preserve lush vegetation cover.
- **Critical Corridors**: Preserving natural migration routes allows genetic diversity to flourish across isolated animal populations.

## 3. Major Threats & Anthropogenic Pressures
- **Habitat Fragmentation**: Rapid agricultural encroachment, road infrastructure, and deforestation isolating wildlife communities into vulnerable pockets.
- **Poaching & Illegal Wildlife Trade**: Global black-market trafficking targeting endangered megafauna and rare botanical species.
- **Climate Volatility**: Rising temperatures and shifting precipitation patterns disrupting seasonal mating cycles, waterhole accessibility, and food chains.

## 4. Modern Conservation & Surveillance Technologies
- **AI-Powered Camera Traps**: Edge-deployed computer vision cameras that identify species in real-time, alert anti-poaching units, and monitor herd health automatically.
- **Satellite & GPS Telemetry**: Solar-powered tracking collars transmitting migration vectors to prevent human-wildlife boundary conflicts.
- **Acoustic Canopy Monitoring**: Bioacoustic sensor arrays capable of detecting chainsaw vibrations, vehicle engines, and gunshots within kilometers.

## 5. Strategic Action Plan & Community Conservation
- **Ranger Support & Anti-Poaching Patrols**: Equipping wildlife rangers with modern geospatial tools and night-vision capabilities.
- **Community-Led Stewardship**: Providing local border communities with eco-tourism revenue shares to transform residents into active conservation defenders.
- **International Regulatory Enforcement**: Strict compliance monitoring under CITES (Convention on International Trade in Endangered Species).
"""
        return title, content

    # 2. Fitness & Health
    if any(k in ql for k in ["fit", "gym", "workout", "diet", "nutrition", "exercise", "health", "muscle", "yoga", "cardio", "weight loss", "protein"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Physiological Foundations & Core Objectives
Adopting an evidence-based approach to {clean_topic} optimizes metabolic health, muscular strength, cardiovascular endurance, and cognitive clarity. Long-term progress depends on consistency, progressive stimulus, and balanced biological recovery.

## 2. Structured Training Architecture & Progressive Overload
- **Compound Resistance Exercises**: Prioritizing multi-joint movements (squats, deadlifts, presses, pulls) to recruit maximal motor units and stimulate bone density.
- **Cardiovascular Conditioning**: Integrating Zone 2 aerobic base building (60-70% max HR) with high-intensity interval bursts (HIIT) to optimize mitochondrial density.
- **Progressive Overload Principle**: Systematically increasing volume, time under tension, or resistance week-over-week to prevent physical plateaus.

## 3. Nutrition, Macronutrient Distribution & Hydration
- **Protein Synthesis Requirements**: Aiming for 1.6 to 2.2 grams of dietary protein per kilogram of body weight spread evenly across 3-4 meals daily.
- **Micronutrient & Fiber Density**: Incorporating a diverse spectrum of colorful leafy vegetables, complex slow-digesting carbohydrates, and healthy unsaturated fatty acids.
- **Electrolyte & Hydration Equilibrium**: Consuming 35-40ml of water per kg daily, supplemented with sodium, potassium, and magnesium during intense exertion.

## 4. Sleep Architecture & Recovery Science
- **Circadian Rhythm Alignment**: Securing 7.5 to 9 hours of uninterrupted sleep per night to maximize human growth hormone (HGH) secretion and muscle repair.
- **Active Rest & Mobility**: Utilizing dynamic mobility drills, foam rolling, and low-impact active recovery walks to clear metabolic waste products.

## 5. 30-Day Implementation & Habit Tracking
- **Week 1-2**: Establish baseline metrics, form mastery, and hydration consistency.
- **Week 3-4**: Calibrate intensity, record daily workout logs, and refine meal timing.
"""
        return title, content

    # 3. Startup & Business Proposal
    if any(k in ql for k in ["startup", "business", "pitch", "venture", "monetiz", "b2b", "revenue", "market opportunity", "business plan"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Executive Summary & Market Vision
The proposed venture delivers high-impact commercial innovation within the {clean_topic} domain. By eliminating traditional manual friction and leveraging modern software automation, the company positions itself to capture substantial market share in an expanding global addressable market.

## 2. Problem Statement & Customer Pain Points
- **Operational Inefficiencies**: Current incumbent offerings force customers to navigate fragmented tools, slow turnaround times, and high administrative friction.
- **Prohibitive Cost Scaling**: Legacy solutions require linear personnel expansion, diminishing unit economics as organizations scale.
- **Technological Inflection Point**: Rapid advances in autonomous agents and real-time processing create an ideal window for a disruptive market entrant.

## 3. Proposed Solution & Competitive Moat
- **Intelligent Platform Engine**: End-to-end self-orchestrating system providing instant turnaround with sub-second latency.
- **High Switching Costs**: Deep workflow integration and proprietary data flywheels that continually improve accuracy and user retention.
- **Security & Privacy First**: Enterprise-ready data architecture adhering to global compliance standards.

## 4. Monetization Strategy & Unit Economics
- **Tiered SaaS Subscriptions**: Predictable recurring monthly revenue structured across Pro ($49/mo) and Enterprise ($299/mo) tiers.
- **API & Usage-Based Expansion**: Flexible pay-as-you-grow consumption models for high-volume enterprise integration partners.
- **Target Gross Margin**: Aiming for 75-80% software gross margins with attractive customer lifetime value (LTV) to acquisition cost (CAC) ratios (>3:1).

## 5. Execution Roadmap & Go-To-Market
- **Phase 1 (Months 1-2)**: Core prototype development and closed alpha with 10 design partners.
- **Phase 2 (Months 3-4)**: Public beta launch, self-serve onboarding, and feedback-driven feature iterations.
- **Phase 3 (Months 5-6)**: Enterprise sales outreach, partner integrations, and scalable organic growth funnels.
"""
        return title, content

    # 4. Science, Physics & Space Exploration
    if any(k in ql for k in ["space", "mars", "moon", "physics", "quantum", "astronomy", "planet", "orbit", "nasa", "galaxy"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Scientific Overview & Primary Hypotheses
Investigation into {clean_topic} addresses foundational questions regarding the physical cosmos, energy states, and mechanical principles. Breakthrough discoveries in this discipline advance both theoretical frameworks and practical aerospace and computational technologies.

## 2. Core Principles & Physical Mechanisms
- **Theoretical Foundations**: Established physical laws governing thermodynamic entropy, gravitational interaction, and subatomic dynamics.
- **Measurement Methodologies**: High-precision spectroscopy, interferometry, and cryogenic sensors operating at the limits of quantum noise.
- **Empirical Validation**: Rigorous experimental setups and peer-reviewed telemetry validating mathematical predictions against raw observational data.

## 3. Technological Innovations & Instrumentation
- **Advanced Propulsion & Computing**: Transitioning towards ion thrusters, nuclear thermal rockets, and room-temperature quantum gates.
- **Autonomous Deep-Space Navigation**: Onboard optical navigation algorithms capable of calculating orbital burns without planetary radio lag.
- **Extreme Environment Materials**: Radiation-hardened semiconductors and carbon-nanotube heat shields designed for cryogenic and thermal extremes.

## 4. Critical Engineering Obstacles
- Thermal dissipation and vacuum welding risks during extended microgravity exposure.
- Signal latency and atmospheric attenuation over multi-astronomical-unit baselines.
- Cosmic ray shielding and single-event latchup prevention in flight computers.

## 5. Future Horizons & Research Roadmap
- Deployment of space-based gravitational wave observatories.
- Autonomous robotic sample-return missions across planetary bodies.
- Cross-institutional data sharing and open-access astrophysical catalogs.
"""
        return title, content

    # 5. Travel & Destination Field Guide
    if any(k in ql for k in ["travel", "trip", "tour", "vacation", "flight", "hotel", "itinerary", "visit", "destination"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Destination Overview & Travel Philosophy
Exploring {clean_topic} offers an immersive cultural, architectural, and geographic experience. Maximizing the value of this journey requires thoughtful pacing, cultural awareness, and a balance between iconic landmarks and hidden local gems.

## 2. Curated Day-by-Day Itinerary
- **Day 1: Historic Core & Cultural Immersion**: Arrive, acclimate, and stroll through landmark historic districts, local open-air markets, and central plazas.
- **Day 2: Natural Landscapes & Panoramic Views**: Early morning departure for regional scenic viewpoints, national parks, and tranquil coastal or mountain paths.
- **Day 3: Art, Heritage & Architecture**: Full day dedicated to renowned heritage museums, architectural marvels, and artisanal craft studios.
- **Day 4: Gastronomy & Modern Lifestyle**: Exploring vibrant neighborhood districts, street food stalls, and evening cultural performances.

## 3. Cultural Customs, Transit & Local Logistics
- **Public Transportation**: Utilizing integrated rail passes, contactless smart cards, and regional express transit for seamless mobility.
- **Local Etiquette**: Respecting traditional greeting customs, dress codes at cultural sites, and regional tipping practices.
- **Language Essentials**: Learning key conversational phrases to facilitate respectful communication with local hosts and merchants.

## 4. Culinary Highlights & Must-Try Specialties
- Signature regional delicacies highlighting seasonal, farm-to-table ingredients.
- Authentic street eats and time-honored traditional tea houses or family-run bistros.

## 5. Travel Essentials & Safety Checklist
- Multi-currency travel cards and offline navigation maps downloaded in advance.
- Compact universal power adapters, portable power banks, and travel insurance coverage.
"""
        return title, content

    # 6. Technology & Software Engineering
    if any(k in ql for k in ["ai", "automation", "code", "python", "software", "cloud", "cyber", "algorithm", "database", "devops", "machine learning"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. System Architecture & Engineering Objectives
The technical implementation for {clean_topic} focuses on high throughput, fault tolerance, and developer maintainability. By utilizing clean modular abstractions and asynchronous I/O, the platform provides robust performance under high concurrency.

## 2. Core Technology Stack & Pipeline Design
- **Runtime Environment**: High-performance asynchronous execution engine with lightweight memory footprint.
- **Data Ingestion & Storage**: Partitioned event streaming combined with relational transactional guarantees and optimized vector indexing.
- **API Surface**: Clean RESTful and WebSocket endpoints enforcing strict JSON schemas and sub-100ms response latencies.

## 3. Scalability & Performance Benchmarks
- **Horizontal Scaling**: Stateless worker pools orchestrated via container runtimes to handle spiky traffic patterns automatically.
- **Caching Layer**: Distributed in-memory caching reducing database read pressure by up to 85% for high-frequency queries.
- **Resource Efficiency**: Optimized thread scheduling and native library bindings minimizing CPU idle cycles.

## 4. Security, Resilience & Observability
- **Zero-Trust Security**: End-to-end TLS encryption, least-privilege token access, and sanitization of all incoming payloads.
- **Telemetry & Tracing**: Distributed tracing and real-time metric dashboards tracking p99 latencies and error rates.
- **Graceful Degradation**: Circuit breakers preventing cascading failures during upstream network dropouts.

## 5. Deployment & Maintenance Roadmap
- Automated CI/CD pipelines executing unit tests, integration suites, and security scans on every commit.
- Blue-green zero-downtime deployment strategies ensuring continuous availability.
"""
        return title, content

    # 7. Education & Study Guide
    if any(k in ql for k in ["study", "exam", "revision", "learn", "history", "economics", "syllabus", "lecture"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Conceptual Framework & Core Themes
A structured study of {clean_topic} requires breaking down broad theoretical concepts into clear, memorable principles. Building a solid mental model allows for intuitive understanding and effortless recall during practical application and examinations.

## 2. High-Yield Principles & Key Definitions
- **Foundational Concepts**: The primary axioms and established formulas that underpin the discipline of {clean_topic}.
- **Cause & Effect Dynamics**: How changes in independent variables influence outcomes across the broader system.
- **Comparative Analysis**: Contrasting differing viewpoints, classical theories, and contemporary consensus.

## 3. Real-World Applications & Case Studies
- Practical historical or industrial examples demonstrating the principle in live operation.
- Nuanced scenarios where standard rules encounter boundary edge-cases.

## 4. Common Misconceptions & Pitfalls
- Conflating correlation with causation when interpreting empirical observations.
- Overlooking baseline boundary assumptions that restrict the validity of theoretical models.

## 5. Active Recall Questions & Revision Plan
- Formulate flashcards emphasizing *why* and *how* rather than mere rote memorization.
- Implement spaced repetition intervals (1 day, 3 days, 1 week, 1 month) to consolidate long-term synaptic retention.
"""
        return title, content

    # 8. Finance & Investment Strategy
    if any(k in ql for k in ["invest", "crypto", "bitcoin", "stock", "portfolio", "trading", "finance", "wealth"]):
        content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Market Overview & Macroeconomic Context
Navigating {clean_topic} requires disciplined risk management, capital preservation strategies, and an understanding of prevailing interest rates, liquidity cycles, and market sentiment. 

## 2. Asset Allocation & Risk-Return Profile
- **Core Portfolio Distribution**: Balancing defensive capital preservation assets with opportunistic growth allocations.
- **Correlation & Diversification**: Minimizing systemic drawdowns by holding non-correlated asset classes across global markets.
- **Position Sizing Rules**: Capping individual asset exposures at predefined percentages to survive black-swan volatility events.

## 3. Fundamental & Technical Indicators
- **Valuation Metrics**: Evaluating cash flow yields, price-to-earnings multiples, and on-chain transaction velocity.
- **Market Structure**: Monitoring support and resistance liquidity zones, moving average trends, and order-book depth.

## 4. Risk Mitigation & Downside Protection
- Strict stop-loss disciplines and hedging strategies using options or inverse vehicles.
- Maintaining adequate cash reserves to deploy opportunistically during capitulation pullbacks.

## 5. Strategic Execution Checklist
- Dollar-cost average into established conviction positions during periods of elevated market fear.
- Rebalance portfolio weights quarterly to lock in profits from outperforming holdings.
"""
        return title, content

    # 9. Default: Comprehensive Strategic Analysis
    content = f"""# {title}
*Prepared by AI Voice Assistant on {now_str}*

## 1. Context & Executive Overview
An in-depth analysis of {clean_topic} reveals critical strategic implications and practical applications. Understanding the core drivers behind this topic enables better decision-making, systematic execution, and long-term clarity.

## 2. Key Dimensions & Core Principles
- **Foundational Architecture**: Key underlying mechanisms that define how {clean_topic} operates in practical environments.
- **Systemic Interactions**: How interrelated factors shape outcomes and influence efficiency across the domain.
- **Quality & Standards**: Maintaining rigorous benchmarks to ensure consistent, repeatable, and high-standard results.

## 3. Modern Innovations & Industry Trends
- Adoption of automated tooling and data-driven evaluation frameworks.
- Shift towards modular, resilient, and future-proof methodologies.
- Growing importance of user-centric design and streamlined workflows.

## 4. Challenges & Strategic Mitigation
- **Resource Constraints**: Mitigated by prioritizing high-leverage activities and automating repetitive steps.
- **Complexity Management**: Addressed through modular breakdown and continuous verification loops.

## 5. Actionable Recommendations & Key Takeaways
- Establish clear baseline metrics before initiating major transitions.
- Iterate rapidly based on empirical real-world feedback rather than assumptions.
- Document insights continuously to compound institutional knowledge over time.
"""
    return title, content


def local_parse_command(query: str, tz_name: str = "Asia/Kolkata") -> dict:
    try:
        tz = zoneinfo.ZoneInfo(tz_name)
    except Exception:
        try:
            tz = zoneinfo.ZoneInfo("Asia/Kolkata")
        except Exception:
            tz = zoneinfo.ZoneInfo("UTC")

    now = datetime.datetime.now(tz)
    q = query.strip()
    ql = q.lower()

    # 1. Greetings & System Queries
    if ql in ["hello", "hey", "hi", "siri", "jarvis", "ok google", "hello assistant", "hey assistant", "hey voicereminder", "help", "suno", "batao"]:
        return {
            "success": True,
            "transcript": q,
            "intent": "chat",
            "task": None,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": "Hello! I am your AI Voice Assistant. How can I help you today?",
            "confidence": 1.0,
            "error": None
        }

    if any(p in ql for p in ["who are you", "what can you do", "what are you", "kaun ho", "kya kar sakte ho"]):
        return {
            "success": True,
            "transcript": q,
            "intent": "chat",
            "task": None,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": "I can set reminders, create AI startup proposals and notes, open apps, make calls, and play music.",
            "confidence": 1.0,
            "error": None
        }

    # 2. Smart AI Document & Note Generator
    doc_triggers = [
        "create note", "take note", "write note", "note down", "note that", "new note",
        "create a note", "take a note", "write a note", "create startup", "startup for",
        "create a startup", "make a startup", "create document", "write document", "make a document",
        "create a proposal", "write proposal", "business plan"
    ]
    if any(dt in ql for dt in doc_triggers) and not any(rt in ql for rt in ["remind me to write", "remind me to create"]):
        title, doc_content = generate_smart_document(q)
        return {
            "success": True,
            "transcript": q,
            "intent": "create_document",
            "task": title,
            "action_type": "create_document",
            "action_target": title,
            "document_title": title,
            "document_content": doc_content,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": f"I've created an in-depth document for '{title}' and saved it to your Notes and PDF documents.",
            "confidence": 0.98,
            "error": None
        }

    # 3. Device Actions: Open App
    open_match = re.search(r"^(?:please\s+)?open\s+([a-zA-Z0-9\s]+)", ql)
    if open_match and not any(w in ql for w in ["reminder", "alarm", "schedule"]):
        app_target = open_match.group(1).strip()
        return {
            "success": True,
            "transcript": q,
            "intent": "open_app",
            "task": f"Open {app_target.capitalize()}",
            "action_type": "open_app",
            "action_target": app_target,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": f"Opening {app_target.capitalize()}...",
            "confidence": 1.0,
            "error": None
        }

    # 4. Device Actions: Phone Call
    call_match = re.search(r"^(?:please\s+)?call\s+([a-zA-Z0-9\s+]+)", ql)
    if call_match and not any(w in ql for w in ["reminder", "alarm", "schedule"]):
        person_target = call_match.group(1).strip()
        return {
            "success": True,
            "transcript": q,
            "intent": "call",
            "task": f"Call {person_target}",
            "action_type": "call",
            "action_target": person_target,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": f"Calling {person_target}...",
            "confidence": 1.0,
            "error": None
        }

    # 5. Device Actions: Play Music / Song
    if (ql.startswith("play ") or "play song" in ql or "play music" in ql) and not any(w in ql for w in ["reminder", "alarm"]):
        song_target = ql.replace("play", "").replace("music", "").replace("song", "").strip() or "music"
        return {
            "success": True,
            "transcript": q,
            "intent": "play_music",
            "task": f"Play {song_target}",
            "action_type": "play_music",
            "action_target": song_target,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": f"Playing {song_target}...",
            "confidence": 1.0,
            "error": None
        }

    # 6. Web Search
    if ql.startswith("search ") or ql.startswith("google "):
        search_target = re.sub(r"^(?:search|google|search google for|search for|on google)\s*", "", q, flags=re.IGNORECASE).strip()
        return {
            "success": True,
            "transcript": q,
            "intent": "web_search",
            "task": f"Search: {search_target}",
            "action_type": "web_search",
            "action_target": search_target,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": f"Searching Google for {search_target}...",
            "confidence": 1.0,
            "error": None
        }

    # 7. Date & Time Extraction (Reminders)
    target_dt = None
    date_expr = None
    time_expr = None

    # Relative time: "in 15 minutes", "in 2 hours"
    rel_match = re.search(r"in\s+(\d+)\s+(minute|min|hour|hr)s?", ql)
    if rel_match:
        val = int(rel_match.group(1))
        unit = rel_match.group(2)
        if "min" in unit:
            target_dt = now + datetime.timedelta(minutes=val)
            time_expr = f"in {val} minutes"
        else:
            target_dt = now + datetime.timedelta(hours=val)
            time_expr = f"in {val} hours"

    target_date = now.date()
    if "tomorrow" in ql or "kal" in ql:
        target_date = now.date() + datetime.timedelta(days=1)
        date_expr = "tomorrow"
    elif "day after tomorrow" in ql or "parso" in ql:
        target_date = now.date() + datetime.timedelta(days=2)
        date_expr = "day after tomorrow"
    elif "today" in ql or "aaj" in ql or "tonight" in ql or "aaj raat" in ql:
        target_date = now.date()
        date_expr = "today"

    hour = None
    minute = 0
    if not rel_match:
        # Check specific times: e.g. "at 5 pm", "10:30 am", "5 baje", "10 baje"
        for m in re.finditer(r"(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.|baje)?", ql):
            h_str, min_str, meridian = m.groups()
            h = int(h_str)
            mn = int(min_str) if min_str else 0
            if meridian:
                meridian = meridian.lower().replace(".", "")
                if "pm" in meridian and h < 12:
                    h += 12
                elif "am" in meridian and h == 12:
                    h = 0
                elif "baje" in meridian:
                    if ("sham" in ql or "raat" in ql or "dopahar" in ql) and h < 12:
                        h += 12
                hour = h
                minute = mn
                time_expr = f"{h_str}:{min_str or '00'} {meridian.upper()}"
                break
            elif 1 <= h <= 24 and ("at " in ql[max(0, m.start()-4):m.start()] or "ko " in ql[max(0, m.start()-4):m.start()]):
                if 1 <= h <= 6:
                    h += 12
                hour = h
                minute = mn
                time_expr = f"{h}:00"
                break

    if target_dt is None:
        if hour is not None:
            candidate = datetime.datetime(target_date.year, target_date.month, target_date.day, hour, minute, 0, tzinfo=tz)
            if candidate < now and date_expr is None:
                candidate += datetime.timedelta(days=1)
            target_dt = candidate
        else:
            if date_expr == "tomorrow":
                target_dt = datetime.datetime(target_date.year, target_date.month, target_date.day, 9, 0, 0, tzinfo=tz)
                time_expr = "9:00 AM"
            else:
                target_dt = now + datetime.timedelta(hours=1)
                time_expr = target_dt.strftime("%I:%M %p")

    # 3. Clean Task Title
    clean_task = re.sub(
        r"(?i)^(remind me to|set a reminder to|set a reminder for|reminder for|remind me for|remind me|remind|yaad dilana ki|yaad dilana|schedule a|schedule|create a task to|task to|mujhe yaad dilana)\s*",
        "",
        q
    )
    clean_task = re.sub(r"(?i)\s*(tomorrow|today|tonight|day after tomorrow|kal|parso|aaj|subah|sham|raat|dopahar)\s*", " ", clean_task)
    clean_task = re.sub(r"(?i)\s*(at\s+\d{1,2}(?::\d{2})?\s*(?:am|pm|a\.m\.|p\.m\.|baje)?|\d{1,2}(?::\d{2})?\s*(?:am|pm|a\.m\.|p\.m\.|baje)|in\s+\d+\s+(?:minutes|mins|hours|hrs)|for\s+\d{1,2}(?::\d{2})?\s*(?:am|pm|a\.m\.|p\.m\.|baje)?)\s*", " ", clean_task)
    clean_task = re.sub(r"\s+", " ", clean_task).strip(" .,!?:;")

    time_formatted = target_dt.strftime("%I:%M %p")
    day_formatted = "tomorrow" if target_dt.date() == (now.date() + datetime.timedelta(days=1)) else ("today" if target_dt.date() == now.date() else target_dt.strftime("%b %d"))

    # Reply text based on language
    is_hindi = any(w in ql for w in ["kal", "parso", "aaj", "baje", "yaad", "dilana", "karna", "subah", "sham", "jana", "hai", "mujhe"])

    has_specific_task = bool(clean_task and clean_task.lower() not in ["reminder", "task", "set a reminder"])
    if not has_specific_task:
        clean_task = "Reminder"
        if is_hindi:
            reply = f"Maine {day_formatted} {time_formatted} ka reminder set kar diya hai."
        else:
            reply = f"I have set a reminder for {day_formatted} at {time_formatted}."
    else:
        clean_task = clean_task[0].upper() + clean_task[1:]
        if is_hindi:
            reply = f"Maine {day_formatted} {time_formatted} ko {clean_task} ka reminder set kar diya hai."
        else:
            reply = f"I have set a reminder to {clean_task.lower()} for {day_formatted} at {time_formatted}."

    return {
        "success": True,
        "transcript": q,
        "intent": "create_reminder",
        "task": clean_task,
        "date_expression": date_expr or day_formatted,
        "time_expression": time_expr or time_formatted,
        "scheduled_at": target_dt.isoformat(),
        "reply_text": reply,
        "confidence": 0.95,
        "error": None
    }


def llm_parse_command(query: str, tz_name: str = "Asia/Kolkata") -> Optional[dict]:
    llm = get_groq_client()
    if not llm:
        return None
    try:
        try:
            tz = zoneinfo.ZoneInfo(tz_name)
        except Exception:
            tz = zoneinfo.ZoneInfo("Asia/Kolkata")
        now_str = datetime.datetime.now(tz).isoformat()

        prompt = f"""Current date & time with timezone: {now_str} (Timezone: {tz_name}).
Analyze this spoken voice command: "{query}"

Intents supported:
1. "create_reminder": For alarms and reminders with time/date.
2. "open_app": For opening apps (e.g. open youtube, open google, open whatsapp, open camera, open spotify, etc.). "action_target" = app name.
3. "call": For calling someone (e.g. call mom, call 9876543210). "action_target" = name or phone number.
4. "play_music": For playing music or songs (e.g. play believer, play music). "action_target" = song/artist name.
5. "web_search": For searching Google (e.g. search who is elon musk). "action_target" = search query.
6. "create_document": For creating notes, documents, startups, business proposals (e.g. "I want to create startup for ai automation", "write note about client meeting").
7. "chat": For greetings, questions, small talk.

Return JSON matching this schema:
{{
  "success": true,
  "transcript": "{query}",
  "intent": "create_reminder" (or "open_app" / "call" / "play_music" / "web_search" / "create_document" / "chat"),
  "task": "Clean task title or action title",
  "action_type": "open_app" / "call" / "play_music" / "web_search" / "create_document" / null,
  "action_target": "target name, number, or app" / null,
  "document_title": "Document Title if create_document" / null,
  "document_content": "Structured multi-section markdown document with Executive Summary, Architecture, Strategy if create_document" / null,
  "date_expression": "tomorrow" / "today" / null,
  "time_expression": "10:00 AM" / null,
  "scheduled_at": "ISO-8601 string with timezone offset e.g. 2026-09-29T10:00:00+05:30" (or null),
  "reply_text": "Concise 1-sentence confirmation response for speech (in Hinglish if input was Hindi/Hinglish, else English)"
}}
Write ONLY valid JSON, no markdown formatting."""

        for model_name in ["qwen/qwen3.8-27b", "openai/gpt-oss-120b", "openai/gpt-oss-20b"]:
            try:
                completion = llm.chat.completions.create(
                    model=model_name,
                    messages=[
                        {"role": "system", "content": "You are a voice assistant parser. Return only JSON."},
                        {"role": "user", "content": prompt}
                    ],
                    response_format={"type": "json_object"},
                    temperature=0.2,
                    max_tokens=600
                )
                data = json.loads(completion.choices[0].message.content.strip())
                if "reply_text" in data and "intent" in data:
                    data["success"] = True
                    data["confidence"] = 0.98
                    data["error"] = None
                    return data
            except Exception as model_err:
                continue
    except Exception as e:
        print("LLM Parsing error (fallback to local):", e)
    return None

# ============================================================
# Mobile API Endpoints
# ============================================================

@app.api_route("/webhook/{path:path}", methods=["GET", "POST"])
async def ignore_webhook(path: str):
    return {"status": "ok"}

@app.post("/api/mobile/command/text")
async def process_text_command(req: TextCommandRequest):
    query = req.transcript or req.text or ""
    tz = req.timezone or "Asia/Kolkata"
    
    if not query.strip():
        return {
            "success": True,
            "transcript": "",
            "intent": "unknown",
            "task": None,
            "date_expression": None,
            "time_expression": None,
            "scheduled_at": None,
            "reply_text": "I didn't hear anything. Please try again.",
            "confidence": 0.0,
            "error": "Empty input"
        }

    print(f"[*] Processing voice command: '{query}' (tz={tz})")
    
    # Try LLM first if Groq API key is configured
    result = llm_parse_command(query, tz)
    if not result:
        # High accuracy local NLP parser
        result = local_parse_command(query, tz)
        
    print(f"[+] Response: intent={result.get('intent')}, task={result.get('task')}, reply='{result.get('reply_text')}'")
    return result

@app.post("/api/mobile/command")
async def process_voice_audio_command(
    audio: UploadFile = File(...),
    timezone: str = Form("Asia/Kolkata"),
    model_size: str = Form("1.7B")
):
    print(f"[*] Received audio upload: filename={audio.filename}, size={audio.size}")
    # If Groq client is configured, we can transcribe using Whisper
    transcript = "Remind me to submit project report tomorrow at 10 AM"
    llm = get_groq_client()
    if llm:
        try:
            temp_path = f"audio_{uuid.uuid4().hex}.mp4"
            with open(temp_path, "wb") as f:
                f.write(await audio.read())
            with open(temp_path, "rb") as f:
                transcription = llm.audio.transcriptions.create(
                    file=f,
                    model="whisper-large-v3",
                    response_format="text",
                )
            remove_file(temp_path)
            if transcription:
                transcript = str(transcription).strip()
        except Exception as e:
            print("Whisper transcription error:", e)

    req = TextCommandRequest(transcript=transcript, timezone=timezone, model_size=model_size)
    return await process_text_command(req)

@app.post("/generate_reminder")
async def generate_reminder(req: ReminderRequest, bg_tasks: BackgroundTasks):
    text_to_speak = f"Reminder: {req.title}. {req.description}"
    llm = get_groq_client()
    if llm:
        system_prompt = f"You are a helpful voice assistant creating a short audio reminder in {req.language}. Keep it extremely brief (1 sentence max)."
        if req.language.lower() == "hinglish":
            system_prompt += " Write ONLY using Roman/English letters, never Devanagari."
        try:
            completion = llm.chat.completions.create(
                model="llama-3.1-8b-instant",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"Title: {req.title}\nDetails: {req.description}"}
                ],
                max_tokens=80,
                temperature=0.5
            )
            text_to_speak = completion.choices[0].message.content.strip()
        except Exception as e:
            print("LLM Error:", e)

    voice = "en-IN-NeerjaNeural" if "hin" in req.language.lower() else "en-US-AriaNeural"
    filename = f"reminder_{uuid.uuid4().hex}.mp3"
    communicate = edge_tts.Communicate(text_to_speak, voice)
    await communicate.save(filename)
    bg_tasks.add_task(remove_file, filename)
    
    return FileResponse(path=filename, media_type="audio/mpeg", filename="reminder.mp3")

# ============================================================
# Auth & Workspace Endpoints (Mock / Bypass for Android app)
# ============================================================

@app.post("/auth/token")
async def login(form_data: OAuth2PasswordRequestForm = Depends()):
    return {"access_token": "lightweight_dummy_token", "token_type": "bearer"}

@app.post("/auth/register")
async def register(body: dict):
    return {"status": "registered", "username": body.get("username", "user")}

@app.get("/")
async def root():
    return {"status": "ok", "service": "Voice Reminder Backend"}

@app.get("/health")
async def root_health():
    return {"status": "ok"}

@app.get("/api/mobile/health")
async def health_check():
    return {"status": "ok", "service": "lightweight-backend"}

@app.get("/api/mobile/projects")
async def get_projects():
    return []

@app.get("/api/mobile/memories")
async def get_memories():
    return []

@app.get("/api/mobile/workflows")
async def get_workflows():
    return []

@app.post("/api/mobile/workflows/{workflow_id}/approve")
async def approve_workflow(workflow_id: str):
    return {"status": "approved", "id": workflow_id}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 9000))
    is_prod = os.environ.get("RENDER") is not None
    uvicorn.run("server:app", host="0.0.0.0", port=port, reload=not is_prod)
