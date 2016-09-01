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

package com.noktuska.bot.noktuskabot_revamped.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.noktuska.bot.noktuskabot_revamped.Console;
import com.noktuska.bot.noktuskabot_revamped.utils.DownloaderUtil;
import com.noktuska.bot.noktuskabot_revamped.utils.ReadableByteChannelWrapper;

public class OsuBeatmapDownloader {
	
	public static final int CONNECTION_TIMEOUT = 5000;
	
	public static final int READ_TIMEOUT = 10000;
	
	public static final int MAX_REDIRECTS = 3;
	
	private static final int UPDATE_INTERVAL = 1000;
	
	public enum Status {
		WAITING("Waiting"),
		DOWNLOADING("Downloading"),
		COMPLETE("Complete"),
		CANCELLED("Cancelled"),
		ERROR("Error");
		
		private final String name;
		
		Status(String name) {
			this.name = name;
		}
		
		public String getName() { return name; }
	}
	
	public interface DownloadListener {
		public void completed();
		public void error();
	}
	
	private String localPath;
	private String rename;
	private URL url;
	private DownloadListener listener;
	private ReadableByteChannelWrapper rbc;
	private FileOutputStream fos;
	private int contentLength = -1;
	private Status status = Status.WAITING;
	private long lastReadSoFarTime = -1;
	private long lastReadSoFar = -1;
	private String lastDownloadSpeed;
	private String lastTimeRemaining;
	
	private Console logger;
	
	public OsuBeatmapDownloader(String remoteURL, String localPath, Console logger) {
		this(remoteURL, localPath, null, logger);
	}
	
	public OsuBeatmapDownloader(String remoteURL, String localPath, String rename, Console logger) {
		try {
			this.url = new URL(remoteURL);
		} catch (MalformedURLException e) {
			this.status = Status.ERROR;
			logger.log(String.format("Bad download URL: '%s'", remoteURL));
			return;
		}
		this.localPath = localPath;
		this.logger = logger;
		this.rename = DownloaderUtil.cleanFileName(rename, '-');
	}
	
	public URL getRemoteURL() { return url; }
	public String getLocalPath() { return (rename != null) ? rename : localPath; }
	public void setListener(DownloadListener listener) { this.listener = listener; }
	
	public void start() {
		if (status != Status.WAITING)
			return;
		
		new Thread() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				try {
					URL downloadURL = url;
					int redirectCount = 0;
					boolean isRedirect = false;
					do {
						isRedirect = false;
						
						conn = (HttpURLConnection)downloadURL.openConnection();
						conn.setConnectTimeout(CONNECTION_TIMEOUT);
						conn.setReadTimeout(READ_TIMEOUT);
						conn.setUseCaches(false);
						
						conn.setInstanceFollowRedirects(false);
						conn.setRequestProperty("User-Agent", "Mozilla/5.0...");
						
						int status = conn.getResponseCode();
						if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM ||
								status == HttpURLConnection.HTTP_SEE_OTHER || status == HttpURLConnection.HTTP_USE_PROXY) {
							URL base = conn.getURL();
							String location = conn.getHeaderField("Location");
							URL target = null;
							if (location != null)
								target = new URL(base, location);
							conn.disconnect();
							
							String error = null;
							if (location == null)
								error = String.format("Download for URL '%s' is attempting to redirect without a 'location' header.", base.toString());
							else if (!target.getProtocol().equals("http") && !target.getProtocol().equals("https"))
								error = String.format("Download for URL '%s' is attempting to redirect to a non HTTP/HTTPS protocol '%s'.", base.toString(), target.getProtocol());
							else if (redirectCount > MAX_REDIRECTS)
								error = String.format("Download for URL '%s' is attempting too many redirects (over %d).", base.toString(), MAX_REDIRECTS);
							if (error != null) {
								logger.log(error);
								throw new IOException();
							}
							
							downloadURL = target;
							redirectCount++;
							isRedirect = true;
						}
					} while (isRedirect);
					
					contentLength = conn.getContentLength();
				} catch (IOException e) {
					status = Status.ERROR;
					if (listener != null)
						listener.error();
					return;
				}
				
