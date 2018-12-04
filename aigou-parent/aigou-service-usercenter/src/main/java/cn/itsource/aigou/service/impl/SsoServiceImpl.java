package cn.itsource.aigou.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;

import cn.itsource.aigou.core.common.base.BaseMapper;
import cn.itsource.aigou.core.common.base.impl.BaseServiceImpl;
import cn.itsource.aigou.core.consts.bis.RegChannelConsts;
import cn.itsource.aigou.core.consts.msg.UserCenterMsgConsts;
import cn.itsource.aigou.core.domain.Sso;
import cn.itsource.aigou.core.mapper.SsoMapper;
import cn.itsource.aigou.core.utils.BitStatesUtils;
import cn.itsource.aigou.core.utils.Ret;
import cn.itsource.aigou.core.utils.StrUtils;
import cn.itsource.aigou.core.utils.encrypt.MD5;
import cn.itsource.aigou.facade.CommonService;
import cn.itsource.aigou.facade.PayCenterService;
import cn.itsource.aigou.service.ISsoService;
import cn.itsource.aigou.service.IVipBaseService;

@Service
public class SsoServiceImpl extends BaseServiceImpl<Sso> implements ISsoService {
	@Autowired
	private SsoMapper mapper;

	@Autowired
	private IVipBaseService vipBaseService;

	@Reference
	private CommonService commonService;

	@Reference
	private PayCenterService payCenterService;

	@Override
	protected BaseMapper<Sso> getMapper() {
		return mapper;
	}

	@Override
	public Sso getSsoByPhone(String phone) {
		Sso sso = mapper.getSsoByPhone(phone);
		return sso;
	}

	@Override
	public Ret regUserByPhone(String phone, String password, String smsCaptcha) {
		//1)校验
				//手机是否注册
				Sso ssoExsit = mapper.getSsoByPhone(phone);
				if (ssoExsit != null) {
					return Ret.me().setSuccess(false)
							.setCode(UserCenterMsgConsts.PHONE_NUMBER_EXISTS);
				}
				//短信验证码是否正确  不要直接返回Boolean，没有提示信息
				Ret ret = commonService.validateSmsCode(phone, smsCaptcha);
				if (!ret.isSuccess()) {
					return ret;
				}
				//2)创建登录账号
				Sso sso = new Sso();
				//设置一堆值
				String salt = StrUtils.getComplexRandomString(32);//盐值
				String md5Pwd = MD5.getMD5(password+salt);//登录比对时，通过获取到的salt以同样规则进行加密加盐
				Long bitState = BitStatesUtils.OP_REGISTED; //已注册
				bitState = BitStatesUtils.addState(bitState, BitStatesUtils.OP_ACTIVED); //已激活
				bitState = BitStatesUtils.addState(bitState, BitStatesUtils.OP_AUTHED_PHONE); //已手机任务
						
				
				sso.setCreateTime(System.currentTimeMillis());
				sso.setUpdateTime(System.currentTimeMillis());
				sso.setNickName(phone);
				sso.setSalt(salt);
				sso.setPassword(md5Pwd);
				sso.setPhone(phone);
				sso.setBitState(bitState);
				//保存
				mapper.savePart(sso);
				//3)创建关联表
				vipBaseService.create(sso, RegChannelConsts.PHONE);
				payCenterService.createVipAccount(sso);
				//4)跳转登录页面 前台已经实现
				return Ret.me();
//		// 判断手机号是否已注册过
//		Sso sso = mapper.getSsoByPhone(phone);
//		if (null != sso) {
//			return Ret.me().setCode(UserCenterMsgConsts.PHONE_NUMBER_EXISTS);
//		}
//
//		// 判断手机的验证码是否有效且正确
//		Ret validateSmsRet = commonService.validateSmsCode(phone, smsCaptcha);
//		if (!validateSmsRet.isSuccess()) {
//			return validateSmsRet;
//		}
//
//		// 生成会员账号并设置账号的基本信息
//		sso = new Sso();
//		sso.setPhone(phone);
//		// 生成随机盐值
//		// String salt = UUID.randomUUID().toString().replace("-", "");
//		String salt = StrUtils.getComplexRandomString(32);
//		// 生成加盐的MD5
//		String md5Pwd = MD5.getMD5(password + salt);
//		sso.setPassword(md5Pwd);
//		sso.setSalt(salt);
//		sso.setNickName(phone);
//		// 设置用户的状态（用户注册成功后的状态）
//		long state = BitStatesUtils.OP_REGISTED;
//		state = BitStatesUtils.addState(state, BitStatesUtils.OP_ACTIVED);
//		state = BitStatesUtils.addState(state, BitStatesUtils.OP_AUTHED_PHONE);
//		sso.setBitState(state);
//		// 存储到数据库
//		mapper.savePart(sso);
//		
//		// 同步生成会员的基本资料和账户信息
//		// 同步创建用户基本资料记录
//		vipBaseService.create(sso, RegChannelConsts.PHONE);
//		// 同步创建用户账户信息
//		payCenterService.createVipAccount(sso);
//		return Ret.me();

	}

	/*
	 * @Override public Ret regUserByPhone(String phone, String password, String
	 * smsCaptcha) { // 参数验证（业务类） // 是否已存在 Sso existSso =
	 * this.getSsoByPhone(phone); if (null != existSso) { return
	 * Ret.me().setSuccess(false).setCode(UserCenterMsgConsts.
	 * PHONE_NUMBER_EXISTS); } // 验证码是否可用 Ret smsCodeValidateRet =
	 * commonService.validateSmsCode(phone, smsCaptcha); if
	 * (!smsCodeValidateRet.isSuccess()) { return smsCodeValidateRet; } //
	 * 开始注册业务 Sso sso = new Sso(); //获取盐值 String salt =
	 * StrUtils.getComplexRandomString(32); //通过盐值进行MD5散列 String md5Password =
	 * MD5.getMD5(password+salt); sso.setCreateTime(System.currentTimeMillis());
	 * sso.setUpdateTime(System.currentTimeMillis()); sso.setNickName(phone);
	 * sso.setSalt(salt); sso.setPassword(md5Password); sso.setPhone(phone);
	 * //状态设置（手机注册，默认即:已经激活，手机已经认证） long state = BitStatesUtils.OP_REGISTED;
	 * state = BitStatesUtils.addState(state, BitStatesUtils.OP_ACTIVED); state
	 * = BitStatesUtils.addState(state, BitStatesUtils.OP_AUTHED_PHONE);
	 * sso.setBitState(state); //存储到数据库 mapper.savePart(sso); //同步创建用户基本资料记录
	 * vipBaseService.create(sso,RegChannelConsts.PHONE); //同步创建用户账户信息
	 * payCenterService.createVipAccount(sso); return Ret.me(); }
	 */
	@Override
	public Ret login(String username, String password) {
		// 通过手机号登录
		Sso sso = this.getSsoByPhone(username);
		// 通过邮箱登录
		// if(sso==null) sso = this.getSsoByEmail(username);
		// 不存在的用户
		if (null == sso) {
			return Ret.me().setSuccess(false).setCode(UserCenterMsgConsts.INVALIDE_USER_PASSWORD);
		}
		// 密码不正确
		if (!sso.getPassword().equals(MD5.getMD5(password + sso.getSalt()))) {
			return Ret.me().setSuccess(false).setCode(UserCenterMsgConsts.INVALIDE_USER_PASSWORD);
		}

		return Ret.me().setData(sso.getId());
	}
}
