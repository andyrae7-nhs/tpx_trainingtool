package com.tpximpact.trainingtool.gacha;

/**
 * One card in the gacha pool.
 *
 * @param kind       ACTION (a small development action), PROJECT (something to build or pitch)
 *                   or RESOURCE (a course, book, event, programme or article from the training catalogue)
 * @param resourceId catalogue id when kind is RESOURCE, otherwise null
 */
public record Idea(String id, Kind kind, Rarity rarity, String title, String description, String icon,
                   String url, String resourceId) {

    public enum Kind { ACTION, PROJECT, RESOURCE }
}
