package cn.itsource.aigou.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.sun.xml.internal.ws.wsdl.writer.document.OpenAtts;

import cn.itsource.aigou.core.domain.Area;
import cn.itsource.aigou.core.domain.Product;
import cn.itsource.aigou.core.domain.ProductExt;
import cn.itsource.aigou.core.domain.Property;
import cn.itsource.aigou.core.domain.Sku;
import cn.itsource.aigou.core.domain.Sso;
import cn.itsource.aigou.core.domain.VipCart;
import cn.itsource.aigou.core.exception.BisException;
import cn.itsource.aigou.core.utils.KV;
import cn.itsource.aigou.core.utils.Ret;
import cn.itsource.aigou.facade.CommonService;
import cn.itsource.aigou.facade.PlatManageService;
import cn.itsource.aigou.facade.ProductCenterService;
import cn.itsource.aigou.facade.UserCenterService;
import cn.itsource.aigou.web.utils.SsoContext;

@Controller
public class CommonController {

	@Reference
	private PlatManageService platManageService;

	@Reference
	private ProductCenterService productCenterService;

	@Reference
	private CommonService commonService;
	
	@Reference
	private UserCenterService userCenterService;
	
	@RequestMapping("/home")
	public String homepage() {
		
		for(int i=0;i<5000000;i++){
			//Date createDate = new Date();
			//Date createDate = Calendar.getInstance().getTime();
			long createDate = System.currentTimeMillis();
			//....
			//  上市时间:2016-9,产品名称:iPhone 7 Plus,屏幕尺寸:5.5英寸
			/*
			id  pid  name	path
			1	0	A		.1.
			2	0	B		.2.
			3	1	A-1		.1.3.
			4	1	A-2		.1.4.
			5	2	B-1		.2.5.
			6	4	A-2-1	.1.4.6.
			如何获取A分类的所有后代节点？ 递归查询  select * from t_type where path like '.1.%'
			*/
		}
		
		return "home";
	}
	
	/**
	 * 返回用户购物车和消息数量
	 * 未登录：{"code":0,"data":{"cart":{"goods_count":0},"message":{"internal_count":"0"}}}
	 * 已登录：{"code":0,"data":{ "cart":{"goods_count":2},
	 * 							"message":{"internal_count":"0"}, 
	 * 							"user_name":"SZY083SDPF5239",
	 * 							"headimg":"14865372927544.png", 
	 * 							"last_time":1486538020,
	 * 							"last_ip":"100.97.126.14", 
	 * 							"last_region_code":null,
	 * 							"last_time_format":"2017-02-08 15:13:40", 
	 * 							"user_rank":{ "rank_id":"1",
	 * 											"rank_name":"初级会员一级", 
	 * 											"rank_img":"14788627664630.gif", 
	 * 											"min_points":"0",
	 * 											"max_points":"1", 
	 * 											"type":"0" 
	 * 										} 
	 * 						} 
	 * 		}
	 * 
	 * @return
	 */
	@RequestMapping("/site-user")
	@ResponseBody
	public Ret siteUser() {
		Sso sso = SsoContext.getSso();
		Map<String, Object> data = new HashMap<>();
		if (sso == null) {
			Map<String, Object> cart = new HashMap<>();
			cart.put("goods_count", 0);
			Map<String, Object> message = new HashMap<>();
			message.put("internal_count", 0);
			data.put("cart", cart);
			data.put("message", message);
		}else{
			Map<String, Object> cart = new HashMap<>();
			cart.put("goods_count", 0);
			
			Map<String, Object> message = new HashMap<>();
			message.put("internal_count", 0);
			
			Map<String, Object> userRankMap = new HashMap<>();
			userRankMap.put("rank_id", "1");
			userRankMap.put("rank_name", "初级会员一级");
			userRankMap.put("rank_img", "14788627664630.gif");
			userRankMap.put("min_points", "0");
			userRankMap.put("max_points", "1");
			userRankMap.put("type", "0");
			
			data.put("cart", cart);
			data.put("message", message);
			data.put("user_rank", userRankMap);
			
			data.put("user_name", sso.getNickName());
			data.put("headimg", sso.getAvatar());
			data.put("last_time", System.currentTimeMillis());
			data.put("last_time_format", "2017-05-05 12:12:12");
			data.put("last_ip", "127.0.0.1");
			data.put("last_region_code", "51,01");
		}
		return Ret.me().setData(data);
	}

	/**
	 * 获取购物车数据
	 */
	@RequestMapping("/cart-data")
	@ResponseBody
	public Ret cartData() {
		Sso sso = SsoContext.getSso();
		Map<String, Object> cartData = new HashMap<>();
		if(null==sso){
			cartData.put("goodsNumber", 0);
			cartData.put("goodsTotalPrice", 0);
		}else{
			cartData = userCenterService.cartInfo(sso.getId());
		}
		return Ret.me().setData(cartData);
	}

