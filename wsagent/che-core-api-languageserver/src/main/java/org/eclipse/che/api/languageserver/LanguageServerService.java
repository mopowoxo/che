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

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.eclipse.che.api.core.jsonrpc.commons.JsonRpcException;
import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.languageserver.server.dto.DtoServerImpls;
import org.eclipse.che.api.languageserver.server.dto.DtoServerImpls.ServerCapabilitiesDto;
import org.eclipse.che.api.languageserver.shared.model.LanguageRegex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language server service that handles JSON-RPC requests related to language server initialization
 * and matching.
 */
@Singleton
class LanguageServerService {
  private static final Logger LOG = LoggerFactory.getLogger(LanguageServerService.class);

  @Inject
  LanguageServerService(
      RequestHandlerConfigurator configurator,
      LanguageServerInitializer languageServerInitializer,
      RegistryContainer registryContainer) {
    configurator
        .newConfiguration()
        .methodName("languageServer/initialize")
        .paramsAsString()
        .resultAsDto(ServerCapabilitiesDto.class)
        .withFunction(
            wsPath -> {
              try {
                LOG.debug("Received 'languageServer/initialize' request for path: {}", wsPath);
                ServerCapabilitiesDto response =
                    new ServerCapabilitiesDto(
                        languageServerInitializer.initialize(wsPath).get(30, SECONDS));
                LOG.debug("Responding: {}", response);

                return response;
              } catch (CompletionException e) {
                LOG.error("Language server initialization procedure failed", e.getCause());
                throw new JsonRpcException(-27000, e.getCause().getMessage());
              } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Language server initialization procedure failed", e);
                throw new JsonRpcException(-27000, e.getMessage());
              }
            });

    configurator
        .newConfiguration()
        .methodName("languageServer/getLanguageRegexes")
        .noParams()
        .resultAsListOfDto(DtoServerImpls.LanguageRegexDto.class)
        .withFunction(
            __ ->
                registryContainer
                    .languageFilterRegistry
                    .getAll()
                    .entrySet()
                    .stream()
                    .map(
                        it -> {
                          LOG.debug("Received 'languageServer/getLanguageRegexes'");

                          String languageId = it.getKey();
                          String regex = it.getValue();
                          LanguageRegex languageRegex = new LanguageRegex();
                          languageRegex.setNamePattern(regex);
                          languageRegex.setLanguageId(languageId);
                          DtoServerImpls.LanguageRegexDto response =
                              new DtoServerImpls.LanguageRegexDto(languageRegex);

                          LOG.debug("Responding: {}", response);
                          return response;
                        })
                    .collect(toList()));
  }
}
