/*******************************************************************************
 * Copyright 2018 RaspInLoop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.raspinloop.hwemulation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


public interface SpiDevice {

    public static final SpiMode DEFAULT_SPI_MODE = SpiMode.MODE_0;
    public static final int DEFAULT_SPI_SPEED = 1000000; // 1MHz (range is 500kHz - 32MHz)
    public static final int MAX_SUPPORTED_BYTES = 2048;
    
	String write(String data, String charset) throws IOException;
	String write(String data, Charset charset) throws IOException;
	ByteBuffer write(ByteBuffer data) throws IOException;
	byte[] write(InputStream input) throws IOException;
	int write(InputStream input, OutputStream output) throws IOException;
	byte[] write(byte[] data) throws IOException;
	short[] write(short[] data) throws IOException;
	byte[] write(byte[] data, int start, int length) throws IOException;
	short[] write(short[] data, int start, int length) throws IOException;

}
