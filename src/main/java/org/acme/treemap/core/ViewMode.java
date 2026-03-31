package org.acme.treemap.core;

/** Layout perspective for rendering dependency data. */
public enum ViewMode {
    /** Preserve dependency tree shape (direct -> transitive nesting). */
    TREE,
    /** Flatten to direct vs transitive buckets for practical size triage. */
    FLAT
}
