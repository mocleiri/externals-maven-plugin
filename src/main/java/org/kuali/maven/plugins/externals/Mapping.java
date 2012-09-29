/**
 * Copyright 2011-2012 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.maven.plugins.externals;

public class Mapping implements Comparable<Mapping> {

	String module;
	String versionProperty;

	public Mapping() {
		this(null, null);
	}

	public Mapping(String module, String versionProperty) {
		super();
		this.module = module;
		this.versionProperty = versionProperty;
	}

	@Override
	public int compareTo(Mapping mapping) {
		return module.compareTo(mapping.getModule());
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getVersionProperty() {
		return versionProperty;
	}

	public void setVersionProperty(String property) {
		this.versionProperty = property;
	}
}
