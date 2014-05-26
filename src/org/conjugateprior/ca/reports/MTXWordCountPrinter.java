package org.conjugateprior.ca.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import org.conjugateprior.ca.IYoshikoderDocument;

public class MTXWordCountPrinter extends WordCountPrinter {				

	int mtxDocumentLineCounter = 0; // this starts at 1
	int mtxMaxColIndex = -1;
	int tripleCount = 0; // how many entries in all

	public MTXWordCountPrinter(WordCounter rep, File folder, Charset c, Locale l, File[] f) {
		super(rep, folder, "data.mtx-tmp", c, l, f);
	}

	@Override
	public String makeLineFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = reporter.getWordCountMapFromDocument(doc);
		mtxDocumentLineCounter++;
		tripleCount += map.keySet().size();
		StringBuilder sb = new StringBuilder();			 
		for (String wd : map.keySet()) {
			Integer id = wordToId.get(wd);
			if (id == null){
				id = idIndex;
				wordToId.put(wd, idIndex);
				idIndex++;
			}
			if (id > mtxMaxColIndex)
				mtxMaxColIndex = id; // update highest word index so far this document

			sb.append(mtxDocumentLineCounter + " ");
			sb.append((id + 1) + " "); // 1 based indexing
			sb.append(map.get(wd) + newline);
		}
		return sb.toString();
	}

	@Override
	protected void postProcess() throws Exception {
		OutputStreamWriter real = new OutputStreamWriter(
				new FileOutputStream(new File(folder, "data.mtx")));
		String header = getHeader();
		real.write(header);
		real.close();

		File tmpfile = new File(folder, datafilename);
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
			inputChannel = new FileInputStream(tmpfile).getChannel();
			outputChannel = new FileOutputStream(new File(folder, "data.mtx"), true).getChannel();
			outputChannel.transferFrom(inputChannel, header.getBytes().length, inputChannel.size());
		} finally {
			inputChannel.close();
			outputChannel.close();
		}
		boolean b = tmpfile.delete();
		if (!b)
			throw new Exception("Could not delete temporary file " + tmpfile.getAbsolutePath());
		datafilename = "data.mtx";
	}

	@Override
	protected void writeReadmeFile() throws Exception {
		extractREADMEFileAndSaveToFolder("README-mtx");
	}
	
	protected String getHeader(){
		StringBuilder sb = new StringBuilder();
		sb.append("%%MatrixMarket matrix coordinate integer general" + newline);
		sb.append(mtxDocumentLineCounter + " ");
		sb.append((mtxMaxColIndex+1) + " ");
		sb.append(tripleCount + newline);
		return sb.toString();
	}

}
