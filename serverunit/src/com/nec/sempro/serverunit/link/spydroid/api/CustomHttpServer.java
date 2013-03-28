/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.nec.sempro.serverunit.link.spydroid.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.nec.sempro.serverunit.link.http.TinyHttpServer;
import com.nec.sempro.serverunit.link.spydroid.SpydroidApplication;
import com.nec.sempro.serverunit.link.streaming.misc.HttpServer;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

/** 
 * HTTP server of Spydroid.
 * Its document root is assets/www, it contains a little user-friendly website to control spydroid from a browser.
 * The default behavior of HttpServer is enhanced with a RequestHandler.
 **/
public class CustomHttpServer extends HttpServer {

	public CustomHttpServer() {
		// The common name that appears in the CA of the HTTPS server of Spydroid
		mCACommonName = "Spydroid CA";
		// If at some point a stream cannot start the exception is stored so that
		// it can be fetched in the HTTP interface to display an appropriate message
		addCallbackListener(mListener);
	}

	private CallbackListener mListener = new CallbackListener() {
		@Override
		public void onError(TinyHttpServer server, Exception e, int error) {
			if (error==HttpServer.ERROR_START_FAILED) {
				SpydroidApplication.getInstance().lastCaughtException = e;
			}
		}
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		addRequestHandler("/request.json*", new CustomRequestHandler());
	}

	class CustomRequestHandler implements HttpRequestHandler {

		public CustomRequestHandler() {}

		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2) throws HttpException, IOException {

			if (request.getRequestLine().getMethod().equals("POST")) {

				// Retrieve the POST content
				HttpEntityEnclosingRequest post = (HttpEntityEnclosingRequest) request;
				byte[] entityContent = EntityUtils.toByteArray(post.getEntity());
				String content = new String(entityContent, Charset.forName("UTF-8"));

				// Execute the request
				final String json = RequestHandler.handle(content);

				// Return the response
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						writer.write(json);
						writer.flush();
					}
				});
				response.setStatusCode(HttpStatus.SC_OK);
				body.setContentType("application/json; charset=UTF-8");
				response.setEntity(body);
			}

		}
	}

}
