package org.btik.espidf.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineParser {
    // 匹配带引号或不带引号的参数
    private static final Pattern ARG_PATTERN = Pattern.compile(
            "\"([^\"]*)\"|" +    // 匹配双引号内的内容
                    "'([^']*)'|" +       // 匹配单引号内的内容
                    "(\\S+)"             // 匹配无空格的单词
    );

    public static String[] parseArgs(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return new String[0];
        }

        ArrayList<String> args = new ArrayList<>();
        Matcher matcher = ARG_PATTERN.matcher(commandLine);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                args.add(matcher.group(1)); // 双引号内内容
            } else if (matcher.group(2) != null) {
                args.add(matcher.group(2)); // 单引号内内容
            } else if (matcher.group(3) != null) {
                args.add(matcher.group(3)); // 无空格单词
            }
        }

        return args.toArray(new String[0]);
    }
}