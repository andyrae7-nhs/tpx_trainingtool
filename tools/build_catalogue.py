"""Generates backend/src/main/resources/data/catalogue.json - the training catalogue.
Edit the lists below (or the JSON directly) to add your own resources."""
import json, re, urllib.parse

def slug(s): return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")
def book(q): return "https://openlibrary.org/search?q=" + urllib.parse.quote_plus(q)
def coursera(q): return "https://www.coursera.org/search?query=" + urllib.parse.quote_plus(q)
def linkedin(q): return "https://www.linkedin.com/learning/search?keywords=" + urllib.parse.quote_plus(q)

R = []
def add(title, type_, provider, url, description, tags, level="Foundation", duration=None, cost="Free", fmt=None):
    R.append(dict(id=slug(title)[:60], title=title, type=type_, provider=provider, url=url,
                  description=description, tags=tags, level=level, duration=duration, cost=cost, format=fmt))

INT = "TPXimpact Talent & Development"
# --- Internal Consulting Skills programme (from 'Consulting Skills - for T&D Hackathon') ---
cs = [
 ("Intro to client contexts, TPXimpact delivery models and mindsets", "Client Relationships", "Foundation",
  "How our clients work, how we deliver with them and the mindsets that make a great TPXimpact consultant. The starting point for everyone on the programme.", ["consulting","trust","stakeholders","delivery","client"]),
 ("Understanding client needs", "Client Relationships", "Foundation",
  "Listen for what clients need rather than what they ask for. Covers discovery conversations, questioning techniques and playing back understanding.", ["client","stakeholders","research","requirements","communication"]),
 ("Building trust and the art of challenge", "Client Relationships", "Intermediate",
  "Become a critical friend: build credibility, then use it to challenge constructively without damaging the relationship.", ["trust","influence","feedback","client","stakeholders"]),
 ("Navigating conflict", "Client Relationships", "Intermediate",
  "Practical tools for difficult conversations, disagreement and tension within teams and with clients.", ["conflict","feedback","communication","teams","resilience"]),
 ("Systemic leadership", "Client Relationships", "Advanced",
  "Lead across organisational boundaries, see the whole system and shape change with multiple stakeholders.", ["leadership","systems-thinking","strategy","influence"]),
 ("Operating effectively in client settings", "Collaborative Working", "Foundation",
  "Show up well on client sites and in multidisciplinary teams: ways of working, etiquette, managing your time and visibility.", ["collaboration","client","ownership","time-management","presenting"]),
 ("Communication and storytelling", "Collaborative Working", "Foundation",
  "Structure your message, tailor it to your audience and tell the story of your work so it lands with clients and colleagues.", ["communication","storytelling","presenting","writing"]),
 ("Structured problem solving", "Collaborative Working", "Intermediate",
  "Break down ambiguous problems, form hypotheses, weigh options and make a clear recommendation with incomplete information.", ["problem-solving","ambiguity","analysis","strategy"]),
 ("Workshop design and facilitation", "Collaborative Working", "Intermediate",
  "Design and run workshops that get to outcomes, in person and remote. Includes agendas, activities and handling tricky dynamics.", ["facilitation","workshops","collaboration","communication"]),
 ("Executive influence", "Collaborative Working", "Advanced",
  "Engage senior stakeholders, frame work around outcomes they care about and influence decisions.", ["influence","stakeholders","presenting","leadership","storytelling"]),
 ("Strategic negotiation", "Commercial Stewardship", "Advanced",
  "Prepare for and conduct negotiations on scope, price and change, creating value for both sides.", ["negotiation","commercial","influence"]),
 ("Ambassador mindsets and the value loop", "Commercial Stewardship", "Foundation",
  "How everyone contributes to growth: noticing opportunities, sharing our work and representing TPXimpact.", ["growth","commercial","storytelling","sharing"]),
 ("Spotting and shaping new opportunities", "Commercial Stewardship", "Intermediate",
  "Turn client conversations into well-shaped opportunities and contribute to bids, proposals and pitches.", ["growth","bids","commercial","client"]),
 ("Anticipating problems and managing risks", "Commercial Stewardship", "Intermediate",
  "Spot delivery and commercial risk early, escalate well and put mitigations in place.", ["risk","delivery","ownership","problem-solving"]),
 ("Commercial acumen and trade-offs", "Commercial Stewardship", "Intermediate",
  "Understand how our projects make money, read a project's financial health and make sensible trade-offs.", ["commercial","finance","time-management","planning"]),
 ("Strategic planning and roadmaps", "Commercial Stewardship", "Advanced",
  "Build roadmaps that connect delivery to outcomes and help clients plan for the long term.", ["strategy","planning","outcomes","product"]),
 ("Building resilience", "Foundation", "Foundation",
  "Look after yourself in demanding client environments: energy, boundaries and bouncing back from setbacks.", ["resilience","wellbeing","time-management"]),
 ("Coaching skills for consultants", "Foundation", "Intermediate",
  "Use coaching conversations to develop colleagues and client teams. Covers questioning, listening and GROW.", ["coaching","mentoring","feedback","capability-building"]),
]
for title, pillar, lvl, desc, tags in cs:
    add(title, "PROGRAMME", INT + " - Consulting Skills: " + pillar, None, desc, tags, level=lvl,
        duration="Half-day workshop + practice", cost="Internal", fmt="Workshop, peer learning and live project application")

