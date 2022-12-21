package net.minecraftforge.gradle.dsl.common.extensions;

import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.ArtifactSide;
import net.minecraftforge.gradle.dsl.common.util.CacheFileSelector;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * Defines a cache for minecraft artifacts.
 */
public interface MinecraftArtifactCache extends BaseDSLElement<MinecraftArtifactCache> {

    /**
     * The directory where the cache is stored.
     */
    @DSLProperty
    DirectoryProperty getCacheDirectory();

    /**
     * Gives access to all cached files in the current session.
     *
     * @return A map of all cached files in the current session.
     */
    @NotNull
    Map<CacheFileSelector, File> getCacheFiles();

    /**
     * Caches an entire game version eagerly.
     *
     * @param gameVersion The game version to cache.
     * @param side The distribution side to cache.
     * @return A map which contains all cached files.
     */
    @NotNull
    Map<GameArtifact, File> cacheGameVersion(@NotNull String gameVersion, @NotNull ArtifactSide side);

    /**
     * Caches an entire game version lazily.
     *
     * @param project The project to use.
     * @param outputDirectory The output directory to use.
     * @param gameVersion The game version to cache.
     * @param side The distribution side to cache.
     * @return A map which maps the game artifact to the task which caches it.
     */
    @NotNull
    Map<GameArtifact, TaskProvider<? extends WithOutput>> cacheGameVersionTasks(@NotNull Project project, @NotNull File outputDirectory, @NotNull String gameVersion, @NotNull ArtifactSide side);

    /**
     * Eagerly caches the launcher metadata.
     *
     * @return The cached launcher metadata.
     */
    @NotNull
    File cacheLauncherMetadata();

    /**
     * Eagerly caches the given game versions manifest.
     *
     * @param gameVersion The game version to cache.
     * @return The cached game versions manifest.
     */
    @NotNull
    File cacheVersionManifest(@NotNull String gameVersion);

    /**
     * Eagerly caches the given artifact of the given game version.
     *
     * @param gameVersion The game version to cache.
     * @param side The distribution side to cache.
     * @return The cached game version.
     */
    @NotNull
    File cacheVersionArtifact(@NotNull String gameVersion, @NotNull ArtifactSide side);

    /**
     * Eagerly caches the given mappings of the given game version.
     *
     * @param gameVersion The game version to cache.
     * @param side The distribution side to cache.
     * @return The cached game version.
     */
    @NotNull
    File cacheVersionMappings(@NotNull String gameVersion, @NotNull ArtifactSide side);

    /**
     * Eagerly caches the URL to the location indicated by the given cache file selector.
     *
     * @param url The URL to cache.
     * @param selector The cache file selector to use.
     * @return The cached file.
     */
    @NotNull
    File cache(@NotNull URL url, @NotNull CacheFileSelector selector);

    /**
     * Eagerly caches the url in the given string to the location indicated by the given cache file selector.
     *
     * @param url The URL to cache.
     * @param selector The cache file selector to use.
     * @return The cached file.
     */
    @NotNull
    File cache(@NotNull String url, @NotNull CacheFileSelector selector);
}