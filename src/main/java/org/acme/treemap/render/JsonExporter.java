package org.acme.treemap.render;

import org.acme.treemap.export.TreemapSnapshot;
import org.acme.treemap.export.TreemapSnapshots;
import org.acme.treemap.maven.DependencyNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/** Writes {@link TreemapSnapshot} as indented JSON. */
public final class JsonExporter implements Generator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Override
    public void generate(DependencyNode root, OutputOptions options) throws IOException {
        TreemapSnapshot snap = TreemapSnapshots.from(root, options.title());
        MAPPER.writeValue(options.output().toFile(), snap);
    }
}
