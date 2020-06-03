package io.quarkus.ts.openshift.common;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ImageStreamSpecBuilder;
import io.fabric8.openshift.api.model.TagReferenceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.ts.openshift.common.config.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ImageOverrides {
    static final String CONFIG_KEY = "ts.image-overrides";

    static void apply(Path yaml, OpenShiftClient oc) throws IOException {
        String configFile = Config.get().getAsString(CONFIG_KEY, null);
        if (configFile == null) {
            return;
        }

        ImageOverridesConfig config = ImageOverridesConfig.load(Paths.get(configFile));

        List<HasMetadata> objs = oc.load(Files.newInputStream(yaml))
                .accept(new TypedVisitor<ContainerBuilder>() {
                    @Override
                    public void visit(ContainerBuilder c) {
                        if (c.hasImage()) {
                            c.withImage(config.overrideFor(c.getImage()));
                        }
                    }
                })
                .accept(new TypedVisitor<ImageStreamSpecBuilder>() {
                    @Override
                    public void visit(ImageStreamSpecBuilder is) {
                        if (is.hasDockerImageRepository()) {
                            is.withDockerImageRepository(config.overrideFor(is.getDockerImageRepository()));
                        }

                        is.accept(new TypedVisitor<TagReferenceBuilder>() {
                            @Override
                            public void visit(TagReferenceBuilder tag) {
                                if (tag.hasFrom() && tag.editFrom().hasName()) {
                                    tag.editFrom().withName(config.overrideFor(tag.editFrom().getName())).endFrom();
                                }
                            }
                        });
                    }
                })
                .get();

        KubernetesList list = new KubernetesList();
        list.setItems(objs);
        Serialization.yamlMapper().writeValue(Files.newOutputStream(yaml), list);
    }

    private static class ImageOverridesConfig {
        private final Map<String, String> data;

        static ImageOverridesConfig load(Path path) throws IOException {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                Map<String, String> data = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .map(line -> line.split("=", 2))
                        .collect(Collectors.toMap(splitLine -> splitLine[0].trim(), splitLine -> splitLine[1].trim()));
                return new ImageOverridesConfig(data);
            }
        }

        private ImageOverridesConfig(Map<String, String> data) {
            this.data = data;
        }

        String overrideFor(String image) {
            if (data.containsKey(image)) {
                return data.get(image);
            }

            String imageWithoutTag = image;
            if (image.contains(":")) {
                imageWithoutTag = image.substring(0, image.lastIndexOf(":"));
            }

            if (data.containsKey(imageWithoutTag)) {
                return data.get(imageWithoutTag);
            }

            return image;
        }

    }
}
