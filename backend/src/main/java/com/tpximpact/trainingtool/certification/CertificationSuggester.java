package com.tpximpact.trainingtool.certification;

import com.tpximpact.trainingtool.catalogue.TagMatcher;
import com.tpximpact.trainingtool.framework.FrameworkModels.GradedItem;
import com.tpximpact.trainingtool.framework.FrameworkModels.Role;
import com.tpximpact.trainingtool.framework.FrameworkModels.RoleSkill;
import com.tpximpact.trainingtool.framework.FrameworkModels.Skill;
import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.user.User;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Suggests which framework items a certification supports. The certification name and issuer are
 * turned into topic tags (e.g. "AWS" becomes cloud and infrastructure), then compared with the tags
 * {@link TagMatcher} gives each skill, behaviour and impact area in the person's role.
 */
@Component
public class CertificationSuggester {

    /** Most skills to suggest for one certification. */
    static final int MAX_SKILLS = 4;

    private final FrameworkService framework;

    public CertificationSuggester(FrameworkService framework) {
        this.framework = framework;
    }

    public record KnownCertification(String name, String issuer) {}

    /** Common certifications, offered as type-ahead suggestions. People can enter anything. */
    public static final List<KnownCertification> KNOWN = List.of(
            new KnownCertification("AWS Certified Cloud Practitioner", "Amazon Web Services"),
            new KnownCertification("AWS Certified Solutions Architect - Associate", "Amazon Web Services"),
            new KnownCertification("AWS Certified Solutions Architect - Professional", "Amazon Web Services"),
            new KnownCertification("AWS Certified Developer - Associate", "Amazon Web Services"),
            new KnownCertification("AWS Certified DevOps Engineer - Professional", "Amazon Web Services"),
            new KnownCertification("AWS Certified Data Engineer - Associate", "Amazon Web Services"),
            new KnownCertification("Microsoft Certified: Azure Fundamentals (AZ-900)", "Microsoft"),
            new KnownCertification("Microsoft Certified: Azure Administrator Associate (AZ-104)", "Microsoft"),
            new KnownCertification("Microsoft Certified: Azure Solutions Architect Expert (AZ-305)", "Microsoft"),
            new KnownCertification("Microsoft Certified: Azure Data Engineer Associate (DP-203)", "Microsoft"),
            new KnownCertification("Microsoft Certified: Power BI Data Analyst Associate (PL-300)", "Microsoft"),
            new KnownCertification("Google Cloud Professional Cloud Architect", "Google Cloud"),
            new KnownCertification("Google Cloud Professional Data Engineer", "Google Cloud"),
            new KnownCertification("HashiCorp Certified: Terraform Associate", "HashiCorp"),
            new KnownCertification("Certified Kubernetes Administrator (CKA)", "The Linux Foundation"),
            new KnownCertification("Certified Kubernetes Application Developer (CKAD)", "The Linux Foundation"),
            new KnownCertification("CompTIA Security+", "CompTIA"),
            new KnownCertification("CISSP", "ISC2"),
            new KnownCertification("ISTQB Certified Tester Foundation Level", "ISTQB"),
            new KnownCertification("ISTQB Advanced Level Test Analyst", "ISTQB"),
            new KnownCertification("TOGAF Enterprise Architecture Foundation", "The Open Group"),
            new KnownCertification("ITIL 4 Foundation", "PeopleCert"),
            new KnownCertification("PRINCE2 Foundation", "PeopleCert"),
            new KnownCertification("PRINCE2 Practitioner", "PeopleCert"),
            new KnownCertification("APM Project Management Qualification (PMQ)", "Association for Project Management"),
            new KnownCertification("Project Management Professional (PMP)", "PMI"),
            new KnownCertification("Professional Scrum Master I (PSM I)", "Scrum.org"),
            new KnownCertification("Professional Scrum Product Owner I (PSPO I)", "Scrum.org"),
            new KnownCertification("Certified ScrumMaster (CSM)", "Scrum Alliance"),
            new KnownCertification("SAFe Agilist", "Scaled Agile"),
            new KnownCertification("ICAgile Certified Professional (ICP)", "ICAgile"),
            new KnownCertification("BCS Foundation Certificate in Business Analysis", "BCS"),
            new KnownCertification("BCS International Diploma in Business Analysis", "BCS"),
            new KnownCertification("IIBA Certified Business Analysis Professional (CBAP)", "IIBA"),
            new KnownCertification("IAAP Certified Professional in Accessibility Core Competencies (CPACC)", "IAAP"),
            new KnownCertification("Nielsen Norman Group UX Certification", "Nielsen Norman Group"),
            new KnownCertification("Google Data Analytics Professional Certificate", "Google"),
            new KnownCertification("Databricks Certified Data Engineer Associate", "Databricks"),
            new KnownCertification("Snowflake SnowPro Core", "Snowflake"),
            new KnownCertification("DAMA Certified Data Management Professional (CDMP)", "DAMA International"),
            new KnownCertification("ILM Level 5 Certificate in Coaching and Mentoring", "ILM"),
            new KnownCertification("MRS Certificate in Market and Social Research", "Market Research Society"));

    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    private static void rule(String keyword, String... tags) {
        RULES.put(keyword, List.of(tags));
    }

