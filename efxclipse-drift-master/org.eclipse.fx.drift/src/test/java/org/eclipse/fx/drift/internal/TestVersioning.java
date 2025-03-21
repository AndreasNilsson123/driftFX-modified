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

import org.junit.Test;

import org.junit.Assert;

public class TestVersioning {

	@Test
	public void readManifest() {
		Assert.assertEquals("999.0.0", Versioning.getVersion());
		Assert.assertEquals("qualifier", Versioning.getQualifier());
		Assert.assertEquals("timestamp", Versioning.getTimestamp());
		Assert.assertEquals("sha", Versioning.getSha());
	}
	
}
