package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.dsl.common.runtime.tasks.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class RuntimeDataImpl implements RuntimeData {

    private final ProviderFactory providerFactory;
    
    @Inject
    public RuntimeDataImpl(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }
    
    public Provider<File> get(String key) {
        return getFiles().map(files -> {
            final Optional<NamedFileRef> namedFile = files.stream().filter(f -> f.getName().equals(key)).findFirst();
            
            return namedFile.map(NamedFileRef::getFile).orElse(null);
        });
    }
    
    public Provider<File> getOrDefault(String key, Provider<File> defaultProvider) {
        return get(key).orElse(defaultProvider);
    }
    
    public Provider<Map<String, Provider<File>>> asMap() {
        final Provider<Map<String, Provider<File>>> filesProvider =
                getFiles().map(files -> files.stream()
                                                .collect(Collectors.toMap(
                                                        NamedFileRef::getName,
                                                        namedFiles -> providerFactory.provider(namedFiles::getFile),
                                                        (a, b) -> b,
                                                        HashMap::new
                                                )));
        
        return filesProvider;
    }
    
    @Override
    public void putFile(String input, Provider<File> fileProvider) {
        getFiles().add(fileProvider.map(file -> new NamedFile(input, file)));
    }
    
    @Override
    public void putRegularFile(String input, Provider<RegularFile> fileProvider) {
        getFiles().add(new NamedRegularFile(input, fileProvider));
    }
    
    @Override
    public void putDirectoryFile(String input, Provider<File> fileProvider) {
        getFiles().add(fileProvider.map(file -> new NamedDirectoryFile(input, file)));
    }
    
    @Override
    public void putDirectory(String input, Provider<Directory> fileProvider) {
        getFiles().add(new NamedDirectory(input, fileProvider));
    }
    
    @Override
    public void putAllFiles(Map<String, File> files) {
        files.forEach((key, file) -> {
            getFiles().add(providerFactory.provider(() -> new NamedFile(key, file)));
        });
    }
    
    @Override
    public void putAllDirectories(Map<String, File> files) {
        files.forEach((key, file) -> {
            getFiles().add(providerFactory.provider(() -> new NamedDirectoryFile(key, file)));
        });
    }
}
