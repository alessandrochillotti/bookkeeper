package org.apache.bookkeeper.bookie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

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
			File old = new File("/tmp/original");
			try {
				fi = new FileInfo(old, masterKey, 1);
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
				e.printStackTrace();
				// Assert.assertEquals(expectedResult, e.getMessage());
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
	        	{ false, null, 0, false, "0" },
	        	{ true, null, 0, false, null },
	        	{ true, ByteBuffer.wrap(new byte[0]), 0, false, "0" },
	        	{ true, ByteBuffer.wrap("ciao".getBytes()), -1, false, "0" },
	        	{ true, ByteBuffer.wrap("ciao".getBytes()), 1, false, "3" },
	        	{ true, ByteBuffer.wrap("ciao".getBytes()), 0, false, "4" },
	        	{ true, ByteBuffer.wrap("ciao".getBytes()), 0, true, "0" },
	        	{ false, ByteBuffer.wrap("ciao".getBytes()), Long.MAX_VALUE, true, "Negative position" },
	        	{ true, ByteBuffer.wrap("ciao".getBytes()), Long.MAX_VALUE, true, "Negative position" }
	        });
	    }
		
		public ReadAbsoluteTest(boolean create, ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			configure(create, bb, start, bestEffort, expectedResult);
		}
		
		public void configure(boolean create, ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			byte[] masterKey = {'a', 'b', 'c'};
			try {
				fi = new FileInfo(new File("/tmp/original"), masterKey, 1);
				fi.checkOpen(create);
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
				e.printStackTrace();
			} catch (NullPointerException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			} catch (IllegalArgumentException e) {
				Assert.assertEquals(expectedResult, e.getMessage());
			}
		} 
	}
	
}
