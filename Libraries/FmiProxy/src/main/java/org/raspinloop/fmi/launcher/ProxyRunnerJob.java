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
package org.raspinloop.fmi.launcher;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.raspinloop.fmi.launcherRunnerIpc.LauncherService;
import org.raspinloop.fmi.launcherRunnerIpc.LauncherService.Iface;
import org.raspinloop.fmi.launcherRunnerIpc.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Start the server communicating with the runner (VM that run the application under debug)
 * And start the VM
 * @author Motte
 *
 */
public abstract class ProxyRunnerJob implements  ServerContext {

	Logger logger = LoggerFactory.getLogger(ProxyRunnerJob.class);
	protected  Proxy proxy;

	private int runnerPort = 0;

	private final String name;
	
	private LinkedList<IReportListener> reportListeners = new LinkedList<>();
	private Runner runner;
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	public ProxyRunnerJob(Proxy proxy, Runner runner) {
		this.runner = runner;
		this.name = proxy.getName() + " running....";
		this.proxy = proxy;
	}

	public void addReportListener(IReportListener listener){
		reportListeners.add(listener);
	}
	
	public void removeReportListener(IReportListener listener) {
		reportListeners.remove(listener);
	}

	public int getRunnerPort() {
		return runnerPort;
	}

	public void setRunnerPort(int runnerPort) {
		this.runnerPort = runnerPort;
	}

	public String getName() {
		return name;
	}

	class LauncherserviceHanlder implements Iface {

		@Override
		public org.raspinloop.fmi.launcherRunnerIpc.Status Report(ReportType type, String message) throws TException {
			logger.trace("IPC: Report called by runner");
			reportListeners.stream().forEach( l -> l.report(type, message));
			return org.raspinloop.fmi.launcherRunnerIpc.Status.OK;
		}

		@Override
		public org.raspinloop.fmi.launcherRunnerIpc.Status ReadyToStart(int runnerClientPort) throws TException {
			logger.trace("IPC: ReadyToStart called by runner");
			ProxyRunnerJob.this.setRunnerPort(runnerClientPort);
			return org.raspinloop.fmi.launcherRunnerIpc.Status.OK;
		}
	}

	
	public class LauncherServerJob implements Runnable {

		private TServer server;
		private final String name;

		

		public LauncherServerJob(int port, IProxyMonitor monitor) throws TTransportException {
			this.name = "Launcher Server";

			TServerTransport serverTransport = new TServerSocket(port); 
			// timeout in seconds?
			Iface launcherServiceHandler = new ProxyRunnerJob.LauncherserviceHanlder();
			LauncherService.Processor<LauncherService.Iface> processor = new LauncherService.Processor<LauncherService.Iface>(launcherServiceHandler);
			this.server = new TSimpleServer(new TSimpleServer.Args(serverTransport).processor(processor));

			server.setServerEventHandler(new LauncherServerEventHandler(server, port, monitor));
		}

		protected void canceling() {
			server.stop();
		}

		@Override
		public void run() {
			server.serve();
		}

		public String getName() {
			return name;
		}


	}

	public class LauncherServerEventHandler implements TServerEventHandler {

		private IProxyMonitor monitor;
		private int port;
		private TServer server;
		
		
		public LauncherServerEventHandler(TServer server, int port, IProxyMonitor monitor) {
			this.server = server;
			this.port = port;
			this.monitor = monitor;
			
		}

		@Override
		public ServerContext createContext(TProtocol arg0, TProtocol arg1) {
			logger.trace("createContext for runner connection");
			return null;
		}

		@Override
		public void deleteContext(ServerContext arg0, TProtocol arg1, TProtocol arg2) {
			logger.trace("deleteContext for runner connection");
			monitor.processStatusChanged(ProcessStatus.STOPPED);
		}

		@Override
		public void preServe() {
				logger.trace("runner connector server listening on port " + port);
					 CompletableFuture.runAsync(runner,executor)
											.handle((r,e) ->  { 
														if (e != null) {
															 server.stop();
					 										monitor.aborted(e);
														}
				                 					    return null;});
		}

		@Override
		public void processContext(ServerContext arg0, TTransport arg1, TTransport arg2) {
			logger.trace("processContext for runner connection");
		}
	}

	public void cancel() {		
		// it will force close of running VM (if not run asynch...).
		if (!executor.isShutdown())
			executor.shutdownNow();
		runner.terminate();
	}

}
