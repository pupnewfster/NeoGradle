package net.minecraftforge.gradle.mcp.runtime.extensions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.common.runtime.tasks.ListLibraries;
import net.minecraftforge.gradle.common.util.*;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.ArtifactSide;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.mcp.extensions.McpExtension;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpecBuilder;
import net.minecraftforge.gradle.mcp.runtime.tasks.Inject;
import net.minecraftforge.gradle.mcp.runtime.tasks.Patch;
import net.minecraftforge.gradle.mcp.runtime.tasks.RecompileSourceJar;
import net.minecraftforge.gradle.mcp.runtime.tasks.StripJar;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"}) // API Design
public abstract class McpRuntimeExtension extends CommonRuntimeExtension<McpRuntimeSpec, McpRuntimeSpecBuilder, McpRuntimeDefinition> {

    @javax.inject.Inject
    public McpRuntimeExtension(Project project) {
        super(project);

        this.getDefaultVersion().convention(
                project.provider(() -> project.getExtensions().getByType(McpExtension.class))
                        .flatMap(McpExtension::getMcpConfigArtifact)
                        .map(Artifact::getVersion)
        );
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpec spec, File mcpDirectory, Map<String, File> data, LinkedHashMap<String, TaskProvider<? extends WithOutput>> tasks, McpConfigConfigurationSpecV1.Step step, Runtime mcpRuntimeTask, Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        mcpRuntimeTask.getArguments().set(buildArguments(spec, step, tasks, mcpRuntimeTask, alternativeInputProvider));
        configureCommonMcpRuntimeTaskParameters(mcpRuntimeTask, data, step.getName(), spec, mcpDirectory);
    }

