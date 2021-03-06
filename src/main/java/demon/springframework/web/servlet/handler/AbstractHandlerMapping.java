package demon.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.web.servlet.HandlerExecutionChain;

import demon.springframework.web.context.support.WebApplicationObjectSupport;
import demon.springframework.web.servlet.HandlerMapping;
import demon.springframework.web.servlet.mvc.Controller;
import demon.springframework.web.servlet.mvc.DefaultController;

/**
 * 源码中还继承了webApplicationObjectSupport
 * 该类的作用是？要封装谁的方法
 * @author demon.song
 *
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport implements HandlerMapping {

	private Object defaultHandler;
	
	//拦截器在handlerMapping中的作用
	//interceptors

	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}
	
	public Object getDefaultHandler() {
		return defaultHandler;
	}
	
	//initApplicationContext() 主要是初始化拦截器
	@Override
	protected void initApplicationContext() throws BeansException {
	}
	
	@Override
	public final Controller getHandler(HttpServletRequest request) throws Exception {
		Object handler =getHandlerInternal(request);
		if(handler == null){
			handler =getDefaultHandler();
		}
		if(handler == null){
			return null;
		}
		if(handler instanceof String){
			String handlerName = (String) handler;
			//通过 applicationContext.getBean方法获得处理器
			handler =getApplicationContext().getBean(handlerName);
		}
		return getHandlerExecutionController(handler, request);
	}

	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	protected Controller getHandlerExecutionController(Object handler, HttpServletRequest request) {
		if (handler instanceof Controller) {
			Controller controller = (Controller) handler;
			return controller;
		}
		else {
			return new DefaultController();
		}
	}
}
