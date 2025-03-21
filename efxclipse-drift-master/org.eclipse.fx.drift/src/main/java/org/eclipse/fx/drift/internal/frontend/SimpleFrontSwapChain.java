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
package org.eclipse.fx.drift.internal.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.eclipse.fx.drift.PresentationMode;
import org.eclipse.fx.drift.TransferType;
import org.eclipse.fx.drift.Vec2i;
import org.eclipse.fx.drift.internal.FPSCounter;
import org.eclipse.fx.drift.internal.common.ImageData;

import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;

@SuppressWarnings("restriction")
public class SimpleFrontSwapChain implements FrontSwapChain {

	private FrontendImpl frontend;
	
	private UUID id;
	private List<FxImage<?>> images = new ArrayList<>();
	private Map<ImageData, FxImage<?>> imageMap = new HashMap<>();
	
	private AtomicReference<ImageData> mailbox = new AtomicReference<>();
	
	private BiConsumer<UUID, ImageData> onRelease;
	private Vec2i size;
	private int imageCount;
	private PresentationMode presentationMode;
	
	public FPSCounter fpsCounter = new FPSCounter(100);
	
	private boolean disposed = false;
	
	
	public SimpleFrontSwapChain(FrontendImpl frontend, UUID id, List<ImageData> images, PresentationMode presentationMode, BiConsumer<UUID, ImageData> onRelease) {
		this.frontend = frontend;
		this.id = id;
		for (ImageData image : images) {
			FxImage<?> fxImage = FxImageFactory.createFxImage(image);
			this.images.add(fxImage);
			imageMap.put(image, fxImage);
		}
		
		this.presentationMode = presentationMode;
		this.onRelease = onRelease;
		
		allocate().join();
	}
	
	@Override
	public Optional<FxImage<?>> getCurrentImage() {
		return Optional.ofNullable(mailbox.get()).map(imageMap::get);
	}
	
	@Override
	public boolean isDisposed() {
		return disposed;
	}
	
	@Override
	public CompletableFuture<Void> allocate() {
		return frontend.asyncCallQuantumRenderer(() -> {
			ResourceFactory factory = GraphicsPipeline.getDefaultResourceFactory();
			try {
				for (FxImage<?> fxImage : images) {
					fxImage.allocate(factory);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		});
	}
	
	@Override
	public CompletableFuture<Void> dispose() {
		return frontend.asyncCallQuantumRenderer(() -> {

			ImageData old = mailbox.getAndSet(null);
			if (old != null) {
				release(old);
			}
			
			for (FxImage<?> fxImage : images) {
				fxImage.release();
			}
			return null;
		})
		.thenRun(() -> disposed = true)
		.thenRun(() -> frontend.sendSwapchainDisposed(id));
	}
	
	
	public Vec2i getSize() {
		return images.get(0).getData().size;
	}
	
	@Override
	public TransferType getTransferType() {
		return images.get(0).getData().type;
	}
	
	
	@Override
	public UUID getId() {
		return id;
	}
	
	// => called by backend
	public void present(ImageData image) {
		frontend.asyncCallQuantumRenderer(() -> {
			ImageData old = mailbox.getAndSet(image);
			if (old != null) {
				release(old);
			}
			return null;
		}).join();
		
		fpsCounter.tick();
	}

	// => calls backend
	private void release(ImageData image) {
//		System.err.println("DriftFX Frontend: Swapchain#release " + image.number);
		onRelease.accept(id, image);
	}
}
