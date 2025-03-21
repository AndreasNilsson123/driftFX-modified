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
package org.eclipse.fx.drift.impl;

import org.eclipse.fx.drift.DriftFXConfig;
import org.eclipse.fx.drift.Placement;
import org.eclipse.fx.drift.internal.DriftFX;
import org.eclipse.fx.drift.internal.DriftLogger;
import org.eclipse.fx.drift.internal.FPSCounter;
import org.eclipse.fx.drift.internal.SurfaceData;
import org.eclipse.fx.drift.internal.frontend.FrontSwapChain;
import org.eclipse.fx.drift.internal.frontend.SimpleFrontSwapChain;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

// Note: this implementation is against internal JavafX API
@SuppressWarnings("restriction")
public class NGDriftFXSurface extends NGNode {
	private static final DriftLogger LOGGER = DriftFX.createLogger(NGDriftFXSurface.class);
	
	private SurfaceData surfaceData;
	private FrontSwapChain nextSwapChain;
	private FrontSwapChain swapChain;
	
	private FPSCounter fxFpsCounter = new FPSCounter(100);
	private Timeline historyTick;
	
	public void setSwapChain(FrontSwapChain swapChain) {
		this.nextSwapChain = swapChain;
	}
	
	public NGDriftFXSurface() {
		
		if (DriftFXConfig.isShowFps()) {
			historyTick = new Timeline(new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					fxFpsCounter.historyTick();
					if (swapChain != null) ((SimpleFrontSwapChain)swapChain).fpsCounter.historyTick();
				}
			}));
			historyTick.setCycleCount(Timeline.INDEFINITE);
			historyTick.play();
		}
		
	}
	
	public void destroy() {
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		destroy();
	}
	
	private float center(float dst, float src) {
		return (dst - src) / 2f;
	}
	private float end(float dst, float src) {
		return dst - src;
	}
	
	static class Pos {
		float x;
		float width;
		float y;
		float height;
		Pos(float x, float width, float y, float height) {
			this.x = x;
			this.width = width;
			this.y = y;
			this.height = height;
		}
	}
	
	private Pos computeCover(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		float dstRatio = dstWidth / dstHeight;
		float srcRatio = srcWidth / srcHeight;
		float width, height;
		if (dstRatio > srcRatio) {
			width = dstWidth;
			height = width / srcRatio;
		}
		else {
			height = dstHeight;
			width = height * srcRatio;
		}
		return new Pos(center(dstWidth, width), width, center(dstHeight, height), height);
		
	}
	private Pos computeContain(float dstWidth, float dstHeight, float srcWidth, float srcHeight)  {
		float dstRatio = dstWidth / dstHeight;
		float srcRatio = srcWidth / srcHeight;
		float width, height;
		if (srcRatio <= dstRatio) {
			height = dstHeight;
			width = height * srcRatio;
		}
		else {
			width = dstWidth;
			height = width / srcRatio;
		}
		return new Pos(center(dstWidth, width), width, center(dstHeight, height), height);
	}
	private Pos computeCenter(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(center(dstWidth, srcWidth), srcWidth, center(dstHeight, srcHeight), srcHeight);
	}
	private Pos computeTopLeft(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(0, srcWidth, 0, srcHeight);
	}
	private Pos computeTopCenter(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(center(dstWidth, srcWidth), srcWidth, 0, srcHeight);
	}
	private Pos computeTopRight(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(end(dstWidth, srcWidth), srcWidth, 0, srcHeight);
	}
	private Pos computeCenterLeft(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(0, srcWidth, center(dstHeight, srcHeight), srcHeight);
	}
	private Pos computeCenterRight(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(end (dstWidth, srcWidth), srcWidth, center(dstHeight, srcHeight), srcHeight);
	}
	private Pos computeBottomLeft(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(0, srcWidth, end(dstHeight, srcHeight), srcHeight);
	}
	private Pos computeBottomCenter(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(center(dstWidth, srcWidth), srcWidth, end(dstHeight, srcHeight), srcHeight);
	}
	private Pos computeBottomRight(float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		return new Pos(end(dstWidth, srcWidth), srcWidth, end(dstHeight, srcHeight), srcHeight);
	}
	
	private Pos computePlacement(Placement placement, float dstWidth, float dstHeight, float srcWidth, float srcHeight) {
		switch (placement) {
		case COVER: return computeCover(dstWidth, dstHeight, srcWidth, srcHeight);
		case CONTAIN: return computeContain(dstWidth, dstHeight, srcWidth, srcHeight);
		case TOP_LEFT: return computeTopLeft(dstWidth, dstHeight, srcWidth, srcHeight);
		case TOP_CENTER: return computeTopCenter(dstWidth, dstHeight, srcWidth, srcHeight);
		case TOP_RIGHT: return computeTopRight(dstWidth, dstHeight, srcWidth, srcHeight);
		case CENTER_LEFT: return computeCenterLeft(dstWidth, dstHeight, srcWidth, srcHeight);
		case CENTER_RIGHT: return computeCenterRight(dstWidth, dstHeight, srcWidth, srcHeight);
		case BOTTOM_LEFT: return computeBottomLeft(dstWidth, dstHeight, srcWidth, srcHeight);
		case BOTTOM_CENTER: return computeBottomCenter(dstWidth, dstHeight, srcWidth, srcHeight);
		case BOTTOM_RIGHT: return computeBottomRight(dstWidth, dstHeight, srcWidth, srcHeight);
		case CENTER:
			default:
				return computeCenter(dstWidth, dstHeight, srcWidth, srcHeight);
		}
	}
	
	private void drawStats(Graphics g) {
		DriftDebug.assertQuantumRenderer();
		if (swapChain != null) {
			g.setPaint(new Color(0,0,0,0.5f));
			g.fillRect(155, 0, 150, 85);
			String info = "Texture: " + swapChain.getSize().x + "x" + swapChain.getSize().y;
			info += "\nTransfer: " + swapChain.getTransferType().id;
			NGRenderUtil.writeText(g, -155, 0, 12, info, Color.WHITE, false);
		}
		NGRenderUtil.drawFPSGraph(g, 0, 0, 150, 40, "JavaFX", fxFpsCounter);
		if (swapChain != null) {
			FPSCounter c = ((SimpleFrontSwapChain)swapChain).fpsCounter;
			NGRenderUtil.drawFPSGraph(g, 0, 45, 150, 40, "Renderer", c);
		}
	}
	
	private void drawTexture(Graphics g, Texture t) {
		float frameContainerWidth = surfaceData.width;
		float frameContainerHeight = surfaceData.height;
		
		int frameTextureWidth = t.getContentWidth();
		int frameTextureHeight = t.getContentHeight();
		
		float frameTextureWidthFxSpace = frameTextureWidth / (surfaceData.userScaleX * surfaceData.renderScaleX);
		float frameTextureHeightFxSpace = frameTextureHeight / (surfaceData.userScaleY * surfaceData.renderScaleY);
		
		Pos pos = computePlacement(surfaceData.placementStrategy, frameContainerWidth, frameContainerHeight, frameTextureWidthFxSpace, frameTextureHeightFxSpace);

		// flip it vertically
		g.scale(1, -1);
		g.translate(0, -frameContainerHeight);		
			
		pos.y = frameContainerHeight - pos.y - pos.height;

		g.drawTexture(t, pos.x, pos.y, 
				pos.x + pos.width, pos.y + pos.height, 0, 0, frameTextureWidth, frameTextureHeight);
	}
	
//	@Override
	protected void renderContent(Graphics g) {
		DriftDebug.assertQuantumRenderer();
		fxFpsCounter.tickStart();
		
		if (swapChain != null && swapChain.isDisposed()) {
			swapChain = null;
		}
		
		if (nextSwapChain != null) {
			swapChain = nextSwapChain;
			nextSwapChain = null;
		}
		
		if (swapChain != null) {
			swapChain.getCurrentImage().ifPresent(image -> {
				
				image.update();
				
				BaseTransform saved = g.getTransformNoClone().copy();
				
				drawTexture(g, image.getTexture());
				
				// restore transform
				g.setTransform(saved);
				
			});
		}

		
		fxFpsCounter.tick();
		
		if (DriftFXConfig.isShowFps()) {
			drawStats(g);
		}
		
	}
	
	public void updateSurface(SurfaceData surfaceData)  {
		LOGGER.debug(() -> "NativeSurface updateSurface("+surfaceData+")");
		if (isValid(surfaceData)) {
			this.surfaceData = surfaceData;
		}
	}
	
	private boolean isValid(SurfaceData data) {
		return true;
	}

	@Override
	protected boolean hasOverlappingContents() {
		return false;
	}


}
