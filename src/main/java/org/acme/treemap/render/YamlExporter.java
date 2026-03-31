package org.acme.treemap.render;

import org.acme.treemap.export.TreemapSnapshot;
import org.acme.treemap.export.TreemapSnapshots;
import org.acme.treemap.maven.DependencyNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;

/** Writes {@link TreemapSnapshot} as YAML. */
public final class YamlExporter implements Generator {

    private static final ObjectMapper MAPPER = new ObjectMapper(
                    new YAMLFactory()
                            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Override
    public void generate(DependencyNode root, OutputOptions options) throws IOException {
        TreemapSnapshot snap = TreemapSnapshots.from(root, options.title());
        MAPPER.writeValue(options.output().toFile(), snap);
    }
}
