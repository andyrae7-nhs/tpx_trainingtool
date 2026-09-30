package com.tpximpact.trainingtool.ai;

import com.tpximpact.trainingtool.journal.JournalEntryRepository;
import com.tpximpact.trainingtool.progression.ProgressionService;
import com.tpximpact.trainingtool.user.User;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pip is TPX Grow's friendly assistant: it pops up with tips and answers questions about
 * progression, evidence and learning. Uses Claude when configured, with a built-in fallback.
 */
@Service
public class PipService {

    private final AnthropicClient ai;
    private final ProgressionService progression;
    private final JournalEntryRepository journal;
    private final Random random = new Random();

    public PipService(AnthropicClient ai, ProgressionService progression, JournalEntryRepository journal) {
        this.ai = ai;
        this.progression = progression;
        this.journal = journal;
    }

    public record ChatMessage(String role, String content) {}

    public record PipReply(String reply, String source, List<String> suggestions) {}

    private static final List<String> TIPS = List.of(
            "It looks like you're working towards a promotion! Want me to find training for your biggest gap?",
            "Tip: write journal entries while they're fresh. Future you, filling in the progression assessment, will say thanks.",
            "Tag each journal entry with the skills and behaviours it shows. The export groups your evidence by them.",
            "Finished a book or course? Share a review so colleagues can decide if it's for them.",
            "Follow a few colleagues to see what they're learning. It's a great way to find hidden gems.",
            "Good evidence says what you did and what changed because of it. Try 'I did X, which meant Y'.",
            "Feedback counts as evidence too. Paste in a kind note from a client or colleague and tag it.",
            "Your streak grows every day you visit. Small, regular steps beat a big push in March!",
            "Behaviours matter as much as technical skills for promotion. Check your behaviour gaps too.",
            "Running a lunch and learn is great evidence for 'Practice area' impact.");

    public String tip() {
        return TIPS.get(random.nextInt(TIPS.size()));
    }

    public PipReply chat(User user, String message, List<ChatMessage> history) {
        String context = context(user);
        if (ai.enabled()) {
            String system = """
                    You are Pip, the cheerful helper who pops up in TPX Grow, TPXimpact's personalised training tool.
                    You help people understand the progression framework, spot gaps, find training, write strong
                    evidence for their end-of-year progression assessment and stay motivated.
                    Personality: friendly, a little playful, never patronising. Keep replies short (under 120 words)
                    unless asked for more. Use plain British English and the active voice; avoid jargon and buzzwords.
                    When helping with evidence, encourage the format: situation, what I did, the impact it had.
                    If you don't know something about TPXimpact specifically, say so and suggest who to ask
                    (line manager, Head of Practice or Talent & Development).

                    What you know about this person:
                    """ + context;
            List<AnthropicClient.Message> msgs = new ArrayList<>();
            if (history != null) {
                for (ChatMessage m : history.stream().skip(Math.max(0, history.size() - 10)).toList()) {
                    String role = "assistant".equals(m.role()) ? "assistant" : "user";
                    if (m.content() != null && !m.content().isBlank()) msgs.add(new AnthropicClient.Message(role, m.content()));
                }
            }
            // The API requires the conversation to start with a user turn.
            while (!msgs.isEmpty() && !"user".equals(msgs.get(0).role())) msgs.remove(0);
            msgs.add(new AnthropicClient.Message("user", message));
            Optional<String> reply = ai.complete(system, mergeConsecutive(msgs), 600);
            if (reply.isPresent()) return new PipReply(reply.get().trim(), "claude", defaultSuggestions());
        }
        return new PipReply(fallback(user, message), "pip", defaultSuggestions());
    }

    private List<AnthropicClient.Message> mergeConsecutive(List<AnthropicClient.Message> in) {
        List<AnthropicClient.Message> out = new ArrayList<>();
        for (AnthropicClient.Message m : in) {
            if (!out.isEmpty() && out.get(out.size() - 1).role().equals(m.role())) {
                AnthropicClient.Message last = out.remove(out.size() - 1);
                out.add(new AnthropicClient.Message(m.role(), last.content() + "\n\n" + m.content()));
            } else {
                out.add(m);
            }
        }
        return out;
    }

