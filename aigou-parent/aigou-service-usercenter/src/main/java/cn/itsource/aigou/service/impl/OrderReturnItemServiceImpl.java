package cn.itsource.aigou.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.itsource.aigou.core.common.base.BaseMapper;
import cn.itsource.aigou.core.common.base.impl.BaseServiceImpl;
import cn.itsource.aigou.core.domain.OrderReturnItem;
import cn.itsource.aigou.core.mapper.OrderReturnItemMapper;
import cn.itsource.aigou.service.IOrderReturnItemService;
@Service
public class OrderReturnItemServiceImpl extends BaseServiceImpl<OrderReturnItem> implements IOrderReturnItemService{
	@Autowired
	private OrderReturnItemMapper mapper;
	
	@Override
	protected BaseMapper<OrderReturnItem> getMapper() {
		return mapper;
	}
}
