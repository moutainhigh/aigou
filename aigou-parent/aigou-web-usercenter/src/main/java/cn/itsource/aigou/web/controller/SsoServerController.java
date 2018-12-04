package cn.itsource.aigou.web.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.DocumentationTool.Location;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;

import cn.itsource.aigou.core.consts.GlobalSettingNames;
import cn.itsource.aigou.core.domain.Sso;
import cn.itsource.aigou.core.utils.GlobalSetting;
import cn.itsource.aigou.core.utils.Ret;
import cn.itsource.aigou.facade.CommonService;
import cn.itsource.aigou.facade.UserCenterService;

@Controller
public class SsoServerController {

	@Reference
	private UserCenterService userCenterService;

	@Reference
	private CommonService commonService;

	/**
	 * SSO服务器统一登录接口 from 登录客户端来源接口地址（回传st）
	 * from sso客户端的请求接口地址 
	 * @return
	 * @throws IOException
	 */
	/*@RequestMapping("/login")
	public String login(String from,String to, Integer relogin, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		//获取cookie中的tgc
		String tgcName = "tgc";
		String tgc = "";
		Cookie[] cookies = request.getCookies();
		if(null!=cookies){
			for (Cookie cookie : cookies) {
				if(tgcName.equals(cookie.getName())){
					tgc = cookie.getValue();
				}
			}
		}
		
		String loginHtml = "redirect:/login.html?from="+from; 
		
		//判断tgc是否存在
		if(StringUtils.isBlank(tgc)){
			return loginHtml;
		}
		
		//Cookie中存在tgc，判断redis中的tgc是否有效
		if(!commonService.validateSsoTGC(tgc)){
			//删除Cookie中的tgc
			Cookie cookie = new Cookie("tgc", "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			
			return loginHtml;
		}
		
		//tgc存在并有效，所以直接生成st并返回给来源的客户端
		String st = commonService.getSsoST(tgc);
		return "redirect:"+from+"?st="+st;
	}*/
	
	
	
	@RequestMapping("/login")
	public String login(String from,String to, Integer relogin, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		if (StringUtils.isBlank(from)) {
			response.sendRedirect(GlobalSetting.get(GlobalSettingNames.URL_WWW));
			return null;
		}
		
		if(StringUtils.isBlank(to)){
			to= "";
		}

		// 获取tgc
		Cookie[] cookies = request.getCookies();
		String tgc = "";
		if(cookies!=null){
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("tgc")) {
					tgc = cookie.getValue();
					// 强制重新登录
					if (null != relogin && 1 == relogin) {
						// 删除tgc
						commonService.deleteSsoTGC(tgc);
						// 重置tgc并删除cookie
						tgc = "";
						cookie.setMaxAge(0);
						cookie.setPath("/");
						response.addCookie(cookie);
					}
				}
			}
		}

		// 存在tgc
		if (StringUtils.isNotBlank(tgc)) {
			boolean validateSsoTGC = commonService.validateSsoTGC(tgc);
			if (validateSsoTGC) {// tgc有效
				// 获取票据
				String st = commonService.getSsoST(tgc);
				// 刷新tgc过期时间
				commonService.refreshTGCExpires(tgc);
				// 返回票据给调用者
				response.sendRedirect(from + "?st=" + st + "&to="+URLEncoder.encode(to,"utf-8"));
				return null;
			}
		}
		
		
		// 需要登录  重定向到登陆页面
		response.sendRedirect("/login.html?wwwurl=" + GlobalSetting.get(GlobalSettingNames.URL_WWW) + "&from=" + from+"&to="+URLEncoder.encode(to,"utf-8"));
		return null;
	}

	
	
	
	/**
	 * sso登录表单登录处理
	 * 
	 * @param username
	 * @param password
	 * @param request
	 * @param response
	 * @return
	 * 
	 */
	/*@RequestMapping("/login/passport/in")
	@ResponseBody
	public Ret loginByPassport(String username, String password, HttpServletRequest request,
			HttpServletResponse response) {
		
		//验证用户名和密码是否正确
		Ret loginRet = userCenterService.login(username, password);
		if(!loginRet.isSuccess()){
			return loginRet;
		}
		
		//登录成功
		String ssoId = (loginRet.getData()+"");
		
		//记录登录用户的信息(生成tgc，并且存入redis)
		String tgc = commonService.getSsoTGC(ssoId);
		
		//将生成的tgc的值写入cookie
		Cookie tgcCookie = new Cookie("tgc", tgc);
		tgcCookie.setPath("/");
		//设置会话期有效(默认不设置maxage就是浏览器会话期)
		response.addCookie(tgcCookie);
		
		//将单点登录用户的ID放入redis（中央session）
		commonService.setRedisSsoId(Long.valueOf(ssoId));
		
		//通过tgc生成访问票据st（st也需要放入redis并且和用户id建立kv关联）
		String st = commonService.getSsoST(tgc);
		
		//返回登录成功的结果对象
		return Ret.me().setData(st);
	}*/
	@RequestMapping("/login/passport/in")
	@ResponseBody
	public Ret loginByPassport(String username, String password, HttpServletRequest request,
			HttpServletResponse response) {
		Ret ret = userCenterService.login(username, password);
		if (ret.isSuccess()) {// 登录成功
			// 获取登录sso用户
			Sso sso = userCenterService.getSsoUser(Long.parseLong(ret.getData().toString()));
			// 写入sso登录信息到redis
			commonService.setRedisSsoId(sso.getId());
			// 写入SSO单点登录TGC到浏览器cookie
			String tgc = commonService.getSsoTGC(sso.getId().toString());
			Cookie tgcCookie = new Cookie("tgc", tgc);
			tgcCookie.setPath("/");// 所有访问路径的目录都可以访问cookie
			response.addCookie(tgcCookie);

			// 返回st
			String st = commonService.getSsoST(tgc);
			ret.setData(st);
		}
		return ret;
	}

	/**
	 * sso注销
	 * 
	 * @param back
	 *            注销后跳转地址
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/logout")
	public String logOut(String back, HttpServletRequest request, HttpServletResponse response) {
		String redirectUrl = back;
		if (StringUtils.isBlank(back)) {
			redirectUrl = "/";
		}

		// 获取tgc，并注销tgc
		Cookie[] cookies = request.getCookies();
		String tgc = "";
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("tgc")) {
				tgc = cookie.getValue();
				// 删除tgc
				commonService.deleteSsoTGC(tgc);
				
				// 删除cookie
				cookie.setMaxAge(0);
				cookie.setPath("/");
				response.addCookie(cookie);
			}
		}
		
		return "redirect:" + redirectUrl;
	}
}
