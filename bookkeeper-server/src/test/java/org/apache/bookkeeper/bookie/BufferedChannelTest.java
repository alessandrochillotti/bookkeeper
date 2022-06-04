package org.apache.bookkeeper.bookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.sun.tools.javac.util.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
		private boolean mock;
		
		private BufferedChannel bC;
		
		@Parameters
		public static Collection<Object[]> data() {
			ByteBuf buf = Unpooled.buffer();
			
	        return Arrays.asList(new Object[][] {
	            { false, 0, 0, null, 0, 0, "0" },
	            { false, 0, 0, null, -1, 0, "0" },
	            { false, 0, 0, null, 1, 0, "0" },
	            { false, 0, 0, null, 0, 1, null },
	            { false, 0, 0, null, 0, -1, "0" },
	            { false, 0, 0, Unpooled.EMPTY_BUFFER, 0, 0, "0"},
	            { false, 0, 10, buf, 0, 10, "10"},
	            { false, 10, 10, buf, 0, 10, "10"},
	            { true, 10, 10, buf, 0, 10, "Reading from filechannel returned a non-positive value. Short read."},
	            { true, 0, 5, buf, 0, 5, "0"},
	            { false, 0, 10, buf, buf.maxCapacity()-1, 1, "0"},
	            { false, 0, 10, buf, buf.maxCapacity()+1, 10, "position"},
	            { false, 0, 10, buf, 0, buf.maxCapacity()-1, "Read past EOF"},
	            { false, 0, 10, buf, 0, buf.maxCapacity()+1, "0"},
	        });
	    }
		
		public ReadTest(boolean mock, int unpersistedBytesBound, int numberOfZero, ByteBuf dest, long pos, int length, String expectedResult) {
			configure(mock, unpersistedBytesBound, numberOfZero, dest, pos, length, expectedResult);
		}
		
		public void configure(boolean mock, int unpersistedBytesBound, int numberOfZero, ByteBuf dest, long pos, int length, String expectedResult) {
			try {
				File newLogFile = File.createTempFile("test", "log");
		        newLogFile.deleteOnExit();
		        RandomAccessFile randomAccessFile = new RandomAccessFile(newLogFile, "rw");
		        FileChannel fC;
		        
		    	ByteBuf toWrite = Unpooled.buffer();
		 		toWrite.markReaderIndex();
		 		toWrite.markWriterIndex();
		        
		 		toWrite.writeZero(numberOfZero);
	 			
		 		ByteBufAllocator allocator;
		 		
		 		if (mock && numberOfZero == 10) {
		 			fC = spy(randomAccessFile.getChannel());
		 			doReturn(0).when(fC).read(any(java.nio.ByteBuffer.class), anyLong());
		 			
		 			allocator = UnpooledByteBufAllocator.DEFAULT;
		 		} else if (mock && numberOfZero == 5) {
		 			fC = randomAccessFile.getChannel();

		 			allocator = mock(ByteBufAllocator.class);
		 			when(allocator.directBuffer(anyInt())).thenReturn(null);
	 			} else {
		 			fC = randomAccessFile.getChannel();
		 			allocator = UnpooledByteBufAllocator.DEFAULT;
		 		}
		 		
				this.bC = new BufferedChannel(
						allocator, 
						fC,
				        INTERNAL_BUFFER_WRITE_CAPACITY, 
				        INTERNAL_BUFFER_READ_CAPACITY, 
				        unpersistedBytesBound);
				
				if (!mock || numberOfZero != 5)
					this.bC.write(toWrite);
				
				if (mock) {
					ByteBuf writeBuffer = Whitebox.getInternalState(bC, "writeBuffer");
					writeBuffer = null;
				}
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
				Assert.assertEquals(this.expectedResult, e.getMessage());
			} catch (NullPointerException e) {
				Assert.assertEquals(this.expectedResult, e.getMessage());
			} catch (Exception e) {
				Assert.assertTrue(e.getMessage().contains(expectedResult));
			}
		}
		
	}
	
