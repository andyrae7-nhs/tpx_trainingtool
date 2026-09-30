package com.tpximpact.trainingtool.config;

import com.tpximpact.trainingtool.gamification.*;
import com.tpximpact.trainingtool.journal.JournalEntry;
import com.tpximpact.trainingtool.journal.JournalEntryRepository;
import com.tpximpact.trainingtool.learning.LearningItem;
import com.tpximpact.trainingtool.learning.LearningItemRepository;
import com.tpximpact.trainingtool.social.*;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Seeds a handful of fictional colleagues so the leaderboard and social feed have content in a demo.
 * Runs only when app.seed-demo-data=true and the database has no users. All demo users share the
 * password "password123".
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final ActivityLogRepository activity;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final FollowRepository follows;
    private final LearningItemRepository learning;
    private final JournalEntryRepository journal;
    private final GamificationService gamification;

    public DemoDataSeeder(AppProperties props, UserRepository users, PasswordEncoder encoder,
                          ActivityLogRepository activity, PostRepository posts, CommentRepository comments,
                          PostLikeRepository likes, FollowRepository follows, LearningItemRepository learning,
                          JournalEntryRepository journal, GamificationService gamification) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
        this.activity = activity;
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.follows = follows;
        this.learning = learning;
        this.journal = journal;
        this.gamification = gamification;
    }

    private record Demo(String name, String email, String role, String grade, String target, String colour, int xp,
                        String bio) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedDemoData() || users.count() > 0) return;
        log.info("Seeding demo data (set SEED_DEMO_DATA=false to turn this off)");
        Random rnd = new Random(42);
        String hash = encoder.encode("password123");

        List<Demo> demos = List.of(
                new Demo("Priya Sharma", "priya.demo@example.com", "software-engineer", "G9", "G10", "#c8e9ff", 1480,
                        "Backend engineer. Java, Kotlin and too many side projects."),
                new Demo("Tom Okafor", "tom.demo@example.com", "delivery-manager", "G8", "G9", "#ffcfca", 960,
                        "Delivery manager who loves a good retro."),
                new Demo("Sophie Green", "sophie.demo@example.com", "service-designer", "G9", "G10", "#cafce5", 1210,
                        "Service designer. Sticky notes are my love language."),
                new Demo("Marcus Lee", "marcus.demo@example.com", "data-engineer", "G8", "G9", "#e7d2ff", 720,
                        "Data pipelines, dbt and strong coffee."),
                new Demo("Hannah Wright", "hannah.demo@example.com", "business-analyst", "G7", "G8", "#c8e9ff", 540,
                        "BA learning something new every day."),
                new Demo("Dev Patel", "dev.demo@example.com", "cloud-engineer", "G10", "G11", "#ffcfca", 1830,
                        "Cloud engineer. Infrastructure as code or it didn't happen."),
                new Demo("Alex Morgan", "demo@example.com", "technology-consultant", "G9", "G10", "#cafce5", 0,
                        "Demo account - sign in with demo@example.com / password123"));

        List<User> created = new ArrayList<>();
        for (Demo d : demos) {
            User u = new User();
            u.setEmail(d.email());
            u.setDisplayName(d.name());
            u.setPasswordHash(hash);
            u.setRoleId(d.role());
            u.setCurrentGrade(d.grade());
            u.setTargetGrade(d.target());
            u.setAvatarColor(d.colour());
            u.setBio(d.bio());
            u.setXp(d.xp());
            u.setStreakDays(rnd.nextInt(12));
            u.setLastActiveDate(LocalDate.now().minusDays(1));
            u.setCreatedAt(Instant.now().minus(60 + rnd.nextInt(90), ChronoUnit.DAYS));
            users.save(u);
            // Spread XP over recent weeks so the weekly/monthly leaderboards have data.
            int remaining = d.xp();
            while (remaining > 0) {
                int chunk = Math.min(remaining, 10 + rnd.nextInt(40));
                ActivityLog a = new ActivityLog(u.getId(), Activity.JOURNAL_ADDED, chunk);
                a.setCreatedAt(Instant.now().minus(rnd.nextInt(60 * 24 * 45), ChronoUnit.MINUTES));
                activity.save(a);
                remaining -= chunk;
            }
            created.add(u);
        }

        User priya = created.get(0), tom = created.get(1), sophie = created.get(2), marcus = created.get(3),
                hannah = created.get(4), dev = created.get(5), alex = created.get(6);

        review(priya, LearningItem.Type.BOOK, "Designing Data-Intensive Applications - Martin Kleppmann", "O'Reilly",
                "designing-data-intensive-applications-martin-kleppmann", 5,
                "Dense but brilliant. Chapter 5 on replication changed how I think about our event pipelines.");
        review(tom, LearningItem.Type.BOOK, "The Trusted Advisor - Maister, Green & Galford", "Maister, Green & Galford",
                "the-trusted-advisor-maister-green-galford", 4,
                "The trust equation is simple and really useful before a tricky client meeting.");
        review(sophie, LearningItem.Type.PROGRAMME, "Workshop design and facilitation",
                "TPXimpact Talent & Development - Consulting Skills: Collaborative Working", "workshop-design-and-facilitation", 5,
                "Loved the practical bits. I used the 'silent start' activity with a client the next week and it worked a treat.");
        review(dev, LearningItem.Type.COURSE, "Terraform tutorials", "HashiCorp", "terraform-tutorials", 4,
                "Great refresher. The module on state was worth it alone.");
        review(hannah, LearningItem.Type.BOOK, "The Pyramid Principle - Barbara Minto", "Barbara Minto",
                "the-pyramid-principle-barbara-minto", 4,
                "Hard going in places, but 'answer first' has made my status updates much clearer.");
        review(marcus, LearningItem.Type.EVENT, "Big Data LDN", "Big Data LDN", "big-data-ldn", 3,
                "Good talks on data contracts. Busy expo floor - plan your sessions in advance.");

        post(tom, "Ran my first fully remote retro with the new client team today. Used the sailboat format and got loads of honest feedback. Anyone got favourite retro formats to try next?", 2);
        post(sophie, "Presented our service blueprint to the client's leadership team. Nervous beforehand but the storytelling session from the Consulting Skills programme really helped.", 1);
        post(dev, "Passed my AWS Solutions Architect Professional exam! Happy to share notes if anyone's working towards it.", 3);
        post(priya, "Pairing with a junior engineer this sprint and learning as much as I'm teaching. Highly recommend it.", 5);
        post(hannah, "Just finished my first process map for a client. Business modelling gap slowly closing!", 0);

        // Follows
        follow(alex, priya); follow(alex, sophie); follow(alex, tom);
        follow(priya, dev); follow(tom, sophie); follow(sophie, tom); follow(hannah, tom); follow(marcus, priya); follow(dev, priya);

        // Likes and comments
        List<Post> all = posts.findAll();
        for (Post p : all) {
            for (User u : created) {
                if (!u.getId().equals(p.getAuthor().getId()) && rnd.nextInt(3) > 0) likes.save(new PostLike(p.getId(), u.getId()));
            }
        }
        all.stream().filter(p -> p.getAuthor().getId().equals(dev.getId())).findFirst()
                .ifPresent(p -> comments.save(new Comment(p, priya, "Congratulations! Would love those notes.")));
        all.stream().filter(p -> p.getAuthor().getId().equals(tom.getId())).findFirst()
                .ifPresent(p -> comments.save(new Comment(p, sophie, "Try 'Start, stop, continue' with dot voting - quick and it gets everyone involved.")));

        // Some journal evidence for the demo account
        entry(alex, "Led the architecture options workshop",
                "Facilitated a half-day workshop with the client's CTO and three product owners to compare hosting options. Prepared an options paper with costs and risks.",
                "The client chose a managed container platform and signed off the alpha plan a week early.",
                LocalDate.now().minusDays(40), List.of("SKILL:solution-architecture", "BEHAVIOUR:communicating-and-collaborating"));
        entry(alex, "Mentored a new joiner through onboarding",
                "Set up weekly pairing sessions and a reading list for a graduate developer.",
                "They shipped their first feature in week three and said the sessions made them feel confident to ask questions.",
                LocalDate.now().minusDays(18), List.of("BEHAVIOUR:supporting-and-developing-others"));

        created.forEach(gamification::evaluate);
        log.info("Seeded {} demo users. Demo login: demo@example.com / password123", created.size());
    }

    private void review(User u, LearningItem.Type type, String title, String provider, String catalogueId, int rating, String text) {
        LearningItem i = new LearningItem();
        i.setUserId(u.getId());
        i.setType(type);
        i.setTitle(title);
        i.setProvider(provider);
        i.setCatalogueId(catalogueId);
        i.setStatus(LearningItem.Status.COMPLETED);
        i.setCompletedOn(LocalDate.now().minusDays(7 + new Random(title.hashCode()).nextInt(40)));
        i.setRating(rating);
        i.setReview(text);
        i.setShared(true);
        learning.save(i);
        Post p = new Post(u, "reviewed '" + title + "' " + "★".repeat(rating) + "☆".repeat(5 - rating) + ": " + text, Post.Kind.LEARNING);
        p.setCreatedAt(Instant.now().minus(new Random(title.hashCode()).nextInt(20) + 1, ChronoUnit.DAYS));
        posts.save(p);
    }

    private void post(User u, String text, int daysAgo) {
        Post p = new Post(u, text, Post.Kind.GENERAL);
        p.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS).minus(new Random(text.hashCode()).nextInt(600), ChronoUnit.MINUTES));
        posts.save(p);
    }

    private void follow(User a, User b) {
        follows.save(new Follow(a.getId(), b.getId()));
    }

    private void entry(User u, String title, String body, String impact, LocalDate date, List<String> refs) {
        JournalEntry e = new JournalEntry();
        e.setUserId(u.getId());
        e.setTitle(title);
        e.setBody(body);
        e.setImpact(impact);
        e.setEntryDate(date);
        e.setRefs(refs);
        journal.save(e);
    }
}
