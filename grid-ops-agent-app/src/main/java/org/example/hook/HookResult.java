package org.example.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HookResult {
    private boolean proceed;
    private String message;
    private Object data;

    public static HookResult proceed() {
        return new HookResult(true, null, null);
    }

    public static HookResult block(String reason) {
        return new HookResult(false, reason, null);
    }

    public static HookResult block(String reason, Object data) {
        return new HookResult(false, reason, data);
    }
}
