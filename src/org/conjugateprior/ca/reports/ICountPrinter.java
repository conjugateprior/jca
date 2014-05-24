package org.conjugateprior.ca.reports;

import java.io.File;

public interface ICountPrinter {

	public enum Format { CSV, LDAC, MTX }
	
	public abstract int getProgress();
	
	public abstract int getMaxProgress();
	
	public abstract File processFiles(boolean showProgress) throws Exception;

}
