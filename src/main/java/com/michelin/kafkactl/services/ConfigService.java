package com.michelin.kafkactl.services;

import com.michelin.kafkactl.config.KafkactlConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Config service.
 */
@Singleton
public class ConfigService {
    @Inject
    public KafkactlConfig kafkactlConfig;

    @Inject
    public LoginService loginService;

    /**
     * Return the name of the current context according to the current api, namespace
     * and token properties.
     *
     * @return The current context name
     */
    public String getCurrentContextName() {
        return kafkactlConfig.getContexts()
            .stream()
            .filter(context -> context.getDefinition().getApi().equals(kafkactlConfig.getApi())
                && context.getDefinition().getNamespace().equals(kafkactlConfig.getCurrentNamespace())
                && context.getDefinition().getUserToken().equals(kafkactlConfig.getUserToken()))
            .findFirst()
            .map(KafkactlConfig.Context::getName)
            .orElse(null);
    }

    /**
     * Get the current context infos if it exists.
     *
     * @return The current context
     */
    public Optional<KafkactlConfig.Context> getContextByName(String name) {
        return kafkactlConfig.getContexts()
            .stream()
            .filter(context -> context.getName().equals(name))
            .findFirst();
    }

    /**
     * Update the current configuration context with the given new context.
     *
     * @param contextToSet The context to set
     * @throws IOException Any exception during file writing
     */
    public void updateConfigurationContext(KafkactlConfig.Context contextToSet) throws IOException {
        Yaml yaml = new Yaml();
        File initialFile = new File(kafkactlConfig.getConfigPath());
        InputStream targetStream = new FileInputStream(initialFile);
        Map<String, LinkedHashMap<String, Object>> rootNodeConfig = yaml.load(targetStream);

        LinkedHashMap<String, Object> kafkactlNodeConfig = rootNodeConfig.get("kafkactl");
        kafkactlNodeConfig.put("current-namespace", contextToSet.getDefinition().getNamespace());
        kafkactlNodeConfig.put("api", contextToSet.getDefinition().getApi());
        kafkactlNodeConfig.put("user-token", contextToSet.getDefinition().getUserToken());

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yamlMapper = new Yaml(options);
        FileWriter writer = new FileWriter(kafkactlConfig.getConfigPath());
        yamlMapper.dump(rootNodeConfig, writer);

        loginService.deleteJwtFile();
    }
}
