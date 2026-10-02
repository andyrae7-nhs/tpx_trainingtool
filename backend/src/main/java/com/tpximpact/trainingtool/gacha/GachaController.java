package com.tpximpact.trainingtool.gacha;

import com.tpximpact.trainingtool.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The idea gacha: spend gems on 10-pulls of random development actions, project ideas and
 * training resources. Gems come from a daily claim, trading in XP, or the (pretend) gem shop.
 */
@RestController
@RequestMapping("/api/gacha")
public class GachaController {

    private final GachaService gacha;
    private final CurrentUser currentUser;

    public GachaController(GachaService gacha, CurrentUser currentUser) {
        this.gacha = gacha;
        this.currentUser = currentUser;
    }

    public record PullRequest(String banner) {}

    public record ExchangeRequest(@Min(1) int xp) {}

    @GetMapping
    public GachaService.Overview overview() {
        return gacha.overview(currentUser.get());
    }

    @PostMapping("/pull")
    public GachaService.PullResult pull(@RequestBody(required = false) PullRequest body) {
        return gacha.pull(currentUser.get(), body == null ? null : body.banner());
    }

    @PostMapping("/daily")
    public GachaService.WalletView daily() {
        return gacha.claimDaily(currentUser.get());
    }

    @PostMapping("/exchange")
    public GachaService.WalletView exchange(@Valid @RequestBody ExchangeRequest body) {
        return gacha.exchangeXp(currentUser.get(), body.xp());
    }

    /** Buys a gem pack with pretend money. No payment is taken, ever. */
    @PostMapping("/shop/{code}")
    public GachaService.WalletView buy(@PathVariable String code) {
        return gacha.buyPack(currentUser.get(), code);
    }

    @GetMapping("/collection")
    public GachaService.CollectionView collection() {
        return gacha.collection(currentUser.id());
    }

    @GetMapping("/history")
    public List<GachaService.HistoryBatch> history() {
        return gacha.history(currentUser.id());
    }
}
