package queries;

import java.util.Random;

public enum SortOrder {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    public static SortOrder random() {
        return values()[new Random().nextInt(values().length)];
    }

    public static SortOrder fromString(String value) {
        for (SortOrder order : values()) {
            if (order.value.equals(value)) {
                return order;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}
