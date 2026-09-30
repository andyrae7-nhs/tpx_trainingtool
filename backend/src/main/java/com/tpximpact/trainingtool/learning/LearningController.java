package com.tpximpact.trainingtool.learning;

import com.tpximpact.trainingtool.catalogue.CatalogueService;
import com.tpximpact.trainingtool.catalogue.Resource;
import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.social.Post;
import com.tpximpact.trainingtool.social.PostRepository;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import com.tpximpact.trainingtool.user.UserViews;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Track and review courses, books, events and programmes. */
@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningItemRepository items;
    private final CurrentUser currentUser;
    private final GamificationService gamification;
    private final CatalogueService catalogue;
    private final PostRepository posts;
    private final UserRepository users;
    private final UserViews views;

    public LearningController(LearningItemRepository items, CurrentUser currentUser, GamificationService gamification,
                              CatalogueService catalogue, PostRepository posts, UserRepository users, UserViews views) {
        this.items = items;
        this.currentUser = currentUser;
        this.gamification = gamification;
        this.catalogue = catalogue;
        this.posts = posts;
        this.users = users;
        this.views = views;
    }

    public record LearningInput(LearningItem.Type type, @Size(max = 300) String title, @Size(max = 200) String provider,
                                @Size(max = 1000) String url, String catalogueId, LearningItem.Status status,
                                LocalDate completedOn, @Min(1) @Max(5) Integer rating,
                                @Size(max = 3000) String review, Boolean shared, List<String> refs) {}

    @GetMapping
    public List<LearningItem> mine() {
        return items.findByUserIdOrderByCreatedAtDesc(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LearningItem create(@Valid @RequestBody LearningInput in) {
        User me = currentUser.get();
        LearningItem item = new LearningItem();
        item.setUserId(me.getId());
        // Adding from the catalogue pre-fills the details.
        if (in.catalogueId() != null) {
            Resource r = catalogue.get(in.catalogueId());
            item.setCatalogueId(r.id());
            item.setTitle(r.title());
            item.setProvider(r.provider());
            item.setUrl(r.url());
            item.setType(mapType(r.type()));
        }
        if ((in.title() == null || in.title().isBlank()) && item.getTitle() == null) throw ApiException.badRequest("title is required");
        LearningItem.Status before = null;
        boolean sharedBefore = false;
        apply(item, in);
        items.save(item);
        gamification.record(me, Activity.LEARNING_ADDED);
        afterChange(me, item, before, sharedBefore);
        return item;
    }

    @PutMapping("/{id}")
    @Transactional
    public LearningItem update(@PathVariable Long id, @Valid @RequestBody LearningInput in) {
        User me = currentUser.get();
        LearningItem item = owned(id);
        LearningItem.Status before = item.getStatus();
        boolean sharedBefore = item.isShared() && item.getReview() != null;
        apply(item, in);
        items.save(item);
        afterChange(me, item, before, sharedBefore);
        gamification.evaluate(me);
        return item;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        items.delete(owned(id));
    }

    private void afterChange(User me, LearningItem item, LearningItem.Status before, boolean sharedBefore) {
        if (item.getStatus() == LearningItem.Status.COMPLETED && before != LearningItem.Status.COMPLETED) {
            if (item.getCompletedOn() == null) item.setCompletedOn(LocalDate.now());
            gamification.record(me, Activity.LEARNING_COMPLETED);
        }
        boolean sharedNow = item.isShared() && item.getReview() != null && !item.getReview().isBlank();
        if (sharedNow && !sharedBefore) {
            gamification.record(me, Activity.REVIEW_SHARED);
            String stars = item.getRating() == null ? "" : " " + "★".repeat(item.getRating()) + "☆".repeat(5 - item.getRating());
            posts.save(new Post(me, "reviewed " + typeLabel(item.getType()) + " '" + item.getTitle() + "'" + stars
                    + ": " + truncate(item.getReview(), 400), Post.Kind.LEARNING));
        }
    }

    private LearningItem owned(Long id) {
        LearningItem i = items.findById(id).orElseThrow(() -> ApiException.notFound("Learning item"));
        if (!i.getUserId().equals(currentUser.id())) throw ApiException.forbidden();
        return i;
    }

    private void apply(LearningItem item, LearningInput in) {
        if (in.type() != null) item.setType(in.type());
        if (in.title() != null && !in.title().isBlank()) item.setTitle(in.title().trim());
        if (in.provider() != null) item.setProvider(in.provider());
        if (in.url() != null) item.setUrl(in.url());
        if (in.status() != null) item.setStatus(in.status());
        if (in.completedOn() != null) item.setCompletedOn(in.completedOn());
        if (in.rating() != null) item.setRating(in.rating());
        if (in.review() != null) item.setReview(in.review().isBlank() ? null : in.review().trim());
        if (in.shared() != null) item.setShared(in.shared());
        if (in.refs() != null) item.setRefs(in.refs());
    }

    private static LearningItem.Type mapType(String catalogueType) {
        return switch (catalogueType) {
            case "BOOK" -> LearningItem.Type.BOOK;
            case "EVENT" -> LearningItem.Type.EVENT;
            case "PROGRAMME" -> LearningItem.Type.PROGRAMME;
            case "COURSE" -> LearningItem.Type.COURSE;
            default -> LearningItem.Type.OTHER;
        };
    }

    private static String typeLabel(LearningItem.Type t) {
        return switch (t) {
            case BOOK -> "the book";
            case EVENT -> "the event";
            case PROGRAMME -> "the programme";
            case COURSE -> "the course";
            default -> "";
        };
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record ReviewView(Long id, String title, String type, String provider, String url, String catalogueId,
                             Integer rating, String review, LocalDate completedOn, Instant createdAt,
                             UserViews.UserSummary author) {}

    /** Reviews colleagues have shared. Filter by catalogue resource with ?catalogueId=. */
    @GetMapping("/reviews")
    public List<ReviewView> reviews(@RequestParam(required = false) String catalogueId) {
        List<LearningItem> shared = catalogueId == null
                ? items.findTop30BySharedTrueOrderByCreatedAtDesc()
                : items.findBySharedTrueAndCatalogueIdOrderByCreatedAtDesc(catalogueId);
        Map<Long, User> authors = new HashMap<>();
        users.findAllById(shared.stream().map(LearningItem::getUserId).distinct().toList())
                .forEach(u -> authors.put(u.getId(), u));
        return shared.stream()
                .filter(i -> i.getReview() != null && authors.containsKey(i.getUserId()))
                .map(i -> new ReviewView(i.getId(), i.getTitle(), i.getType().name(), i.getProvider(), i.getUrl(),
                        i.getCatalogueId(), i.getRating(), i.getReview(), i.getCompletedOn(), i.getCreatedAt(),
                        views.summary(authors.get(i.getUserId()))))
                .toList();
    }
}
