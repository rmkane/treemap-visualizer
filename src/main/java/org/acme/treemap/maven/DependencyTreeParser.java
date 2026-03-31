package org.acme.treemap.maven;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code mvn dependency:tree} text lines into a {@link DependencyNode}
 * tree.
 */
public final class DependencyTreeParser {

    private static final Pattern INFO_LINE = Pattern.compile("^\\[INFO\\] (.*)$");
    /**
     * Tree drawing: repeated {@code |  } or {@code   } (three spaces) per depth
     * level, then {@code +- } or {@code \- }. Maven uses spaces under a last
     * sibling instead of {@code |  } for the same depth.
     */
    private static final Pattern TREE_CHILD = Pattern.compile("^((?:\\|  |   )*)([+\\\\]- )(.*)$");

    private DependencyTreeParser() {
    }

    public static DependencyNode parse(List<String> lines) {
        List<ParsedLine> parsed = new ArrayList<>();
        for (String raw : lines) {
            Matcher info = INFO_LINE.matcher(raw);
            if (!info.matches()) {
                continue;
            }
            String body = info.group(1).stripTrailing();
            if (body.isEmpty()
                    || body.startsWith("---")
                    || body.startsWith("Building ")
                    || body.startsWith("Total time")
                    || body.startsWith("Finished at")
                    || body.contains("from pom.xml")) {
                continue;
            }
            ParsedLine pl = parseBody(body);
            if (pl != null) {
                parsed.add(pl);
            }
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No dependency tree lines found in Maven output.");
        }

        ParsedLine rootLine = parsed.getFirst();
        if (rootLine.depth != 0) {
            throw new IllegalArgumentException("Expected root artifact as first tree line.");
        }
        DependencyNode root = new DependencyNode(rootLine.key);
        Deque<DependencyNode> stack = new ArrayDeque<>();
        stack.push(root);

        for (int i = 1; i < parsed.size(); i++) {
            ParsedLine pl = parsed.get(i);
            while (stack.size() > pl.depth) {
                stack.pop();
            }
            DependencyNode parent = stack.peek();
            if (parent == null) {
                throw new IllegalStateException(
                        "Malformed dependency tree (depth " + pl.depth + "): " + pl.key.shortLabel());
            }
            DependencyNode child = new DependencyNode(pl.key);
            parent.children().add(child);
            stack.push(child);
        }
        return root;
    }

    private static ParsedLine parseBody(String body) {
        Matcher child = TREE_CHILD.matcher(body);
        if (child.matches()) {
            String indent = child.group(1) == null ? "" : child.group(1);
            int depth = 1 + (indent.isEmpty() ? 0 : indent.length() / 3);
            ArtifactKey key = parseCoordinates(child.group(3).strip());
            if (key == null) {
                return null;
            }
            return new ParsedLine(depth, key);
        }
        String trimmed = body.strip();
        if (trimmed.startsWith("+- ") || trimmed.startsWith("\\- ")) {
            return null;
        }
        ArtifactKey rootKey = parseCoordinates(trimmed);
        if (rootKey != null) {
            return new ParsedLine(0, rootKey);
        }
        return null;
    }

    /**
     * Accepts {@code g:a:packaging:version}, {@code g:a:packaging:version:scope},
     * or {@code g:a:packaging:classifier:version:scope}.
     */
    static ArtifactKey parseCoordinates(String coord) {
        String[] p = coord.split(":");
        if (p.length == 4) {
            return new ArtifactKey(p[0], p[1], p[2], p[3], "compile", Optional.empty());
        }
        if (p.length == 5) {
            return new ArtifactKey(p[0], p[1], p[2], p[3], p[4], Optional.empty());
        }
        if (p.length == 6) {
            return new ArtifactKey(p[0], p[1], p[2], p[4], p[5], Optional.of(p[3]));
        }
        return null;
    }

    private record ParsedLine(int depth, ArtifactKey key) {
    }
}
