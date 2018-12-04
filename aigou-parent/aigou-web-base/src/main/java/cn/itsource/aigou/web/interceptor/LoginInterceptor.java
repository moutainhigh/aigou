package cn.itsource.aigou.web.interceptor;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSON;

import cn.itsource.aigou.core.consts.GlobalSettingNames;
import cn.itsource.aigou.core.consts.ICodes;
import cn.itsource.aigou.core.domain.Sso;
import cn.itsource.aigou.core.utils.GlobalSetting;
import cn.itsource.aigou.core.utils.Ret;
import cn.itsource.aigou.web.utils.SsoContext;

public class LoginInterceptor extends HandlerInterceptorAdapter {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		
		//获取请求地址  /user/cart
		StringBuffer requestURL = request.getRequestURL();
		String to = request.getHeader("Referer");
		System.out.println(requestURL);
		System.out.println(to);
		//requestURL : http://127.0.0.1:8081/user/cart
		//to : http://127.0.0.1:8081/
		if(StringUtils.isBlank(to)){
			to = "";
		}
		
		Sso sso = SsoContext.getSso(); //获取当前项目session中是否有用户
		boolean isLogin = sso != null;
		if(isLogin){//如果有session
			//单点退出的检测
			String ssoId = RedisSsoUtils.getSsoId(sso.getId());
			if(StringUtils.isBlank(ssoId)){//中央session已过期
				SsoContext.logOut(); //自动退出
				isLogin = false;
			}else{//中央session未过期
				RedisSsoUtils.refreshRedisSsoId(sso.getId());
			}
		}
		
		if (!isLogin) { //没有登录
			String header = request.getHeader("X-Requested-With"); //获取请求方式
			boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(header);
			//   /sso/login
			String clientLogin = "/"+GlobalSetting.get(GlobalSettingNames.URL_CLIENT_LOGIN);
			if (!isAjax) {
				to = requestURL.toString();
				clientLogin+="?to="+URLEncoder.encode(to,"utf-8");
				response.sendRedirect(clientLogin); // 调到客户端的统一登录接口
			} else {
				clientLogin+="?to="+URLEncoder.encode(to,"utf-8");
				Ret ret = Ret.me().setSuccess(false).setCode(ICodes.UNAUTHED).setData(clientLogin);
				String retJson = JSON.toJSONString(ret);
				response.setContentType("application/json;charset=utf-8");
				response.getWriter().write(retJson); // 返回的json对象，告诉前端应该调往的接口
			}
		}
		return isLogin;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

	}
}
