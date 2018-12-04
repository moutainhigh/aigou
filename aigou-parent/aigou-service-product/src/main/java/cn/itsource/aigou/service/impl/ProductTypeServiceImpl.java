package cn.itsource.aigou.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.runtime.directive.Foreach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;

import cn.itsource.aigou.core.common.base.BaseMapper;
import cn.itsource.aigou.core.common.base.impl.BaseServiceImpl;
import cn.itsource.aigou.core.consts.GlobalSettingNames;
import cn.itsource.aigou.core.domain.ProductType;
import cn.itsource.aigou.core.mapper.ProductTypeMapper;
import cn.itsource.aigou.core.utils.GlobalSetting;
import cn.itsource.aigou.core.utils.VelocityUtils;
import cn.itsource.aigou.facade.CommonService;
import cn.itsource.aigou.service.IProductTypeService;

@Service
public class ProductTypeServiceImpl extends BaseServiceImpl<ProductType> implements IProductTypeService {
	@Autowired
	private ProductTypeMapper mapper;

	@Reference
	private CommonService commonService;

	@Override
	protected BaseMapper<ProductType> getMapper() {
		return mapper;
	}

	@Override
	public void save(ProductType o) {
		super.save(o);
		this.handleSave(o);
		this.updatePart(o);
		this.staticCat();
	}

	@Override
	public void update(ProductType o) {
		this.handleSave(o);
		super.update(o);
		this.staticCat();
	}

	@Override
	public void updatePart(ProductType o) {
		this.handleSave(o);
		super.updatePart(o);
		this.staticCat();
	}

	/**
	 * 存储修改分类前进行预处理数据
	 * 
	 * @param o
	 */
	private void handleSave(ProductType o) {
		Long id = o.getId();
		Long pid = o.getPid();
		String path = "";
		if (pid == null || 0 == pid) {// 一级分类
			path = "." + id + ".";
		} else {// 子类
			ProductType parentType = this.get(pid);
			path = parentType.getPath() + id + ".";
		}
		o.setPath(path);
	}

	/**
	 * 分类修改后静态化分类信息
	 */
	private void staticCat() {
		// 静态化生成新的json数据，并上传到七牛云空间  
		List<ProductType> productTypes = this.getAllTree(0L);
		String jsonData = JSONObject.toJSONString(productTypes);
		commonService.uploadToQiniuCdn(jsonData.getBytes(), "types.json", null);

		// 静态化商品分类
		String staticRoot = GlobalSetting.get(GlobalSettingNames.WWW_WEB_STATIC_DIR);
		String catTemplateFile = staticRoot + GlobalSetting.get(GlobalSettingNames.WWW_WEB_STATIC_CAT_TEMPLATE);
		/**
		 * VelocityUtils工具    静态化重点
		 */
		VelocityUtils.staticByTemplate(productTypes, catTemplateFile, catTemplateFile + ".html");
		
		// 静态化首页
		String homeTemplateFile = staticRoot + GlobalSetting.get(GlobalSettingNames.WWW_WEB_STATIC_HOME_TEMPLATE);
		Map<String, Object> modelMap = new HashMap<>();
		modelMap.put("staticRoot", staticRoot);
		VelocityUtils.staticByTemplate(modelMap, homeTemplateFile, staticRoot + "home.html");
	}

	@Override
	public List<ProductType> getAll() {
		return mapper.getAll();
	}

