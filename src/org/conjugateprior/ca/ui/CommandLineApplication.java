package org.conjugateprior.ca.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

abstract public class CommandLineApplication {

	protected Options options;
	protected HelpFormatter helpFormatter;

	public void printUsageAndOptions(){
		helpFormatter.printHelp(getUsageString(), options);
	}
	
	protected boolean onWindows(){
		return System.getProperty("os.name").toLowerCase().startsWith("win");
	}
	
	public CommandLineApplication() {
		helpFormatter = new HelpFormatter();
		options = new Options();
	}
	
	public void addCommandLineOption(Option opt){
		options.addOption(opt);
	}
	
	abstract protected String getUsageString();
	
	/**
	 * Get files from a folder and all files not beginning with period one folder down
	 * from it
	 * @param array of filenames
	 * @return array of files
	 * @throws Exception one of the filenames does not exist
	 */
	protected File[] getRecursiveDepthOneFileArray(String[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		File fail = null;
		for (int ii = 0;  ii < files.length; ii++) {
			File f = new File(files[ii]);
			if (!f.exists()){
				fail = f;
				break;
			} if (f.isDirectory()){
				File[] contents = f.listFiles();
				for (int jj = 0; jj < contents.length; jj++) {
					if (!contents[jj].isDirectory() && !contents[jj].getName().startsWith("."))
						if (contents[jj].length() > 0)
							filelist.add(contents[jj]); // an imperfect filter but...
				}
			} else {
				if (f.length() > 0)
					filelist.add(f);
			}
		}
		if (fail != null)
			throw new Exception("File " + fail.getAbsolutePath() + " does not exist.");
		
		return filelist.toArray(new File[filelist.size()]);
	}

	// does this work for dumb input?
	protected Locale translateLocale(String s) throws Exception {
		String[] bits = s.split("_");
		int bl = bits.length;
		Locale l = null;
		if (bl == 1)
			l = new Locale(bits[0]); // language
		else if (bl == 2)
			l = new Locale(bits[0], bits[1]); // language country
		else if (bl == 3)
			l = new Locale(bits[0], bits[1], bits[2]); // lang country var
		else {
			throw new Exception("Could not parse locale");
		}
		return l;
	}

	public void process(String[] args) throws Exception {
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		line = parser.parse(options, args);

		processLine(line);
	}
	
	abstract protected void processLine(CommandLine line) throws Exception;
	
}
