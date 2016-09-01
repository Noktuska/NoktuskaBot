/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 * 
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with opsu!. If not, see <http://www.gnu.org/licenses/>.
 */

package com.noktuska.bot.noktuskabot_revamped.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ReadableByteChannelWrapper implements ReadableByteChannel {
	
	private final ReadableByteChannel rbc;
	
	private long bytesRead;
	
	public ReadableByteChannelWrapper(ReadableByteChannel rbc) {
		this.rbc = rbc;
	}
	
	@Override
	public void close() throws IOException { rbc.close(); }
	
	@Override
	public boolean isOpen() { return rbc.isOpen(); }
	
	@Override
	public int read(ByteBuffer bb) throws IOException {
		int bytes;
		if ((bytes = rbc.read(bb)) > 0)
			bytesRead += bytes;
		return bytes;
	}
	
	public long getReadSoFar() { return bytesRead; }
	
}
