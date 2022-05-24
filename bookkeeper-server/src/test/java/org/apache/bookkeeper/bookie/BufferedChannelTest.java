package org.apache.bookkeeper.bookie;

import static org.mockito.Mockito.mockitoSession;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

@RunWith(Enclosed.class)
public class BufferedChannelTest {
	
	private static final int INTERNAL_BUFFER_WRITE_CAPACITY = 65536;
    private static final int INTERNAL_BUFFER_READ_CAPACITY = 512;
	
	@RunWith(Parameterized.class)
	public static class ReadTest {
		
		private ByteBuf dest;
		private long pos;
		private int length;
		private String expectedResult;
		
		private BufferedChannel bC;
		
		@Parameters
		public static Collection<Object[]> data() {
			ByteBuf buf = Unpooled.buffer();
			
	        return Arrays.asList(new Object[][] {
	            { 0, null, 0, 0, "0" },
	            { 0, null, -1, 0, "0" },
	            { 0, null, 1, 0, "0" },
	            { 0, null, 0, 1, null },
	            { 0, null, 0, -1, "0" },
	            { 0, buf, 0, 0, "0"},
	            { 10, buf, 0, 10, "10"},
	            { 10, buf, buf.maxCapacity()-1, 1, "0"},
	            { 10, buf, buf.maxCapacity()+1, 10, "position"},
	            { 10, buf, 0, buf.maxCapacity()-1, "10"},
	            { 10, buf, 0, buf.maxCapacity()+1, "0"}
	        });
	    }
		
		public ReadTest(int numberOfZero, ByteBuf dest, long pos, int length, String expectedResult) {
			configure(numberOfZero, dest, pos, length, expectedResult);
		}
		
		public void configure(int numberOfZero, ByteBuf dest, long pos, int length, String expectedResult) {
			try {
				File newLogFile = File.createTempFile("test", "log");
		        newLogFile.deleteOnExit();
		        RandomAccessFile randomAccessFile = new RandomAccessFile(newLogFile, "rw");
		        
		    	ByteBuf toWrite = Unpooled.buffer();
		 		toWrite.markReaderIndex();
		 		toWrite.markWriterIndex();
		        
		 		toWrite.writeZero(numberOfZero);
		 		
				this.bC = new BufferedChannel(
						UnpooledByteBufAllocator.DEFAULT, 
						randomAccessFile.getChannel(),
				        INTERNAL_BUFFER_WRITE_CAPACITY, 
				        INTERNAL_BUFFER_READ_CAPACITY, 
				        0);
				this.bC.write(toWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.dest = dest;
			this.pos = pos;
			this.length = length;
			this.expectedResult = expectedResult;
		}
		
		@Test
		public void readTest() {
			try {
				Assert.assertEquals(this.expectedResult, Integer.toString(bC.read(this.dest, this.pos, this.length)));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				Assert.assertEquals(this.expectedResult, e.getMessage());
			} catch (Exception e) {
				Assert.assertTrue(e.getMessage().contains(expectedResult));
			}
		}
		
	}
	
	@RunWith(Parameterized.class)
	public static class WriteTest {
		
		private ByteBuf src;
		private int unpersistedBytesBound;
		private String expectedResult;

		private BufferedChannel bC;
		
		@Parameters
		public static Collection<Object[]> data() {
	        return Arrays.asList(new Object[][] {
	            { 0, null, 0, null},
	            { 0, Unpooled.buffer(), 0, "0"},
	            { 10, Unpooled.buffer(), 0, "10"},
	            { 10, Unpooled.buffer(), 1, "0"},
	        });
	    }
		
		public WriteTest(int numberOfZero, ByteBuf src, int unpersistedBytesBound, String expectedResult) {
			configure(numberOfZero, src, unpersistedBytesBound, expectedResult);
		}
		
		public void configure(int numberOfZero, ByteBuf src, int unpersistedBytesBound, String expectedResult) {
			if (src != null)
				src.writeZero(numberOfZero);
			
			this.src = src;
			this.expectedResult = expectedResult;
			this.unpersistedBytesBound = unpersistedBytesBound;
			
			File newLogFile;
			try {
				newLogFile = File.createTempFile("test", "log");
		        newLogFile.deleteOnExit();
		        RandomAccessFile randomAccessFile = new RandomAccessFile(newLogFile, "rw");
				this.bC = new BufferedChannel(
					UnpooledByteBufAllocator.DEFAULT, 
					randomAccessFile.getChannel(),
			        INTERNAL_BUFFER_WRITE_CAPACITY, 
			        INTERNAL_BUFFER_READ_CAPACITY, 
			        this.unpersistedBytesBound);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Test
		public void writeTest() {
			int initialByte;
			
			try {
				initialByte = bC.getNumOfBytesInWriteBuffer();
				bC.write(this.src);
				Assert.assertEquals(this.expectedResult, Integer.toString(bC.getNumOfBytesInWriteBuffer() - initialByte));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				Assert.assertEquals(this.expectedResult, e.getMessage());
			}
		}
	}
	
}