    private List<String> defaultSuggestions() {
        return List.of("What's my biggest gap?", "How do I write good evidence?", "Suggest a book", "What does my target grade need?");
    }

    private String context(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getDisplayName()).append('\n');
        if (!user.isOnboarded()) {
            sb.append("They have not yet chosen their role and grade.\n");
            return sb.toString();
        }
        try {
            var report = progression.report(user, null);
            sb.append("Role: ").append(report.roleName()).append(" (").append(report.capability()).append(")\n");
            sb.append("Current grade: ").append(report.currentGradeName()).append("; target: ").append(report.targetGradeName()).append('\n');
            sb.append("Readiness for target: ").append(report.summary().readinessPercent()).append("% of expectations met\n");
            var gaps = progression.topGaps(report, 5);
            if (!gaps.isEmpty()) {
                sb.append("Top gaps:\n");
                gaps.forEach(g -> sb.append("- ").append(g.name()).append(" (").append(g.type()).append("): now ")
                        .append(g.selfLevel() == null ? "not started" : g.selfLevel()).append(", needs ").append(g.targetExpected()).append('\n'));
            }
        } catch (Exception ignored) {
            // context is best-effort
        }
        var entries = journal.findByUserIdOrderByEntryDateDescIdDesc(user.getId());
        sb.append("Journal entries so far: ").append(entries.size()).append('\n');
        entries.stream().limit(3).forEach(e -> sb.append("- recent: ").append(e.getTitle()).append('\n'));
        return sb.toString();
    }

    private String fallback(User user, String message) {
        String m = message == null ? "" : message.toLowerCase(Locale.UK);
        if (!user.isOnboarded()) {
            return "Hi " + firstName(user) + "! First things first: pick your job role, current grade and target grade on your profile. "
                    + "Then I can show you exactly where your gaps are.";
        }
        if (m.contains("gap") || m.contains("focus") || m.contains("promot")) {
            try {
                var report = progression.report(user, null);
                var gaps = progression.topGaps(report, 3);
                if (gaps.isEmpty()) {
                    return "Great news: based on your self-assessment you're meeting every expectation for "
                            + report.targetGradeName() + ". Keep building evidence in your journal!";
                }
                StringBuilder sb = new StringBuilder("Your biggest gaps for " + report.targetGradeName() + " are:\n");
                gaps.forEach(g -> sb.append("• ").append(g.name()).append(" (").append(g.selfLevel() == null ? "not started" : g.selfLevel()).append(" → ")
                        .append(g.targetExpected()).append(")\n"));
                sb.append("Head to My Plan and I'll suggest training for each one.");
                return sb.toString();
            } catch (Exception e) {
                return "Open the Gap Analysis page and I'll show you where to focus.";
            }
        }
        if (m.contains("evidence") || m.contains("journal") || m.contains("write")) {
            return "Strong evidence has three parts:\n• Situation: what was going on?\n• Action: what did *you* do?\n"
                    + "• Impact: what changed as a result?\nKeep it specific, add numbers or feedback where you can, "
                    + "and tag it with the skills and behaviours it shows.";
        }
        if (m.contains("book") || m.contains("read")) {
            return "Popular picks with consultants: 'The Trusted Advisor' for client relationships, 'The Pyramid Principle' "
                    + "for structuring your thinking and 'Radical Candor' for feedback. Find them in the Training Library.";
        }
        if (m.contains("target") || m.contains("grade") || m.contains("expect")) {
            return "Open Gap Analysis and expand any item: I show you the description for your target grade "
                    + "side by side with where you are now.";
        }
        if (m.contains("hello") || m.contains("hi") || m.contains("hey")) {
            return "Hello " + firstName(user) + "! " + tip();
        }
        return "I'm a simple helper right now (ask your admin to add an Anthropic API key to make me smarter!). "
                + "Try asking about your gaps, writing evidence, or book suggestions. " + tip();
    }

    private static String firstName(User u) {
        String n = u.getDisplayName() == null ? "" : u.getDisplayName().trim();
        int sp = n.indexOf(' ');
        return sp > 0 ? n.substring(0, sp) : n;
    }
}
