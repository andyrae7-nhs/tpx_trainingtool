package com.tpximpact.trainingtool.gacha;

import com.tpximpact.trainingtool.catalogue.CatalogueService;
import com.tpximpact.trainingtool.catalogue.Resource;
import com.tpximpact.trainingtool.gacha.Idea.Kind;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Everything you can pull: hand-written development actions and project ideas, plus every
 * resource in the training catalogue. Catalogue rarity comes from its level (Foundation = Common,
 * Intermediate = Rare, Advanced = Epic) and events go up one tier. Legendaries are hand-picked.
 */
@Component
public class IdeaPool {

    private final List<Idea> all;
    private final Map<String, Idea> byId = new LinkedHashMap<>();
    private final Map<Rarity, List<Idea>> byRarity = new EnumMap<>(Rarity.class);

    public IdeaPool(CatalogueService catalogue) {
        List<Idea> ideas = new ArrayList<>(HANDWRITTEN);
        for (Resource r : catalogue.all()) {
            ideas.add(new Idea("res:" + r.id(), Kind.RESOURCE, rarityFor(r), r.title(),
                    Objects.requireNonNullElse(r.description(), ""), iconFor(r.type()), r.url(), r.id()));
        }
        this.all = List.copyOf(ideas);
        for (Rarity rarity : Rarity.values()) byRarity.put(rarity, new ArrayList<>());
        for (Idea idea : all) {
            if (byId.put(idea.id(), idea) != null) throw new IllegalStateException("Duplicate idea id " + idea.id());
            byRarity.get(idea.rarity()).add(idea);
        }
        byRarity.forEach((rarity, list) -> {
            if (list.isEmpty()) throw new IllegalStateException("No ideas with rarity " + rarity);
        });
    }

    public List<Idea> all() { return all; }

    public Optional<Idea> find(String id) { return Optional.ofNullable(byId.get(id)); }

    public List<Idea> ofRarity(Rarity rarity) { return byRarity.get(rarity); }

    public int size() { return all.size(); }

    static Rarity rarityFor(Resource r) {
        Rarity base = switch (Objects.requireNonNullElse(r.level(), "Foundation")) {
            case "Advanced" -> Rarity.EPIC;
            case "Intermediate" -> Rarity.RARE;
            default -> Rarity.COMMON;
        };
        if ("EVENT".equals(r.type()) && base != Rarity.EPIC) {
            base = Rarity.values()[base.ordinal() + 1];
        }
        return base;
    }

    static String iconFor(String type) {
        return switch (Objects.requireNonNullElse(type, "")) {
            case "BOOK" -> "📖";
            case "COURSE" -> "💻";
            case "EVENT" -> "🎟️";
            case "PROGRAMME" -> "🎓";
            case "ARTICLE" -> "📰";
            default -> "📚";
        };
    }

    private static Idea action(String id, Rarity rarity, String icon, String title, String description) {
        return new Idea("act:" + id, Kind.ACTION, rarity, title, description, icon, null, null);
    }

    private static Idea project(String id, Rarity rarity, String icon, String title, String description) {
        return new Idea("prj:" + id, Kind.PROJECT, rarity, title, description, icon, null, null);
    }

