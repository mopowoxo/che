/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.event;

import org.eclipse.che.api.core.notification.RemoteSubscriptionManager;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Register subscriber on {@link MachineLogEvent machine log event} for resending
 * this type of event via JSON-RPC to clients.
 *
 * @author Anton Korneta
 */
@Singleton
public class MachineLogJsonRpcMessenger {

    private final RemoteSubscriptionManager subscriptionManager;

    @Inject
    public MachineLogJsonRpcMessenger(RemoteSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @PostConstruct
    private void postConstruct() {
        subscriptionManager.register("machine/log",
                                     MachineLogEvent.class,
                                     this::predicate);
    }

    private boolean predicate(MachineLogEvent event, Map<String, String> scope) {
        return event.getRuntimeId().getWorkspaceId().equals(scope.get("workspaceId"));
    }
}