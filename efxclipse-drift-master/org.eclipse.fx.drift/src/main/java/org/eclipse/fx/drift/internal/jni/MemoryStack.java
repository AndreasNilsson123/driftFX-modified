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
package org.eclipse.fx.drift.internal.jni;

import java.nio.ByteBuffer;
import java.util.Stack;

import org.eclipse.fx.drift.internal.DriftFX;
import org.eclipse.fx.drift.internal.DriftLogger;


public class MemoryStack implements IMemoryStack {
	private static final DriftLogger LOGGER = DriftFX.createLogger(MemoryStack.class);
	
	public class StackData implements Pointer {
		public final int offset;
		public final int size;
		public StackData(int offset, int size) {
			this.offset = offset;
			this.size = size;
		}
		public void allocate() {
			beginOffset += size;
		}
		@Override
		public long getAddress() {
			return address + offset;
		}
	}
	
	public class Long extends StackData {
		public Long(int offset) {
			super(offset, java.lang.Long.SIZE);
		}
		public long get() {
			buffer.position(offset);
			byte[] data = new byte[size / 8];
			buffer.get(data);
			return bytesToLong(data, 0);
		}
		
		public void set(long value) {
			nSetLong(address + offset, value);
		}
		
		public long bytesToLong(final byte[] bytes, final int offset) {
		    long result = 0;
		    for (int i = 7; i >= 0 + offset; i--) {
		        result <<= 8;
		        result |= (bytes[i] & 0xFF);
		    }
		    return result;
		}
	}
	
	private long address;
	private ByteBuffer buffer;
	
	private int beginOffset;
	
	private Stack<java.lang.Integer> stack = new Stack<>();
	
	private static ThreadLocal<MemoryStack> localStack = new ThreadLocal<MemoryStack>();
	
	public static MemoryStack get() {
		if (localStack.get() == null) {
			localStack.set(new MemoryStack());
		}
		return localStack.get();
	}
	public static IScopedMemeoryStack scoped() {
		return MemoryStack.get().new ScopedMemoryStack();
	}
	
	class ScopedMemoryStack implements IScopedMemeoryStack {
		ScopedMemoryStack() {
			push();
		}
		@Override
		public void close() {
			pop();
		}
		@Override
		public Long allocateLong() {
			return MemoryStack.this.allocateLong();
		}
		@Override
		public Long allocateLong(long initialValue) {
			return MemoryStack.this.allocateLong(initialValue);
		}
	}
	
	public MemoryStack() {
		buffer = ByteBuffer.allocateDirect(1024 * 1024);
		address = nGetBufferAddress(buffer);
		beginOffset = 0;
	}
	
	
	private static native long nGetBufferAddress(ByteBuffer buffer);
	
	public static long getBufferAddress(ByteBuffer buffer) {
		return nGetBufferAddress(buffer);
	}
	
	public long getAddress() {
		return address;
	}
	
	@Override
	public Long allocateLong() {
		Long value = new Long(beginOffset);
		value.allocate();
		return value;
	}
	
	@Override
	public Long allocateLong(long initialValue) {
		Long value = allocateLong();
		value.set(initialValue);
		return value;
	}
	
	public void push() {
		stack.push(beginOffset);
	}
	
	public void pop() {
		beginOffset = stack.pop();
	}
	
	private static native void nSetLong(long target, long value);
	private static native void nOutputLong(long target);
	
	public static void output(IMemoryStack stack, Long t) {
		LOGGER.debug(() -> "0x" + java.lang.Long.toHexString(t.get()) + " (" + t.get() + ")");
		nOutputLong(t.getAddress());
	}
	public static void main(String[] args) {
		DriftFX.require();
		MemoryStack stack = new MemoryStack();
		
		
		Long l1 = stack.allocateLong();
		//l1.set(java.lang.Long.MIN_VALUE);
		
		output(stack, l1);
		
		nSetLong(stack.getAddress(), java.lang.Long.MAX_VALUE);
		
		output(stack, l1);
		
		Long l2 = stack.allocateLong();
		l2.set(2 + 256);
		
		stack.push();
		{
			Long l3 = stack.allocateLong();
			l3.set(3);
			Long l4 = stack.allocateLong();
			l4.set(4);
			
			output(stack, l4);
		}
		stack.pop();
		
		Long l3 = stack.allocateLong();
		output(stack, l3);
	}
}
