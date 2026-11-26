import java.math.BigDecimal;

public class FarmTool extends Equipment {
    private final String type;

    public FarmTool(int id, int ownerId, String ownerName, String name,
                    BigDecimal rate, String location, String status, String type) {
        super(id, ownerId, ownerName, name, rate, location, status);
        this.type = type;
    }

    @Override public String getType() {
        return type.replace('_', ' ');
    }

    @Override public String getSpecs() {
        return "Agricultural tool";
    }
}
