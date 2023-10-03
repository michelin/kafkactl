package com.michelin.kafkactl;

import com.michelin.kafkactl.models.ObjectMeta;
import com.michelin.kafkactl.models.Resource;
import com.michelin.kafkactl.parents.DryRunCommand;
import com.michelin.kafkactl.services.FormatService;
import com.michelin.kafkactl.services.ResourceService;
import com.michelin.kafkactl.utils.VersionProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Reset offsets subcommand.
 */
@Command(name = "reset-offsets",
    headerHeading = "@|bold Usage|@:",
    synopsisHeading = " ",
    descriptionHeading = "%n@|bold Description|@:%n%n",
    description = "Reset consumer group offsets.",
    parameterListHeading = "%n@|bold Parameters|@:%n",
    optionListHeading = "%n@|bold Options|@:%n",
    commandListHeading = "%n@|bold Commands|@:%n",
    usageHelpAutoWidth = true,
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class ResetOffsetsSubcommand extends DryRunCommand {
    public static final String RESET_METHOD = "method";
    public static final String OPTIONS = "options";

    @Inject
    public ResourceService resourceService;

    @Inject
    public FormatService formatService;

    @Option(names = {"--group"}, required = true, description = "Consumer group name.")
    public String group;

    @ArgGroup(multiplicity = "1")
    public TopicArgs topic;

    @ArgGroup(multiplicity = "1")
    public ResetMethod method;

    /**
     * Run the "reset-offsets" command.
     *
     * @return The command return code
     * @throws Exception Any exception during the run
     */
    @Override
    public Integer onAuthSuccess() throws Exception {
        String namespace = kafkactlCommand.optionalNamespace.orElse(kafkactlConfig.getCurrentNamespace());

        Map<String, Object> consumerGroupResetOffsetSpec = new HashMap<>();
        consumerGroupResetOffsetSpec.put("topic", topic.allTopics ? "*" : topic.topic);

        if (method.earliest) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "TO_EARLIEST");
        } else if (method.latest) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "TO_LATEST");
        } else if (method.datetime != null) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "TO_DATETIME");
            consumerGroupResetOffsetSpec.put(OPTIONS, method.datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else if (method.shiftBy != null) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "SHIFT_BY");
            consumerGroupResetOffsetSpec.put(OPTIONS, method.shiftBy);
        } else if (method.duration != null) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "BY_DURATION");
            consumerGroupResetOffsetSpec.put(OPTIONS, method.duration.toString());
        } else if (method.offset != null) {
            consumerGroupResetOffsetSpec.put(RESET_METHOD, "TO_OFFSET");
            consumerGroupResetOffsetSpec.put(OPTIONS, method.offset);
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

        return resourceService.resetOffsets(namespace, group, consumerGroupResetOffset, dryRun, commandSpec);
    }

    /**
     * Topic arguments.
     */
    public static class TopicArgs {
        @Option(names = {"--topic"}, required = true, description = "Topic name or topic:partition.")
        public String topic;

        @Option(names = {"--all-topics"}, required = true, description = "All topics.")
        public boolean allTopics;
    }

    /**
     * Reset method arguments.
     */
    public static class ResetMethod {
        @Option(names = {"--to-earliest"}, description = "Set offset to its earliest value (reprocess all).",
            required = true)
        public boolean earliest;

        @Option(names = {"--to-latest"}, description = "Set offset to its latest value (skip all).",
            required = true)
        public boolean latest;

        @Option(names = {"--to-datetime"}, description = "Set offset to a specific ISO8601 date time "
            + "with time zone (yyyy-MM-ddTHH:mm:ssZ).", required = true)
        public OffsetDateTime datetime;

        @Option(names = {"--shift-by"}, description = "Shift offset by a number. "
            + "Negative to reprocess or positive to skip.", required = true)
        public Integer shiftBy;

        @Option(names = {
            "--by-duration"}, description = "Shift offset by a duration format (PnDTnHnMnS).", required = true)
        public Duration duration;

        @Option(names = {"--to-offset"}, description = "Set offset to a specific index.", required = true)
        public Integer offset;
    }
}
