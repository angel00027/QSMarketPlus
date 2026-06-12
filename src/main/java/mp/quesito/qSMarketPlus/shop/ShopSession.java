package mp.quesito.qSMarketPlus.shop;

public class ShopSession {

    private int categoryIndex;
    private int page;

    public ShopSession(int categoryIndex, int page) {
        this.categoryIndex = categoryIndex;
        this.page = page;
    }

    public int getCategoryIndex() {
        return categoryIndex;
    }

    public void setCategoryIndex(int categoryIndex) {
        this.categoryIndex = categoryIndex;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}