    private static void configureMcpRuntimeTaskWithDefaults(McpRuntimeSpec spec, File mcpDirectory, Map<String, File> data, Runtime mcpRuntimeTask) {
        mcpRuntimeTask.getArguments().set(Maps.newHashMap());
        configureCommonMcpRuntimeTaskParameters(mcpRuntimeTask, data, McpRuntimeUtils.buildStepName(spec, mcpRuntimeTask.getName()), spec, mcpDirectory);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable
    private static TaskProvider<? extends WithOutput> createBuiltIn(final McpRuntimeSpec spec, McpConfigConfigurationSpecV2 mcpConfigV2, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviders, final Optional<TaskProvider<? extends WithOutput>> adaptedInput) {
        switch (step.getType()) {
            case "downloadManifest":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.LAUNCHER_MANIFEST, a -> {
                    throw new IllegalStateException("Launcher Manifest is required for this step, but was not provided");
                });
            case "downloadJson":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.VERSION_MANIFEST, a -> {
                    throw new IllegalStateException("Version Manifest is required for this step, but was not provided");
                });
            case "downloadClient":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_JAR, a -> {
                    throw new IllegalStateException("Client Jar is required for this step, but was not provided");
                });
            case "downloadServer":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_JAR, a -> {
                    throw new IllegalStateException("Server Jar is required for this step, but was not provided");
                });
            case "strip":
                return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step)));
            case "listLibraries":
                return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    task.getDownloadedVersionJsonFile().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step, "downloadJson", adaptedInput));
                });
            case "inject":
                return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Inject.class, task -> {
                    task.getInjectionSource().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step));
                    if (spec.side().equals(ArtifactSide.SERVER)) {
                        task.getInclusionFilter().set("**/server/**");
                    } else if (spec.side().equals(ArtifactSide.CLIENT)) {
                        task.getInclusionFilter().set("**/client/**");
                    }
                });
            case "patch":
                return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Patch.class, task -> task.getInput().fileProvider(McpRuntimeUtils.getTaskInputFor(spec, tasks, step)));
        }
        if (mcpConfigV2.getSpec() >= 2) {
            switch (step.getType()) {
                case "downloadClientMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_MAPPINGS, a -> {
                        throw new IllegalStateException("Client Mappings are required for this step, but were not provided");
                    });
                case "downloadServerMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_MAPPINGS, a -> {
                        throw new IllegalStateException("Server Mappings are required for this step, but were not provided");
                    });
            }
        }

        return null;
    }

    private static TaskProvider<? extends Runtime> createExecute(final McpRuntimeSpec spec, final McpConfigConfigurationSpecV1.Step step, final McpConfigConfigurationSpecV1.Function function) {
        return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), Execute.class, task -> {
            task.getExecutingArtifact().set(function.getVersion());
            task.getJvmArguments().addAll(function.getJvmArgs());
            task.getProgramArguments().addAll(function.getArgs());
        });
    }

    private static Map<String, Provider<String>> buildArguments(final McpRuntimeSpec spec, McpConfigConfigurationSpecV1.Step step, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        step.getValues().forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends WithOutput>> dependentTask;
                if (!Objects.equals(key, "input") || !alternativeInputProvider.isPresent()) {
                    dependentTask = McpRuntimeUtils.getInputTaskForTaskFrom(spec, value, tasks);
                } else {
                    dependentTask = alternativeInputProvider;
                }

                dependentTask.ifPresent(taskForArguments::dependsOn);
                dependentTask.ifPresent(task -> arguments.put(key, task.flatMap(t -> t.getOutput().getAsFile().map(File::getAbsolutePath))));
            } else {
                arguments.put(key, spec.project().provider(() -> value));
            }
        });

        return arguments;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Map<String, File> buildDataMap(McpConfigConfigurationSpecV2 mcpConfig, final ArtifactSide side, final File unpackedMcpDirectory) {
        return mcpConfig.getData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new File(unpackedMcpDirectory, e.getValue() instanceof Map ? ((Map<String, String>) e.getValue()).get(side.getName()) : (String) e.getValue())
        ));
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @NotNull
    protected McpRuntimeDefinition doCreate(final McpRuntimeSpec spec) {
        if (this.runtimes.containsKey(spec.name()))
            throw new IllegalArgumentException("Cannot register runtime with name '" + spec.name() + "' because it already exists");

        final Minecraft minecraftExtension = spec.project().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.project().getExtensions().getByType(MinecraftArtifactCache.class);
        final Dependency mcpConfigDependency = spec.project().getDependencies().create("de.oceanlabs.mcp:mcp_config:" + spec.mcpVersion() + "@zip");
        final Configuration mcpDownloadConfiguration = spec.project().getConfigurations().detachedConfiguration(mcpConfigDependency);
        final ResolvedConfiguration resolvedConfiguration = mcpDownloadConfiguration.getResolvedConfiguration();
        final File mcpZipFile = resolvedConfiguration.getFiles().iterator().next();

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.minecraftVersion(), spec.side());

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Failed to read VersionJson from the launcher metadata for the minecraft version: %s", spec.minecraftVersion()), e);
        }

        final Configuration minecraftDependenciesConfiguration = spec.project().getConfigurations().detachedConfiguration();
        minecraftDependenciesConfiguration.setCanBeResolved(true);
        minecraftDependenciesConfiguration.setCanBeConsumed(false);
        for (VersionJson.Library library : versionJson.getLibraries()) {
            minecraftDependenciesConfiguration.getDependencies().add(
                    spec.project().getDependencies().create(library.getName())
            );
        }

        final File mcpDirectory = spec.project().getLayout().getBuildDirectory().dir(String.format("mcp/%s", spec.name())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpDirectory, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        FileUtils.unzip(mcpZipFile, unpackedMcpZipDirectory);

        final File mcpConfigFile = new File(unpackedMcpZipDirectory, "config.json");
        final McpConfigConfigurationSpecV2 mcpConfig = McpConfigConfigurationSpecV2.get(mcpConfigFile);

        mcpConfig.getLibraries(spec.side().getName()).forEach(library -> minecraftDependenciesConfiguration.getDependencies().add(
                spec.project().getDependencies().create(library)
        ));

        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec, mcpDirectory);

        final Map<String, File> data = buildDataMap(mcpConfig, spec.side(), unpackedMcpZipDirectory);

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.project().getTasks().register("supplySourcesFor" + spec.name(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(mcpDirectory, "sources.jar"));
            configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task);
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.project().getTasks().register("supplyRawJarFor" + spec.name(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(mcpDirectory, "raw.jar"));
            configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task);
        });

        return new McpRuntimeDefinition(spec, new LinkedHashMap<>(), unpackedMcpZipDirectory, mcpConfig, sourceJarTask, rawJarTask, gameArtifactTasks, gameArtifacts, minecraftDependenciesConfiguration);
    }

    @Override
    protected McpRuntimeSpecBuilder createBuilder() {
        return McpRuntimeSpecBuilder.from(getProject());
    }

    @Override
    protected void bakeDefinition(McpRuntimeDefinition definition) {
        final McpRuntimeSpec spec = definition.spec();
        final McpConfigConfigurationSpecV2 mcpConfig = definition.mcpConfig();

        final Minecraft minecraftExtension = spec.project().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.project().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final File mcpDirectory = spec.project().getLayout().getBuildDirectory().dir(String.format("mcp/%s", spec.name())).get().getAsFile();
        final File unpackedMcpZipDirectory = new File(mcpDirectory, "unpacked");
        final File stepsMcpDirectory = new File(mcpDirectory, "steps");

        final Map<String, File> data = buildDataMap(mcpConfig, spec.side(), unpackedMcpZipDirectory);

        final List<McpConfigConfigurationSpecV1.Step> steps = mcpConfig.getSteps(spec.side().getName());
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Unknown side: " + spec.side() + " For Config: " + definition.spec().mcpVersion());
        }

        final LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs = definition.taskOutputs();
        for (McpConfigConfigurationSpecV1.Step step : steps) {
            Optional<TaskProvider<? extends WithOutput>> adaptedInput = Optional.empty();

            if (spec.preTaskTypeAdapters().containsKey(step.getName())) {
                final String inputArgumentMarker = step.getValue("input");
                if (inputArgumentMarker == null) {
                    throw new IllegalStateException("Can not change input chain on: " + step.getName() + " it has no input to transform!");
                }

                Optional<TaskProvider<? extends WithOutput>> inputTask = McpRuntimeUtils.getInputTaskForTaskFrom(spec, inputArgumentMarker, taskOutputs);

                if (!spec.preTaskTypeAdapters().get(step.getName()).isEmpty() && inputTask.isPresent()) {
                    for (TaskTreeAdapter taskTreeAdapter : spec.preTaskTypeAdapters().get(step.getName())) {
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(spec, inputTask.get(), taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
                        modifiedTree.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));
                        inputTask = Optional.of(modifiedTree);
                    }

                    adaptedInput = inputTask;
                }
            }

            TaskProvider<? extends WithOutput> mcpRuntimeTaskProvider = createBuiltIn(spec, mcpConfig, step, taskOutputs, definition.gameArtifactProvidingTasks(), adaptedInput);

            if (mcpRuntimeTaskProvider == null) {
                McpConfigConfigurationSpecV1.Function function = mcpConfig.getFunction(step.getType());
                if (function == null) {
                    throw new IllegalArgumentException(String.format("Invalid MCP Config, Unknown function step type: %s File: %s", step.getType(), mcpConfig));
                }

                mcpRuntimeTaskProvider = createExecute(spec, step, function);
            }

            Optional<TaskProvider<? extends WithOutput>> finalAdaptedInput = adaptedInput;
            mcpRuntimeTaskProvider.configure((WithOutput mcpRuntimeTask) ->{
                if (mcpRuntimeTask instanceof Runtime) {
                    final Runtime runtimeTask = (Runtime) mcpRuntimeTask;
                    configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, taskOutputs, step, runtimeTask, finalAdaptedInput);
                }
            });

            if (!spec.postTypeAdapters().containsKey(step.getName())) {
                taskOutputs.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            } else {
                for (TaskTreeAdapter taskTreeAdapter : spec.postTypeAdapters().get(step.getName())) {
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(spec, mcpRuntimeTaskProvider, dependentTaskProvider -> dependentTaskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
                    taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));
                    mcpRuntimeTaskProvider = taskProvider;
                }

                taskOutputs.put(mcpRuntimeTaskProvider.getName(), mcpRuntimeTaskProvider);
            }
        }

        final TaskProvider<? extends WithOutput> lastTask = Iterators.getLast(taskOutputs.values().iterator());

        final Map<String, String> versionData = Maps.newHashMap(mappingsExtension.getVersion().get());
        versionData.put(NamingConstants.Version.MINECRAFT_VERSION, spec.minecraftVersion());
        versionData.put(McpRuntimeConstants.Naming.Version.MCP_RUNTIME, spec.name());
        definition.configureMappingVersionData(versionData);
        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                spec.project(), "mapGame", lastTask, definition.gameArtifactProvidingTasks(), versionData, additionalRuntimeTasks
        );

        final TaskProvider<? extends Runtime> remapTask = context.getNamingChannel().getApplySourceMappingsTaskBuilder().get().build(context);
        additionalRuntimeTasks.forEach(taskProvider -> taskProvider.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task)));
        remapTask.configure(task -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, task));

        final FileCollection recompileDependencies = spec.additionalRecompileDependencies().plus(spec.project().files(definition.minecraftDependenciesConfiguration()));
        final TaskProvider<? extends Runtime> recompileTask = spec.project()
                .getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "recompile"), RecompileSourceJar.class, recompileSourceJar -> {
                    recompileSourceJar.getInputJar().set(remapTask.flatMap(WithOutput::getOutput));
                    recompileSourceJar.getCompileClasspath().setFrom(recompileDependencies);
                    recompileSourceJar.getStepName().set("recompile");
                });
        recompileTask.configure(mcpRuntimeTask -> configureMcpRuntimeTaskWithDefaults(spec, mcpDirectory, data, mcpRuntimeTask));

        taskOutputs.put(recompileTask.getName(), recompileTask);

        definition.sourceJarTask().configure(task -> {
            task.getInput().set(remapTask.flatMap(WithOutput::getOutput));
            task.dependsOn(remapTask);
        });
        definition.rawJarTask().configure(task -> {
            task.getInput().set(recompileTask.flatMap(WithOutput::getOutput));
            task.dependsOn(recompileTask);
        });
    }

    public abstract Property<ArtifactSide> getSide();

    public abstract Property<String> getDefaultVersion();

}