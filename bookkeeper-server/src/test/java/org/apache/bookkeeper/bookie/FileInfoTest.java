package org.apache.bookkeeper.bookie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class FileInfoTest {
	
	@RunWith(Parameterized.class)
	public static class MoveToNewLocationTest {
		
		private File newFile;
		
		private long size;		
		private String expectedResult;
	
		private FileInfo fi;
		
		@Parameters
		public static Collection<Object[]> data() throws IOException {
	        return Arrays.asList(new Object[][] {
	        	{ true, null, 0, false, null },
	        	{ true, File.createTempFile("origin","file"), 0, false, "true" },
	        	{ true, File.createTempFile("origin","file"), -1, false, "true" }, 
	        	{ false, File.createTempFile("origin","file"), 10, false, "false" },
	        	{ true, File.createTempFile("origin","file"), 10, false, "true" },
	        	{ true, new File("/tmp/original"), 10, false, "true" },
	        	{ true, File.createTempFile("origin","file"), 10, true, "true" },
	        	{ true, File.createTempFile("origin","file"), Integer.MAX_VALUE, false, "true" }
	        });
	    }
		
		public MoveToNewLocationTest(boolean create, File newFile, long size, boolean rlocfile, String expectedResult) {
				configure(create, newFile, size, rlocfile, expectedResult);
		}
		
		public void configure(boolean create, File newFile, long size, boolean rlocfile, String expectedResult) {
			byte[] masterKey = {'a', 'b', 'c'};
			File oldFile = new File("/tmp/original");
			try {
				fi = new FileInfo(oldFile, masterKey, 1);
				fi.checkOpen(create);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.newFile = newFile;
			this.size = size;
			this.expectedResult = expectedResult;
			
			if (rlocfile) {
				File rlocFile =  new File(newFile.getParentFile(), newFile.getName() + IndexPersistenceMgr.RLOC);
				rlocFile.deleteOnExit();
				
                FileWriter write;
				try {
					write = new FileWriter(rlocFile);
	                write.write("contentofrlocfile");
					write.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Test
		public void moveToNewLocationTest() {
			try {
				fi.moveToNewLocation(newFile, size);
				Assert.assertEquals(expectedResult, String.valueOf(fi.isSameFile(newFile)));
			} catch (IOException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			} catch (NullPointerException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			}
		}
	}	
	
	/**
	 * The tested method is ReadAbsolute, but since that ReadAbsolute is private, it is used
	 * read as entrypoint
	 * 
	 * @author alessandro
	 * 
	 */
	@RunWith(Parameterized.class)
	public static class ReadAbsoluteTest {
		
		private ByteBuffer bb; 
		private long start;
		private boolean bestEffort;
		private String expectedResult;
		
		private FileInfo fi;
		
		@Parameters
		public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {
	        	{ null, null, 0, false, "0" },
	        	{ null, ByteBuffer.allocate(0), 0, false, "0" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), -1, false, "4" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), 1, false, "Short read at" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), 1, true, "3" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), 0, false, "4" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), 0, true, "4" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), Long.MAX_VALUE, true, "Negative position" },
	        	{ ByteBuffer.wrap("ciao".getBytes()), ByteBuffer.allocate(4), 4, true, "0" }
	        });
	    }
		
		public ReadAbsoluteTest(ByteBuffer content, ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			configure(content, bb, start, bestEffort, expectedResult);
		}
		
		@After
		public void tearDownEnvironment() {
			try {
				fi.delete();
				fi.close(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void configure(ByteBuffer content, ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			byte[] masterKey = {'a', 'b', 'c', 'd'};
			
			try {
				fi = new FileInfo(new File("/tmp/file"), masterKey, 0);
				
				if (content != null) {
					ByteBuffer[] array = new ByteBuffer[1];
					array[0] = content;
					
					fi.write(array, 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.bb = bb;
			this.start = start;
			this.bestEffort = bestEffort;
			this.expectedResult = expectedResult;
		}
		
		@Test
		public void readAbsoluteTest() {
			try {
				Assert.assertEquals(expectedResult, Integer.toString(fi.read(bb, start, bestEffort)));
			} catch (IOException e) {
				Assert.assertTrue(e.getMessage().startsWith(expectedResult));
			} catch (NullPointerException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			} catch (IllegalArgumentException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			} 
		} 
	}
	
}