add("Communities of Practice", "PROGRAMME", "TPXimpact", None,
    "Join your practice's Community of Practice to share work in the open, learn from peers and contribute to playbooks.",
    ["sharing","learning","capability-building","collaboration","practice"], cost="Internal", fmt="Community")
add("Run a lunch and learn", "EVENT", "TPXimpact", None,
    "Share something you've learned or built in a 30-minute internal session. Great evidence for 'Practice area' and 'Developing your craft'.",
    ["sharing","presenting","capability-building","storytelling","practice"], cost="Internal", fmt="Internal talk")
add("Bid and proposal shadowing", "PROGRAMME", "TPXimpact Growth team", None,
    "Pair with the Growth team on a live bid: write a section, review a response or join a pitch rehearsal.",
    ["bids","growth","writing","commercial"], level="Intermediate", cost="Internal", fmt="On the job")
add("Mentoring scheme", "PROGRAMME", "TPXimpact", None,
    "Be matched with a mentor from another practice, or become a mentor yourself.",
    ["mentoring","coaching","learning","capability-building","feedback"], cost="Internal", fmt="1-to-1")

# --- Public: government and standards ---
add("GOV.UK Service Manual", "ARTICLE", "Government Digital Service", "https://www.gov.uk/service-manual",
    "Guidance on designing, building and running public services, from discovery to live. The reference for how government delivers digital work.",
    ["delivery","lifecycle","agile","service-design","product","research","public-sector"])
add("Agile delivery in government", "ARTICLE", "Government Digital Service", "https://www.gov.uk/service-manual/agile-delivery",
    "How agile works in the public sector: phases, team roles, governance and ceremonies.",
    ["agile","delivery","lifecycle","teams","public-sector"])
add("The Service Standard", "ARTICLE", "Government Digital Service", "https://www.gov.uk/service-manual/service-standard",
    "The 14 points government services are assessed against. Useful for anyone preparing a team for a service assessment.",
    ["lifecycle","service-design","product","outcomes","public-sector","assurance"])
add("GOV.UK Design System", "ARTICLE", "Government Digital Service", "https://design-system.service.gov.uk/",
    "Styles, components and patterns for building accessible government services, with research behind each one.",
    ["interaction-design","accessibility","content-design","frontend","patterns"])
add("Writing to GOV.UK standards", "ARTICLE", "GOV.UK Publishing", "https://guidance.publishing.service.gov.uk/writing-to-gov-uk-standards/",
    "GOV.UK's guidance on user needs, plain English and writing for the web.",
    ["content-design","writing","accessibility","communication"])
add("Web Content Accessibility Guidelines (WCAG)", "ARTICLE", "W3C Web Accessibility Initiative", "https://www.w3.org/WAI/standards-guidelines/wcag/",
    "The international standard for accessible digital content, with quick references and techniques.",
    ["accessibility","inclusion","interaction-design","content-design","testing","frontend"])
add("Digital Accessibility Foundations", "COURSE", "W3C Web Accessibility Initiative", "https://www.w3.org/WAI/courses/foundations-course/",
    "Free self-paced course on the principles of accessibility, how people with disabilities use the web and how to start applying standards.",
    ["accessibility","inclusion","interaction-design","content-design","research"], duration="~15 hours", fmt="Self-paced online")
