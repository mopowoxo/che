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
package org.eclipse.che.plugin.csharp.languageserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.languageserver.DefaultInstanceProvider;
import org.eclipse.che.api.languageserver.LanguageServerConfig;
import org.eclipse.che.api.languageserver.ProcessCommunicationProvider;
import org.eclipse.che.plugin.csharp.inject.CSharpModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Evgen Vidolob */
@Singleton
public class CSharpLanguageServerConfig implements LanguageServerConfig {
  private static final Logger LOG = LoggerFactory.getLogger(CSharpLanguageServerConfig.class);

  private static final String REGEX = ".*\\.(cs|csx)";

  private final EventService eventService;

  private final Path launchScript;

  @Inject
  public CSharpLanguageServerConfig(EventService eventService) {
    this.eventService = eventService;

    this.launchScript = Paths.get(System.getenv("HOME"), "che/ls-csharp/launch.sh");
  }

  // TODO needs rework
  //  @PostConstruct
  //  private void subscribe() {
  //    eventService.subscribe(this::onLSProxyInitialized, LsProxyInitializedEvent.class);
  //  }
  //
  //  private void onLSProxyInitialized(LsProxyInitializedEvent event) {
  //    try {
  //      restoreDependencies(event.getLsProxyConfig().getProjectWsPath());
  //    } catch (LanguageServerException e) {
  //      LOG.error(e.getMessage(), e);
  //    }
  //  }
  //
  //  private void restoreDependencies(String projectPath) throws LanguageServerException {
  //    ProcessBuilder processBuilder = new ProcessBuilder("dotnet", "restore");
  //    processBuilder.directory(new File(LanguageServiceUtils.removeUriScheme(projectPath)));
  //    try {
  //      Process process = processBuilder.start();
  //      int resultCode = process.waitFor();
  //      if (resultCode != 0) {
  //        String err = IoUtil.readStream(process.getErrorStream());
  //        String in = IoUtil.readStream(process.getInputStream());
  //        throw new LanguageServerException(
  //            "Can't restore dependencies. Error: " + err + ". Output: " + in);
  //      }
  //    } catch (IOException | InterruptedException e) {
  //      throw new LanguageServerException("Can't start CSharp language server", e);
  //    }
  //  }

  @Override
  public RegexProvider getRegexpProvider() {
    return new RegexProvider() {
      @Override
      public Map<String, String> getLanguageRegexes() {
        return Collections.singletonMap(CSharpModule.LANGUAGE_ID, REGEX);
      }

      @Override
      public Set<String> getFileWatchPatterns() {
        return Collections.emptySet();
      }
    };
  }

  @Override
  public CommunicationProvider getCommunicationProvider() {
    ProcessBuilder processBuilder = new ProcessBuilder(launchScript.toString());
    processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
    processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);

    return new ProcessCommunicationProvider(processBuilder, CSharpModule.LANGUAGE_ID);
  }

  @Override
  public InstanceProvider getInstanceProvider() {
    return DefaultInstanceProvider.getInstance();
  }
}
