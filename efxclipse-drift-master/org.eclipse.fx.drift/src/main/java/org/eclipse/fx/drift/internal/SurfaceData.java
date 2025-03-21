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
package org.eclipse.fx.drift.internal;

import org.eclipse.fx.drift.Placement;

public class SurfaceData {
	public final float width;
	public final float height;
	public final float renderScaleX;
	public final float renderScaleY;
	public final float userScaleX;
	public final float userScaleY;
	public final int transferMode;
	public final Placement placementStrategy;
	
	public SurfaceData(float width, float height, float renderScaleX, float renderScaleY, float userScaleX, float userScaleY, int transferMode, Placement placementStrategy) {
		super();
		this.width = width;
		this.height = height;
		this.renderScaleX = renderScaleX;
		this.renderScaleY = renderScaleY;
		this.userScaleX = userScaleX;
		this.userScaleY = userScaleY;
		this.transferMode = transferMode;
		this.placementStrategy = placementStrategy;
	}
	
	@Override
	public String toString() {
		return String.format("SurfaceData[%5.3f x %5.3f] (renderScale: %5.3f x %5.3f, userScale: %5.3f x %5.3f, placement: %s)", width, height, renderScaleX, renderScaleY, userScaleX, userScaleY, placementStrategy.toString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(height);
		result = prime * result + ((placementStrategy == null) ? 0 : placementStrategy.hashCode());
		result = prime * result + Float.floatToIntBits(renderScaleX);
		result = prime * result + Float.floatToIntBits(renderScaleY);
		result = prime * result + transferMode;
		result = prime * result + Float.floatToIntBits(userScaleX);
		result = prime * result + Float.floatToIntBits(userScaleY);
		result = prime * result + Float.floatToIntBits(width);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SurfaceData other = (SurfaceData) obj;
		if (Float.floatToIntBits(height) != Float.floatToIntBits(other.height))
			return false;
		if (placementStrategy != other.placementStrategy)
			return false;
		if (Float.floatToIntBits(renderScaleX) != Float.floatToIntBits(other.renderScaleX))
			return false;
		if (Float.floatToIntBits(renderScaleY) != Float.floatToIntBits(other.renderScaleY))
			return false;
		if (transferMode != other.transferMode)
			return false;
		if (Float.floatToIntBits(userScaleX) != Float.floatToIntBits(other.userScaleX))
			return false;
		if (Float.floatToIntBits(userScaleY) != Float.floatToIntBits(other.userScaleY))
			return false;
		if (Float.floatToIntBits(width) != Float.floatToIntBits(other.width))
			return false;
		return true;
	}

	

	
	
}
