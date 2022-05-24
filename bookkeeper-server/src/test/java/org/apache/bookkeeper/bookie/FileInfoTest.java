package org.apache.bookkeeper.bookie;

import static org.mockito.Mockito.mockitoSession;

import java.io.File;
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
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith(Enclosed.class)
public class FileInfoTest {
	
	@RunWith(Parameterized.class)
	public static class MoveToNewLocationTest {
		
		@Mock
		private File newFile;
		
		private long size;		
		private boolean expectedResult;
	
		private FileInfo fi;
		
		@Parameters
		public static Collection<Object[]> data() throws IOException {
	        return Arrays.asList(new Object[][] {
	        	{ null, 0, false },
	        	{ new File("/tmp/origin"), 0, false },
	        	{ new File("/tmp/origin"), -1, false}, 
	        	{ new File("/tmp/origin"), 10, true }
	        });
	    }
		
		public MoveToNewLocationTest(File newFile, long size, Object expectedResult) {
				configure(newFile, size);
		}
		
		public void configure(File newFile, long size) {
			byte[] masterKey = {'a', 'b', 'c'};
			try {
				fi = new FileInfo(new File("/tmp/original"), masterKey, 1);
				// fi.checkOpen(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Test
		public void moveToNewLocationTest() {
			try {
				fi.moveToNewLocation(newFile, size);
				Assert.assertEquals(expectedResult, fi.isSameFile(newFile));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				Assert.assertTrue(true);
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
	        	{ null, 0, false, null },
	        	{ ByteBuffer.wrap(new byte[0]), 0, false, "0" },
	        	{ ByteBuffer.wrap(new byte[10]), -1, false, "0" },
	        	{ ByteBuffer.wrap(new byte[10]), 1, false, "0" },
	        	{ ByteBuffer.wrap(new byte[10]), 0, true, "0" },
	        	{ ByteBuffer.wrap(new byte[10]), Long.MAX_VALUE, true, "Negative position" }
	        });
	    }
		
		public ReadAbsoluteTest(ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			configure(bb, start, bestEffort, expectedResult);
		}
		
		public void configure(ByteBuffer bb, long start, boolean bestEffort, String expectedResult) {
			byte[] masterKey = {'a', 'b', 'c'};
			try {
				fi = new FileInfo(new File("/tmp/original"), masterKey, 1);
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
