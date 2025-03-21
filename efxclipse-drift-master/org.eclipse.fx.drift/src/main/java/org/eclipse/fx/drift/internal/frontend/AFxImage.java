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

import org.eclipse.fx.drift.internal.DriftFX;
import org.eclipse.fx.drift.internal.DriftLogger;
import org.eclipse.fx.drift.internal.common.ImageData;
import org.eclipse.fx.drift.internal.prism.Prism;

import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;

@SuppressWarnings("restriction")
public abstract class AFxImage<D extends ImageData> implements FxImage<D> {
	private static final DriftLogger LOGGER = DriftFX.createLogger(AFxImage.class);
	
	private Texture texture;
	
	protected final D data;
	
	public AFxImage(D data) {
		this.data = data;
	}
	
	@Override
	public D getData() {
		return data;
	}

	@Override
	public void allocate(ResourceFactory rf) throws Exception {
		texture = rf.createTexture(PixelFormat.BYTE_BGRA_PRE, Texture.Usage.DYNAMIC, Texture.WrapMode.CLAMP_NOT_NEEDED, data.size.x, data.size.y);
		texture.makePermanent();
		LOGGER.debug(() -> "Texture Created! " + this + " / " + texture + " / " + Prism.getTextureHandle(texture));
	}

	@Override
	public void release() {
		texture.dispose();
	}

	@Override
	public void update() {
		// default implementation does nothing
	}

	@Override
	public Texture getTexture() {
		return texture;
	}

}
