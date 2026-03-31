package org.acme.treemap.core;

/** Source of byte metrics used for treemap sizing. */
public enum AnalysisMode {
    /** Sizes from dependency artifacts in local Maven repository. */
    DEPENDENCY,
    /** Sizes from entries actually present in the built output JAR. */
    PACKAGED,
    /** Prefer packaged mode when a JAR is found, otherwise dependency mode. */
    AUTO
}