	@Override
	public List<ProductType> getAllTree(Long pid) {
		//return getAllTreeByRec1(pid);
//		return getAllTreeByLoop1(pid);
		//方案2：循环方案-只执行一次sql
				return createAllTreeByLoop(pid);
	}
	
	
	/*
	 * 自己的无限级菜单
	 * @param pid
	 * @return
	 */
	private List<ProductType> createAllTreeByLoop(Long pid) {
		List<ProductType> result =  new ArrayList<>();
		//1)获取所有的类型
		List<ProductType> allProductType = mapper.getAll();
		//建立Id和类型直接Map对应关系
		Map<Long,ProductType> productTypeDto = new HashMap<>();
		for (ProductType productType : allProductType) {
			productTypeDto.put(productType.getId(), productType);
		}
		for (ProductType productType : allProductType) {
			if (productType.getPid()==pid) {
				//2）进行遍历，如果是一级类型，直接放入返回列表
				result.add(productType);
			}else {
				//3)否则，建立父子关系，把自己作为父亲的一个儿子放入即可
				//3.1 获取父亲
				Long parentId = productType.getPid();
				//方案2：Map-直接通过parentId直接获取
				ProductType parent = productTypeDto.get(parentId);
				//3.2 把自己作为一个儿子放进去
				parent.getChildren().add(productType);
			}
		}
		
		return result;
	}
	
	
	/*
	 * 
	 *  树状递归思路      只查一次。然后用map存储所有数据，通过pid来拿上一级分类！！！
	 *  确定递归方法 : 通过父类ID获取子类的列表
	 *  递归的实现结构：
	 *  List<Type> getChildren(Long pid){
	 *  	//根据PID先获取它的所有的子节点
	 *  	//循环所有的子节点(隐藏了退出条件)
	 *  		根据子节点的ID作为PID再调用自己这个方法
	 *  		将返回的子节点列表作为当前分类的children的值即可。
	 *  	//返回PID对应的子节点的列表
	 *  }
	 */
	
	

	public List<ProductType> getAllTreeByRec1(Long pid){
		//根据PID先获取它的所有的子节点
		List<ProductType> children = mapper.getChildren(pid);
		
		//循环所有的子节点(隐藏了退出条件)
		for (ProductType productType : children) {
			//根据子节点的ID作为PID再调用自己这个方法
			List<ProductType> theChildren = getAllTreeByRec1(productType.getId());
		   	//将返回的子节点列表作为当前分类的children的值即可。
			productType.setChildren(theChildren);
		}
		  	
		//返回PID对应的子节点的列表
		return children;
	}
	
	
	// 优化递归的性能，通过循环的方式来优化
	public List<ProductType> getAllTreeByLoop1(Long pid){
		//解决不频繁发送SQL的问题，一次性将所有相关的分类拿出来
		List<ProductType> originalList ;
		if(0L==pid){
			originalList = mapper.getAll();
		}else{
			originalList = mapper.getTypes(pid);
		}
		//定义一个Map集合来存储原始的分类信息
		Map<Long, ProductType> typeMap = new HashMap<>();
		for (ProductType productType : originalList) {
			typeMap.put(productType.getId(), productType);
		}
		//重新组织结构，放入我们的目标列表
		List<ProductType> treeList = new ArrayList<>();
		
		//第一次循环获取顶级分类
		for (ProductType productType : originalList) {
			if(productType.getPid().equals(pid)){ // 判断是否是顶级分类
				treeList.add(productType); //放入目标列表
			}else{ // 不是顶级分类，子节点
				//一定有父节点对象，先获取父节点对象
				ProductType parentType = typeMap.get(productType.getPid());
				//将当前分类加到它父对象的子节点列表中
				parentType.getChildren().add(productType);
			}
		}
		return treeList;
	}
	
	
	
	

	/**
	 * 通过递归获取分类树（数据量大时，效率低）
	 * 
	 * @param pid
	 * @return
	 */
	/*private List<ProductType> getAllTreeByRec(Long pid) {
		List<ProductType> childList = mapper.getChildren(pid);
		if (childList == null || childList.size() == 0) {
			return null;
		}
		for (ProductType productType : childList) {
			List<ProductType> children = this.getAllTree(productType.getId());
			if (children != null) {
				productType.setChildren(children);
			}
		}
		return childList;
	}*/

