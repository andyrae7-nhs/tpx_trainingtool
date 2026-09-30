package com.tpximpact.trainingtool.journal;

import com.tpximpact.trainingtool.ai.AnthropicClient;
import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.progression.ProgressionService;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/journal")
public class JournalController {

    private static final DateTimeFormatter UK_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private final JournalEntryRepository entries;
    private final CurrentUser currentUser;
    private final GamificationService gamification;
    private final ProgressionService progression;
    private final AnthropicClient ai;

    public JournalController(JournalEntryRepository entries, CurrentUser currentUser, GamificationService gamification,
                             ProgressionService progression, AnthropicClient ai) {
        this.entries = entries;
        this.currentUser = currentUser;
        this.gamification = gamification;
        this.progression = progression;
        this.ai = ai;
    }

    public record EntryView(Long id, String title, String body, String impact, LocalDate entryDate,
                            List<RefView> refs, Instant createdAt, Instant updatedAt) {}

    public record RefView(String ref, String name) {}

    public record EntryInput(@NotBlank @Size(max = 200) String title, @Size(max = 5000) String body,
                             @Size(max = 3000) String impact, LocalDate entryDate, List<String> refs) {}

    private EntryView view(JournalEntry e) {
        return new EntryView(e.getId(), e.getTitle(), e.getBody(), e.getImpact(), e.getEntryDate(),
                e.getRefs().stream().map(r -> new RefView(r, progression.refName(r).orElse(r))).toList(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    @GetMapping
    public List<EntryView> list() {
        return entries.findByUserIdOrderByEntryDateDescIdDesc(currentUser.id()).stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EntryView create(@Valid @RequestBody EntryInput in) {
        User me = currentUser.get();
        JournalEntry e = new JournalEntry();
        e.setUserId(me.getId());
        apply(e, in);
        entries.save(e);
        gamification.record(me, Activity.JOURNAL_ADDED);
        return view(e);
    }

    @PutMapping("/{id}")
    @Transactional
    public EntryView update(@PathVariable Long id, @Valid @RequestBody EntryInput in) {
        JournalEntry e = owned(id);
        apply(e, in);
        e.setUpdatedAt(Instant.now());
        entries.save(e);
        gamification.evaluate(currentUser.get());
        return view(e);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        entries.delete(owned(id));
    }

    private JournalEntry owned(Long id) {
        JournalEntry e = entries.findById(id).orElseThrow(() -> ApiException.notFound("Journal entry"));
        if (!e.getUserId().equals(currentUser.id())) throw ApiException.forbidden();
        return e;
    }

    private void apply(JournalEntry e, EntryInput in) {
        e.setTitle(in.title().trim());
        e.setBody(in.body());
        e.setImpact(in.impact());
        e.setEntryDate(in.entryDate() == null ? LocalDate.now() : in.entryDate());
        List<String> refs = in.refs() == null ? List.of() : in.refs().stream()
                .filter(r -> progression.refName(r).isPresent()).distinct().toList();
        e.setRefs(refs);
    }

    public record ExportResponse(String text, int entryCount, String generatedBy) {}

    /**
     * Export evidence grouped by framework item, ready to copy and paste into the progression assessment.
     * @param polish when true and Claude is configured, drafts a short summary paragraph per item.
     */
    @GetMapping("/export")
    @Transactional
    public ExportResponse export(@RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to,
                                 @RequestParam(defaultValue = "false") boolean polish) {
        User me = currentUser.get();
        LocalDate start = from == null ? LocalDate.of(1970, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.of(9999, 12, 31) : to;
        List<JournalEntry> list = entries.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(me.getId(), start, end);

        // Group by framework item, keeping the Skills -> Behaviours -> Impact order.
        Map<String, List<JournalEntry>> grouped = new TreeMap<>(Comparator
                .comparingInt((String r) -> r.startsWith("SKILL") ? 0 : r.startsWith("BEHAVIOUR") ? 1 : r.startsWith("IMPACT") ? 2 : 3)
                .thenComparing(r -> progression.refName(r).orElse(r)));
        List<JournalEntry> untagged = new ArrayList<>();
        for (JournalEntry e : list) {
            if (e.getRefs().isEmpty()) untagged.add(e);
            e.getRefs().forEach(r -> grouped.computeIfAbsent(r, k -> new ArrayList<>()).add(e));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PROGRESSION ASSESSMENT EVIDENCE - ").append(me.getDisplayName()).append('\n');
        if (from != null || to != null) {
            sb.append("Period: ").append(from == null ? "start" : from.format(UK_DATE)).append(" to ")
                    .append(to == null ? "today" : to.format(UK_DATE)).append('\n');
        }
        sb.append('\n');
        String lastSection = null;
        boolean usedAi = false;
        for (var group : grouped.entrySet()) {
            String section = group.getKey().split(":")[0];
            if (!section.equals(lastSection)) {
                sb.append(switch (section) {
                    case "SKILL" -> "== TECHNICAL SKILLS ==";
                    case "BEHAVIOUR" -> "== BEHAVIOURS ==";
                    default -> "== IMPACT ==";
                }).append("\n\n");
                lastSection = section;
            }
            String name = progression.refName(group.getKey()).orElse(group.getKey());
            sb.append(name).append('\n');
            if (polish) {
                Optional<String> summary = summarise(name, group.getValue());
                if (summary.isPresent()) {
                    sb.append(summary.get().trim()).append("\n\nSupporting examples:\n");
                    usedAi = true;
                }
            }
            for (JournalEntry e : group.getValue()) sb.append(bullet(e));
            sb.append('\n');
        }
        if (!untagged.isEmpty()) {
            sb.append("== OTHER EVIDENCE ==\n\n");
            untagged.forEach(e -> sb.append(bullet(e)));
        }
        if (list.isEmpty()) sb.append("No journal entries in this period yet.\n");

        gamification.record(me, Activity.JOURNAL_EXPORTED);
        return new ExportResponse(sb.toString().trim() + "\n", list.size(), usedAi ? "claude" : "plain");
    }

    private String bullet(JournalEntry e) {
        StringBuilder sb = new StringBuilder("• ").append(e.getEntryDate().format(UK_DATE)).append(" - ").append(e.getTitle());
        if (e.getBody() != null && !e.getBody().isBlank()) sb.append(": ").append(e.getBody().trim().replace("\n", " "));
        if (e.getImpact() != null && !e.getImpact().isBlank()) sb.append(" Impact: ").append(e.getImpact().trim().replace("\n", " "));
        return sb.append('\n').toString();
    }

    private Optional<String> summarise(String itemName, List<JournalEntry> evidence) {
        if (!ai.enabled()) return Optional.empty();
        StringBuilder sb = new StringBuilder();
        evidence.forEach(e -> sb.append(bullet(e)));
        String system = "You help TPXimpact consultants write their end-of-year progression assessment. "
                + "Write in the first person, plain British English, active voice, no jargon. Do not invent facts.";
        String prompt = "Write one paragraph (60-100 words) summarising how this evidence shows '" + itemName
                + "'. Use only what is in the evidence.\n\nEvidence:\n" + sb;
        return ai.complete(system, List.of(new AnthropicClient.Message("user", prompt)), 400);
    }
}
