package org.conjugateprior.ca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileBasedYoshikoderDocumentTest {

	FileBasedYoshikoderDocument fbyd1;
	FileBasedYoshikoderDocument fbyd2;
	SimpleDocumentTokenizer sdt;
	Locale loc = Locale.ENGLISH;
	
	@Before
	public void setUp() throws Exception {
		String txt = "Bustopher Jones is not skin and bones. In\n" +
				"fact, he's remarkably fat.";
		File f = File.createTempFile("jca", ".txt");
		try (FileWriterWithEncoding writer = new FileWriterWithEncoding(f, "UTF-8")){
			writer.write(txt);
		}
		sdt = new SimpleDocumentTokenizer(loc);
		Date d = new GregorianCalendar(2001, 2, 3).getTime();
		fbyd1 = new FileBasedYoshikoderDocument("Bustopher1", 
				txt, d, sdt, f, Charset.forName("UTF-8"));
		
		txt = "He doesn't haunt pubs. He has eight or nine clubs.\n"
				+ "For he's the St. James' Street cat.  He's the cat\n"
				+ "we all meet as we walk down the street in his\n"
				+ "coat of fastidious black. No commonplace mousers\n"
				+ "have such well cut trousers, or such an impeccable back.";
		f = File.createTempFile("jca", ".txt");
		try (FileWriterWithEncoding writer = new FileWriterWithEncoding(f, "UTF-8")){
			writer.write(txt);
		}
		d = new GregorianCalendar(1933, 3, 5).getTime();
		fbyd2 = new FileBasedYoshikoderDocument("Bustopher2", 
				txt, d, sdt, f, Charset.forName("UTF-8"));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetWordCount() {
		assertEquals(fbyd1.getWordCount(), 12);
		assertEquals(fbyd2.getWordCount(), 48);
	}

	@Test
	public void testGetWordTypes() {
		Set<String> wds = fbyd1.getWordTypes();
		assertTrue(wds.contains("he's"));
		assertTrue(!wds.contains("mousers"));
		assertEquals(wds.size(), 12);
	}

	@Test
	public void testGetWordAtIndex() {
		assertTrue(fbyd1.getWordAt(0).equals("bustopher")); // lowercase
		assertTrue(fbyd1.getWordAt(11).equals("fat"));
		assertTrue(fbyd1.getWordAt(12)==null);
	}

	@Test
	public void testGetWordIndexesForWordType() {
		assertTrue(fbyd2.getWordIndexesForWordType("cat").contains(19));
		assertTrue(fbyd2.getWordIndexesForWordType("cat").contains(16));
		assertTrue(!fbyd2.getWordIndexesForWordType("cat").contains(20));
		assertEquals(fbyd2.getWordIndexesForWordType("cat").size(), 2);
	}

	@Test
	public void testGetCharacterOffsetsForWordType() {
		
	}

	@Test
	public void testGetConcordanceCharacterOffsetsForWordType() {
		//("Not yet implemented");
	}

	@Test
	public void testGetConcordanceWordIndexOffsetsForWordType() {
		//("Not yet implemented");
	}

	@Test
	public void testGetConcordanceWordIndexOffsetsForPattern() {
		//("Not yet implemented");
	}

	@Test
	public void testGetCharacterOffsetsForWordIndex() {
		//("Not yet implemented");
	}

	@Test
	public void testGetCharacterOffsetsForSentenceIndex() {
		//("Not yet implemented");
	}

	@Test
	public void testGetSentenceCount() {
		//("Not yet implemented");
	}

	@Test
	public void testGetWordCountMap() {
		//("Not yet implemented");
	}

	@Test
	public void testSetTitle() {
		//("Not yet implemented");
	}

	@Test
	public void testSetLocale() {
		//("Not yet implemented");
	}

	@Test
	public void testGetText() {
		//("Not yet implemented");
	}

	@Test
	public void testSetDate() {
		//("Not yet implemented");
	}

	@Test
	public void testGetDate() {
		//("Not yet implemented");
	}

	@Test
	public void testGetWordCounts() {
		//("Not yet implemented");
	}

	@Test
	public void testGetTitle() {
		//("Not yet implemented");
	}

	@Test
	public void testGetLocale() {
		//("Not yet implemented");
	}

	@Test
	public void testGetVocabulary() {
		//("Not yet implemented");
	}

	@Test
	public void testGetWordAt() {
		//("Not yet implemented");
	}

	@Test
	public void testGetWordIndexesForPattern() {
		//("Not yet implemented");
	}

	@Test
	public void testGetCharacterOffsetsForPattern() {
		//("Not yet implemented");
	}

	@Test
	public void testGetConcordanceCharacterOffsetsForPattern() {
		//("Not yet implemented");
	}

}