	/**
	 * 通过循环获取分类树
	 * 
	 * @param pid
	 * @return
	 */
	private List<ProductType> getAllTreeByLoop(Long pid) {
		List<ProductType> orginals = null;
		if (pid == 0) {
			orginals = mapper.getAll();
		} else {
			orginals = mapper.getTypes(pid);
		}
		if (null == orginals)
			return null;
		Map<Long, ProductType> dtoMap = new HashMap<Long, ProductType>();
		for (ProductType node : orginals) {
			dtoMap.put(node.getId(), node);
		}

		List<ProductType> resultList = new ArrayList<ProductType>();
		for (Map.Entry<Long, ProductType> entry : dtoMap.entrySet()) {
			ProductType node = entry.getValue();
			if (node.getPid() == pid) {
				// 如果是顶层节点，直接添加到结果集合中
				resultList.add(node);
			} else {
				// 如果不是顶层节点，找的起父节点，然后添加到父节点的子节点中
				if (dtoMap.get(node.getPid()) != null) {
					dtoMap.get(node.getPid()).addChild(node);
				}
			}
		}
		return resultList;
	}

	/*
	 * 获取面包屑
	 */
	@Override
	public List<Map<String, Object>> getTypeBreadcrumb(Long productTypeId) {
		List<Map<String, Object>> result = new ArrayList<>();
		//1)获取ProductType
		ProductType productType = mapper.get(productTypeId);
		//2)从ProductType获取path，并且分割得到Long[]的层次结构id 【1,2,3】
		String path = productType.getPath();
		path = path.substring(1,path.length()-1);//1.2.3
		String[] idsStrs = path.split("\\.");
		for (String idsStr : idsStrs) {
			result.add(id2map(idsStr));
		}
		
		return result;
	}
	//通过一个id获取一个节点数据
	private Map<String, Object> id2map(String idsStr) {
		Map<String, Object> nodeData = new HashMap<>();
		//4)Map里面应该包含自己（currentType）及其兄弟列表（otherTypes）
		ProductType currentType = mapper.get(Long.parseLong(idsStr));
		nodeData.put("currentType", currentType);
		//获取父亲所有儿子再删除自己
		Long pid = currentType.getPid();
		//获取父亲所有儿子
		List<ProductType> otherTypes = mapper.getChildren(pid);
		Iterator<ProductType> iterator = otherTypes.iterator();
		while (iterator.hasNext()) {
			//删除自己
			if (currentType.getId().longValue() == iterator.next().getId().longValue()) {
				iterator.remove();
				break;
			}
		}
		nodeData.put("otherTypes", otherTypes);
		
		
		return nodeData;
	}
	
	
	
	
	
	/*@Override
	public List<Map<String, Object>> getTypeBreadcrumb(Long productTypeId) {
		List<Map<String, Object>> breadCrumbList = new ArrayList<>();
		
		//获取当前的分类对象
		ProductType currentType = this.get(productTypeId);
		//获取分类的path  .1.2.3.
		String path = currentType.getPath();
		path = path.substring(1, path.length() - 1); //   1.2.3
		String[] pathArr = path.split("\\."); // [1,2,3]
		for (int i = 0; i < pathArr.length; i++) {
			Map<String, Object> breadCrumbMap = new HashMap<>();
			
			// 获取每一级的分类ID
			Long typeId = Long.parseLong(pathArr[i]);
			long pid = typeId;
			//根节点
			if(0==i){
				pid = 0L;
			}else{
				pid = Long.parseLong(pathArr[i-1]);
			}
			//获取父节点所有的子节点
			List<ProductType> types = mapper.getChildren(pid);
			
			//查找所有子节点中和当前分类ID一致对象,通过索引j最后进行排除
			int j = 0;
			for (; j < types.size(); j++) {
				if (types.get(j).getId() == typeId.longValue()) {
					break;
				}
			}
			
			breadCrumbMap.put("currentType", types.get(j));
			types.remove(j);
			breadCrumbMap.put("otherTypes", types);
			breadCrumbList.add(breadCrumbMap);
		}
		return breadCrumbList;
	}*/
}
