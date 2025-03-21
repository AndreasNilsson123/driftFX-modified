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
package org.eclipse.fx.drift.internal.jni.win32;

import org.eclipse.fx.drift.internal.DriftFX;
import org.eclipse.fx.drift.internal.DriftLogger;
import org.eclipse.fx.drift.internal.GL;

public class NVDXInterop {
	private static final DriftLogger LOGGER = DriftFX.createLogger(NVDXInterop.class);
	
	public final static int WGL_ACCESS_READ_ONLY_NV = 0x0000;
	public final static int WGL_ACCESS_READ_WRITE_NV = 0x0001;
	public final static int WGL_ACCESS_WRITE_DISCARD_NV = 0x0002;
	
	static {
		GL.require();
	}
	
	public static Win32.HANDLE wglDXOpenDeviceNV(Win32.IDirect3DDevice9Ex dxDevice) throws WindowsError {
		long result = nWglDXOpenDeviceNV(dxDevice.address);
		if (result == 0) {
			WindowsError.checkLastError();
		}
		return new Win32.HANDLE(result);
	}
	private native static long nWglDXOpenDeviceNV(long dxDevice);
	
	
	public static boolean wglDXCloseDeviceNV(Win32.HANDLE hDevice) throws WindowsError {
		boolean result = nWglDXCloseDeviceNV(hDevice.address);
		if (!result) {
			WindowsError.checkLastError();
		}
		return result;
	}
	private native static boolean nWglDXCloseDeviceNV(long hDevice);
	
	public static boolean isAvailable() {
		return nIsAvailable();
	}
	private native static boolean nIsAvailable();
	
	public static boolean wglDXSetResourceShareHandleNV(Win32.IDirect3DResource9 dxResource, Win32.HANDLE shareHandle) throws WindowsError {
		boolean result = nWglDXSetResourceShareHandleNV(dxResource.address, shareHandle.address);
		LOGGER.debug(() -> result + " = wglDXSetResourceShareHandleNV(" + Long.toHexString(dxResource.address) + ", " + Long.toHexString(shareHandle.address) + ")");
		
		if (!result) {
			WindowsError.checkLastError();
		}
		return result;
	}
	private native static boolean nWglDXSetResourceShareHandleNV(long dxResource, long shareHandle);

	public static Win32.HANDLE wglDXRegisterObjectNV(Win32.HANDLE hDevice, Win32.IDirect3DResource9 dxResource, int name, int type, int access) throws WindowsError {
		long result = nWglDXRegisterObjectNV(hDevice.address, dxResource.address, name, type, access);
		LOGGER.debug(() -> Long.toHexString(result) + " = wglDXRegisterObjectNV(" + Long.toHexString(hDevice.address) + ", " + Long.toHexString(dxResource.address) + ", " + name + ", " + type + ", " + access + ")");
		if (result == 0) {
			WindowsError.checkLastError();
		}
		return new Win32.HANDLE(result);
	}
	private native static long nWglDXRegisterObjectNV(long hDevice, long dxResource, int name, int type, int access);

	public static boolean wglDXUnregisterObjectNV(Win32.HANDLE hDevice, Win32.HANDLE hObject) throws WindowsError {
		boolean result = nWglDXUnregisterObjectNV(hDevice.address, hObject.address);
		LOGGER.debug(() -> "calling wglDXUnregisterObjectNV(" + Long.toHexString(hDevice.address) + ", " + Long.toHexString(hObject.address) + ")");
		if (!result) {
			WindowsError.checkLastError();
		}
		return result;
	}
	private native static boolean nWglDXUnregisterObjectNV(long hDevice, long hObject);
	
	public static boolean wglDXLockObjectsNV(Win32.HANDLE hDevice, Win32.HANDLE hObject) throws WindowsError {
		boolean result = nWglDXLockObjectsNV(hDevice.address, hObject.address);
		if (!result) {
			WindowsError.checkLastError();
		}
		return result;
	}
	private native static boolean nWglDXLockObjectsNV(long hDevice, long hObject);
	
	public static boolean wglDXUnlockObjectsNV(Win32.HANDLE hDevice, Win32.HANDLE hObject) throws WindowsError {
		boolean result = nWglDXUnlockObjectsNV(hDevice.address, hObject.address);
		if (!result) {
			WindowsError.checkLastError();
		}
		return result;
	}
	private native static boolean nWglDXUnlockObjectsNV(long hDevice, long hObject);
	
}
