package net.minecraftforge.gradle.dsl.common.util;

import org.gradle.api.tasks.Input;

import java.io.Serializable;

abstract class CacheFileSelector implements Serializable {

    static CacheFileSelector launcherMetadata() {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return "launcher_metadata.json";
            }
        };
    }

    static CacheFileSelector forVersionJson(final String version) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("versions/%s/metadata.json", version);
            }
        };
    }

    static CacheFileSelector forVersionJar(final String version, final String side) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("versions/%s/%s.jar", version, side);
            }
        };
    }

    static CacheFileSelector forVersionMappings(final String version, final String side) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("versions/%s/%s.txt", version, side);
            }
        };
    }

    @Input
    abstract String getCacheFileName();

    @Override
    int hashCode() {
        return this.getCacheFileName().hashCode();
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof CacheFileSelector)) {
            return false;
        }

        final CacheFileSelector cacheFileSelector = (CacheFileSelector) obj;

        return cacheFileSelector.getCacheFileName() == this.getCacheFileName();
    }
}
