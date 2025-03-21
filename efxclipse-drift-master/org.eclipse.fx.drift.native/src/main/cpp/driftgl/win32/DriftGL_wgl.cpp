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
 * Changes to prevent OpenGL initialization messages to stdout
 *      Andreas Nilsson
*/


#ifdef DRIFTGL_WGL
#include "DriftGL.h"
#include "DriftGL_wgl.h"

#include <iostream>
#include <mutex>
#include <map>

#pragma comment (lib, "opengl32.lib")

namespace driftgl {

	int WGL_DRAW_TO_WINDOW_ARB = 0x2001;
	int WGL_ACCELERATION_ARB = 0x2003;
	int WGL_SUPPORT_OPENGL_ARB = 0x2010;
	int WGL_DOUBLE_BUFFER_ARB = 0x2011;
	int WGL_PIXEL_TYPE_ARB = 0x2013;
	int WGL_COLOR_BITS_ARB = 0x2014;
	int WGL_DEPTH_BITS_ARB = 0x2022;
	int WGL_STENCIL_BITS_ARB = 0x2023;

	int WGL_FULL_ACCELERATION_ARB = 0x2027;
	int WGL_TYPE_RGBA_ARB = 0x202B;

	int WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091;
	int WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092;
	int WGL_CONTEXT_PROFILE_MASK_ARB = 0x9126;
	int WGL_CONTEXT_FLAGS_ARB = 0x2094;
	int WGL_CONTEXT_DEBUG_BIT_ARB = 0x00000001;

	int WGL_CONTEXT_CORE_PROFILE_BIT_ARB = 0x00000001;
	int WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB = 0x00000002;

	std::mutex knownContextsMutex;
	std::map<HGLRC, Context*> knownContexts;

	void logError(std::string message) {
		// TODO send message to java based logger
		// std::cerr << "[C] " << message << std::endl;
	}

	void registerContext(HGLRC hglrc, Context* context) {
		knownContextsMutex.lock();
		knownContexts[hglrc] = context;
		knownContextsMutex.unlock();
	}
	void unregisterContext(HGLRC hglrc) {
		knownContextsMutex.lock();
		knownContexts.erase(hglrc);
		knownContextsMutex.unlock();
	}
	Context* getContext(HGLRC hglrc) {
		knownContextsMutex.lock();
		Context* context = knownContexts[hglrc];
		knownContextsMutex.unlock();
		return context;
	}


	typedef HGLRC(APIENTRY * PFNWGLCREATECONTEXTATTRIBSARBPROC)(HDC hDC, HGLRC hShareContext, const int *attribList);
	typedef BOOL(APIENTRY * PFNWGLCHOOSEPIXELFORMATARBPROC)(HDC hDC, const int *piAttribIList, const FLOAT *pfAttribFList, UINT nMaxFormats, int * piFormats, UINT *nNumFormats);
	static PFNWGLCREATECONTEXTATTRIBSARBPROC pfnWglCreateContextAttribsARB = 0;
	static PFNWGLCHOOSEPIXELFORMATARBPROC pfnWglChoosePixelFormatARB = 0;

	HGLRC wglCreateContextAttribsARB(HDC hDC, HGLRC hShareContext, const int *attribList) {
		return pfnWglCreateContextAttribsARB(hDC, hShareContext, attribList);
	}

