package org.kuali.maven.plugins.externals;

public class BuildTag {
	String sourceUrl;
	long sourceRevision;
	String tagUrl;
	boolean skipped;

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public long getSourceRevision() {
		return sourceRevision;
	}

	public void setSourceRevision(long sourceRevision) {
		this.sourceRevision = sourceRevision;
	}

	public String getTagUrl() {
		return tagUrl;
	}

	public void setTagUrl(String tagUrl) {
		this.tagUrl = tagUrl;
	}

	public boolean isSkipped() {
		return skipped;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
}
