package com.tpximpact.trainingtool.social;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import com.tpximpact.trainingtool.user.UserViews;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/** The informal social space: posts, likes, comments and following colleagues. */
@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final FollowRepository follows;
    private final UserRepository users;
    private final UserViews views;
    private final CurrentUser currentUser;
    private final GamificationService gamification;

    public SocialController(PostRepository posts, CommentRepository comments, PostLikeRepository likes,
                            FollowRepository follows, UserRepository users, UserViews views,
                            CurrentUser currentUser, GamificationService gamification) {
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.follows = follows;
        this.users = users;
        this.views = views;
        this.currentUser = currentUser;
        this.gamification = gamification;
    }

    public record CommentView(Long id, String content, Instant createdAt, UserViews.UserSummary author, boolean mine) {}

    public record PostView(Long id, String content, String kind, Instant createdAt, UserViews.UserSummary author,
                           long likeCount, boolean likedByMe, List<CommentView> comments, boolean mine) {}

    private PostView view(Post p, Long me) {
        List<CommentView> cs = comments.findByPostIdOrderByCreatedAtAsc(p.getId()).stream()
                .map(c -> new CommentView(c.getId(), c.getContent(), c.getCreatedAt(), views.summary(c.getAuthor()),
                        c.getAuthor().getId().equals(me)))
                .toList();
        return new PostView(p.getId(), p.getContent(), p.getKind().name(), p.getCreatedAt(), views.summary(p.getAuthor()),
                likes.countByPostId(p.getId()), likes.findByPostIdAndUserId(p.getId(), me).isPresent(), cs,
                p.getAuthor().getId().equals(me));
    }

    /**
     * @param scope "following" (default: you and people you follow), "everyone" or "user" (with userId)
     */
    @GetMapping("/feed")
    public List<PostView> feed(@RequestParam(defaultValue = "following") String scope,
                               @RequestParam(required = false) Long userId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "30") int size) {
        Long me = currentUser.id();
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        List<Post> list = switch (scope) {
            case "everyone" -> posts.findAllByOrderByCreatedAtDesc(pr);
            case "user" -> posts.findByAuthorIdOrderByCreatedAtDesc(userId == null ? me : userId, pr);
            default -> {
                Set<Long> ids = new HashSet<>();
                ids.add(me);
                follows.findByFollowerId(me).forEach(f -> ids.add(f.getFolloweeId()));
                yield posts.findByAuthorIdInOrderByCreatedAtDesc(ids, pr);
            }
        };
        return list.stream().map(p -> view(p, me)).toList();
    }

    public record PostInput(@NotBlank @Size(max = 2000) String content) {}

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PostView create(@Valid @RequestBody PostInput in) {
        User me = currentUser.get();
        Post p = posts.save(new Post(me, in.content().trim(), Post.Kind.GENERAL));
        gamification.record(me, Activity.POST_CREATED);
        return view(p, me.getId());
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        Post p = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        if (!p.getAuthor().getId().equals(currentUser.id())) throw ApiException.forbidden();
        comments.deleteByPostId(id);
        likes.deleteByPostId(id);
        posts.delete(p);
    }

    /** Toggle a like. */
    @PostMapping("/posts/{id}/like")
    @Transactional
    public PostView like(@PathVariable Long id) {
        Long me = currentUser.id();
        Post p = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        likes.findByPostIdAndUserId(id, me).ifPresentOrElse(likes::delete, () -> likes.save(new PostLike(id, me)));
        if (!p.getAuthor().getId().equals(me)) gamification.evaluate(p.getAuthor());
        return view(p, me);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PostView comment(@PathVariable Long id, @Valid @RequestBody PostInput in) {
        User me = currentUser.get();
        Post p = posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
        comments.save(new Comment(p, me, in.content().trim()));
        gamification.record(me, Activity.COMMENTED);
        return view(p, me.getId());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteComment(@PathVariable Long id) {
        Comment c = comments.findById(id).orElseThrow(() -> ApiException.notFound("Comment"));
        if (!c.getAuthor().getId().equals(currentUser.id())) throw ApiException.forbidden();
        comments.delete(c);
    }

    @PostMapping("/follow/{userId}")
    @Transactional
    public UserViews.UserView follow(@PathVariable Long userId) {
        User me = currentUser.get();
        if (me.getId().equals(userId)) throw ApiException.badRequest("You can't follow yourself");
        User them = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (follows.findByFollowerIdAndFolloweeId(me.getId(), userId).isEmpty()) {
            follows.save(new Follow(me.getId(), userId));
            gamification.record(me, Activity.FOLLOWED);
        }
        return views.full(them, me.getId());
    }

    @DeleteMapping("/follow/{userId}")
    @Transactional
    public UserViews.UserView unfollow(@PathVariable Long userId) {
        Long me = currentUser.id();
        User them = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        follows.findByFollowerIdAndFolloweeId(me, userId).ifPresent(follows::delete);
        return views.full(them, me);
    }

    @GetMapping("/following")
    public List<UserViews.UserView> following(@RequestParam(required = false) Long userId) {
        Long me = currentUser.id();
        Long who = userId == null ? me : userId;
        return users.findAllById(follows.findByFollowerId(who).stream().map(Follow::getFolloweeId).toList())
                .stream().map(u -> views.full(u, me)).toList();
    }

    @GetMapping("/followers")
    public List<UserViews.UserView> followers(@RequestParam(required = false) Long userId) {
        Long me = currentUser.id();
        Long who = userId == null ? me : userId;
        return users.findAllById(follows.findByFolloweeId(who).stream().map(Follow::getFollowerId).toList())
                .stream().map(u -> views.full(u, me)).toList();
    }
}
