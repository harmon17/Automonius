package org.automonius.Actions.Tools;

import org.automonius.Annotations.ActionMeta;

import java.util.Arrays;

public class WindowsTool {
    @ActionMeta(
            objectName = "Calculator",
            description = "Perform arithmetic on ten numbers with a chosen operation",
            inputs = {
                    "num1","num2","num3","num4","num5",
                    "num6","num7","num8","num9","num10","operation"
            }
    )
    public static double calculateTen(
            double num1, double num2, double num3, double num4, double num5,
            double num6, double num7, double num8, double num9, double num10,
            String operation
    ) {
        double[] nums = {num1,num2,num3,num4,num5,num6,num7,num8,num9,num10};

        switch (operation.toLowerCase()) {
            case "sum":
            case "add":
            case "+":
                return Arrays.stream(nums).sum();

            case "average":
                return Arrays.stream(nums).average().orElse(0);

            case "max":
                return Arrays.stream(nums).max().orElse(Double.NaN);

            case "min":
                return Arrays.stream(nums).min().orElse(Double.NaN);

            case "multiply":
            case "*":
                return Arrays.stream(nums).reduce(1, (a,b) -> a * b);

            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }
}