//	@RunWith(Parameterized.class)
//	public static class WriteTest {
//		
//		private ByteBuf src;
//		private int unpersistedBytesBound;
//		private String expectedResult;
//
//		private BufferedChannel bC;
//		
//		@Parameters
//		public static Collection<Object[]> data() {
//	        return Arrays.asList(new Object[][] {
//	            { -1, 65536, 0, null},
//	            { 0, 65536, 0, ""},
//	            { 4, 65536, 0, "4" },
//	            { 4, 65536, 1, "4"},
//	            { 10, 65536, Integer.MAX_VALUE, "10"},
//	            { 15, 2, 0, "15"}
//	        });
//	    }
//				
//		public WriteTest(int contentSrc, int capacity, int unpersistedBytesBound, String expectedResult) {
//			configure(contentSrc, capacity, unpersistedBytesBound, expectedResult);
//		}
//		
//		public void configure(int contentSrc, int capacity, int unpersistedBytesBound, String expectedResult) {
//			if (contentSrc == 0) {
//				this.src = Unpooled.EMPTY_BUFFER;
//			} else if (contentSrc > 0){
//				this.src = Unpooled.buffer();
//				this.src.writeInt(contentSrc);
//			} else {
//				this.src = null;
//			}
//			this.expectedResult = expectedResult;
//			this.unpersistedBytesBound = unpersistedBytesBound;
//			
//			File newLogFile;
//			try {
//				newLogFile = File.createTempFile("test", "log");
//		        newLogFile.deleteOnExit();
//		        RandomAccessFile randomAccessFile = new RandomAccessFile(newLogFile, "rw");
//				this.bC = new BufferedChannel(
//					UnpooledByteBufAllocator.DEFAULT, 
//					randomAccessFile.getChannel(),
//					capacity, 
//			        INTERNAL_BUFFER_READ_CAPACITY,
//			        this.unpersistedBytesBound);
//				
//				bC.readBuffer.markReaderIndex();
//				bC.writeBuffer.markWriterIndex();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		@Test
//		public void writeTest() {
//			try {
//				ByteBuf dest = Unpooled.buffer(10);
//				
//				bC.write(this.src);
//				bC.read(dest, 0, 4);
//				
//				Assert.assertEquals(expectedResult, Integer.toString(dest.getInt(0)));
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (NullPointerException e) {
//				Assert.assertEquals(this.expectedResult, e.getMessage());
//			}
//		}
//	}
//	
//	@RunWith(PowerMockRunner.class)
//	public static class ReadWriteTest {
//		
//		private BufferedChannel bC;
//		
//		public ReadWriteTest() {
//			
//		}
//		
//		@Test
//		public void readAssert() {
//			ByteBuf dest = Unpooled.buffer();
//			
//			ByteBuf writeBuffer = Whitebox.getInternalState(bC, "writeBuffer");
//			
//			writeBuffer.writeInt(10);
//			
//			try {
//				bC.read(dest, 0);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			Assert.assertEquals(10, dest.getInt(0));
//		}
//		
//		@Before
//		public void setup() {
//			File newLogFile;
//			try {
//				newLogFile = File.createTempFile("test", "log");
//		        newLogFile.deleteOnExit();
//		        RandomAccessFile randomAccessFile = new RandomAccessFile(newLogFile, "rw");
//		        bC = new BufferedChannel(
//					UnpooledByteBufAllocator.DEFAULT, 
//					randomAccessFile.getChannel(),
//			        INTERNAL_BUFFER_WRITE_CAPACITY, 
//			        INTERNAL_BUFFER_READ_CAPACITY,
//			        0);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		@Test
//		public void writeInvocation() {
//			ByteBuf src = Unpooled.buffer();
//			src.markWriterIndex();
//			src.markReaderIndex();
//			src.writeInt(1998);
//			
//			try {
//				bC.write(src);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			ByteBuf writeBuffer = Whitebox.getInternalState(bC, "writeBuffer");
//			
//			Assert.assertEquals(1998, writeBuffer.readInt());
//		}
//	}
}
