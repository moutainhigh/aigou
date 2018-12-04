package cn.itsource.aigou.facade.impl;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.alibaba.dubbo.config.annotation.Service;

import cn.itsource.aigou.core.exception.BisException;
import cn.itsource.aigou.facade.QuartzService;
import cn.itsource.aigou.facade.query.QuartzJobInfo;
import cn.itsource.aigou.job.QuartzUtils;
/*
 * DAY09   zhongyao!!!!
 */
@Service
public class QuartzServiceImpl implements QuartzService {

	@Autowired
	private SchedulerFactoryBean schedulerFactory;
	//添加订单的取消定时任务
	@Override
	public void addJob(QuartzJobInfo info) {
		try {
			/*
			 * UC-service提供了一个接口QuartzService，
			 * 到Quartz-service中拿到schedulerFactory和QuartzUtils完成sche的传入后，
			 * 返回到UC-service进行处理
			 */
			Scheduler sche = schedulerFactory.getScheduler();
			QuartzUtils.addJob(sche, info.getJobName(), cn.itsource.aigou.job.MainJob.class, info, info.getCronj());
		} catch (Exception e) {
			throw BisException.me().setInfo(e.getLocalizedMessage());
		}
	}
	//删除订单的取消定时任务
	@Override
	public void delJob(String jobName) {
		try {
			Scheduler sche = schedulerFactory.getScheduler();
			QuartzUtils.removeJob(sche, jobName);
		} catch (Exception e) {
			throw BisException.me().setInfo(e.getLocalizedMessage());
		}
	}

}