    private static final List<Idea> HANDWRITTEN = List.of(
            // ---------- Development actions: consulting ----------
            action("playback", Rarity.COMMON, "🔁", "Play it back",
                    "In your next client conversation, summarise what you heard in one sentence before responding. Note what changed."),
            action("five-whys", Rarity.COMMON, "❓", "Five whys",
                    "Take one request from your backlog and ask 'why?' five times. Write down the real need you uncovered."),
            action("stakeholder-map", Rarity.COMMON, "🗺️", "Map your stakeholders",
                    "Sketch a power/interest grid for your current project in 15 minutes. Share it with a teammate and compare."),
            action("feedback-ask", Rarity.COMMON, "🙋", "Ask for one piece of feedback",
                    "Ask a colleague: 'What's one thing I could do differently in meetings?' Log the answer in your journal."),
            action("one-pager", Rarity.COMMON, "📄", "One-page summary",
                    "Rewrite your last long update as a one-pager: situation, options, recommendation."),
            action("meeting-purpose", Rarity.COMMON, "🎯", "Purpose, outcome, agenda",
                    "Open your next meeting by stating its purpose and desired outcome in under 30 seconds."),
            action("lunch-learn", Rarity.RARE, "🥪", "Host a lunch and learn",
                    "Run a 20-minute session on something you know well. Ask attendees for one takeaway each."),
            action("challenge-kindly", Rarity.RARE, "🤝", "Challenge, kindly",
                    "Find one assumption on your project you disagree with and raise it using 'I've noticed… I wonder if…'."),
            action("workshop-design", Rarity.RARE, "🧩", "Design a workshop",
                    "Plan a 60-minute workshop with clear outcomes, activities and timings, even if you don't run it yet."),
            action("proposal-section", Rarity.RARE, "✍️", "Write a proposal section",
                    "Volunteer to draft one section of a bid or proposal. Ask the bid lead for feedback on it."),
            action("coach-someone", Rarity.EPIC, "🌱", "Coach someone for a month",
                    "Offer four 30-minute coaching sessions to a more junior colleague. Agree a goal at the start."),
            action("lead-retro", Rarity.EPIC, "🔄", "Facilitate a client retro",
                    "Run a retrospective with the client in the room. Capture actions with owners and dates."),

            // ---------- Development actions: technical ----------
            action("read-pr", Rarity.COMMON, "👀", "Review a stranger's PR",
                    "Review a pull request in a codebase you don't normally touch. Leave one question and one compliment."),
            action("til", Rarity.COMMON, "💡", "Today I learned",
                    "Post a 'TIL' in the community feed about something technical you learned this week."),
            action("explain-simply", Rarity.COMMON, "🧒", "Explain it simply",
                    "Explain a technical concept from your project to a non-technical colleague in two minutes."),
            action("adr", Rarity.RARE, "🏛️", "Write an ADR",
                    "Document one architectural decision on your project: context, options, decision, consequences."),
            action("pair", Rarity.RARE, "👯", "Pair for a morning",
                    "Pair program or pair design with someone from another capability for half a day."),
            action("accessibility-audit", Rarity.RARE, "♿", "Mini accessibility audit",
                    "Run one page of your service through a screen reader and an automated checker. Fix or log the issues."),
            action("threat-model", Rarity.EPIC, "🛡️", "Threat model a feature",
                    "Use STRIDE on one feature with your team. Turn the top three risks into backlog items."),
            action("tech-talk", Rarity.EPIC, "🎤", "Give a tech talk",
                    "Submit a 15-minute talk to an internal community of practice or a local meetup."),

            // ---------- Project and hackathon ideas ----------
            project("slack-standup", Rarity.COMMON, "🤖", "Stand-up bot",
                    "A tiny bot that collects async stand-up updates and posts a summary to the team channel."),
            project("jargon-buster", Rarity.COMMON, "📘", "Jargon buster",
                    "A searchable glossary of client and public sector acronyms, built from your project docs."),
            project("meeting-cost", Rarity.COMMON, "⏱️", "Meeting cost clock",
                    "A browser widget that shows the running cost of a meeting based on who's in the room."),
            project("retro-generator", Rarity.RARE, "🎲", "Retro format generator",
                    "Suggests a fresh retrospective format each sprint based on team mood and size."),
            project("user-research-repo", Rarity.RARE, "🔎", "Research insight repository",
                    "Tag and search user research quotes across projects so insights don't get lost."),
            project("carbon-dashboard", Rarity.RARE, "🌍", "Cloud carbon dashboard",
                    "Estimate the carbon footprint of a service's cloud usage and suggest greener regions."),
            project("gov-form-ai", Rarity.EPIC, "📝", "Plain English form checker",
                    "An AI tool that rewrites government form questions to meet the GOV.UK style guide."),
            project("bid-library", Rarity.EPIC, "📚", "Reusable bid library",
                    "Search past winning bid answers by capability and client, with AI-suggested first drafts."),
            project("skills-graph", Rarity.EPIC, "🕸️", "Skills graph",
                    "Visualise who knows what across the company from journal tags, so staffing gets easier."),

            // ---------- Legendary ----------
            action("shadow-partner", Rarity.LEGENDARY, "👑", "Shadow a partner on a pitch",
                    "Ask to join a senior leader on a real client pitch, from prep to debrief. Write up what you'd copy."),
            action("run-discovery", Rarity.LEGENDARY, "🧭", "Lead a discovery",
                    "Put yourself forward to lead the next discovery phase, with a senior sponsor to back you up."),
            action("publish-blog", Rarity.LEGENDARY, "🌟", "Publish on the company blog",
                    "Write and publish a blog post on something you've delivered. Tag the client (with permission)."),
            action("conference-talk", Rarity.LEGENDARY, "🏟️", "Speak at a conference",
                    "Submit a talk to an external conference. Your community of practice will help you rehearse."),
            project("open-source", Rarity.LEGENDARY, "🐙", "Open-source a tool",
                    "Take something your team built and release it as open source, with docs and a licence."),
            project("hackathon-win", Rarity.LEGENDARY, "🏆", "Turn a hack into a product",
                    "Pick your best hackathon idea, write a one-page business case and pitch it to leadership.")
    );
}
