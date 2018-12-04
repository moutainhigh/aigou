package cn.itsource.aigou.es;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.alibaba.fastjson.JSONObject;

import cn.itsource.aigou.core.consts.GlobalSettingNames;
import cn.itsource.aigou.core.query.BaseQuery;
import cn.itsource.aigou.core.utils.GlobalSetting;
import cn.itsource.aigou.core.utils.Page;
import cn.itsource.aigou.facade.query.ProductQuery;

public class ProductSearchHelper {
	public static final String INDEX = "aigou";
	public static final String TYPE = "product";
	public static final String ALL_FIELD = "productAll";
	public static final String PRODUCT_STATE = "state";
	public static final String PRODUCT_TYPE = "productType";
	public static final String PRODUCT_BRAND = "brandId";
	public static final String PRODUCT_MAX_PRICE = "maxPrice";
	public static final String PRODUCT_MIN_PRICE = "minPrice";
	public static final String PRODUCT_SALE_COUNT = "saleCount";
	public static final String PRODUCT_VIEW_COUNT = "viewCount";
	public static final String PRODUCT_ONSALE_TIME = "onSaleTime";
	public static final String PRODUCT_COMMENT_COUNT = "commentCount";

	private static TransportClient client = null;

	/**
	 * 获取es客户端
	 * 
	 * @return
	 */
	@SuppressWarnings("resource")
	public static TransportClient getClient() {
		if (null == client) {
			Settings settings = Settings.builder().put("client.transport.sniff", true).build();
			try {
				client = new PreBuiltTransportClient(settings).addTransportAddress(
						new InetSocketTransportAddress(InetAddress.getByName(GlobalSetting.get(GlobalSettingNames.ES_CLUSTER_HOST)),
								Integer.parseInt(GlobalSetting.get(GlobalSettingNames.ES_CLUSTER_PORT))));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return client;
	}

	/**
	 * 新增或更新文档
	 * 
	 * @param id
	 *            文档ID
	 * @param json
	 *            文档json格式字符串数据
	 */
	public static void saveOrUpdate(String id, String json) {
		TransportClient client = ProductSearchHelper.getClient();
		IndexRequest indexRequest = new IndexRequest(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id)
				.source(json);
		UpdateRequest updateRequest = new UpdateRequest(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id)
				.doc(json).upsert(indexRequest);
		try {
			UpdateResponse updateResponse = client.update(updateRequest).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 批量新增/更新商品信息
	 * @param dataList
	 */
	public static void saveOrUpdate(List<Map<String, Object>> dataList) {
		//获取es的客户端，并且准备一个批量请求的对象
		BulkRequestBuilder bulkRequest = getClient().prepareBulk();
		//遍历传入的每个商品的文档
		for (Map<String, Object> productMap : dataList) {
			
			Map<String, Object> aviProductMap = new HashMap<>();
			//删除productMap中null值
			for(String key : productMap.keySet()){
				if(null!=productMap.get(key)){
					aviProductMap.put(key, productMap.get(key));
				}
			}
			
			//获取文档的ID
			String id = aviProductMap.get("id").toString();
			//将map转换为json字符串的文档
			//String json = JSONObject.toJSONString(aviProductMap);
			//添加索引的请求对象
			IndexRequest indexRequest = new IndexRequest(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id)
					.source(aviProductMap);
			//更新索引的请求对象，但是如果文档不存在，那么使用upsert方法中指定的添加索引的请求来处理
			UpdateRequest updateRequest = new UpdateRequest(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id)
					.doc(aviProductMap).upsert(indexRequest);
			
			//将当前文档的索引请求添加到批量请求列表中
			bulkRequest.add(updateRequest);
		}
		//发起请求完成索引的创建
		BulkResponse bulkResponse = bulkRequest.get();
		if (bulkResponse.hasFailures()) {
		}
	}

	/**
	 * 删除文档
	 * 
	 * @param id
	 *            文档ID
	 */
	public static void delete(String id) {
		DeleteResponse response = getClient().prepareDelete(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id)
				.get();
	}
	
	/**
	 * 批量删除文档
	 * @param ids
	 */
	public static long delete(Long[] ids) {
		if(ids==null || ids.length==0) return 0;
		long[] idArr = new long[ids.length];
		for (int i=0;i<ids.length;i++) {
			idArr[i] = ids[i];
		}
		BulkIndexByScrollResponse response =
			    DeleteByQueryAction.INSTANCE.newRequestBuilder(getClient())
			        .filter(QueryBuilders.termsQuery("id", idArr)) 
			        .source("aigou")
			        .get();                                             
		long deleted = response.getDeleted();
		return deleted;
	}

	/**
	 * 通过ID获取文档
	 * 
	 * @param id
	 *            文档ID
	 * @return
	 */
	public static Map<String, Object> get(String id) {
		GetResponse response = getClient().prepareGet(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, id).get();
		if (response.isExists()) {
			return response.getSource();
		} else {
			return new HashMap<>();
		}
	}

	/**
	 * 批量获取文档
	 * 
	 * @param ids
	 *            文档ID数组
	 * @return
	 */
	public static List<Map<String, Object>> get(String... ids) {
		List<Map<String, Object>> resultList = new ArrayList<>();
		if (ids == null || ids.length == 0)
			return resultList;
		MultiGetResponse multiGetItemResponses = getClient().prepareMultiGet()
				.add(ProductSearchHelper.INDEX, ProductSearchHelper.TYPE, ids).get();
		for (MultiGetItemResponse itemResponse : multiGetItemResponses) {
			GetResponse response = itemResponse.getResponse();
			if (response.isExists()) {
				resultList.add(response.getSource());
			}
		}
		return resultList;
	}
	
	
	/*自己写的 es查询
	 * 通过dsl模板和参数获取文档
	 * 
	 * @param dslTemplate dsl模板
	 * @param params   参数 
	 * 如：
	 *  dslTtemplate ： {"query":{"match":{"id":"{{id}}"}}}
	 *  params(map) : new HashMap().put("id",1);
	 * @return
	 */
	public static List<Map<String, Object>> query(String dslTemplate, Map<String, Object> params) {
		SearchResponse response = new SearchTemplateRequestBuilder(getClient()).setScript(dslTemplate)
				.setScriptType(ScriptType.INLINE).setScriptParams(params).setRequest(new SearchRequest()).get()
				.getResponse();
		List<Map<String, Object>> resultList = new ArrayList<>();
		for (SearchHit hit : response.getHits().getHits()) {
			resultList.add(hit.getSource());
		}
		return resultList;
		
	}
	/**
	 * dsl=查询+过滤+排序+分页+截取字段
	 * query
	 *    bool
	 *       must
	 *         match
	 *       filter
	 * sort
	 * from
	 * size
	 * _source
	 * @param query
	 * @return
	 */
	public static Page<Map<String, Object>> query(ProductQuery query) {
		//1）获取客户端
		TransportClient client = getClient();
		SearchRequestBuilder builder = client.prepareSearch(INDEX);
		builder.setTypes(TYPE);
		//2)dsl=查询+过滤
				//关键字+类型+品牌+价格区间
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();//query bool
		String keyword = query.getKeyword();
		//查询：关键字
		if (StringUtils.isNotBlank(keyword)) {
			boolQuery.must(QueryBuilders.matchQuery(ALL_FIELD, keyword));//query bool must match
		}
		//过滤：类型+品牌+价格区间
		List<QueryBuilder> filter = boolQuery.filter();
		//类型
		Long productType = query.getProductType();
		if (productType!=null) {
			filter.add(QueryBuilders.termQuery(PRODUCT_TYPE, productType));
		}
		//品牌
		Long brand = query.getBrand();
		if (brand!=null) {
			filter.add(QueryBuilders.termQuery(PRODUCT_BRAND, brand));
		}
		//价格区间
		//minPrice <= priceMax &&
		Integer priceMax = query.getPriceMax();
		if (priceMax!=null) {
			priceMax = priceMax * 100;//索引库存放的是分，传入的是元，所以要进行转换
			filter.add(QueryBuilders.rangeQuery(PRODUCT_MIN_PRICE).lte(priceMax));
		}
		//maxPrice >= priceMin
		Integer priceMin = query.getPriceMin();
		if (priceMin != null) {
			priceMin = priceMin * 100;
			filter.add(QueryBuilders.rangeQuery(PRODUCT_MAX_PRICE).gte(priceMin));
		}
		
		builder.setQuery(boolQuery);
		
		
		
		//3)排序
		String sortField = query.getSort();
		String sortMethod = query.getOrder();//两种，先给默认值如果不对，再改值
		
		if (StringUtils.isNotBlank(sortMethod) && StringUtils.isNotBlank(sortField)) {
			SortOrder sortOrder = SortOrder.DESC;
			if (!sortMethod.equals("desc")) {
				sortOrder = SortOrder.ASC;
			}
			//销量
			if (sortField.equals(ProductQuery.ORDER_XL)) {
				builder.addSort(PRODUCT_SALE_COUNT, sortOrder);
			}
			//新品
			if (sortField.equals(ProductQuery.ORDER_XP)) {
				builder.addSort(PRODUCT_ONSALE_TIME, sortOrder);
			}
			//评论
			if (sortField.equals(ProductQuery.ORDER_PL)) {
				builder.addSort(PRODUCT_COMMENT_COUNT, sortOrder);
			}
			//人气
			if (sortField.equals(ProductQuery.ORDER_RQ)) {
				builder.addSort(PRODUCT_VIEW_COUNT, sortOrder);
			}
			//价格-如果是升序，想买便宜的用min_price,如果是降序，用户想要买贵的，用max_price
			if (sortField.equals(ProductQuery.ORDER_JG)) {
				if (sortOrder.equals(SortOrder.DESC)) {
					builder.addSort(PRODUCT_MAX_PRICE, sortOrder);
				}else{
					builder.addSort(PRODUCT_MIN_PRICE, sortOrder);
				}
			}
		}
		
		//4分页
		builder.setFrom(query.getStart()).setSize(query.getRows());
		//5）截取字段-用不着
		//6)封装数据
		SearchResponse response = builder.get();
		SearchHits hits = response.getHits();//获取命中返回值=命中总数+命中哪些
		Long totalHits = hits.getTotalHits();
		if (totalHits<1) {
			return new Page<>();
		}
		List<Map<String,Object>> rows = new ArrayList<>();
		SearchHit[] hits2 = hits.getHits();//命中的哪些
		for (SearchHit searchHit : hits2) {
			rows.add(searchHit.getSource());//把命中的数据封装进rows
		}
		return new Page<>(rows, totalHits.intValue(), query);
		
		
	}
	
	
	
	
	
	
	
	

//	/**原版的通过dsl模板和参数获取文档   一共两个方法
//	 * 通过dsl模板和参数获取文档
//	 * 
//	 * @param dslTemplate dsl模板
//	 * @param params   参数 
//	 * 如：
//	 *  dslTtemplate ： {"query":{"match":{"id":"{{id}}"}}}
//	 *  params(map) : new HashMap().put("id",1);
//	 * @return
//	 */
//	public static List<Map<String, Object>> query(String dslTemplate, Map<String, Object> params) {
//		SearchResponse response = new SearchTemplateRequestBuilder(getClient()).setScript(dslTemplate)
//				.setScriptType(ScriptType.INLINE).setScriptParams(params).setRequest(new SearchRequest()).get()
//				.getResponse();
//		List<Map<String, Object>> resultList = new ArrayList<>();
//		for (SearchHit hit : response.getHits().getHits()) {
//			resultList.add(hit.getSource());
//		}
//		return resultList;
//	}
//
//	
//	public static Page<Map<String, Object>> query(ProductQuery query) {
//		
//		//获取客户端连接
//		TransportClient transportClient = getClient();
//		
//		//准备一个搜索请求对象
//	//  aigou/product
//		SearchRequestBuilder requestBuilder = transportClient.prepareSearch(ProductSearchHelper.INDEX).setTypes(ProductSearchHelper.TYPE); 
//		//设置分页条件
//		requestBuilder.setFrom(query.getStart()).setSize(query.getRows());
//		//设置查询和过滤条件
//		/*
//		 {
//		 	query : {
//		 		"bool" : {
//		 			must : [],
//		 			filter : []
//		 		}
//		 	}
//		 }
//		 */
//		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();//query bool
//		//分别添加查询条件和过滤条件
//		//查询条件
//		if(StringUtils.isNotBlank(query.getKeyword())){
//			List<QueryBuilder> must = queryBuilder.must();//query bool must
//			must.add(QueryBuilders.matchQuery(ALL_FIELD, query.getKeyword()));//query bool must match
//		}
//		
//		//过滤条件
//		List<QueryBuilder> filter = queryBuilder.filter();//query bool filter
//		//商品分类的过滤条件
//		Long productTypeId = query.getProductType();
//		if(null!=productTypeId){
//			filter.add(QueryBuilders.termQuery(PRODUCT_TYPE, productTypeId));
//		}
//		
//		//品牌的过滤条件
//		Long brandId = query.getBrand();
//		if(null!=brandId){
//			filter.add(QueryBuilders.termQuery(PRODUCT_BRAND, brandId));
//		}
//		
//		//价格的过滤条件
//		//按价格搜索的思路
//		// 假设用户给出的价格区间是[a,b]，商品的价格区间是[x,y]
//		// 不满足用户的价格条件的情况： x>b or y<a
//		// 满足价格搜索的条件  not(x>b or y<a) == "x<=b and y>=a"
//		Integer priceMax = query.getPriceMax();
//		Integer priceMin = query.getPriceMin();
//		if(null!=priceMax){ // x<=priceMax
//			filter.add(QueryBuilders.rangeQuery(PRODUCT_MIN_PRICE).lte(priceMax));
//		}
//		if(null!=priceMin){ // y>=priceMin
//			filter.add(QueryBuilders.rangeQuery(PRODUCT_MAX_PRICE).gte(priceMin));
//		}
//		
//		requestBuilder.setQuery(queryBuilder);
//		
//		//设置排序条件
//		String sort = query.getSort(); // 按照哪个字段来排序  xl pl rq jg....
//		
//		String orderMethod = query.getOrder(); // 按照升序或降序排  asc和desc
//		SortOrder sortOrder = SortOrder.DESC;
//		if(BaseQuery.ASC.equals(orderMethod)){
//			sortOrder = SortOrder.ASC;
//		}
//		
//		
//		
//		if(ProductQuery.ORDER_XL.equals(sort)){//销量排序
//			requestBuilder.addSort(PRODUCT_SALE_COUNT, sortOrder);
//		}
//		
//		if(ProductQuery.ORDER_XP.equals(sort)){//新品排序
//			requestBuilder.addSort(PRODUCT_ONSALE_TIME, sortOrder);
//		}
//		
//		if(ProductQuery.ORDER_PL.equals(sort)){//评论排序
//			requestBuilder.addSort(PRODUCT_COMMENT_COUNT, sortOrder);
//		}
//		//人气排序
//		if(ProductQuery.ORDER_RQ.equals(sort)){
//			requestBuilder.addSort(PRODUCT_VIEW_COUNT, sortOrder);
//		}
//		//价格排序
//		//如果是升序，说明用户想要买贵的
//		if(ProductQuery.ORDER_JG.equals(sort)){
//			if(sortOrder.equals(SortOrder.DESC)){
//				requestBuilder.addSort(PRODUCT_MAX_PRICE, sortOrder);
//			}else{
//				requestBuilder.addSort(PRODUCT_MIN_PRICE, sortOrder);
//			}
//		}
//		
//		//发起请求，获取返回的结果
//		/*
//		 {
//		 	hits : {
//		 		total....: 230,
//		 		hits : [
//		 			{
//		 				_index : xxx,
//		 				_type : yyy,
//		 				_source : {
//		 					id : 1,
//		 					name : "越南喜娘",
//		 					....
//		 				}
//		 			},
//		 			{}
//		 		]
//		 	}
//		 }
//		 
//		 */
//		//发起请求
//		SearchResponse response = requestBuilder.get();
//		
//		SearchHits searchHits = response.getHits();
//		
//		long totalHits = searchHits.getTotalHits(); // 找到符合条件的总文档数量
//		//获取返回的当前页的es文档列表
//		SearchHit[] hits = searchHits.getHits();//命中返回值
//		
//		List<Map<String, Object>> rows = new ArrayList<>();
//		for (SearchHit searchHit : hits) {
//			Map<String, Object> productMap = searchHit.getSource();
//			rows.add(productMap);
//		}
//		//构造一个page对象
//		Page<Map<String, Object>> page = new Page<>(rows, (int)totalHits, query);
//		return page;
//	}
	
	
	
	
	/**
	 * 通过product查询对象对product的es数据进行查询分页
	 * @param query
	 * @return
	 */
	/*public static Page<Map<String, Object>> query(ProductQuery query) {
		SearchRequestBuilder sRequestBuilder = getClient().prepareSearch(ProductSearchHelper.INDEX)
				.setTypes(ProductSearchHelper.TYPE);
		// 设置搜索类型
		sRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		// 查询条件
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		List<QueryBuilder> must = boolQuery.must();

		// 关键字
		if (StringUtils.isNotBlank(query.getKeyword())) {
			must.add(QueryBuilders.matchQuery(ProductSearchHelper.ALL_FIELD, query.getKeyword()));
		}

		// 条件过滤器
		List<QueryBuilder> filters = boolQuery.filter();
		if (query.getProductType() != null) {
			filters.add(QueryBuilders.termQuery(PRODUCT_TYPE, query.getProductType()));
		}
		if (query.getBrand() != null) {
			filters.add(QueryBuilders.termQuery(PRODUCT_BRAND, query.getBrand()));
		}
		if (query.getPriceMax() != null) {
			filters.add(QueryBuilders.rangeQuery(PRODUCT_MIN_PRICE).from(0).to(query.getPriceMax()));
		}
		if (query.getPriceMin() != null) {
			filters.add(QueryBuilders.rangeQuery(PRODUCT_MAX_PRICE).from(query.getPriceMin()).to(Integer.MAX_VALUE));
		}

		// 添加到查询构造器
		sRequestBuilder.setQuery(boolQuery);

		// 排序字段
		String sort = query.getSort();
		// 排序方式
		String order = query.getOrder();
		SortOrder sortOrder = SortOrder.DESC;
		if (StringUtils.isNotBlank(order)) {
			if ("asc".equals(order.toLowerCase())) {
				sortOrder = SortOrder.ASC;
			}
		}

		if ("s".equals(sort)) {// 综合
		} else if ("xl".equals(sort)) {// 销量
			sRequestBuilder.addSort(PRODUCT_SALE_COUNT, sortOrder);
		}
		if ("xp".equals(sort)) {// 新品
			sRequestBuilder.addSort(PRODUCT_ONSALE_TIME, sortOrder);
		}
		if ("pl".equals(sort)) {// 评论
			sRequestBuilder.addSort(PRODUCT_COMMENT_COUNT, sortOrder);
		}
		if ("jg".equals(sort)) {// 价格
			if (sortOrder == SortOrder.DESC) {
				sRequestBuilder.addSort(PRODUCT_MAX_PRICE, sortOrder);
			} else {
				sRequestBuilder.addSort(PRODUCT_MIN_PRICE, sortOrder);
			}
		}
		if ("rq".equals(sort)) {// 人气
			sRequestBuilder.addSort(PRODUCT_VIEW_COUNT, sortOrder);
		}
		// 分页
		sRequestBuilder.setFrom(query.getStart()).setSize(query.getRows()).setExplain(false);
		// 开始查询
		SearchResponse response = sRequestBuilder.get();
		int total = (int)response.getHits().getTotalHits();
		List<Map<String, Object>> resultList = new ArrayList<>();
		for (SearchHit hit : response.getHits().getHits()) {
			resultList.add(hit.getSource());
		}
		
		Page<Map<String, Object>> page = new Page<>();
		page.setRows(resultList);
		page.setTotal(total);
		return page;
	}*/

	/**
	 * 关闭es客户端
	 */
	public static void close() {
		getClient().close();
	}

	public static void main(String[] args) {

		/*TransportClient client = ProductSearchHelper.getClient();
		for (long i = 1; i < 100; i++) {
			String jsonDataText = "";
			Map<String, Object> product = new HashMap<>();
			product.put("name", "2017春秋款大花花纹连衣裙-" + i);
			product.put("brandId", 125L + i);
			product.put("id", i);
			jsonDataText = JSONObject.toJSONString(product);
			IndexResponse response = client.prepareIndex("aigou", "product", product.get("id").toString())
					.setSource(jsonDataText).get();
		}

		Map<String, Object> params = new HashMap<>();
		params.put("id", 47L);

		String dsl = "{}";
		try {
			XContentBuilder builder = jsonBuilder().startObject().field("query").startObject().field("match")
					.startObject().field("id", "{{id}}").endObject().endObject().endObject();
			dsl = builder.string();
			System.out.println(dsl);
		} catch (IOException e) {
		}
		SearchRequest searchRequest = new SearchRequest("aigou").types("product");
		SearchResponse response = new SearchTemplateRequestBuilder(client).setScript(dsl)
				.setScriptType(ScriptType.INLINE).setScriptParams(params).setRequest(searchRequest).get().getResponse();

		for (SearchHit hit : response.getHits().getHits()) {
			System.out.println(hit.getSource());
		}*/

		/*
		 * ProductQuery query = new ProductQuery(); 
		 * query.setBrand(1L);
		 * query.setProductType(2L); 
		 * query.setKeyword("连衣裙");
		 * query.setPriceMax(200); 
		 * query.setPriceMin(100); query.setPage(2);
		 * query.setSort("s"); 
		 * query(query);
		 * 
		 * client.close();
		 */
	}
}
