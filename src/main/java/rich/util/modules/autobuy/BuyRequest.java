package rich.util.modules.autobuy;

public class BuyRequest {
    public int price;
    public String itemId;
    public String displayName;

    public BuyRequest(int price, String itemId, String displayName) {
        this.price = price;
        this.itemId = itemId;
        this.displayName = displayName;
    }
}