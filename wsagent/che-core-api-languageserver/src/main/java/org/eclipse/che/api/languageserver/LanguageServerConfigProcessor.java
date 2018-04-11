/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.languageserver;

import static java.util.stream.Collectors.toSet;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.languageserver.LanguageServerConfig.CommunicationProvider;
import org.eclipse.che.api.languageserver.LanguageServerConfig.InstanceProvider;
import org.eclipse.che.api.languageserver.RegistryContainer.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process all provided language server configuration and fill corresponding registries. */
@Singleton
class LanguageServerConfigProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(LanguageServerConfigProcessor.class);
  private final Registry<String> idRegistry;
  private final Registry<Set<PathMatcher>> pathMatcherRegistry;
  private final Registry<Set<Pattern>> patternRegistry;
  private final Registry<InstanceProvider> instanceProviderRegistry;
  private final Registry<CommunicationProvider> communicationProviderRegistry;
  private final Registry<Boolean> localityRegistry;
  private final Registry<String> languageFilterRegistry;
  private Set<LanguageServerConfigProvider> providers;

  @Inject
  LanguageServerConfigProcessor(
      Set<LanguageServerConfigProvider> providers, RegistryContainer registryContainer) {
    this.providers = providers;
    this.idRegistry = registryContainer.idRegistry;
    this.pathMatcherRegistry = registryContainer.pathMatcherRegistry;
    this.patternRegistry = registryContainer.patternRegistry;
    this.instanceProviderRegistry = registryContainer.instanceProviderRegistry;
    this.communicationProviderRegistry = registryContainer.communicationProviderRegistry;
    this.localityRegistry = registryContainer.localityRegistry;
    this.languageFilterRegistry = registryContainer.languageFilterRegistry;
  }

  @PostConstruct
  private void process() {
    LOG.debug("Language server config processing: started");
    for (LanguageServerConfigProvider provider : providers) {
      Map<String, LanguageServerConfig> configs = provider.getAll();
      for (Entry<String, LanguageServerConfig> entry : configs.entrySet()) {
        String id = entry.getKey();
        LOG.debug("Processing for language server {}: started", id);

        LanguageServerConfig config = entry.getValue();
        CommunicationProvider communicationProvider = config.getCommunicationProvider();
        InstanceProvider instanceProvider = config.getInstanceProvider();

        Map<String, String> languageRegexes = config.getRegexpProvider().getLanguageRegexes();
        LOG.debug("Language regexes: {}", languageRegexes);

        Set<String> fileWatchPatterns = config.getRegexpProvider().getFileWatchPatterns();
        LOG.debug("File watch patterns: {}", fileWatchPatterns);

        boolean isLocal = config.isLocal();
        LOG.debug("Locality: {}", isLocal);

        Set<Pattern> patterns =
            languageRegexes.values().stream().map(Pattern::compile).collect(toSet());

        FileSystem fileSystem = FileSystems.getDefault();
        Set<PathMatcher> pathMatchers =
            fileWatchPatterns.stream().map(fileSystem::getPathMatcher).collect(toSet());

        idRegistry.add(id, id);
        pathMatcherRegistry.add(id, pathMatchers);
        patternRegistry.add(id, patterns);
        instanceProviderRegistry.add(id, instanceProvider);
        communicationProviderRegistry.add(id, communicationProvider);
        localityRegistry.add(id, isLocal);

        languageRegexes.forEach(languageFilterRegistry::add);

        LOG.debug("Processing for language server {}: finished", id);
      }
    }
    LOG.debug("Language server config processing: finished");
  }
}