add("The Green Book: appraisal and evaluation", "ARTICLE", "HM Treasury", "https://www.gov.uk/government/publications/the-green-book-appraisal-and-evaluation-in-central-government",
    "How government appraises options and builds business cases. Essential for benefits cases and option analysis.",
    ["benefits","outcomes","business-analysis","finance","options","public-sector","impact"], level="Intermediate")
add("The Magenta Book: guidance for evaluation", "ARTICLE", "HM Treasury", "https://www.gov.uk/government/publications/the-magenta-book",
    "Government guidance on designing and running evaluations - theory of change, methods and measuring impact.",
    ["impact","research","outcomes","benefits","analytics","public-sector"], level="Intermediate")
add("The Technology Code of Practice", "ARTICLE", "Central Digital and Data Office", "https://www.gov.uk/guidance/the-technology-code-of-practice",
    "Criteria for designing, building and buying technology in government: open standards, cloud first, security and more.",
    ["architecture","cloud","security","strategy","public-sector"])
add("Government Digital and Data Profession Capability Framework", "ARTICLE", "Central Digital and Data Office", "https://ddat-capability-framework.service.gov.uk/",
    "Roles and skill levels across digital, data and technology. Handy to compare against our own framework and client expectations.",
    ["learning","capability-building","public-sector","career"])
add("The Government Data Quality Framework", "ARTICLE", "Government Data Quality Hub", "https://www.gov.uk/government/publications/the-government-data-quality-framework",
    "Principles and practical tools for assessing and improving data quality.",
    ["data","data-governance","data-engineering","analytics","public-sector"])

# --- Agile, product, BA ---
add("The Scrum Guide", "ARTICLE", "Scrum.org / Scrum Inc.", "https://scrumguides.org/scrum-guide.html",
    "The definitive short guide to Scrum's roles, events and artefacts. Twenty minutes well spent.",
    ["agile","delivery","teams","product"], duration="20 minutes")
add("Atlassian Team Playbook", "ARTICLE", "Atlassian", "https://www.atlassian.com/team-playbook",
    "Free, step-by-step 'plays' for retrospectives, health monitors, roles and responsibilities and more.",
    ["facilitation","workshops","teams","agile","collaboration","wellbeing"])
add("Mind the Product", "ARTICLE", "Mind the Product", "https://www.mindtheproduct.com/",
    "Articles, talks and events on product management, roadmaps, discovery and outcomes.",
    ["product","strategy","outcomes","roadmaps","benefits"])
add("Business analysis courses", "COURSE", "Coursera", coursera("business analysis"),
    "A range of courses on requirements, process modelling and business analysis techniques.",
    ["business-analysis","requirements","modelling","process"], cost="Free to audit / Paid", fmt="Self-paced online")
add("IIBA - business analysis body of knowledge and certification", "PROGRAMME", "International Institute of Business Analysis", "https://www.iiba.org/",
    "Home of the BABOK Guide and ECBA/CCBA/CBAP certifications.",
    ["business-analysis","requirements","modelling","process"], level="Intermediate", cost="Paid")
add("Process improvement with Lean", "COURSE", "LinkedIn Learning", linkedin("lean process improvement"),
    "Short courses on value stream mapping, waste and continuous improvement.",
    ["process","agile","continuous-improvement","modelling"], cost="Paid", fmt="Self-paced online")
add("Project and financial management courses", "COURSE", "LinkedIn Learning", linkedin("project finance budgeting"),
    "Budgeting, forecasting and managing project finances.",
    ["finance","commercial","delivery","planning"], cost="Paid", fmt="Self-paced online")

# --- Design & research ---
add("Nielsen Norman Group articles", "ARTICLE", "Nielsen Norman Group", "https://www.nngroup.com/articles/",
    "Evidence-based articles on UX research methods, interaction design and usability.",
    ["research","interaction-design","service-design","usability","content-design"])
add("Design Council resources", "ARTICLE", "Design Council", "https://www.designcouncil.org.uk/",
    "Home of the Double Diamond and the Systemic Design Framework - useful for design strategy and systems change.",
    ["service-design","design-strategy","systems-thinking","org-design"])
add("Service design courses", "COURSE", "LinkedIn Learning", linkedin("service design"),
    "Service blueprints, journey mapping and designing end-to-end services.",
    ["service-design","research","org-design"], cost="Paid", fmt="Self-paced online")
