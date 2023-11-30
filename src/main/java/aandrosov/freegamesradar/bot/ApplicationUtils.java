package aandrosov.freegamesradar.bot;

import java.util.Properties;

public class ApplicationUtils {

    public static void terminateWithMessage(String message) {
        System.out.println(message);
        System.exit(-1);
    }

    public static long getLongValueFromPropertiesOrTerminateIfError(Properties properties, String propertyName) {
        try {
            return Long.parseLong(properties.getProperty(propertyName));
        } catch (NumberFormatException exception) {
            terminateWithMessage("Error: property: \"" + propertyName + "\" must be a number!");
            return 0;
        }
    }

    public static boolean isPropertiesEmpty(Properties properties, String... names) {
        boolean isEmpty = false;

        for (String property : names) {
            String propertyValue = ((String) properties.getOrDefault(property, ""));

            if (propertyValue.isEmpty()) {
                isEmpty = true;
                properties.setProperty(property, "");
                System.out.println("Property \"" + property + "\" is empty!");
            }
        }

        return isEmpty;
    }
}