	/**
	 * 获取城市列表数据
	 * 
	 * @param region_code
	 *            初始化选中区域
	 * @param parent_code
	 *            通过父code返回子级
	 * @return region_code返回
	 *         {"code":0,"region_names":{"51":"四川省","51,01":"成都市"}, "data":[ [
	 *         {"region_id":510000,"region_code":"51","region_name":"四川省","parent_code":"0","level":1},
	 *         {...},... ], [
	 *         {"region_id":510100,"region_code":"51,01","region_name":"成都市","parent_code":"51","level":2},
	 *         {...},... ], [
	 *         {"region_id":510101,"region_code":"51,01,01","region_name":"青羊区","parent_code":"51,01","level":3},
	 *         {...},... ] ] }
	 *
	 *         parent_code返回 {"code":0, "data":[ [
	 *         {"region_id":510100,"region_code":"51,01","region_name":"成都市","parent_code":"51","level":2},
	 *         {...},... ] ] }
	 *
	 */
	@RequestMapping("/region-list")
	@ResponseBody
	public Map<String, Object> regionList(String region_code, String parent_code) {
		Map<String, Object> retMap = new HashMap<>();
		if (StringUtils.isNotBlank(region_code)) {
			retMap = platManageService.getRegionListMap(region_code);
		} else if (StringUtils.isNotBlank(parent_code)) {
			retMap = platManageService.getRegionChildrenListMap(parent_code);
		}
		return retMap;
	}

	/**
	 * 根据传入的ID集合，返回ID的收藏状态 {"code":0,"data":{"1811":"1","1800":"1"}}
	 * 
	 * @param goods_ids
	 * @return
	 */
	@RequestMapping("/goods-collect-state")
	@ResponseBody
	public Ret goodsCollect(String goods_ids) {

		return Ret.me();
	}

	/**
	 * * 根据传入的ID集合，返回ID的对比状态 {"code":0,"data":{"1811":"1","1800":"1"}}
	 * 
	 * @param goods_ids
	 * @return
	 */
	@RequestMapping("/goods-compare-state")
	@ResponseBody
	public Ret goodsCompare(String goods_ids) {

		return Ret.me();
	}

	/**
	 * 返回对比栏的数据
	 * 
	 * @return
	 */
	@RequestMapping("/compare/box-goods-list")
	@ResponseBody
	public Ret goodsCompareBox() {

		return Ret.me();
	}

	/**
	 * 添加商品到对比栏
	 * 
	 * @param goods_ids
	 * @return
	 */
	@RequestMapping("/compare/add")
	@ResponseBody
	public Ret addCompare(Long goods_ids) {

		return Ret.me();
	}

	/**
	 * 从对比栏删除
	 * 
	 * @param goods_ids
	 * @return
	 */
	@RequestMapping("/compare/remove")
	@ResponseBody
	public Ret removeCompare(Long goods_ids) {

		return Ret.me();
	}

	/**
	 * 获取商品的SKU列表页面
	 * 
	 * @param goods_id
	 * @return
	 */
	@RequestMapping("/product/sku")
	public String getProductSkus(Long productId, Model model) {
		this.setSkuData(productId, model);
		// 输出商品信息
		Map<String, Object> productMap = commonService.getEsProduct(productId.toString());
		model.addAttribute("product", productMap);
		return "skuSelector";
	}

