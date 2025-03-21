/* ******************************************************************************
 * Copyright (c) 2019, 2020 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0 
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Caks <ccaks@bestsolution.at> - initial API and implementation
 * ******************************************************************************/
package org.eclipse.fx.drift.internal.backend;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.fx.drift.SwapchainConfig;
import org.eclipse.fx.drift.internal.transport.Command;

public interface Backend {

	BackendSwapchain createSwapchain(SwapchainConfig config);
	
	// transport api
	void setCommandChannel(Consumer<Command> commandChannel);
	void receiveCommand(Command command); 
	void sendCommand(Command command);
	
	<C extends Command> CompletableFuture<C> waitForCommand(Class<C> type, Predicate<C> filter);
}