	BOOL wglChoosePixelFormatARB(HDC hDC, const int *piAttribIList, const FLOAT *piAttribFList, UINT nMaxFormats, int *piFormats, UINT *nNumFormats) {
		return pfnWglChoosePixelFormatARB(hDC, piAttribIList, piAttribFList, nMaxFormats, piFormats, nNumFormats);
	}

struct WGLContext : Context {
	HWND hWND;
	HDC hDC;
	HGLRC hGLRC;
};

ATOM registerClass(HINSTANCE hInstance) {

	WNDCLASSEX wcex;
	ZeroMemory(&wcex, sizeof(wcex));
	wcex.cbSize = sizeof(wcex);
	wcex.style = CS_HREDRAW | CS_VREDRAW | CS_OWNDC;
	wcex.lpfnWndProc = DefWindowProc;
	wcex.hInstance = hInstance;
	wcex.hCursor = LoadCursor(NULL, IDC_ARROW);
	wcex.lpszClassName = "DriftGLDummyWindow";

	return RegisterClassEx(&wcex);
}

HWND CreateDriftGLWindow(HINSTANCE hInstance) {
	return CreateWindow(
		"DriftGLDummyWindow", "DriftGL Dummy Window",      // window class, title
		WS_CLIPSIBLINGS | WS_CLIPCHILDREN, // style
		0, 0,                       // position x, y
		1, 1,                       // width, height
		NULL, NULL,                 // parent window, menu
		hInstance, NULL);           // instance, param
}

bool SetupPixelFormat(HDC hDC) {
	bool success = false;
	int pixel_format_attribs[] = {
	WGL_SUPPORT_OPENGL_ARB,     (int) GL_TRUE,
	WGL_ACCELERATION_ARB,       WGL_FULL_ACCELERATION_ARB,
	0
	};

	int pixel_format;
	UINT num_formats;
	if (!wglChoosePixelFormatARB(hDC, pixel_format_attribs, 0, 1, &pixel_format, &num_formats)) {
		return false;
	}

	PIXELFORMATDESCRIPTOR pfd;
	int index = DescribePixelFormat(hDC, pixel_format, sizeof(pfd), &pfd);
	if (index > 0) {
		if (SetPixelFormat(hDC, pixel_format, &pfd)) {
			success = true;
		}
		else {
			//SetPixelFormat() failed.
		}
	}
	else {
		//DescribePixelFormat() failed.
	}
	return success;
}

typedef HANDLE (* PFNWGLDXOPENDEVICENV) (void* dxDevice);
typedef bool (* PFNWGLDXCLOSEDEVICENV) (HANDLE hDevice);
typedef HANDLE (* PFNWGLDXREGISTEROBJECTNV) (HANDLE hDevice, void* dxObject, GLuint name, GLenum type, GLenum access);
typedef bool (* PFNWGLDXUNREGISTEROBJECTNV) (HANDLE hDevice, HANDLE hObject);
typedef bool (* PFNWGLDXOBJECTACCESSNV) (HANDLE hObject, GLenum access);
typedef bool (* PFNWGLDXLOCKOBJECTSNV) (HANDLE hDevice, GLint count, HANDLE* hObjects);
typedef bool (* PFNWGLDXUNLOCKOBJECTSNV) (HANDLE hDevice, GLint count, HANDLE* hObjects);
typedef bool (* PFNWGLDXSETRESOURCESHAREHANDLENV) (void* dxObject, HANDLE shareHandle);

PFNWGLDXOPENDEVICENV pfnWglDXOpenDeviceNV;
PFNWGLDXCLOSEDEVICENV pfnWglDXCloseDeviceNV;
PFNWGLDXREGISTEROBJECTNV pfnWglDXRegisterObjectNV;
PFNWGLDXUNREGISTEROBJECTNV pfnWglDXUnregisterObjectNV;
PFNWGLDXOBJECTACCESSNV pfnWglDXObjectAccessNV;
PFNWGLDXLOCKOBJECTSNV pfnWglDXLockObjectsNV;
PFNWGLDXUNLOCKOBJECTSNV pfnWglDXUnlockObjectsNV;
PFNWGLDXSETRESOURCESHAREHANDLENV pfnWglDXSetResourceShareHandleNV;

bool isDXInteropAvailable() {
	// simple test if function pointer is resolved
	return pfnWglDXOpenDeviceNV != nullptr;
}

HANDLE wglDXOpenDeviceNV(void* dxDevice) {
	return pfnWglDXOpenDeviceNV(dxDevice);
}
bool wglDXCloseDeviceNV(HANDLE hDevice) {
	return pfnWglDXCloseDeviceNV(hDevice);
}
HANDLE wglDXRegisterObjectNV(HANDLE hDevice, void* dxObject, GLuint name, GLenum type, GLenum access) {
	return pfnWglDXRegisterObjectNV(hDevice, dxObject, name, type, access);
}
bool wglDXUnregisterObjectNV(HANDLE hDevice, HANDLE hObject) {
	return pfnWglDXUnregisterObjectNV(hDevice, hObject);
}
bool wglDXObjectAccess(HANDLE hObject, GLenum access) {
	return pfnWglDXObjectAccessNV(hObject, access);
}
bool wglDXLockObjectsNV(HANDLE hDevice, GLint count, HANDLE* hObjects) {
	return pfnWglDXLockObjectsNV(hDevice, count, hObjects);
}
bool wglDXUnlockObjectsNV(HANDLE hDevice, GLint count, HANDLE* hObjects) {
	return pfnWglDXUnlockObjectsNV(hDevice, count, hObjects);
}
bool wglDXSetResourceShareHandleNV(void* dxObject, HANDLE shareHandle) {
	return pfnWglDXSetResourceShareHandleNV(dxObject, shareHandle);
}
typedef GLubyte* (*PFNGLGETSTRING) (GLenum name);
void showContextInfo(std::string name, PFNGLGETSTRING glGetString) {
	//std::cout << "[C] OpenGL Context information \"" << name << "\"" << std::endl;
	//std::cout << "[C]   Version: " << glGetString(GL_VERSION) << std::endl;
	//std::cout << "[C]   Vendor: " << glGetString(GL_VENDOR) << std::endl;
	//std::cout << "[C]   Renderer: " << glGetString(GL_RENDERER) << std::endl;
}

PROC doGetProcAddress(LPCSTR name) {
	PROC proc = wglGetProcAddress(name);
	//std::cout << " * " << name << ": " << proc << std::endl;
	if (proc == 0) {
		DWORD errWgl = GetLastError();
		// fallback for old functions on m$
		HMODULE hModuleGL = GetModuleHandle("opengl32");
		proc = GetProcAddress(hModuleGL, name);
		if (proc == 0) {
			DWORD errWin = GetLastError();
			//std::cout << " ! Could not acquire " << name << " (Error: " << errWgl << " / " << errWin << ")" << std::endl;
		}
	}
	return proc;
}



void debugCurrentContext(std::string name) {
	PFNGLGETSTRING glGetString = (PFNGLGETSTRING)doGetProcAddress("glGetString");
	showContextInfo(name, glGetString);
}

bool successContextCreationPointers = false;
bool successGLPointers = false;


void doInitializeContextCreationPointers(HINSTANCE hInstance) {
	ATOM cls = registerClass(hInstance);
	HWND window = CreateDriftGLWindow(hInstance);
	HDC hDC = GetDC(window);
	PIXELFORMATDESCRIPTOR pfd =
	{
		sizeof(PIXELFORMATDESCRIPTOR),
		1,
		PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER,    // Flags
		PFD_TYPE_RGBA,        // The kind of framebuffer. RGBA or palette.
		32,                   // Colordepth of the framebuffer.
		0, 0, 0, 0, 0, 0,
		0,
		0,
		0,
		0, 0, 0, 0,
		24,                   // Number of bits for the depthbuffer
		8,                    // Number of bits for the stencilbuffer
		0,                    // Number of Aux buffers in the framebuffer.
		PFD_MAIN_PLANE,
		0,
		0, 0, 0
	};
	int pf = ChoosePixelFormat(hDC, &pfd);
	if (pf != 0) {
		if (SetPixelFormat(hDC, pf, &pfd) == TRUE) {
			HGLRC dummyContext = wglCreateContext(hDC);
			if (dummyContext != 0) {
				if (wglMakeCurrent(hDC, dummyContext) == TRUE) {
					
					debugCurrentContext("Dummy Context");

					// get the pointers
					pfnWglChoosePixelFormatARB = (PFNWGLCHOOSEPIXELFORMATARBPROC)wglGetProcAddress("wglChoosePixelFormatARB");
					pfnWglCreateContextAttribsARB = (PFNWGLCREATECONTEXTATTRIBSARBPROC)wglGetProcAddress("wglCreateContextAttribsARB");

					if (pfnWglChoosePixelFormatARB != 0 && pfnWglCreateContextAttribsARB != 0) {
						successContextCreationPointers = true;
					}
					else {
						SetLastDriftGLError("failed to get proc address of 'wglChoosePixelFormatARB' and 'wglCreateContextAttribsARB' from the dummy context.");
					}

					wglMakeCurrent(NULL, NULL);
				}
				else {
					SetLastDriftGLError("failed to make the dummy opengl context current.");
				}
				wglDeleteContext(dummyContext);
			}
			else {
				SetLastDriftGLError("failed to create a dummy opengl context.");
			}
		}
		else {
			SetLastDriftGLError("failed to set the pixel format during dummy context creation.");
		}
	}
	else {
		SetLastDriftGLError("failed to choose the pixel format during dummy context creation.");
	}
	ReleaseDC(window, hDC);
	DestroyWindow(window);
}

bool InitializeContextCreationPointers(HINSTANCE hInstance) {

	if (pfnWglChoosePixelFormatARB == 0) {

		// we do this on a different thread to prevent changes to the calling thread's current gl context
		std::thread initPointers(doInitializeContextCreationPointers, hInstance);
		initPointers.join();
	}

	return successContextCreationPointers;
}


void doInitializeGLPointers(HINSTANCE hInstance) {
	HWND window = CreateDriftGLWindow(hInstance);
	HDC hDC = GetDC(window);
	if (SetupPixelFormat(hDC) == true) {
		int attribList[] = {
		WGL_CONTEXT_MAJOR_VERSION_ARB, 4,
		WGL_CONTEXT_MINOR_VERSION_ARB, 5,
		WGL_CONTEXT_PROFILE_MASK_ARB,  WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
		0,
		};
		HGLRC realContext = wglCreateContextAttribsARB(hDC, NULL, attribList);
		if (realContext != 0) {
			if (wglMakeCurrent(hDC, realContext) == TRUE) {

				debugCurrentContext("Function Pointer Resolving Context");

				// init GL pointers
				procs::Initialize([](const char* name) { return (void*)doGetProcAddress(name); });

				// init NV_DX_interop pointers
				pfnWglDXOpenDeviceNV = (PFNWGLDXOPENDEVICENV)wglGetProcAddress("wglDXOpenDeviceNV");
				pfnWglDXCloseDeviceNV = (PFNWGLDXCLOSEDEVICENV)wglGetProcAddress("wglDXCloseDeviceNV");
				pfnWglDXRegisterObjectNV = (PFNWGLDXREGISTEROBJECTNV)wglGetProcAddress("wglDXRegisterObjectNV");
				pfnWglDXUnregisterObjectNV = (PFNWGLDXUNREGISTEROBJECTNV)wglGetProcAddress("wglDXUnregisterObjectNV");
				pfnWglDXObjectAccessNV = (PFNWGLDXOBJECTACCESSNV)wglGetProcAddress("wglDXObjectAccessNV");
				pfnWglDXLockObjectsNV = (PFNWGLDXLOCKOBJECTSNV)wglGetProcAddress("wglDXLockObjectsNV");
				pfnWglDXUnlockObjectsNV = (PFNWGLDXUNLOCKOBJECTSNV)wglGetProcAddress("wglDXUnlockObjectsNV");
				pfnWglDXSetResourceShareHandleNV = (PFNWGLDXSETRESOURCESHAREHANDLENV)wglGetProcAddress("wglDXSetResourceShareHandleNV");

				successGLPointers = true;

				wglMakeCurrent(NULL, NULL);
			}
			else {
				SetLastDriftGLError("failed to make the function pointer resolving context current.");
			}
			wglDeleteContext(realContext);
		}
		else {
			SetLastDriftGLError("failed to create the function pointer resolving context.");
		}
	}
	else {
		SetLastDriftGLError("failed to setup the pixel format during function pointer resolving context creation.");
	}
	ReleaseDC(window, hDC);
	DestroyWindow(window);
}

bool InitializeGLPointers(HINSTANCE hInstance) {

	// we do this on a different thread to prevent changes to the calling thread's current gl context
	std::thread initPointers(doInitializeGLPointers, hInstance);
	initPointers.join();

	return successGLPointers;
}


bool Initialize() {
	HINSTANCE hInstance = NULL;

	if (!InitializeContextCreationPointers(hInstance)) {
		return false;
	}

	return InitializeGLPointers(hInstance);
}

bool Destroy() {

	return true;
}

Context* CreateContext(Context* sharedContext, int majorHint, int minorHint) {
	//std::cout << "CreateContext" << std::endl << std::flush;
	WGLContext* sharedCtx = (WGLContext*)sharedContext;
	HGLRC shared = sharedCtx == nullptr ? nullptr : sharedCtx->hGLRC;
	WGLContext* ctx = new WGLContext();

	HINSTANCE hInstance = NULL;

	ctx->hWND = CreateDriftGLWindow(hInstance);
	//std::cout << " - hWND = " << ctx->hWND << std::endl;
	if (ctx->hWND == NULL) {
		// we failed to create the window
		delete ctx;
		return NULL;
	}
	ctx->hDC = GetDC(ctx->hWND);
	if (ctx->hDC == NULL) {
		// we failed to get the HDC
		DestroyWindow(ctx->hWND);
		delete ctx;
		return NULL;
	}
	//std::cout << " - hDC = " << ctx->hDC << std::endl;

	//std::cout << "SetupPixelForm from CreateContext" << std::endl;
	SetupPixelFormat(ctx->hDC);

// glfw context creation hints
//CONTEXT_CREATION_API NATIVE_CONTEXT_API NATIVE_CONTEXT_API EGL_CONTEXT_API OSMESA_CONTEXT_API 
//CONTEXT_VERSION_MAJOR 1 Any valid major version number of the chosen client API 
//CONTEXT_VERSION_MINOR 0 Any valid minor version number of the chosen client API 
//CONTEXT_ROBUSTNESS NO_ROBUSTNESS NO_ROBUSTNESS NO_RESET_NOTIFICATION LOSE_CONTEXT_ON_RESET 
//CONTEXT_RELEASE_BEHAVIOR ANY_RELEASE_BEHAVIOR ANY_RELEASE_BEHAVIOR RELEASE_BEHAVIOR_FLUSH RELEASE_BEHAVIOR_NONE 
//CONTEXT_NO_ERROR FALSE TRUE or FALSE 
//OPENGL_FORWARD_COMPAT FALSE TRUE or FALSE 
//OPENGL_DEBUG_CONTEXT FALSE TRUE or FALSE 
//OPENGL_PROFILE OPENGL_ANY_PROFILE OPENGL_ANY_PROFILE OPENGL_CORE_PROFILE OPENGL_COMPAT_PROFILE 


	int attribList[] = {
		WGL_CONTEXT_MAJOR_VERSION_ARB, majorHint,
		WGL_CONTEXT_MINOR_VERSION_ARB, minorHint,
		
		WGL_CONTEXT_FLAGS_ARB, WGL_CONTEXT_DEBUG_BIT_ARB,
		// TODO compat vs core contexts
		WGL_CONTEXT_PROFILE_MASK_ARB,  WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB,
	0,
	};

	ctx->hGLRC = wglCreateContextAttribsARB(ctx->hDC, shared, attribList);
	//std::cout << " - hGLRC = " << ctx->hGLRC << std::endl << std::flush;

	// MakeContextCurrent(ctx);
	//	std::cout << "OpenGL " << glGetString(GL_VERSION) << std::endl;

	if (ctx->hGLRC == NULL) {
		// we failed to create the context
		ReleaseDC(ctx->hWND, ctx->hDC);
		DestroyWindow(ctx->hWND);
		delete ctx;
		return NULL;
	}

	registerContext(ctx->hGLRC, ctx);

	return ctx;
}

Context* WrapContext(long long nativeContextHandle) {
	// TODO win32 wrapped context missing hdc
	WGLContext* ctx = new WGLContext();
	ctx->hGLRC = (HGLRC) nativeContextHandle;

	//registerContext(ctx->hGLRC, ctx);
	return ctx;
}


Context* CreateSharedCompatContext(Context* sharedContext) {
		//std::cout << "CreateContext" << std::endl << std::flush;
	WGLContext* sharedCtx = (WGLContext*)sharedContext;
	HGLRC shared = sharedCtx == nullptr ? nullptr : sharedCtx->hGLRC;
	WGLContext* ctx = new WGLContext();

	HINSTANCE hInstance = NULL;

	ctx->hWND = CreateDriftGLWindow(hInstance);
	//std::cout << " - hWND = " << ctx->hWND << std::endl;
	if (ctx->hWND == NULL) {
		// we failed to create the window
		delete ctx;
		return NULL;
	}
	ctx->hDC = GetDC(ctx->hWND);
	if (ctx->hDC == NULL) {
		// we failed to get the HDC
		DestroyWindow(ctx->hWND);
		delete ctx;
		return NULL;
	}
	//std::cout << " - hDC = " << ctx->hDC << std::endl;

	//std::cout << "SetupPixelForm from CreateSharedCompatContext" << std::endl;
	SetupPixelFormat(ctx->hDC);

	
	int context_attribs[] = {
			WGL_CONTEXT_MAJOR_VERSION_ARB, 2,
			WGL_CONTEXT_MINOR_VERSION_ARB, 1,
			WGL_CONTEXT_FLAGS_ARB, WGL_CONTEXT_DEBUG_BIT_ARB,
			WGL_CONTEXT_PROFILE_MASK_ARB, WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB,
			0
	};

	ctx->hGLRC = wglCreateContextAttribsARB(ctx->hDC, shared, context_attribs);
	//std::cout << " - hGLRC = " << ctx->hGLRC << std::endl << std::flush;

	// MakeContextCurrent(ctx);
	//	std::cout << "OpenGL " << glGetString(GL_VERSION) << std::endl;

	if (ctx->hGLRC == NULL) {
		// we failed to create the context
		ReleaseDC(ctx->hWND, ctx->hDC);
		DestroyWindow(ctx->hWND);
		delete ctx;
		return NULL;
	}

	registerContext(ctx->hGLRC, ctx);

	return ctx;
}

void DestroyContext(Context* context) {
	WGLContext* ctx = (WGLContext*)context;
	unregisterContext(ctx->hGLRC);
	wglDeleteContext(ctx->hGLRC);
	ReleaseDC(GetDesktopWindow(), ctx->hDC);
	DestroyWindow(ctx->hWND);
	delete ctx;
}

bool MakeContextCurrent(Context* context) {
	WGLContext* ctx = (WGLContext*)context;
	if (ctx == nullptr) {
		return wglMakeCurrent(nullptr, nullptr);
	}
	else {
		return wglMakeCurrent(ctx->hDC, ctx->hGLRC);
	}
}

bool IsContextCurrent(Context* context) {
	HGLRC currentContext = wglGetCurrentContext();
	WGLContext* ctx = (WGLContext*) context;
	return currentContext == ctx->hGLRC;
}

void* GetNativeContextHandle(Context* context) {
	WGLContext* ctx = (WGLContext*)context;
	
	return ctx->hGLRC;
}

Context* GetCurrentContext() {
	HGLRC currentContext = wglGetCurrentContext();
	return getContext(currentContext);
}

}

#endif
