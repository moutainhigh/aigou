package cn.itsource.aigou.core.query;

public class BaseQuery {
	public static final String ASC = "asc";
	// 每页返回行数
	private int rows = 10;
	// 当前页码
	private int page = 1;
	// 排序列名
	private String sort;
	// 排序顺序
	private String order = ASC;

	public int getRows() {
		return rows;
	}

	public void setRows(int pageSize) {
		this.rows = pageSize;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	/**
	 * 获取数据的起始位置
	 * 
	 * @return
	 */
	public int getStart() {
		return (page - 1) * rows;
	}
}