				try (
						InputStream in = conn.getInputStream();
						ReadableByteChannel readableByteChannel = Channels.newChannel(in);
						FileOutputStream fileOutputStream = new FileOutputStream(localPath);
				) {
					rbc = new ReadableByteChannelWrapper(readableByteChannel);
					fos = fileOutputStream;
					status = Status.DOWNLOADING;
					updateReadSoFar();
					long bytesRead = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
					if (status == Status.DOWNLOADING) {
						if (bytesRead < contentLength) {
							status = Status.ERROR;
							logger.log(String.format("Download '%s' failed: %d bytes expected, %d bytes received.", url.toString(), contentLength, bytesRead));
							if (listener != null)
								listener.error();
							return;
						}
						
						rbc.close();
						fos.close();
						if (rename != null) {
							Path source = new File(localPath).toPath();
							Files.move(source,  source.resolveSibling(rename), StandardCopyOption.REPLACE_EXISTING);
						}
						status = Status.COMPLETE;
						if (listener != null)
							listener.completed();
					}
				} catch (Exception e) {
					status = Status.ERROR;
					logger.log("Failed to start download: " + e.getMessage());
					if (listener != null)
						listener.error();
				}
			}
		}.start();
	}
	
	public Status getStatus() { return status; }
	public boolean isTransferring() {
		return (rbc != null && rbc.isOpen() && fos != null && fos.getChannel().isOpen());
	}
	public boolean isActive() {
		return (status == Status.WAITING || status == Status.DOWNLOADING);
	}
	public int contentLength() { return contentLength; }
	public float getProgress() {
		switch (status) {
		case WAITING:
			return 0f;
		case COMPLETE:
			return 100f;
		case DOWNLOADING:
			if (rbc != null && fos != null && contentLength > 0)
				return (float)rbc.getReadSoFar() / (float)contentLength * 100f;
			else
				return 0f;
		case CANCELLED:
		case ERROR:
		default:
			return -1f;
		}
	}
	
	public long readSoFar() {
		switch (status) {
		case COMPLETE:
			return (rbc != null) ? rbc.getReadSoFar() : contentLength;
		case DOWNLOADING:
			if (rbc != null)
				return rbc.getReadSoFar();
		case WAITING:
		case CANCELLED:
		case ERROR:
		default:
			return 0;
		}
	}
	
	public String getDownloadSpeed() {
		updateReadSoFar();
		return lastDownloadSpeed;
	}
	
	public String getTimeRemaining() {
		updateReadSoFar();
		return lastTimeRemaining;
	}
	
	private void updateReadSoFar() {
		if (status != Status.DOWNLOADING) {
			this.lastDownloadSpeed = null;
			this.lastTimeRemaining = null;
			return;
		}
		
		if (System.currentTimeMillis() > lastReadSoFarTime + UPDATE_INTERVAL) {
			long readSoFar = readSoFar();
			long readSoFarTime = System.currentTimeMillis();
			long dlspeed = (readSoFar - lastReadSoFar) * 1000 / (readSoFarTime - lastReadSoFarTime);
			if (dlspeed > 0) {
				this.lastDownloadSpeed = String.format("%s/s", DownloaderUtil.bytesToString(dlspeed));
				long t = (contentLength - readSoFar) / dlspeed;
				if (t >= 3600) {
					this.lastTimeRemaining = String.format("%dh%dm%ds", t / 3600, (t / 60) % 60, t % 60);
				} else {
					this.lastTimeRemaining = String.format("%dm%ds", t / 60, t % 60);
				}
			} else {
				this.lastDownloadSpeed = String.format("%s/s", DownloaderUtil.bytesToString(0));
				this.lastTimeRemaining = "Infinite";
			}
			this.lastReadSoFarTime = readSoFarTime;
			this.lastReadSoFar = readSoFar;
		} else if (lastReadSoFarTime <= 0) {
			this.lastReadSoFar = readSoFar();
			this.lastReadSoFarTime = System.currentTimeMillis();
		}
	}
	
	public void cancel() {
		try {
			this.status = Status.CANCELLED;
			boolean transferring = isTransferring();
			if (rbc != null && rbc.isOpen())
				rbc.close();
			if (fos != null && fos.getChannel().isOpen())
				fos.close();
			if (transferring) {
				File f = new File(localPath);
				if (f.isFile())
					f.delete();
			}
		} catch (IOException e) {
			this.status = Status.ERROR;
			logger.log("Failed to cancel download: " + e.getMessage());
		}
	}
	
}