	/**
	 * 商品详细页
	 * 
	 * @param id
	 * @param model
	 * @return
	 */
	@RequestMapping("/p/{id}")
	public String product(@PathVariable(value = "id") Long id, Model model) {
		//通过商品的ID查询Es中商品的信息
		Map<String, Object> esProduct = commonService.getEsProduct(String.valueOf(id));
		
		List<Integer> productTypes = (List<Integer>) esProduct.get("productType");
		Integer productTypeId = productTypes.get(productTypes.size()-1);
		
		Product product = productCenterService.getProduct(id);
		//model.addAttribute("breads", productCenterService.getTypeBreadcrumb(product.getProductType()));
		model.addAttribute("breads", productCenterService.getTypeBreadcrumb(Long.valueOf(productTypeId)));
		model.addAttribute("product", esProduct);
		
		//输出商品的显示属性信息  List<Map<String,Object>>
		
		List<KV<String, String>> viewPropertyList = new ArrayList<>();
		//根据商品的显示属性摘要解析为需要列表即可
		// 3:接口类型:10:USB3.0_7:套餐类型:0:官方套餐_9:成色:16:全新
		String viewProperties = product.getViewProperties();
		String[] attrs = viewProperties.split("_");
		for (String attrData : attrs) {
			String[] attrDataArr = attrData.split(":");
			viewPropertyList.add(new KV<String, String>(attrDataArr[1], attrDataArr[3]));
		}
		
		model.addAttribute("viewPropertyList", viewPropertyList );
		
		//获取商品的图文详情(都是图片，不是全文数据)  在数据库查
		ProductExt productExt = productCenterService.getProductExt(id);
		model.addAttribute("productExt", productExt);
		
		//获取商品的SKU体系数据(关于sku匹配的问题，采用后台获取所有SKU的信息直接返给前台，由前台根据返回的数据自行匹配SKU)
		//attrNameList sku规格名的列表（直接展示在页面上供用户选择）
		List<String> attrNameList = new ArrayList<>();
		//返回完整的SKU信息skuListJson（当用户选择某个规格后，可以通过该数据匹配到对应的sku）
		List<Map<String, Object>> skuDataList = new ArrayList<>();
		String skuListJson = "";
		//获取商品的sku列表
		List<Sku> skuList = productCenterService.getSkuList(id);
		for (Sku sku : skuList) {
			attrNameList.add(sku.getSkuName());
			
			Map<String, Object> skuDataMap = new HashMap<>();
			skuDataMap.put("sku_id", sku.getId());
			skuDataMap.put("spec_ids", sku.getSkuName()); //规格名
			skuDataMap.put("goods_number", sku.getAvailableStock());
			skuDataMap.put("goods_price", sku.getPrice());
			skuDataMap.put("sku_image", sku.getSkuMainPic());
			skuDataMap.put("sku_image_thumb", sku.getSkuMainPic());
			skuDataMap.put("market_price", sku.getMarketPrice());
			
			skuDataList.add(skuDataMap);
		}
		
		skuListJson = JSONObject.toJSONString(skuDataList);
		
		model.addAttribute("attrNameList", attrNameList);
		model.addAttribute("skuListJson", skuListJson);
		
		
		return "p";
	}
	
	
	/*
	@RequestMapping("/p/{id}")
	public String product(@PathVariable(value = "id") Long id, Model model) {
		if (id == null) {
			throw BisException.me().setInfo("非法请求");
		}
		this.setSkuData(id, model);
		// 输出商品信息
		Map<String, Object> productMap = commonService.getEsProduct(id.toString());
		model.addAttribute("product", productMap);
		// 获取面包屑列表
		List<Map<String, Object>> breads = productCenterService
				.getTypeBreadcrumb(productCenterService.getProduct(id).getProductType());
		model.addAttribute("breads", breads);
		
		//获取商品的详细介绍
		ProductExt productExt = productCenterService.getProductExt(id);
		model.addAttribute("productExt", productExt);
		
		//商品的显示属性
		//17:裙长:29:中长裙_16:组合形式:25:单件
		String viewProperties = productMap.get("viewProperties").toString();
		List<KV<String,String>> viewPropertyList = new ArrayList<>();
		String[] propList = viewProperties.split("_");
		for (String props : propList) {
			String[] propArr = props.split(":");
			KV<String, String> kv = new KV<String, String>(propArr[1], propArr[3]);
			viewPropertyList.add(kv);
		}
		model.addAttribute("viewPropertyList", viewPropertyList);
		return "p";
	}*/
	
	//设置商品sku信息到页面模型中
	private void setSkuData(Long productId, Model model) {
		System.out.println("setSkuData.....");
		List<Sku> skuList = productCenterService.getSkuList(productId);
		// 按序存储商品所有属性名字
		List<String> attrNameList = new ArrayList<>();
		
		// sku格式化列表
		List<Map<String, Object>> skuDataList = new ArrayList<>();

		for (int i = 0; i < skuList.size(); i++) {
			Sku sku = skuList.get(i);
			attrNameList.add(sku.getSkuName());
			// 格式化sku信息列表
			Map<String, Object> skuMap = new HashMap<>();
			skuMap.put("sku_id", sku.getId());
			skuMap.put("spec_ids", sku.getSkuName());
			skuMap.put("goods_number", sku.getAvailableStock());
			skuMap.put("goods_price", sku.getPrice());
			skuMap.put("sku_image", sku.getSkuMainPic());
			skuMap.put("sku_image_thumb", sku.getSkuMainPic());
			skuMap.put("market_price", sku.getMarketPrice());
			skuDataList.add(skuMap);
		}

		// 输出属性名字列表
		model.addAttribute("attrNameList", attrNameList);

		// 格式化为json
		String skuListJson = JSONObject.toJSONString(skuDataList);
		model.addAttribute("skuListJson", skuListJson);
	}

}
