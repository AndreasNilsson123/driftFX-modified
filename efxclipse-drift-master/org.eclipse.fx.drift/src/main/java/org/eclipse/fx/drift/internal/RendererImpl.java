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
/*
 * Removed static map holding renders due to memory leaks with this approach
 *      Andreas Nilsson
 */
package org.eclipse.fx.drift.internal;


import org.eclipse.fx.drift.DriftFXSurface;
import org.eclipse.fx.drift.Renderer;
import org.eclipse.fx.drift.Swapchain;
import org.eclipse.fx.drift.SwapchainConfig;
import org.eclipse.fx.drift.Vec2d;
import org.eclipse.fx.drift.Vec2i;
import org.eclipse.fx.drift.internal.backend.Backend;
import org.eclipse.fx.drift.internal.backend.BackendImpl;
import org.eclipse.fx.drift.internal.frontend.Frontend;
import org.eclipse.fx.drift.internal.frontend.FrontendImpl;
import org.eclipse.fx.drift.internal.transport.VMTransport;

public class RendererImpl implements Renderer {
	private static final DriftLogger LOGGER = DriftFX.createLogger(RendererImpl.class);
	
	//private static Map<DriftFXSurface, Renderer> renderers = new HashMap<>();
	
	public static Renderer getRenderer(DriftFXSurface surface) {
		//return renderers.computeIfAbsent(surface, surf -> new RendererImpl(surf));
		return new RendererImpl(surface);
	}

	private DriftFXSurface surface;
	private Backend backend;
	private Frontend frontend;
	private VMTransport transport;
	
	public RendererImpl(DriftFXSurface surface) {
		this.surface = surface;
		
		backend = new BackendImpl();
		frontend = new FrontendImpl(surface);
		transport = new VMTransport(frontend, backend);
		transport.start();
	}
	
	@Override
	public Vec2i getSize() {
		double w = surface.getWidth();
		double h = surface.getHeight();
		double userScale = surface.getUserScaleFactor();
		double screenScale = surface.getScreenScaleFactor();
		
		int x = (int) Math.ceil(w * userScale * screenScale);
		int y = (int) Math.ceil(h * userScale * screenScale);
		
		return new Vec2i(x, y);
	}
	
	
	@Override
	public Vec2d getLogicalSize() {
		double w = surface.getWidth();
		double h = surface.getHeight();
		return new Vec2d(w, h);
	}
	
	@Override
	public Vec2d getUserScale() {
		double userScale = surface.getUserScaleFactor();
		return new Vec2d(userScale, userScale);
	}
	
	@Override
	public Vec2d getScreenScale() {
		double screenScale = surface.getScreenScaleFactor();
		return new Vec2d(screenScale, screenScale);
	}

	@Override
	public Swapchain createSwapchain(SwapchainConfig config) {
		return backend.createSwapchain(config);
	}
}
