{{javaComment 'license-header.txt'~}}
package {{targetPackage}};

import java.util.Arrays;
import java.util.stream.Collectors;

public class {{targetFileClass}} {
    public static void main(String[] args) {
        String sender = (args.length == 0) ? "{{targetFileClass}}"
                    : Arrays.stream(args).collect(Collectors.joining(" and "));
        System.out.println("{{moduleName}}: Hello from " + sender + "!");
    }
}