add("Just Enough Research - Erika Hall", "BOOK", "A Book Apart", book("Just Enough Research Erika Hall"),
    "A short, practical book on planning and running research that answers the questions that matter.",
    ["research","service-design","product"])
add("Content Design - Sarah Winters", "BOOK", "Content Design London", book("Content Design Sarah Winters"),
    "The book on content design from one of the people who shaped GOV.UK.",
    ["content-design","writing","accessibility"])
add("Org Design for Design Orgs - Peter Merholz & Kristin Skinner", "BOOK", "O'Reilly", book("Org Design for Design Orgs"),
    "How to build and lead design teams inside organisations.",
    ["org-design","design-strategy","leadership","capability-building"], level="Intermediate")

# --- Tech & data ---
add("AWS Skill Builder", "COURSE", "Amazon Web Services", "https://skillbuilder.aws/",
    "Free and paid AWS training from cloud essentials to specialty certifications.",
    ["cloud","infrastructure","architecture","devops","security"], cost="Free / Paid", fmt="Self-paced online")
add("Microsoft Learn training", "COURSE", "Microsoft", "https://learn.microsoft.com/en-us/training/",
    "Free learning paths for Azure, Power Platform, Dynamics 365 and more.",
    ["cloud","infrastructure","business-applications","data","devops"], fmt="Self-paced online")
add("Google Cloud Skills Boost", "COURSE", "Google Cloud", "https://www.cloudskillsboost.google/",
    "Hands-on labs and courses for Google Cloud, data and machine learning.",
    ["cloud","data","ai","infrastructure"], cost="Free / Paid", fmt="Hands-on labs")
add("Terraform tutorials", "COURSE", "HashiCorp", "https://developer.hashicorp.com/terraform/tutorials",
    "Learn infrastructure as code with Terraform across AWS, Azure and GCP.",
    ["infrastructure","cloud","devops","automation"], fmt="Hands-on tutorials")
add("Kubernetes tutorials", "COURSE", "Kubernetes", "https://kubernetes.io/docs/tutorials/",
    "The official hands-on introduction to deploying and scaling containerised apps.",
    ["cloud","devops","infrastructure","containers"], level="Intermediate")
add("OWASP Top 10", "ARTICLE", "OWASP Foundation", "https://owasp.org/www-project-top-ten/",
    "The most critical web application security risks and how to prevent them.",
    ["security","software-engineering","testing","architecture"])
add("NCSC guidance", "ARTICLE", "National Cyber Security Centre", "https://www.ncsc.gov.uk/",
    "UK guidance on cloud security, secure development and cyber resilience.",
    ["security","cloud","architecture","public-sector"])
add("Martin Fowler's articles", "ARTICLE", "martinfowler.com", "https://martinfowler.com/",
    "Long-standing writing on software architecture, refactoring, microservices and delivery.",
    ["software-engineering","architecture","coding","devops"], level="Intermediate")
add("The Twelve-Factor App", "ARTICLE", "12factor.net", "https://12factor.net/",
    "A short methodology for building modern, deployable, scalable services.",
    ["software-engineering","cloud","architecture","devops"])
add("roadmap.sh", "ARTICLE", "roadmap.sh", "https://roadmap.sh/",
    "Community learning roadmaps for backend, frontend, DevOps, QA, data and more.",
    ["software-engineering","coding","devops","testing","data","learning"])
add("MDN: Learn web development", "COURSE", "Mozilla", "https://developer.mozilla.org/en-US/docs/Learn",
    "Free structured curriculum for HTML, CSS, JavaScript and accessibility.",
    ["coding","frontend","accessibility","software-engineering"], fmt="Self-paced online")
add("Test Automation University", "COURSE", "Applitools", "https://testautomationu.applitools.com/",
    "Free courses on test automation frameworks, API testing and CI.",
    ["testing","automation","software-engineering","coding"], fmt="Self-paced online")
add("Ministry of Testing", "PROGRAMME", "Ministry of Testing", "https://www.ministryoftesting.com/",
    "Community, courses and events for software testers.",
    ["testing","automation","learning"], cost="Free / Paid", fmt="Community")
