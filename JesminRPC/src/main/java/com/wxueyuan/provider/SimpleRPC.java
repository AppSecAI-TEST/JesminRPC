package com.wxueyuan.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleRPC {
	/**
	 * @param service  要发布的服务全名,如 com.service.A
	 * @param Impl     服务的实现类全名,如 com.service.impl.AImpl
	 * @param port     服务发布的端口号
	 * @throws Exception 
	 */
	public static void exportService(String service, String Impl, int port) throws Exception{
		
		if(Class.forName(service) == null)
			throw new IllegalArgumentException(service+" 服务不存在");
		if(Class.forName(Impl) == null)
			throw new IllegalArgumentException(Impl+" 服务实现类不存在");
		if(!Class.forName(service).isInterface())
			throw new IllegalArgumentException(service+" 服务不是接口类");
		if(Class.forName(Impl).isInterface())
			throw new IllegalArgumentException(service+" 服务实现不能是接口类");
		if(!Class.forName(service).isAssignableFrom(Class.forName(Impl)))
			throw new IllegalArgumentException(" 服务实现类必须实现服务接口类");
		if(port<=0 || port>65535)
			throw new IllegalArgumentException(port+" 非法端口号");
		
		ServerSocket server = new ServerSocket(port);
		System.out.println("服务发布成功");
		while(true){
			Socket socket = server.accept();
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try(ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						) {
						
						String serviceName = ois.readUTF();
						if(!service.equals(serviceName))
							throw new ClassNotFoundException();
						Class serviceClass = Class.forName(serviceName);
						
						String methodName = ois.readUTF();
						Class<?>[] parameterTypes = (Class<?>[]) ois.readObject();
						Object[] args = (Object[]) ois.readObject();
						
						Method method = serviceClass.getMethod(methodName, parameterTypes);
						try(ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());){
							Object ret = method.invoke(Class.forName(Impl).newInstance(), args);
							oos.writeObject(ret);
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally{
						if(socket != null){
							try {
								socket.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}).start();
		}
		
	}
	
	
	
	/**
	 * @param service  要引用的服务全名,如com.service.A
	 * @param host     远端服务发布的ip地址
	 * @param port     远端服务发布的端口
	 * @return         服务的本地代理
	 */
	@SuppressWarnings("unchecked")
	public static <T> T referService(String service, String host, int port) throws Exception{
		
		if(Class.forName(service) == null)
			throw new IllegalArgumentException(service+" 服务不存在");
		if(!Class.forName(service).isInterface())
			throw new IllegalArgumentException(service+" 服务不是接口类");
		if(host ==null || host.length()==0)
			throw new IllegalArgumentException("host 为 null");
		if(port<=0 || port> 65535)
			throw new IllegalArgumentException("非法端口");
		Class serviceClass = Class.forName(service);
		return (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[] {serviceClass}, 
				new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						// TODO Auto-generated method stub
						Socket socket = new Socket(host,port);
						try(ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())){
							oos.writeUTF(service);
							oos.writeUTF(method.getName());
							oos.writeObject(method.getParameterTypes());
							oos.writeObject(args);
							try(ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())){
								Object ret = ois.readObject();
								if (ret instanceof Throwable) {  
	                                throw (Throwable) ret;  
	                            }
								return ret;
							}
							
						}finally{
							socket.close();
						}
						
					}
				}
				);
	}
}
