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
package org.eclipse.fx.drift;

import java.util.Optional;

public interface Swapchain {
	
	SwapchainConfig getConfig();
	
	RenderTarget acquire() throws InterruptedException;
	Optional<RenderTarget> tryAcquire();
	
	void present(RenderTarget image);
	
	void dispose();
}