add("Kaggle Learn", "COURSE", "Kaggle", "https://www.kaggle.com/learn",
    "Free, short, hands-on courses in Python, SQL, data visualisation and machine learning.",
    ["data","analytics","ai","coding","data-engineering"], fmt="Hands-on notebooks")
add("DAMA Data Management Body of Knowledge", "BOOK", "DAMA International", "https://www.dama.org/cpages/body-of-knowledge",
    "The reference for data governance, architecture, quality and metadata management.",
    ["data-governance","data","architecture","data-engineering"], level="Advanced", cost="Paid")
add("Designing Data-Intensive Applications - Martin Kleppmann", "BOOK", "O'Reilly", book("Designing Data-Intensive Applications"),
    "How databases, streams and distributed systems really work. A must for data engineers and architects.",
    ["data-engineering","architecture","software-engineering","data"], level="Advanced")
add("Storytelling with Data - Cole Nussbaumer Knaflic", "BOOK", "Wiley", book("Storytelling with Data Knaflic"),
    "Turn analysis into clear charts and narratives that drive decisions.",
    ["analytics","storytelling","data","communication","presenting"])
add("ITIL 4 Foundation", "PROGRAMME", "PeopleCert", "https://www.peoplecert.org/",
    "The standard framework for IT service management: incident, problem, change and continual improvement.",
    ["service-management","support","continuous-improvement","process"], cost="Paid", fmt="Certification")
add("Accelerate - Forsgren, Humble & Kim", "BOOK", "IT Revolution", book("Accelerate Forsgren Humble Kim"),
    "The research behind high-performing technology teams and the four key delivery metrics.",
    ["devops","delivery","software-engineering","leadership","outcomes"], level="Intermediate")
add("Team Topologies - Skelton & Pais", "BOOK", "IT Revolution", book("Team Topologies Skelton Pais"),
    "How to organise technology teams for fast flow.",
    ["org-design","teams","architecture","leadership"], level="Intermediate")
add("Generative AI courses", "COURSE", "Coursera", coursera("generative AI"),
    "Introductions to generative AI, prompt design and responsible AI.",
    ["ai","emerging-tech","data"], cost="Free to audit / Paid", fmt="Self-paced online")

# --- Consulting, leadership and behaviours (books & courses) ---
books = [
 ("The Trusted Advisor - Maister, Green & Galford", "Building client trust through credibility, reliability, intimacy and low self-orientation.", ["trust","client","consulting","stakeholders","influence"]),
 ("The Pyramid Principle - Barbara Minto", "Structure your thinking and writing so the answer comes first. The classic for consultants.", ["communication","writing","problem-solving","storytelling","presenting"]),
 ("Flawless Consulting - Peter Block", "How to contract, diagnose and implement with clients - the human side of consulting.", ["consulting","client","trust","influence"]),
 ("Crucial Conversations - Patterson, Grenny, McMillan & Switzler", "Tools for talking when stakes are high, opinions vary and emotions run strong.", ["conflict","feedback","communication","stakeholders"]),
 ("Radical Candor - Kim Scott", "Care personally and challenge directly: a framework for feedback that helps people grow.", ["feedback","leadership","mentoring","teams"]),
 ("The Coaching Habit - Michael Bungay Stanier", "Seven questions that make everyday conversations more coaching-led.", ["coaching","mentoring","leadership","feedback","capability-building"]),
 ("Thinking in Systems - Donella Meadows", "An accessible introduction to systems thinking, feedback loops and leverage points.", ["systems-thinking","strategy","ambiguity","problem-solving"]),
 ("Good Strategy Bad Strategy - Richard Rumelt", "What good strategy looks like: diagnosis, guiding policy and coherent action.", ["strategy","planning","leadership","problem-solving"]),
 ("Never Split the Difference - Chris Voss", "Negotiation techniques built on tactical empathy and listening.", ["negotiation","influence","commercial"]),
 ("Facilitator's Guide to Participatory Decision-Making - Sam Kaner", "The go-to handbook for running inclusive meetings and workshops.", ["facilitation","workshops","collaboration","inclusion"]),
 ("Gamestorming - Gray, Brown & Macanufo", "A playbook of workshop games for innovators and changemakers.", ["facilitation","workshops","collaboration"]),
 ("The Culture Map - Erin Meyer", "Work well across cultures by understanding different communication and feedback styles.", ["inclusion","communication","collaboration","feedback"]),
 ("Experiential Learning - David Kolb", "The theory behind learning by doing, reflecting and applying - the basis of our Consulting Skills programme.", ["learning","reflection","capability-building"]),
 ("The Phoenix Project - Kim, Behr & Spafford", "A novel about DevOps, flow and fixing a failing IT project.", ["devops","delivery","process","service-management"]),
 ("Inspired - Marty Cagan", "How to create tech products customers love, and the role of the product manager.", ["product","strategy","outcomes","roadmaps"]),
 ("Drive - Daniel Pink", "What really motivates people: autonomy, mastery and purpose.", ["leadership","teams","wellbeing","mentoring"]),
 ("Four Thousand Weeks - Oliver Burkeman", "Time management for people who know they can't do everything.", ["time-management","wellbeing","resilience","ownership"]),
 ("The Fearless Organization - Amy Edmondson", "Psychological safety: why it matters and how leaders build it.", ["inclusion","teams","leadership","wellbeing"]),
]
for t, d, tags in books:
    add(t, "BOOK", t.split(" - ")[1] if " - " in t else "", book(t.split(" - ")[0]), d, tags, level="Intermediate")

