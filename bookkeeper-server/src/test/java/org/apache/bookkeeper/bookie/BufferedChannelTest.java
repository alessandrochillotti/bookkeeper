package org.apache.bookkeeper.bookie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

@RunWith(value=Parameterized.class)
public class BufferedChannelTest {
	
	private static final int INTERNAL_BUFFER_WRITE_CAPACITY = 65536;
    private static final int INTERNAL_BUFFER_READ_CAPACITY = 512;
	
    enum Type {WRITE, READ};
    
	// parameters of test
    private Type type;
    private ByteBuf buf;	// this ByteBuf is src or dest
	private long pos;
	private int length;
	private String expectedResult;
	
	private BufferedChannel bC;
	
	@Parameters
	public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
        		// test case for method 'read'
                { Type.READ, 0, null, 0, 0, "0" },
                { Type.READ, 0, null, -1, 0, "0" },
                { Type.READ, 0, null, 1, 0, "0" },
                { Type.READ, 0, null, 0, 1, null },
                { Type.READ, 0, null, 0, -1, "0" },
                { Type.READ, 10, Unpooled.buffer(), 0, 10, "10"},
                // test case for method 'write'
                { Type.WRITE, 0, null , 0, 0, null},
                { Type.WRITE, 10, Unpooled.buffer(), 0, 0, "10"}
        });
    }
	
	public BufferedChannelTest(Type type, int numberOfZero, ByteBuf buf, long pos, int length, String expectedResult) {
		this.type = type;
		
		if (this.type == Type.READ)
			configure(numberOfZero, buf, pos, length, expectedResult);
		else 
			configure(numberOfZero, buf, expectedResult);
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
		
		this.buf = dest;
		this.pos = pos;
		this.length = length;
		this.expectedResult = expectedResult;
	}
	
	public void configure(int numberOfZero, ByteBuf src, String expectedResult) {
		if (src != null)
			src.writeZero(numberOfZero);
		
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
		        0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.buf = src;
		this.expectedResult = expectedResult;
	}
	
	@Test
	public void readTest() {
		Assume.assumeTrue(type == Type.READ);
		
		try {
			Assert.assertEquals(this.expectedResult, Integer.toString(bC.read(this.buf, this.pos, this.length)));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			Assert.assertEquals(this.expectedResult, e.getMessage());
		}
	}
	
	@Test
	public void writeTest() {
		Assume.assumeTrue(type == Type.WRITE);
		int initialByte;
		
		try {
			initialByte = bC.getNumOfBytesInWriteBuffer();
			bC.write(this.buf);
			Assert.assertEquals(this.expectedResult, Integer.toString(bC.getNumOfBytesInWriteBuffer() - initialByte));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			Assert.assertEquals(this.expectedResult, e.getMessage());
		}
	}
}
