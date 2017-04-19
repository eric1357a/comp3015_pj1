package COMP3015_Project_1.Common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arguments {

    List<String> content;


    Arguments(String[] content) {
        this(Arrays.asList(content));
    }

    Arguments(List<String> content) {
        this.content = content;
    }


    static List<String> parse(String concatd) {
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(concatd);

        List<String> found = new LinkedList<>();
        while (regexMatcher.find()) {
            found.add(regexMatcher.group());
        }

        return found;
    }

    public String get(int index) {
        if (index > -1 && index < content.size())
            return this.content.get(index);
        return "";
    }

    public int length() {
        return this.content.size();
    }


}