courses = [
 ("Learning How to Learn", "Coursera", "https://www.coursera.org/learn/learning-how-to-learn", "A hugely popular free course on how learning works and how to do it better.", ["learning","reflection"]),
 ("Negotiation courses", "Coursera", coursera("negotiation"), "Foundations of preparing for and conducting negotiations.", ["negotiation","commercial","influence"]),
 ("Facilitation skills", "LinkedIn Learning", linkedin("facilitation skills"), "Short courses on planning and running effective sessions.", ["facilitation","workshops"]),
 ("Giving and receiving feedback", "LinkedIn Learning", linkedin("giving feedback"), "Practical models for feedback conversations.", ["feedback","mentoring","communication"]),
 ("Stakeholder management", "LinkedIn Learning", linkedin("stakeholder management"), "Map, engage and influence stakeholders.", ["stakeholders","influence","communication","client"]),
 ("Presentation and storytelling skills", "LinkedIn Learning", linkedin("storytelling presentation"), "Structure and deliver compelling presentations.", ["presenting","storytelling","communication"]),
 ("Critical thinking and problem solving", "LinkedIn Learning", linkedin("critical thinking problem solving"), "Frameworks for analysing problems and making decisions.", ["problem-solving","ambiguity","analysis"]),
 ("Bid and proposal writing", "LinkedIn Learning", linkedin("proposal writing"), "Write winning proposals and bid responses.", ["bids","writing","growth","commercial"]),
 ("Free courses from OpenLearn", "The Open University", "https://www.open.edu/openlearn/", "Hundreds of free short courses in management, leadership, psychology and technology.", ["learning","leadership","management"]),
 ("Inclusive leadership", "LinkedIn Learning", linkedin("inclusive leadership"), "Build inclusive teams and practices.", ["inclusion","leadership","teams"]),
]
for t, p, u, d, tags in courses:
    add(t, "COURSE", p, u, d, tags, cost="Free / Paid", fmt="Self-paced online")

events = [
 ("Agile on the Beach", "https://agileonthebeach.com/", "Annual UK conference on agile, product and team practices.", ["agile","product","teams","delivery"]),
 ("LeadDev", "https://leaddev.com/", "Conferences and articles for engineering leaders.", ["leadership","software-engineering","teams","mentoring"]),
 ("UX London", "https://uxlondon.com/", "Three days of talks and workshops on UX and design leadership.", ["research","interaction-design","service-design","design-strategy"]),
 ("Big Data LDN", "https://bigdataldn.com/", "Free UK data and AI conference and exhibition.", ["data","ai","analytics","data-engineering"]),
 ("AWS Summits", "https://aws.amazon.com/events/summits/", "Free one-day cloud events with technical sessions.", ["cloud","architecture","infrastructure","devops"]),
]
for t, u, d, tags in events:
    add(t, "EVENT", t, u, d, tags, cost="Free / Paid", fmt="Conference")

ids = [r["id"] for r in R]
assert len(ids) == len(set(ids)), "duplicate ids"
json.dump(R, open("backend/src/main/resources/data/catalogue.json", "w"), indent=1, ensure_ascii=False)
print(len(R), "resources")
