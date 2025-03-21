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

import org.eclipse.fx.drift.internal.common.NVDXInteropImageData;
import org.eclipse.fx.drift.internal.jni.win32.Win32;
import org.eclipse.fx.drift.internal.jni.win32.Win32.IDirect3DTexture9;
import org.eclipse.fx.drift.internal.prism.Prism;
import org.eclipse.fx.drift.internal.prism.PrismD3D;

import com.sun.prism.ResourceFactory;

@SuppressWarnings("restriction")
public class NVDXInteropFXImage extends AFxImage<NVDXInteropImageData> {

	public NVDXInteropFXImage(NVDXInteropImageData data) {
		super(data);
	}

	@Override
	public void allocate(ResourceFactory rf) throws Exception {
		super.allocate(rf);
		
		// replace the prism texture with a shared one
		Win32.HANDLE shareHandle = new Win32.HANDLE(data.dxShareHandle);
		IDirect3DTexture9 newTexture = Prism.getD3DDevice().CreateTexture(getTexture().getContentWidth(), getTexture().getContentHeight(), 0, Win32.D3DUSAGE_DYNAMIC, Win32.D3DFMT_A8R8G8B8, Win32.D3DPOOL_DEFAULT, shareHandle);
		long prismTexture = Prism.getTextureHandle(getTexture());
		org.eclipse.fx.drift.internal.jni.win32.Prism.replacePrismD3DTexture(prismTexture, newTexture);
		
	}


	@Override
	public void update() {
		
	}

}
