package cn.itsource.aigou.web.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.velocity.runtime.directive.Foreach;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;

import cn.itsource.aigou.core.domain.Brand;
import cn.itsource.aigou.core.domain.Product;
import cn.itsource.aigou.core.utils.Page;
import cn.itsource.aigou.facade.CommonService;
import cn.itsource.aigou.facade.ProductCenterService;
import cn.itsource.aigou.facade.query.ProductQuery;

@Controller
public class ListController {
	@Reference
	private CommonService commonService;
	
	@Reference
	private ProductCenterService productCenterService;
	
	@RequestMapping("/list")
	public String listmy(ProductQuery query,Model model) {
		//准备数据
		Long productTypeId = query.getProductType();
		if (productTypeId!=null) {//只有类型查找才有面包屑和品牌
			//1)面包屑-参数：当前类型 返回值：List<层次>-->List<Node(自己，List兄弟)>---List<Map<String,Object>(Map有两个key-value))
			List<Map<String,Object>> breads = productCenterService.getTypeBreadcrumb(productTypeId);
			model.addAttribute("breads",breads);
			
			//2)品牌
			//类型锁对应所有品牌
			List<Brand> brands = productCenterService.getBrandsByProductType(productTypeId);
			//选中品牌置顶
			Long chekedBrand = query.getBrand();
			if (chekedBrand != null) {
				Brand branTmp = null;
				Iterator<Brand> iterator = brands.iterator();
				while (iterator.hasNext()) {
					branTmp = iterator.next();
					//删除并且引用它
					if (chekedBrand.longValue()==branTmp.getId().longValue()) {
						iterator.remove();
						break;
					}
				}
				brands.add(branTmp);//在最开始进行插入
			}
			model.addAttribute("brands",brands);
			//首字母-不能重复，要排序（TreeSet），LinkedHashMap(有序的Map)
			Set<String> letters = new TreeSet<>();
			for (Brand brand : brands) {
				letters.add(brand.getFirstLetter());
			}
			model.addAttribute("letters", letters);
			
		}
		
		//3)列表数据-不能直接返回Product，因为里面太多不用属性，而且es查询出来就是Map
		query.setRows(16);
		Page<Map<String,Object>> page = commonService.queryEsProducts(query);
		model.addAttribute("page", page);
		
		//返回页面
		return "list";
	}
	/*@RequestMapping("/list")
	public String list(ProductQuery query,Model model) {
		query.setRows(16);//不然默认basequery里每页rows=10
		if(null!=query.getProductType()){//只有类型查找才有面包屑和品牌！
			//实现面包屑的数据展示   可以用①node对象  ②List<Map<String, Object>> 
			List<Map<String, Object>> breads = productCenterService.getTypeBreadcrumb(query.getProductType());
			model.addAttribute("breads", breads);
			
			//输出品牌的数据
			List<Brand> brands = productCenterService.getBrandsByProductType(query.getProductType());
			
			Set<String> letters = new TreeSet<>();
			for (Brand brand : brands) {
				letters.add(brand.getFirstLetter());
			}
			
			//选择的品牌置顶
			Long selectBrandId = query.getBrand();
			Brand selectedBrand = null;
			for (int i=0;i<brands.size();i++) {
				if(brands.get(i).getId().equals(selectBrandId)){
					selectedBrand = brands.get(i);
					break;
				}
			}
			
			if(null!=selectedBrand){
				brands.remove(selectedBrand);
				brands.add(0, selectedBrand);//在列表头插入selectedBrand
			}
			
			model.addAttribute("letters", letters);
			model.addAttribute("brands", brands);
		}
		//输出根据用户的高级查询分页条件查询的分页对象
		Page<Map<String, Object>> page = commonService.queryEsProducts(query);
		model.addAttribute("page", page);
		
		return "list";
	}*/
	
	/*@RequestMapping("/list")
	public String list(ProductQuery query,Model model) {
		
		query.setRows(16);
		if(null!=query.getPriceMin()){
			query.setPriceMin(100 * query.getPriceMin());
		}
		if(null!=query.getPriceMax()){
			query.setPriceMax(100 * query.getPriceMax());
		}
		//通过es查询商品信息
		Page<Map<String, Object>> productPage = commonService.queryEsProducts(query);
		model.addAttribute("page", productPage);
		
		if(query.getProductType()!=null){
			List<Brand> brands = productCenterService.getBrandsByProductType(query.getProductType());
			//获取所有品牌的首字母集合
			Set<String> letterList = new HashSet<String>();
			if(brands!=null){
				for (Brand brand : brands) {
					letterList.add(brand.getFirstLetter());
				}
			}
			//获取面包屑列表
			List<Map<String, Object>> breads = productCenterService.getTypeBreadcrumb(query.getProductType());
			
			//置顶选中的品牌
			Long brandId = query.getBrand();
			if(brandId!=null && brands!=null){
				int index = 0;
				for (;index<brands.size();index++) {
					Brand brand = brands.get(index);
					Long theBrandId = brand.getId();
					if(theBrandId.longValue() == brandId){
						break;
					}
				}
				if(index>0){
					Brand brand = brands.get(index);
					brands.remove(index);
					brands.add(0, brand);
				}
			}
			model.addAttribute("letters", letterList);
			model.addAttribute("brands", brands);
			model.addAttribute("breads", breads);
		}
		return "list";
	}*/
}