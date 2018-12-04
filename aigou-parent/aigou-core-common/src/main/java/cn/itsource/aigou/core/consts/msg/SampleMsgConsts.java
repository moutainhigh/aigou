package cn.itsource.aigou.core.consts.msg;

import cn.itsource.aigou.core.consts.ConstName;
import cn.itsource.aigou.core.consts.ICodes;
/**
 * 演示消息码常量（文章管理相关消息常量）
 * 必须继承自cn.itsource.aigou.core.consts.ICodes
 * 2017年2月16日
 * @author nixianhua
 *
 */
public interface SampleMsgConsts extends ICodes{
	public static final int BASECODE = 10000;//基准常量码
	
	@ConstName("发布成功")
	public static final int PUBLISH_SUCCESS = 0+BASECODE;
	
	@ConstName("含有非法字符")
	public static final int INVALIDE_TITLE = 1+BASECODE;
}
