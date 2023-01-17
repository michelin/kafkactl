package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.LoginService;
import com.michelin.kafkactl.services.ResourceService;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "reset-offsets", description = "Reset Consumer Group offsets")
public class ResetOffsetsSubcommand implements Callable<Integer> {
    @Inject
    public LoginService loginService;

    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Inject
    public KafkactlConfig kafkactlConfig;

    @CommandLine.ParentCommand
    public KafkactlCommand kafkactlCommand;

    @Option(names = {"--group"}, required = true, description = "Consumer group name")
    public String group;

    @Option(names = {"--dry-run"}, description = "Does not persist resources. Validate only")
    public boolean dryRun;

    @ArgGroup(multiplicity = "1")
    public TopicArgs topic;

    @ArgGroup(multiplicity = "1")
    public ResetMethod method;

    public static class TopicArgs {
        @Option(names = {"--topic"}, required = true, description = "Topic or Topic:Partition [ topic[:partition] ]")
        public String topic;

        @Option(names = {"--all-topics"}, required = true, description = "All topics")
        public boolean allTopics;
    }

    public static class ResetMethod {
        @Option(names = {"--to-earliest"}, description = "Set offset to its earliest value [ reprocess all ]", required = true)
        public boolean earliest;

        @Option(names = {"--to-latest"}, description = "Set offset to its latest value [ skip all ]", required = true)
        public boolean latest;

        @Option(names = {"--to-datetime"}, description = "Set offset to a specific ISO 8601 DateTime with Time zone [ yyyy-MM-dd'T'HH:mm:ss.SSSXXX ]", required = true)
        public OffsetDateTime datetime;

        @Option(names = {"--shift-by"}, description = "Shift offset by a number [ negative to reprocess, positive to skip ]", required = true)
        public Integer shiftBy;

        @Option(names = {"--by-duration"}, description = "Shift offset by a duration format [ PnDTnHnMnS ]", required = true)
        public Duration duration;

        @Option(names = {"--to-offset"}, description = "Set offset to a specific index", required = true)
        public Integer offset;
    }

    @CommandLine.Spec
    public CommandLine.Model.CommandSpec commandSpec;

    /**
     * Run the "reset-offsets" command
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer call() throws Exception {
        if (dryRun) {
            System.out.println("Dry run execution");
        }

        boolean authenticated = loginService.doAuthenticate();
        if (!authenticated) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Login failed");
        }

        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        Map<String, Object> consumerGroupResetOffsetSpec = new HashMap<>();

        consumerGroupResetOffsetSpec.put("topic", topic.allTopics ? "*" : topic.topic);

        if (method.earliest) {
            consumerGroupResetOffsetSpec.put("method", "TO_EARLIEST");
        } else if (method.latest) {
            consumerGroupResetOffsetSpec.put("method", "TO_LATEST");
        } else if (method.datetime != null) {
            consumerGroupResetOffsetSpec.put("method", "TO_DATETIME");
            consumerGroupResetOffsetSpec.put("options", method.datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else if (method.shiftBy != null) {
            consumerGroupResetOffsetSpec.put("method", "SHIFT_BY");
            consumerGroupResetOffsetSpec.put("options", method.shiftBy);
        } else if (method.duration != null) {
            consumerGroupResetOffsetSpec.put("method", "BY_DURATION");
            consumerGroupResetOffsetSpec.put("options", method.duration.toString());
        } else if (method.offset != null) {
            consumerGroupResetOffsetSpec.put("method", "TO_OFFSET");
            consumerGroupResetOffsetSpec.put("options", method.offset);
        }

        Resource consumerGroupResetOffset = Resource.builder()
                .apiVersion("v1")
                .kind("ConsumerGroupResetOffsets")
                .metadata(ObjectMeta.builder()
                        .namespace(namespace)
                        .name(group)
                        .build())
                .spec(consumerGroupResetOffsetSpec)
                .build();

        List<Resource> resources = resourceService.resetOffsets(namespace, group, consumerGroupResetOffset, dryRun);
        if (!resources.isEmpty()) {
            formatService.displayList("ConsumerGroupResetOffsetsResponse", resources, "table");
            return 0;
        }

        return 1;
    }
}