    static {
        rule("aws", "cloud", "infrastructure");
        rule("amazon web services", "cloud", "infrastructure");
        rule("azure", "cloud", "infrastructure");
        rule("google cloud", "cloud", "infrastructure");
        rule("gcp", "cloud", "infrastructure");
        rule("cloud", "cloud", "infrastructure");
        rule("solutions architect", "architecture", "integration");
        rule("architect", "architecture");
        rule("togaf", "architecture", "strategy");
        rule("developer", "coding", "software-engineering");
        rule("devops", "devops", "automation");
        rule("terraform", "infrastructure", "automation", "devops");
        rule("kubernetes", "infrastructure", "devops", "cloud");
        rule("docker", "infrastructure", "devops");
        rule("administrator", "infrastructure", "service-management");
        rule("security", "security");
        rule("cissp", "security", "risk");
        rule("cyber", "security");
        rule("istqb", "testing");
        rule("test", "testing");
        rule("itil", "service-management", "continuous-improvement", "support");
        rule("prince2", "delivery", "planning", "risk");
        rule("project management", "delivery", "planning", "finance");
        rule("pmp", "delivery", "planning", "finance");
        rule("pmq", "delivery", "planning", "finance");
        rule("scrum", "agile", "delivery");
        rule("agile", "agile", "delivery");
        rule("safe", "agile", "delivery");
        rule("icagile", "agile", "delivery");
        rule("kanban", "agile", "process");
        rule("product owner", "product", "roadmaps", "outcomes");
        rule("pspo", "product", "roadmaps", "outcomes");
        rule("business analysis", "business-analysis", "requirements", "modelling");
        rule("cbap", "business-analysis", "requirements");
        rule("data engineer", "data-engineering", "data");
        rule("data analy", "analytics", "data");
        rule("power bi", "analytics", "data");
        rule("databricks", "data-engineering", "data");
        rule("snowflake", "data-engineering", "data");
        rule("data management", "data-governance", "data");
        rule("cdmp", "data-governance", "data");
        rule("data", "data");
        rule("machine learning", "ai", "data");
        rule("ai ", "ai");
        rule("accessib", "accessibility", "inclusion");
        rule("cpacc", "accessibility", "inclusion");
        rule("wcag", "accessibility");
        rule("ux", "interaction-design", "research");
        rule("user experience", "interaction-design", "research");
        rule("service design", "service-design");
        rule("research", "research");
        rule("coaching", "coaching", "mentoring");
        rule("mentoring", "coaching", "mentoring");
        rule("leadership", "leadership", "teams");
        rule("facilitat", "facilitation", "workshops");
        rule("change management", "strategy", "stakeholders");
        rule("lean", "agile", "process", "continuous-improvement");
        rule("six sigma", "process", "continuous-improvement");
    }

    /** Topic tags for a certification from its name and issuer. */
    static Map<String, Integer> tagsFor(String name, String issuer) {
        String text = (" " + Objects.toString(name, "") + " " + Objects.toString(issuer, "") + " ").toLowerCase(Locale.UK);
        Map<String, Integer> weights = new HashMap<>();
        RULES.forEach((kw, tags) -> {
            if (text.contains(kw)) tags.forEach(t -> weights.merge(t, 1, Integer::sum));
        });
        return weights;
    }

    public record Suggestion(String ref, String name, String type, int score) {}

    /** Framework items this certification probably supports, best match first. */
    public List<Suggestion> suggest(User user, String name, String issuer) {
        Map<String, Integer> certTags = tagsFor(name, issuer);
        List<Suggestion> skills = new ArrayList<>();
        if (!certTags.isEmpty()) {
            for (Skill s : candidateSkills(user)) {
                int score = score(certTags, TagMatcher.tagsFor(s.name(), s.definition()));
                if (score > 0) skills.add(new Suggestion("SKILL:" + s.id(), s.name(), "SKILL", score));
            }
        }
        skills.sort(Comparator.comparingInt(Suggestion::score).reversed().thenComparing(Suggestion::name));
        List<Suggestion> out = new ArrayList<>(skills.subList(0, Math.min(MAX_SKILLS, skills.size())));

        // Behaviours and impact areas only when the match is strong, so they don't crowd out skills.
        List<GradedItem> graded = new ArrayList<>(framework.behaviours());
        graded.addAll(framework.impacts());
        for (GradedItem g : graded) {
            int score = certTags.isEmpty() ? 0 : score(certTags, TagMatcher.tagsFor(g.name(), g.definition()));
            if (score >= 3) out.add(new Suggestion(g.type() + ":" + g.id(), g.name(), g.type(), score));
        }

        // Gaining a certification is always a way of developing your craft.
        framework.findBehaviour("developing-your-craft").ifPresent(b -> {
            String ref = "BEHAVIOUR:" + b.id();
            if (out.stream().noneMatch(s -> s.ref().equals(ref))) out.add(new Suggestion(ref, b.name(), "BEHAVIOUR", 1));
        });
        return out;
    }

    private List<Skill> candidateSkills(User user) {
        Optional<Role> role = framework.findRole(user.getRoleId());
        if (role.isPresent()) {
            return role.get().skills().stream().map(RoleSkill::skillId).distinct()
                    .map(framework::findSkill).flatMap(Optional::stream).toList();
        }
        return framework.framework().skills();
    }

    private static int score(Map<String, Integer> certTags, Map<String, Integer> itemTags) {
        int score = 0;
        for (var e : certTags.entrySet()) score += e.getValue() * itemTags.getOrDefault(e.getKey(), 0);
        return score;
    }
}
