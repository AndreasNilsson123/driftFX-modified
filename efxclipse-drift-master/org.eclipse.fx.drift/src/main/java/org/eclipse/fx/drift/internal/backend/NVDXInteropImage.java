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

import static org.eclipse.fx.drift.internal.GL.glDeleteTexture;
import static org.eclipse.fx.drift.internal.GL.glGenTexture;

import org.eclipse.fx.drift.Vec2i;
import org.eclipse.fx.drift.internal.GL;
import org.eclipse.fx.drift.internal.common.ImageData;
import org.eclipse.fx.drift.internal.common.NVDXInteropImageData;
import org.eclipse.fx.drift.internal.jni.win32.D3D9;
import org.eclipse.fx.drift.internal.jni.win32.NVDXInterop;
import org.eclipse.fx.drift.internal.jni.win32.Win32;
import org.eclipse.fx.drift.internal.jni.win32.WindowsError;

public class NVDXInteropImage implements Image {

	public static final Object syncedNVDXInterop = new Object();
	
	public static final ImageType TYPE = new ImageType("NVDXInterop");
	
	
	private int number;
	private Vec2i size;
	
	private NVDXInteropImageData data;
	
	int glTexture;
	
	static Win32.IDirect3DDevice9Ex dxDevice = D3D9.CreateOffscreenDevice();
//	private static DXInteropDevice dxInteropDevice = new DXInteropDevice(dxDevice);
	
	private NVDXInteropDevice device;
	
	private Win32.IDirect3DTexture9 dxTexture;
	private Win32.HANDLE dxTextureShareHandle;
	
	private Win32.HANDLE hObject;
	
	public NVDXInteropImage(int number, Vec2i size) {
		this.number = number;
		this.size = size;
	}
	
	@Override
	public ImageData getData() {
		return data;
	}
	
	@Override
	public int getGLTexture() {
		return glTexture;
	}
	
	@Override
	public void allocate() {
		synchronized (syncedNVDXInterop) {
			try {
				device = NVDXInteropDevice.openDevice(dxDevice);
				
				glTexture = glGenTexture();
		
				dxTexture = dxDevice.CreateTexture(size.x, size.y, 0, Win32.D3DUSAGE_DYNAMIC, Win32.D3DFMT_A8R8G8B8, Win32.D3DPOOL_DEFAULT);
				
				NVDXInterop.wglDXSetResourceShareHandleNV(dxTexture, dxTexture.shareHandle);
				// TODO add constant: WGL_ACCESS_READ_WRITE_NV 0x0001
				
				hObject = NVDXInterop.wglDXRegisterObjectNV(device.hDevice, dxTexture, glTexture, GL.GL_TEXTURE_2D, 0x0001);
				
				this.data = new NVDXInteropImageData(number, size, dxTexture.shareHandle.address);
			}
			catch (WindowsError e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void release() {
		synchronized (syncedNVDXInterop) {
			try {
				NVDXInterop.wglDXUnregisterObjectNV(device.hDevice, hObject);
				
				glDeleteTexture(glTexture);
				dxTexture.Release();
				
				device.closeDevice();
			}
			catch (WindowsError e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void onAcquire() {
		synchronized (syncedNVDXInterop) {
			try {
				NVDXInterop.wglDXLockObjectsNV(device.hDevice, hObject);
			}
			catch (WindowsError e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void onPresent() {
		synchronized (syncedNVDXInterop) {
			try {
				NVDXInterop.wglDXUnlockObjectsNV(device.hDevice, hObject);
			}
			catch (WindowsError e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public String toString() {
		return TYPE+"Image("+number+")";
	}
}
