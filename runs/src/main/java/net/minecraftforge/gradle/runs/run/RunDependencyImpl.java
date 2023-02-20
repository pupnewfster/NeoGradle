package net.minecraftforge.gradle.runs.run;

import net.minecraftforge.gradle.dsl.runs.run.RunDependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class RunDependencyImpl implements RunDependency {

    @Inject
    public RunDependencyImpl(Project project, Dependency dependency) {
        getIdentity().convention(dependency.toString());
        getDependency().from(project.provider(() -> {
            final Configuration configuration = project.getConfigurations().detachedConfiguration(dependency);
            final ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
            final ConfigurableFileCollection files = project.files();
            return files.from(resolvedConfiguration.getFiles());
        }));
    }

    @Override
    public abstract ConfigurableFileCollection getDependency();

    @Override
    public abstract Property<String> getIdentity();
}