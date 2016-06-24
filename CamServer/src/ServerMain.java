import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.yjm.shared.Data;

class IOStream {
	InputStream inputStream;
	OutputStream outputStream;
	Socket socket;

	public IOStream(InputStream inputStream, OutputStream outputStream, Socket socket) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.socket = socket;
	}
}

class TransferThread extends Thread {
	InputStream inputStream;
	OutputStream outputStream;
	Socket socket;
	private String message;

	public void setMessage(String message) {
		this.message = message;
	}

	public TransferThread(InputStream inputStream, OutputStream outputStream, Socket socket) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.socket = socket;
	}

	public TransferThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		System.out.println(Utility.getNow() + message + "Thread Run" + this);
		try {
			byte[] bytes = new byte[4096];
			int len;
			while ((len = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, len);
				outputStream.flush();
			}
		} catch (IOException e) {
			System.out.print(Utility.getNow());
			e.printStackTrace();
		} finally {
			// try {
			// inputStream.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			try {
				if (outputStream != null) {
					outputStream.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(Utility.getNow() + message + "Thread Stop " + this);
	}

	void stopTransfer() {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class HeartBeatThread extends Thread {
	Socket socket;
	OutputStream outputStream;

	public HeartBeatThread(Socket socket) {
		this.socket = socket;

	}

	@Override
	public void run() {
		System.out.println(Utility.getNow() + "Heart Beat Thread Run" + this);
		try {
			outputStream = socket.getOutputStream();
			while (true) {
				Data data = new Data(Data.TAG_HELLO);
				outputStream.write(data.toBytes());
				outputStream.flush();
				Thread.sleep(2000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(Utility.getNow() + "Heart Beat Thread Stop" + this);
	}
}

public class ServerMain {
	Map<String, IOStream> streams = new ConcurrentHashMap<>();
	Map<String, HeartBeatThread> heartBeatThreads = new ConcurrentHashMap<>();
	Map<String, TransferThread> transferThreads = new ConcurrentHashMap<>();

	public ServerMain() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(3295);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(Utility.getNow() + "Server Start");
		while (true) {
			try {
				final Socket socket = serverSocket.accept();
				socket.setKeepAlive(true);
				System.out.println(Utility.getNow() + "Accept");
				final InputStream inputStream = socket.getInputStream();
				final OutputStream outputStream = socket.getOutputStream();
				new Thread(new Runnable() {
					@Override
					public void run() {
						Data data = Data.fromInputStream(inputStream);
						try {
							if (data != null) {
								// 被监控端链接到服务器
								if (data.getTag() == Data.TAG_CAM_CONN_TO_SERVER) {
									String id = new String(data.getData(), "utf-8");
									HeartBeatThread heartBeatThread;
									IOStream camIOStream;

									camIOStream = streams.get(id);
									if (camIOStream != null) {
										camIOStream.socket.close();
										streams.remove(id);
									}
									camIOStream = new IOStream(inputStream, outputStream, socket);
									streams.put(id, camIOStream);

									heartBeatThread = heartBeatThreads.get(id);
									if (heartBeatThread != null) {
										heartBeatThread.socket.close();
										heartBeatThreads.remove(id);
									}
									heartBeatThread = new HeartBeatThread(socket);
									heartBeatThread.start();
									heartBeatThreads.put(id, heartBeatThread);
									System.out.println(Utility.getNow() + "Cam Conn To Server Id:" + id);
								} else if (data.getTag() == Data.TAG_MON_CONN_TO_SERVER) {
									String id = new String(data.getData(), "utf-8");
									IOStream camIOStream = streams.get(id);
									if (camIOStream != null) {
										TransferThread camToMonThread = transferThreads.get(id);
										TransferThread monToCamThread = transferThreads.get(id);
										if (camToMonThread != null) {
											camToMonThread.stopTransfer();
											transferThreads.remove(id);
										}
										if (monToCamThread != null) {
											monToCamThread.stopTransfer();
											transferThreads.remove(id);
										}

										monToCamThread = new TransferThread(inputStream, camIOStream.outputStream, socket);
										monToCamThread.setMessage("MonToCam");
										monToCamThread.start();
										transferThreads.put(id, monToCamThread);

										camToMonThread = new TransferThread(camIOStream.inputStream, outputStream, camIOStream.socket);
										camToMonThread.setMessage("CamToMon");
										camToMonThread.start();
										transferThreads.put(id, camToMonThread);
										System.out.println(Utility.getNow() + "Mon Conn To Server id:" + id);
									}
								} else {
									System.out.println(Utility.getNow() + "Other TAG");
								}
							} else {
								System.out.println(Utility.getNow() + "Data is null");
								// data为null，说明读失败，关闭socket
								// inputStream.close();
								if (outputStream != null) {
									outputStream.close();
								}
								if (socket != null) {
									socket.close();
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							try {
								// inputStream.close();
								if (outputStream != null) {
									outputStream.close();
								}
								if (socket != null) {
									socket.close();
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							}

						}
					}
				}).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new ServerMain();
	}
}
